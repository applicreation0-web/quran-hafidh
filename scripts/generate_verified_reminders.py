#!/usr/bin/env python3
import io
import json
import os
import re
import unicodedata
import urllib.request
import zipfile
import xml.etree.ElementTree as ET

TARGET = 127
OUT = "app/src/main/assets/reminders/hadeethenc_snapshot.json"
FR_URL = "https://hadeethenc.com/browse/download/fr"
AR_URL = "https://hadeethenc.com/browse/download/ar"
FR_VERSION = "v1.17.0"
UA = {"User-Agent": "QuranSafeguard-verification/0.8"}

BOOK_PATTERNS = [
    ("Sahih al-Bukhari", ("bukhari", "bukhârî", "bukhari")),
    ("Sahih Muslim", ("muslim",)),
    ("Sunan Abi Dawud", ("abu dawud", "abi dawud", "abû dâwud", "abou dawoud")),
    ("Jami’ at-Tirmidhi", ("tirmidhi", "tirmidhî")),
    ("Sunan an-Nasa’i", ("nasa", "nasâ")),
    ("Sunan Ibn Majah", ("ibn majah", "ibn mâjah")),
    ("Musnad Ahmad", ("ahmad", "aḥmad")),
]

def norm(value):
    value = unicodedata.normalize("NFKD", str(value or ""))
    return "".join(ch for ch in value if not unicodedata.combining(ch)).lower().strip()

def fetch(url):
    request = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(request, timeout=90) as response:
        data = response.read()
    if len(data) < 1000:
        raise RuntimeError("Unexpectedly small HadeethEnc export from %s" % url)
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
    if "xl/sharedStrings.xml" in zf.namelist():
        root = ET.fromstring(zf.read("xl/sharedStrings.xml"))
        ns = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
        for si in root.findall("a:si", ns):
            shared.append("".join(t.text or "" for t in si.findall(".//a:t", ns)))

    sheet_names = sorted(
        name for name in zf.namelist()
        if name.startswith("xl/worksheets/sheet") and name.endswith(".xml")
    )
    rows = []
    ns = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    for sheet_name in sheet_names:
        root = ET.fromstring(zf.read(sheet_name))
        for row in root.findall(".//a:row", ns):
            values = {}
            for cell in row.findall("a:c", ns):
                ref = cell.attrib.get("r", "")
                idx = column_letter_to_index(ref)
                cell_type = cell.attrib.get("t")
                if cell_type == "inlineStr":
                    value = "".join(t.text or "" for t in cell.findall(".//a:t", ns))
                else:
                    v = cell.find("a:v", ns)
                    raw = v.text if v is not None and v.text is not None else ""
                    if cell_type == "s" and raw.isdigit():
                        pos = int(raw)
                        value = shared[pos] if 0 <= pos < len(shared) else ""
                    else:
                        value = raw
                values[idx] = value
            if values:
                max_idx = max(values)
                rows.append([values.get(i, "") for i in range(max_idx + 1)])
    return rows

def as_records(rows):
    if not rows:
        return [], []
    header_index = next(
        (i for i, row in enumerate(rows[:20]) if sum(bool(str(v).strip()) for v in row) >= 3),
        0
    )
    headers = [str(v).strip() for v in rows[header_index]]
    records = []
    for row in rows[header_index + 1:]:
        if not any(str(v).strip() for v in row):
            continue
        padded = row + [""] * max(0, len(headers) - len(row))
        records.append({headers[i]: str(padded[i]).strip() for i in range(len(headers))})
    return headers, records

def find_key(headers, candidates, exclude=()):
    normalized = {header: norm(header) for header in headers}
    for candidate in candidates:
        c = norm(candidate)
        for header, n in normalized.items():
            if c == n and not any(e in n for e in exclude):
                return header
    for candidate in candidates:
        c = norm(candidate)
        for header, n in normalized.items():
            if c in n and not any(e in n for e in exclude):
                return header
    return None

def contains_arabic(text):
    return any("\u0600" <= ch <= "\u06ff" for ch in text)

def choose_text(record, headers, language):
    candidates = []
    preferred = [
        "hadeeth", "hadith", "texte du hadith", "texte", "translation",
        "traduction", "title", "titre"
    ]
    for header in headers:
        n = norm(header)
        if any(token in n for token in preferred) and not any(
            blocked in n for blocked in ("explanation", "explication", "hint", "benefit")
        ):
            value = record.get(header, "").strip()
            if value:
                candidates.append((0, len(value), value))

    for value in record.values():
        value = value.strip()
        if not value:
            continue
        is_ar = contains_arabic(value)
        if language == "ar" and is_ar:
            candidates.append((1, len(value), value))
        elif language == "fr" and not is_ar and 20 <= len(value) <= 800:
            candidates.append((1, len(value), value))

    if not candidates:
        return ""
    candidates.sort(key=lambda item: (item[0], item[1]))
    return candidates[0][2]

