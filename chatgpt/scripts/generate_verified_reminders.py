#!/usr/bin/env python3
import io
import json
import os
import unicodedata
import re
import urllib.request
import zipfile
import xml.etree.ElementTree as ET

OUT = "app/src/main/assets/reminders/hadeethenc_snapshot.json"
FR_URL = "https://hadeethenc.com/browse/download/fr"
FR_VERSION = "v1.17.0"
UA = {"User-Agent": "QuranSafeguard-verification/0.8"}

BOOK_PATTERNS = [
    ("Sahih al-Bukhari", ("bukhari", "bukhârî")),
    ("Sahih Muslim", ("muslim",)),
    ("Sunan Abi Dawud", ("abu dawud", "abi dawud", "abû dâwud", "abou dawoud")),
    ("Jami’ at-Tirmidhi", ("tirmidhi", "tirmidhî")),
    ("Sunan an-Nasa’i", ("nasa", "nassai", "nasâ")),
    ("Sunan Ibn Majah", ("ibn majah", "ibn mâjah")),
    ("Musnad Ahmad", ("ahmad", "aḥmad")),
]

THEMES = [
    ("coran", (
        "coran", "qur'an", "sourate", "verset", "reciter", "recitation",
        "apprendre le coran", "enseigner le coran", "memoriser le coran"
    )),
    ("famille", (
        "ses parents", "vos parents", "les parents", "lien de parente",
        "liens de parente", "entretenir les liens", "envers sa famille",
        "envers votre famille", "ses enfants", "vos enfants", "orphelin"
    )),
    ("voisinage", (
        "son voisin", "votre voisin", "le voisin", "les voisins",
        "chemin des gens", "sur le chemin", "nuisance", "passant"
    )),
    ("douceur", (
        "douceur", "doux", "misericorde", "clemence", "indulgence",
        "pardonne", "pardon", "colere", "bon caractere", "meilleur comportement"
    )),
    ("patience", (
        "patience", "patient", "epreuve", "endure", "endurance",
        "constance", "affliction", "ne te mets pas en colere",
        "patience est une lumiere", "don meilleur que la patience",
        "affaire du croyant"
    )),
    ("sincerite", (
        "intention", "sincerite", "sincere", "ostentation",
        "pour allah", "visage d'allah"
    )),
    ("gratitude", (
        "remercie", "remercier", "reconnaissant", "gratitude",
        "bienfait d'allah", "bienfaits d'allah", "ne remercie pas les gens",
        "regardez ceux qui sont en dessous"
    )),
    ("generosite", (
        "aumone", "charite", "genereux", "depense pour", "nourrir",
        "donner a manger", "cadeau", "faites l'aumone",
        "toute bonne action est une aumone", "bonne parole est une aumone"
    )),
    ("proprete", (
        "siwak", "dents", "purification est", "ablutions",
        "se purifier", "proprete", "propre"
    )),
    ("entraide", (
        "vient en aide", "aide son frere", "soulage", "besoin de son frere",
        "visite le malade", "visiter le malade", "reconcilier",
        "facilite a", "dissipe une", "retire du chemin",
        "soulage un croyant", "allah vient en aide", "repandez le salut",
        "droits du musulman", "aime pour son frere"
    )),
    ("discipline", (
        "actions les plus aimees", "action la plus aimee", "regularite",
        "assiduite", "constamment", "ne faiblis pas", "ce qui t'est utile",
        "profite de", "cinq avant cinq", "croyant fort", "sois assidu",
        "oeuvre reguliere", "action reguliere"
    )),
    ("bonnes_moeurs", (
        "verite", "mensonge", "langue", "insulte", "injure", "pudeur",
        "modestie", "humilite", "orgueil", "arrogance", "medisance",
        "calomnie", "soupcon", "trahison", "honnetete", "justice",
        "injustice", "oppression", "sourire", "parle en bien",
        "qu'il se taise", "bon comportement", "bonnes manieres",
        "bon caractere", "meilleur d'entre vous", "meilleurs d'entre vous",
        "bonne parole", "a l'abri de sa langue", "a l'abri de sa main",
        "ne vous enviez pas", "ne vous detestez pas", "ne vous tournez pas le dos"
    )),
    ("science", (
        "recherche de la science", "recherche d'une science", "cherche la science",
        "comprehension de la religion", "comprenne la religion",
        "apprend une science", "enseigne une science", "chemin vers le paradis",
        "savants sont les heritiers", "transmettez de moi"
    )),
    ("dhikr", (
        "deux paroles legeres", "subhanallah", "gloire a allah",
        "louange a allah", "rappel d'allah", "se souvient d'allah",
        "invoque allah", "demande pardon", "cent fois"
    )),
    ("priere", (
        "cinq prieres", "les cinq prieres", "priere est une lumiere",
        "priere en groupe", "priere en congregation", "priere efface",
        "prosternes-toi davantage", "plus proche de son seigneur"
    )),
]

