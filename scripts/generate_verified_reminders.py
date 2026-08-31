#!/usr/bin/env python3
import io
import json
import os
import unicodedata
import urllib.request
import zipfile
import xml.etree.ElementTree as ET

TARGET = 127
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
    headers = [str(v).strip() for v in rows[0]]
    records = []
    for row in rows[1:]:
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
    if len(french) > 520 or len(arabic) > 850:
        continue

    priority = 0 if any(
        book in ("Sahih al-Bukhari", "Sahih Muslim") for book in books
    ) else 1

    candidates.append((
        priority,
        {
            "id": "he_" + hid,
            "type": "HADITH",
            "theme": "bonnes_moeurs",
            "arabicText": arabic,
            "frenchText": french,
            "author": "Prophète Muhammad ﷺ",
            "book": " / ".join(books),
            "reference": (
                "HadeethEnc.com Français " + FR_VERSION +
                " • " + takhrij + " • " + link
            ),
            "authenticity": grade,
            "tags": ["bonnes mœurs", "comportement"],
            "sourceId": hid,
            "translationSource": "HadeethEnc.com Français " + FR_VERSION,
            "sourceSnapshotDate": "2026-08-31"
        }
    ))

candidates.sort(key=lambda item: (item[0], item[1]["id"]))
unique = []
seen = set()
for _, record in candidates:
    if record["id"] in seen:
        continue
    seen.add(record["id"])
    unique.append(record)
    if len(unique) >= TARGET:
        break

if len(unique) < TARGET:
    raise SystemExit(
        "Only %d authentic records with recognized collection and precise "
        "HadeethEnc reference collected; need %d" % (len(unique), TARGET)
    )

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as handle:
    json.dump(unique, handle, ensure_ascii=False, indent=2)

primary = sum(
    1 for item in unique
    if "Sahih al-Bukhari" in item["book"] or "Sahih Muslim" in item["book"]
)
print(
    "generated", len(unique), OUT,
    "from HadeethEnc Français", FR_VERSION,
    "Bukhari/Muslim-priority entries:", primary
)
