#!/usr/bin/env python3
import argparse
import hashlib
import json
import re
import unicodedata
import zipfile
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

SOURCE_PROVIDER = "HadeethEnc.com"
BUILD_REVIEW_DATE = "2026-08-31"

NS = {
    "m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}

# Authenticity and editorial suitability are distinct gates.
# These IDs remain authentic in HadeethEnc but were manually rejected for the
# daily-reminder surface because the short title is overly contextual, harsh,
# intimate/legal, fragmentary, or likely to be misunderstood without commentary.
EDITORIAL_EXCLUDED_IDS = {
    "2996", "3005", "3044", "3070", "3120", "3140", "3306", "3415",
    "3533", "3563", "3667", "3915", "4179", "4234", "4293", "4830",
    "5367", "5513", "5735", "5738", "58078", "58098", "58102", "58172",
    "6020", "6045", "6083", "6094", "6212", "6376", "6377", "6404",
    "65009", "6612", "66513", "8291", "8308", "8915", "10567", "10895",
    "10968", "10992", "11168", "11180", "11218", "11269",
}

TAG_RULES = {
    "bonnes mœurs": ["meilleurs comportements", "meilleur comportement", "bon comportement", "bonne parole", "sourire", "n'insultez", "n’insultez", "pudeur", "modestie", "caractère", "caractere"],
    "comportement": ["comportement", "caractère", "caractere", "bonne parole", "sourire", "pudeur", "insulte"],
    "douceur": ["douceur", "doux", "bienveillance", "miséricorde", "misericorde", "clémence", "clemence"],
    "patience": ["patience", "patient", "patiente", "éprouve", "eprouve", "épreuve", "epreuve"],
    "maîtrise de soi": ["colère", "colere", "fort n'est pas", "fort n’est pas", "maîtrise", "maitrise", "pardonne", "pardon"],
    "sincérité": ["sincérité", "sincerite", "intention", "intentions", "ostentation"],
    "intention": ["intention", "intentions"],
    "gratitude": ["remercie", "remercier", "remerciement", "reconnaissance", "bonne nouvelle", "réjouissant", "rejouissant"],
    "générosité": ["aumône", "aumone", "dépense", "depense", "dépenses", "depenses", "donne", "donner", "charité", "charite", "générosité", "generosite", "généreux", "genereux"],
    "pardon": ["pardonne", "pardonner", "pardon", "indulgence"],
    "mérite du Coran": ["meilleur d'entre vous est celui qui a appris le coran", "meilleur d’entre vous est celui qui a appris le coran", "mérite du coran", "merite du coran"],
    "lecture du Coran": ["récite le coran", "recite le coran", "récitait le coran", "recitait le coran", "réciter le coran", "reciter le coran", "lecture du coran", "récitation du coran", "recitation du coran", "lisez le coran"],
    "apprentissage du Coran": ["appris le coran", "apprendre le coran", "enseigne le coran", "enseigné le coran", "enseigner le coran"],
    "mise en pratique du Coran": ["mettaient en application", "mettre le coran en application", "met le coran en application", "comportement du prophète", "comportement du prophete"],
    "coran": ["coran", "sourate", "verset"],
    "famille": ["mère", "mere", "père", "pere", "parents", "épouse", "epouse", "époux", "epoux", "enfant", "enfants", "parenté", "parente"],
    "parents": ["mère", "mere", "père", "pere", "parents"],
    "conjoint": ["épouse", "epouse", "époux", "epoux", "mariage", "conjoint"],
    "enfants": ["enfant", "enfants"],
    "liens de parenté": ["parenté", "parente", "liens de parenté", "liens de parente"],
    "voisinage": ["voisin", "voisins"],
    "respect du voisin": ["voisin", "voisins"],
    "entraide": ["aide son frère", "aide son frere", "vient en aide", "soulage", "besoin de son frère", "besoin de son frere", "entraide"],
    "vie en communauté": ["frère musulman", "frere musulman", "musulman est le frère", "musulman est le frere", "réconcilie", "reconcilie", "salue son frère", "salue son frere"],
    "propreté": ["purification", "ablution", "ablutions", "siwâk", "siwak", "bouche", "dents", "impureté", "impurete", "laver", "lavage"],
    "hygiène": ["siwâk", "siwak", "dents", "laver", "lavage", "purification pour la bouche"],
    "pureté": ["purification", "état de pureté", "etat de purete"],
    "ablutions": ["ablution", "ablutions"],
    "soin du corps": ["siwâk", "siwak", "dents", "cheveux", "ongles", "laver le corps", "lavage du corps"],
    "propreté des vêtements et des lieux": ["vêtement", "vetement", "vêtements", "vetements", "mosquée", "mosquee", "route", "chemin", "impureté", "impurete"],
    "hygiène bucco-dentaire": ["siwâk", "siwak", "dents", "purification pour la bouche"],
    "respect des espaces communs": ["route", "chemin", "mosquée", "mosquee", "épine", "epine", "nuisance"],
    "gestion du temps": ["temps libre", "deux bienfaits", "matin", "soir", "chaque jour", "jeudi", "temps"],
    "discipline personnelle": ["chaque jour", "habitude", "avait l'habitude", "avait l’habitude", "assidu", "régulier", "regulier", "constance"],
    "bonnes habitudes": ["chaque jour", "habitude", "avait l'habitude", "avait l’habitude", "assidu", "régulier", "regulier", "constance"],
}

PRIMARY_THEME_PRIORITY = [
    "voisinage",
    "famille",
    "propreté",
    "douceur",
    "coran",
    "patience",
    "pardon",
    "maîtrise de soi",
    "sincérité",
    "gratitude",
    "générosité",
    "entraide",
    "vie en communauté",
    "gestion du temps",
    "discipline personnelle",
    "bonnes mœurs",
]

def norm(value):
    value = unicodedata.normalize("NFKD", value or "")
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    return re.sub(r"\s+", " ", value.lower()).strip()

def contains_phrase(text, phrase):
    normalized_text = norm(text)
    normalized_phrase = norm(phrase)
    return re.search(
        r"(?<!\w)" + re.escape(normalized_phrase) + r"(?!\w)",
        normalized_text
    ) is not None

def col_index(ref):
    letters = "".join(ch for ch in ref if ch.isalpha())
    value = 0
    for ch in letters:
        value = value * 26 + (ord(ch.upper()) - 64)
    return value - 1

def workbook_rows(path):
    with zipfile.ZipFile(path) as z:
        shared = []
        if "xl/sharedStrings.xml" in z.namelist():
            root = ET.fromstring(z.read("xl/sharedStrings.xml"))
            for si in root.findall("m:si", NS):
                shared.append("".join(t.text or "" for t in si.findall(".//m:t", NS)))

        workbook = ET.fromstring(z.read("xl/workbook.xml"))
        rels = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
        relmap = {rel.attrib["Id"]: rel.attrib["Target"] for rel in rels}
        first = workbook.find("m:sheets/m:sheet", NS)
        if first is None:
            raise RuntimeError(f"No worksheet in {path}")
        rid = first.attrib["{%s}id" % NS["r"]]
        target = relmap[rid]
        sheet_path = target if target.startswith("xl/") else "xl/" + target.lstrip("/")
        root = ET.fromstring(z.read(sheet_path))

        rows = []
        for row in root.findall(".//m:sheetData/m:row", NS):
            values = {}
            for cell in row.findall("m:c", NS):
                idx = col_index(cell.attrib.get("r", "A1"))
                typ = cell.attrib.get("t")
                inline = cell.find("m:is/m:t", NS)
                value_node = cell.find("m:v", NS)
                value = ""
                if inline is not None:
                    value = inline.text or ""
                elif value_node is not None:
                    value = value_node.text or ""
                    if typ == "s" and value.isdigit():
                        value = shared[int(value)]
                values[idx] = value
            if values:
                rows.append([values.get(i, "") for i in range(max(values) + 1)])
        return rows

def load_table(path):
    rows = workbook_rows(path)
    if len(rows) < 3:
        raise RuntimeError(f"Unexpected workbook structure: {path}")

    metadata = rows[0][0] if rows[0] else ""
    headers = rows[1]
    table = []
    for raw in rows[2:]:
        padded = raw + [""] * (len(headers) - len(raw))
        table.append({headers[i]: padded[i] for i in range(len(headers))})
    return metadata, table

def parse_version(metadata, language):
    match = re.search(r"\(v([0-9.]+)\)", metadata)
    if not match:
        raise RuntimeError(f"Missing version in {language} workbook metadata")
    return f"{language}-v{match.group(1)}"

def parse_last_update(metadata):
    match = re.search(r"Last update:\s*([0-9-]+)", metadata)
    return match.group(1) if match else BUILD_REVIEW_DATE

def is_accepted_grade(row):
    grade_ar = row.get("grade_ar", "").strip()
    grade_fr = norm(row.get("grade", ""))
    if "ضعيف" in grade_ar or "faible" in grade_fr or "weak" in grade_fr:
        return False
    return "صحيح" in grade_ar or "حسن" in grade_ar or "authentique" in grade_fr or "[bon" in grade_fr

def hadith_grade(row):
    grade_ar = row.get("grade_ar", "").strip()
    if "صحيح" in grade_ar:
        return "Sahih"
    if "حسن" in grade_ar:
        return "Hasan"
    return row.get("grade", "").strip()

def collection_from_takhrij(takhrij_fr, takhrij_ar):
    raw = norm(takhrij_fr + " " + takhrij_ar)
    collections = []
    mapping = [
        (["bukh", "boukh", "البخاري", "متفق عليه"], "Sahih al-Bukhari"),
        (["muslim", "مسلم", "متفق عليه"], "Sahih Muslim"),
        (["tirmidh", "الترمذي"], "Jami’ at-Tirmidhi"),
        (["nasa", "النسائي"], "Sunan an-Nasa’i"),
        (["abu daw", "abou daw", "أبو داود"], "Sunan Abi Dawud"),
        (["ibn maj", "ابن ماجه"], "Sunan Ibn Majah"),
        (["ahmad", "أحمد"], "Musnad Ahmad"),
        (["darimi", "الدارمي"], "Sunan ad-Darimi"),
        (["malik", "مالك"], "Al-Muwatta’"),
    ]
    for needles, label in mapping:
        if any(norm(needle) in raw for needle in needles):
            collections.append(label)
    return " / ".join(dict.fromkeys(collections)) if collections else "Recueil indiqué par HadeethEnc"

EXCLUDED_TONE_KEYWORDS = [
    "enfer",
    "châtiment",
    "chatiment",
    "maudit",
    "malédiction",
    "malediction",
    "lapidation",
    "fornication",
    "adultère",
    "adultere",
    "coupez",
    "combat",
    "tuez",
    "mise à mort",
    "mise a mort",
    "flagella",
    "alcool",
    "enivre",
    "hypocrite",
    "polythéisme",
    "polytheisme",
    "martyr",
    "tombe",
    "tombes",
    "antéchrist",
    "antechrist",
    "sang",
    "peste",
    "guerre",
    "détruits",
    "detruits",
    "morts",
    "malheur",
    "tranché",
    "tranche",
    "n'entrera pas au paradis",
    "n’entrera pas au paradis",
    "enfants d'israël",
    "enfants d’israël",
    "péri",
    "périrent",
    "perirent",
    "chaque époque à venir est pire",
    "chaque epoque a venir est pire",
]

def has_unsuitable_daily_tone(row):
    title = row.get("title", "")
    return any(contains_phrase(title, word) for word in EXCLUDED_TONE_KEYWORDS)

def derive_tags(row):
    # The daily theme must be visible in the short reminder itself.
    # Explanations/benefits are not used for classification, preventing contextual false positives.
    title = row.get("title", "")
    tags = set()
    for tag, needles in TAG_RULES.items():
        if any(contains_phrase(title, needle) for needle in needles):
            tags.add(tag)
    return tags

def primary_theme(tags):
    for theme in PRIMARY_THEME_PRIORITY:
        if theme in tags:
            return theme
    return None

def source_priority(item):
    book = item["book"]
    if "Sahih al-Bukhari" in book or "Sahih Muslim" in book:
        return 0
    if item["authenticity"] == "Sahih":
        return 1
    return 2

def build_candidates(fr_rows, ar_by_id, fr_version, fr_last_update):
    candidates = []
    seen = set()

    for row in fr_rows:
        hid = row.get("id", "").strip()
        if not hid or hid in seen or hid in EDITORIAL_EXCLUDED_IDS:
            continue
        seen.add(hid)

        arabic_row = ar_by_id.get(hid)
        if arabic_row is None:
            continue
        if not is_accepted_grade(row):
            continue
        if has_unsuitable_daily_tone(row):
            continue

        title_ar = row.get("title_ar", "").strip()
        title_fr = row.get("title", "").strip()
        if not title_ar or not title_fr:
            continue

        # Cross-check official French embedded Arabic title against Arabic workbook.
        if norm(title_ar) != norm(arabic_row.get("title", "")):
            continue

        grade_ar = row.get("grade_ar", "").strip()
        if grade_ar and grade_ar != arabic_row.get("grade", "").strip():
            continue

        # Keep reminders genuinely short while preserving HadeethEnc wording verbatim.
        if len(title_fr) > 520 or len(title_ar) > 520:
            continue

        tags = derive_tags(row)
        theme = primary_theme(tags)
        if theme is None:
            continue

        takhrij_fr = row.get("takhrij", "").strip()
        takhrij_ar = row.get("takhrij_ar", "").strip()
        if not takhrij_fr or not takhrij_ar:
            continue

        candidates.append({
            "id": "hadeethenc_" + hid,
            "type": "HADITH",
            "theme": theme,
            "arabicText": title_ar,
            "frenchText": title_fr,
            "author": "Prophète Muhammad ﷺ",
            "book": collection_from_takhrij(takhrij_fr, takhrij_ar),
            "reference": "HadeethEnc #" + hid + " • " + takhrij_fr,
            "authenticity": hadith_grade(row),
            "tags": sorted(tags | {"hadith authentifié"}),
            "sourceProvider": SOURCE_PROVIDER,
            "sourceId": hid,
            "sourceVersion": fr_version,
            "sourceFetchedAt": fr_last_update,
            "reviewStatus": "VERIFIED_OFFICIAL_SOURCE",
            "translationStatus": "SOURCE_TRANSLATION_UNMODIFIED",
            "sourceUrl": row.get("link", "").strip(),
        })

    return candidates

def select_items(candidates, target):
    ordered = sorted(
        candidates,
        key=lambda item: (
            source_priority(item),
            len(item["frenchText"]),
            int(item["sourceId"]) if item["sourceId"].isdigit() else 10**9,
        )
    )

    selected = []
    selected_ids = set()

    # Coverage is a hard requirement, but we never pad a theme with weaker content.
    # Pick the highest-priority verified item that carries every requested tag.
    for required_tag in TAG_RULES:
        match = next(
            (item for item in ordered if required_tag in item["tags"]),
            None
        )
        if match is None:
            raise RuntimeError(
                f"Requested thematic coverage has no verified short item: {required_tag!r}"
            )
        if match["id"] not in selected_ids:
            selected.append(match)
            selected_ids.add(match["id"])

    # Then fill in round-robin by weekly theme, preserving source priority
    # inside each theme. This prevents one large theme from dominating the library.
    by_theme = {
        theme: [item for item in ordered if item["theme"] == theme]
        for theme in PRIMARY_THEME_PRIORITY
    }
    cursors = {theme: 0 for theme in PRIMARY_THEME_PRIORITY}
    made_progress = True
    while len(selected) < target and made_progress:
        made_progress = False
        for theme in PRIMARY_THEME_PRIORITY:
            items = by_theme[theme]
            while cursors[theme] < len(items):
                item = items[cursors[theme]]
                cursors[theme] += 1
                if item["id"] in selected_ids:
                    continue
                selected.append(item)
                selected_ids.add(item["id"])
                made_progress = True
                break
            if len(selected) >= target:
                break

    # If a small theme is exhausted, fill the remainder from all verified items.
    for item in ordered:
        if len(selected) >= target:
            break
        if item["id"] not in selected_ids:
            selected.append(item)
            selected_ids.add(item["id"])

    if len(selected) != target:
        raise RuntimeError(
            f"Expected exactly {target} verified reminders; selected {len(selected)} "
            f"from {len(candidates)} eligible entries."
        )
    return selected

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--fr-xlsx", required=True)
    parser.add_argument("--ar-xlsx", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--target", type=int, default=144)
    args = parser.parse_args()

    fr_meta, fr_rows = load_table(args.fr_xlsx)
    ar_meta, ar_rows = load_table(args.ar_xlsx)
    fr_version = parse_version(fr_meta, "fr")
    ar_version = parse_version(ar_meta, "ar")
    fr_last_update = parse_last_update(fr_meta)

    if fr_version != "fr-v1.17.0":
        raise SystemExit(
            f"French source version changed from reviewed fr-v1.17.0 to {fr_version}; "
            "manual review required before updating the app."
        )

    ar_by_id = {row.get("id", "").strip(): row for row in ar_rows if row.get("id", "").strip()}
    candidates = build_candidates(fr_rows, ar_by_id, fr_version, fr_last_update)
    items = select_items(candidates, args.target)

    requested_tags = set(TAG_RULES)
    covered_tags = {tag for item in items for tag in item["tags"]}
    missing_tags = sorted(requested_tags - covered_tags)
    if missing_tags:
        raise SystemExit(
            "Requested thematic coverage missing after selection: " + ", ".join(missing_tags)
        )

    theme_counts = {
        theme: sum(1 for item in items if item["theme"] == theme)
        for theme in PRIMARY_THEME_PRIORITY
    }
    book_counts = defaultdict(int)
    grade_counts = defaultdict(int)
    for item in items:
        book_counts[item["book"]] += 1
        grade_counts[item["authenticity"]] += 1

    payload = {
        "schemaVersion": 2,
        "sourceProvider": SOURCE_PROVIDER,
        "frenchSourceVersion": fr_version,
        "arabicSourceVersion": ar_version,
        "sourceLastUpdate": fr_last_update,
        "reviewedAt": BUILD_REVIEW_DATE,
        "count": len(items),
        "themeCounts": theme_counts,
        "gradeCounts": dict(sorted(grade_counts.items())),
        "items": items,
    }

    canonical = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    digest = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(canonical, encoding="utf-8")
    out.with_suffix(out.suffix + ".sha256").write_text(digest + "\n", encoding="utf-8")

    print(f"French source: {fr_version} updated {fr_last_update}")
    print(f"Arabic cross-check source: {ar_version}")
    print(f"Eligible verified short reminders: {len(candidates)}")
    print(f"Selected: {len(items)}")
    print("Theme counts:", json.dumps(theme_counts, ensure_ascii=False, sort_keys=True))
    print("Grade counts:", json.dumps(dict(grade_counts), ensure_ascii=False, sort_keys=True))
    print("Top source groups:", json.dumps(
        dict(sorted(book_counts.items(), key=lambda p: (-p[1], p[0]))[:12]),
        ensure_ascii=False
    ))
    print(f"sha256={digest}")

if __name__ == "__main__":
    main()