EXCLUDED_CONTEXT_MARKERS = (
    "guerre", "combat", "combatt", "tuer", "tue ", "tua ", "mise a mort",
    "epée", "epee", "armee", "expedition", "butin", "captif", "prisonnier",
    "esclave", "affranchi", "fornication", "adultere", "lapid", "fouet",
    "flog", "peine legale", "chatiment corporel", "menstrue", "regles",
    "rapport charnel", "rapports charnels", "organe genital", "sperme",
    "coit", "janaba", "impurete majeure", "divorce", "talaq", "heritage",
    "succession", "dette", "usure", "vente", "transaction", "juge",
    "temoignage juridique", "testament", "sacrifice animal", "egorger"
)

def marker_present(text, marker):
    normalized_marker = norm(marker)
    pattern = r"(?<![a-z0-9])" + re.escape(normalized_marker) + r"(?![a-z0-9])"
    return re.search(pattern, text) is not None

def thematic_fit(record):
    # HadeethEnc's title is the editorial summary of the hadith's central point.
    # Requiring the theme there avoids false matches caused by narrator names,
    # family relations or incidental details in the full report.
    title = norm(record.get("title", ""))
    context = norm(
        " ".join(
            record.get(key, "") for key in
            ("title", "hadith_text", "explanation", "benefits")
        )
    )

    # Sensitive/context-heavy material is excluded aggressively, including
    # inflected forms and technical/legal contexts.
    hard_exclusions = EXCLUDED_CONTEXT_MARKERS + (
        "sacrifi", "egorg", "chatie", "chatiment", "malediction",
        "grand peche", "grands peches", "tetee", "allaitement",
        "epousa", "epousee", "mariage", "sacralisation",
        "ablution majeure", "toilette intime", "position des mains",
        "lever les mains", "sur sa monture", "priere du couchant",
        "sermon du vendredi", "revelation descend", "alcool", "vin",
        "noms d'allah, cent moins un"
    )
    if any(norm(marker) in context for marker in hard_exclusions):
        return None

    best_theme = None
    best_score = 0
    for theme, markers in THEMES:
        title_hits = sum(1 for marker in markers if marker_present(title, marker))
        if title_hits <= 0:
            continue
        contextual_hits = sum(1 for marker in markers if marker_present(context, marker))
        score = title_hits * 20 + contextual_hits
        if score > best_score:
            best_theme = theme
            best_score = score

    if best_theme is None:
        return None
    return best_theme, best_score

def theme_tags(theme):
    mapping = {
        "coran": ["mérite du Coran", "lecture du Coran", "apprentissage du Coran"],
        "famille": ["famille", "parents", "enfants", "liens de parenté"],
        "voisinage": ["voisinage", "respect du voisin", "espaces communs"],
        "douceur": ["douceur", "pardon", "maîtrise de soi", "bonnes mœurs"],
        "patience": ["patience", "maîtrise de soi", "bonnes habitudes"],
        "sincerite": ["sincérité", "intention", "bonnes habitudes"],
        "gratitude": ["gratitude", "bienfaits"],
        "generosite": ["générosité", "entraide", "vie en communauté"],
        "proprete": ["propreté", "hygiène", "pureté", "ablutions"],
        "entraide": ["entraide", "vie en communauté", "bonnes mœurs"],
        "discipline": ["discipline personnelle", "gestion du temps", "bonnes habitudes"],
        "bonnes_moeurs": ["bonnes mœurs", "comportement"],
        "science": ["science utile", "apprentissage", "transmission"],
        "dhikr": ["rappel d’Allah", "bonnes habitudes"],
        "priere": ["prière", "discipline personnelle"],
    }
    return mapping.get(theme, ["bonnes mœurs", "comportement"])

def norm(value):
    value = unicodedata.normalize("NFKD", str(value or ""))
    return "".join(ch for ch in value if not unicodedata.combining(ch)).lower().strip()

def fetch(url):
    request = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(request, timeout=90) as response:
        data = response.read()
    if len(data) < 1000:
        raise RuntimeError("Unexpectedly small HadeethEnc export")
    return data

def column_letter_to_index(reference):
    letters = "".join(ch for ch in reference if ch.isalpha())
    result = 0
    for ch in letters.upper():
        result = result * 26 + (ord(ch) - ord("A") + 1)
    return result - 1