def source_blob(record, headers):
    selected = []
    for header in headers:
        n = norm(header)
        if any(token in n for token in (
            "reference", "source", "takhrij", "takhreej", "book",
            "rapport", "recueil", "grade", "authentic", "degre", "attribution"
        )):
            value = record.get(header, "").strip()
            if value:
                selected.append(value)
    if not selected:
        selected = [v.strip() for v in record.values() if v.strip()]
    return " • ".join(dict.fromkeys(selected))

def extract_books(blob):
    n = norm(blob)
    books = []
    for display, variants in BOOK_PATTERNS:
        if any(norm(variant) in n for variant in variants):
            books.append(display)
    return books

def has_precise_reference(blob, books):
    n = norm(blob)
    if not books:
        return False
    return bool(re.search(r"\b\d{1,5}[a-z]?\b", n))

def authentic_grade(blob):
    n = norm(blob)
    if any(word in n for word in ("daif", "faible", "weak", "mawdu", "fabrique")):
        return None
    if "authentique" in n or "sahih" in n:
        return "Sahih / authentique selon la fiche source"
    if "hasan" in n or "bon" in n:
        return "Hasan selon la fiche source"
    return None

fr_headers, fr_rows = as_records(read_xlsx(fetch(FR_URL)))
ar_headers, ar_rows = as_records(read_xlsx(fetch(AR_URL)))

print("HadeethEnc FR headers:", fr_headers)
print("HadeethEnc AR headers:", ar_headers)

fr_id_key = find_key(fr_headers, ("id", "hadeeth id", "hadith id", "numero", "number"))
ar_id_key = find_key(ar_headers, ("id", "hadeeth id", "hadith id", "numero", "number"))
if not fr_id_key or not ar_id_key:
    raise SystemExit("Unable to identify a common HadeethEnc ID column")

ar_by_id = {row.get(ar_id_key, "").strip(): row for row in ar_rows if row.get(ar_id_key, "").strip()}
records = []

for fr in fr_rows:
    hid = fr.get(fr_id_key, "").strip()
    ar = ar_by_id.get(hid)
    if not hid or ar is None:
        continue

    french = choose_text(fr, fr_headers, "fr")
    arabic = choose_text(ar, ar_headers, "ar")
    if not french or not arabic:
        continue
    if len(french) > 520 or len(arabic) > 850:
        continue

    blob = source_blob(fr, fr_headers) + " • " + source_blob(ar, ar_headers)
    books = extract_books(blob)
    if not has_precise_reference(blob, books):
        continue
    grade = authentic_grade(blob)
    if grade is None:
        continue

    # Prefer Bukhari/Muslim, then the other recognized collections.
    priority = 0 if any(book in ("Sahih al-Bukhari", "Sahih Muslim") for book in books) else 1
    reference = source_blob(fr, fr_headers)
    if not any(char.isdigit() for char in reference):
        reference = source_blob(ar, ar_headers)

    records.append((
        priority,
        {
            "id": "he_" + hid,
            "type": "HADITH",
            "theme": "bonnes_moeurs",
            "arabicText": arabic,
            "frenchText": french,
            "author": "Prophète Muhammad ﷺ",
            "book": " / ".join(books),
            "reference": reference,
            "authenticity": grade,
            "tags": ["bonnes mœurs", "comportement"],
            "sourceId": hid,
            "translationSource": "HadeethEnc.com Français " + FR_VERSION,
            "sourceSnapshotDate": "2026-08-31"
        }
    ))

records.sort(key=lambda item: (item[0], item[1]["id"]))
unique = []
seen = set()
for _, record in records:
    if record["id"] in seen:
        continue
    seen.add(record["id"])
    unique.append(record)
    if len(unique) >= TARGET:
        break

if len(unique) < TARGET:
    raise SystemExit(
        "Only %d precisely sourced authentic records collected; need %d" %
        (len(unique), TARGET)
    )

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as handle:
    json.dump(unique, handle, ensure_ascii=False, indent=2)

print("generated", len(unique), OUT, "from HadeethEnc French", FR_VERSION)