def read_xlsx(data):
    zf = zipfile.ZipFile(io.BytesIO(data))
    shared = []
    ns = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    if "xl/sharedStrings.xml" in zf.namelist():
        root = ET.fromstring(zf.read("xl/sharedStrings.xml"))
        for si in root.findall("a:si", ns):
            shared.append("".join(t.text or "" for t in si.findall(".//a:t", ns)))

    rows = []
    sheet_names = sorted(
        name for name in zf.namelist()
        if name.startswith("xl/worksheets/sheet") and name.endswith(".xml")
    )
    for sheet_name in sheet_names:
        root = ET.fromstring(zf.read(sheet_name))
        for row in root.findall(".//a:row", ns):
            values = {}
            for cell in row.findall("a:c", ns):
                idx = column_letter_to_index(cell.attrib.get("r", ""))
                cell_type = cell.attrib.get("t")
                if cell_type == "inlineStr":
                    value = "".join(t.text or "" for t in cell.findall(".//a:t", ns))
                else:
                    node = cell.find("a:v", ns)
                    raw = node.text if node is not None and node.text is not None else ""
                    if cell_type == "s" and raw.isdigit():
                        pos = int(raw)
                        value = shared[pos] if 0 <= pos < len(shared) else ""
                    else:
                        value = raw
                values[idx] = value
            if values:
                rows.append([values.get(i, "") for i in range(max(values) + 1)])
    return rows

def records_from_rows(rows):
    header_index = None
    headers = []
    for index, row in enumerate(rows[:30]):
        candidate = [str(v).strip() for v in row]
        names = set(candidate)
        if "id" in names and "hadith_text" in names and "grade" in names:
            header_index = index
            headers = candidate
            break

    if header_index is None:
        raise SystemExit("Unable to locate HadeethEnc header row")

    records = []
    for row in rows[header_index + 1:]:
        padded = row + [""] * max(0, len(headers) - len(row))
        record = {headers[i]: str(padded[i]).strip() for i in range(len(headers))}
        if record.get("id"):
            records.append(record)
    return headers, records

def extract_books(takhrij):
    normalized = norm(takhrij)
    books = []
    for display, variants in BOOK_PATTERNS:
        if any(norm(variant) in normalized for variant in variants):
            books.append(display)
    return books

def verified_grade(grade):
    normalized = norm(grade)
    if any(word in normalized for word in ("daif", "faible", "weak", "mawdu", "fabrique")):
        return None
    if "authentique" in normalized or "sahih" in normalized:
        return grade or "Sahih / authentique"
    if "hasan" in normalized or normalized == "bon":
        return grade or "Hasan"
    return None

headers, rows = records_from_rows(read_xlsx(fetch(FR_URL)))
required = {
    "id", "title_ar", "title", "hadith_text_ar", "hadith_text",
    "grade", "takhrij", "link"
}
missing = required - set(headers)
if missing:
    raise SystemExit("Missing HadeethEnc columns: " + ", ".join(sorted(missing)))

candidates = []
for row in rows:
    hid = row["id"].strip()
    arabic = row["hadith_text_ar"].strip() or row["title_ar"].strip()
    french = row["hadith_text"].strip() or row["title"].strip()
    grade = verified_grade(row["grade"])
    takhrij = row["takhrij"].strip()
    link = row["link"].strip()
    books = extract_books(takhrij)

    if not hid or not arabic or not french or grade is None:
        continue
    if not books or not takhrij or not link:
        continue

    # Short daily reminders only; exact HadeethEnc wording is never rewritten.
    if len(french) > 420 or len(arabic) > 750:
        continue

    fit = thematic_fit(row)
    if fit is None:
        continue
    theme, theme_score = fit

    if not any(
        book in ("Sahih al-Bukhari", "Sahih Muslim") for book in books
    ):
        continue

    candidates.append((
        -theme_score,
        {
            "id": "he_" + hid,
            "type": "HADITH",
            "theme": theme,
            "arabicText": arabic,
            "frenchText": french,
            "author": "Prophète Muhammad ﷺ",
            "book": " / ".join(books),
            "reference": (
                "HadeethEnc.com Français " + FR_VERSION +
                " • " + takhrij + " • " + link
            ),
            "authenticity": grade,
            "tags": theme_tags(theme),
            "sourceId": hid,
            "translationSource": "HadeethEnc.com Français " + FR_VERSION,
            "sourceSnapshotDate": "2026-08-31",
            "displayEligible": True
        }
    ))

candidates.sort(key=lambda item: (item[0], item[1]["id"]))

by_theme = {}
for candidate in candidates:
    record = candidate[1]
    by_theme.setdefault(record["theme"], []).append(candidate)

# Keep every candidate that passes authenticity + topic rules, but cap
# very broad themes so the daily library remains balanced.
selected = []
selected_ids = set()
per_theme_cap = 16
for theme, _ in THEMES:
    for candidate in by_theme.get(theme, [])[:per_theme_cap]:
        record = candidate[1]
        if record["id"] in selected_ids:
            continue
        selected_ids.add(record["id"])
        selected.append(record)

unique = selected
if not unique:
    raise SystemExit("No thematically suitable Bukhari/Muslim reminders found")

counts = {}
for item in unique:
    counts[item["theme"]] = counts.get(item["theme"], 0) + 1


os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as handle:
    json.dump(unique, handle, ensure_ascii=False, indent=2)

print(
    "generated", len(unique), OUT,
    "from HadeethEnc Français", FR_VERSION,
    "Bukhari/Muslim only; theme counts:", counts
)
