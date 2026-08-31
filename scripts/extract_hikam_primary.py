#!/usr/bin/env python3
import html
import json
import re
import unicodedata
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

UA = {"User-Agent": "QuranSafeguard-HikamPrimary/1.0"}
BASE = "https://ablibrary.net/book_content/8787/"
OUT = Path("app/src/main/assets/hikam/primary_extraction.json")

# Dedicated matn pages and their expected numbering. This prevents a typo/
# duplicate marker on another page from being mistaken for the canonical entry.
PAGE_RANGES = {
    95: (1, 12),
    96: (13, 21),
    97: (22, 34),
    98: (35, 45),
    99: (46, 58),
    100: (59, 71),
    101: (72, 88),
    102: (89, 104),
    103: (105, 118),
    104: (119, 132),
    105: (133, 145),
    106: (146, 159),
    107: (160, 174),
    108: (175, 189),
    109: (190, 204),
    110: (205, 218),
    111: (219, 232),
    112: (234, 245),
    113: (246, 256),
    114: (257, 264),
}

def fetch_page(page):
    url = BASE + str(page)
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=15) as response:
        return page, response.read().decode("utf-8", errors="replace")

def textify(raw):
    raw = re.sub(r"<script\b[^>]*>.*?</script>", " ", raw, flags=re.S|re.I)
    raw = re.sub(r"<style\b[^>]*>.*?</style>", " ", raw, flags=re.S|re.I)
    raw = re.sub(r"<br\s*/?>", "\n", raw, flags=re.I)
    raw = re.sub(r"</p>|</li>|</div>|</h\d>|</a>", "\n", raw, flags=re.I)
    raw = re.sub(r"<[^>]+>", " ", raw)
    raw = html.unescape(raw).replace("\xa0", " ")
    raw = re.sub(r"[ \t]+", " ", raw)
    raw = re.sub(r"\n+", "\n", raw)
    return raw

def clean(body):
    body = re.sub(r"\s+", " ", body).strip()
    # Strip navigation/site metadata that follows the last numbered hikma
    # on a digital page, without touching the matn itself.
    for marker in (
        "صفحات الكتاب", "الرئيسية /", "الصفحة السابقة",
        "© 2026", "Ahlulbayt Library", "مكتبة أهل البيت الرقمية",
        "تمّت بعونه تعالى الحكم العطائية الكبرى",
        "تمت بعونه تعالى الحكم العطائية الكبرى"
    ):
        if marker in body:
            body = body.split(marker, 1)[0].strip()
    body = body.strip("[](){} ـ")
    return body

def normalize(value):
    value = unicodedata.normalize("NFC", value)
    value = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", value)
    value = value.replace("ـ", "").replace("ٱ", "ا")
    value = re.sub(r"[«»“”\[\](){}،؛؟.,:;!?]", " ", value)
    return re.sub(r"\s+", " ", value).strip()

def extract(text):
    # Parse the matn as a sequence: a numbered marker opens one Hikma and
    # continuation lines belong to it until the next numbered marker.
    # This avoids both truncating multi-line Hikam and swallowing later ones.
    digit_map = str.maketrans("٠١٢٣٤٥٦٧٨٩", "0123456789")
    lines = [
        re.sub(r"\\s+", " ", x.translate(digit_map)).strip()
        for x in text.splitlines()
        if x.strip()
    ]

    entries = []
    current_number = None
    buffer = []

    def flush():
        nonlocal current_number, buffer
        if current_number is None:
            buffer = []
            return
        body = clean(" ".join(buffer))
        if (
            1 <= current_number <= 264
            and re.search(r"[\\u0600-\\u06ff]", body)
            and 12 <= len(normalize(body)) <= 1800
        ):
            entries.append((current_number, body))
        current_number = None
        buffer = []

    for line in lines:
        # Stop page-navigation/footer material from attaching to the last Hikma.
        if any(marker in line for marker in (
            "صفحات الكتاب", "الرئيسية /", "الصفحة السابقة",
            "© 2026", "Ahlulbayt Library"
        )):
            flush()
            continue

        m = re.match(r"^(\\d{1,3})\\s*[-–—]\\s*(.*)$", line)
        if m:
            flush()
            current_number = int(m.group(1))
            first = m.group(2).strip()
            if first:
                buffer.append(first)
            continue

        if current_number is not None:
            buffer.append(line)

    flush()
    return entries

pages = {}
failures = []
with ThreadPoolExecutor(max_workers=32) as pool:
    # The complete numbered matn is contained in digital pages 95..114.
    # Earlier pages contain commentary and unrelated numbered lists.
    futures = [pool.submit(fetch_page, p) for p in range(95, 115)]
    for fut in as_completed(futures):
        try:
            page, raw = fut.result()
            pages[page] = raw
        except Exception as exc:
            failures.append(str(exc))

candidates = {}
for page in sorted(pages):
    lower, upper = PAGE_RANGES[page]
    for number, body in extract(textify(pages[page])):
        if not (lower <= number <= upper):
            continue
        candidates.setdefault(number, []).append({"page": page, "arabic": body})

selected = {}
duplicates = []
for number, rows in sorted(candidates.items()):
    # Sequence parsing already preserves complete boundaries. If the HTML
    # repeats a number, keep the longest unique occurrence on its canonical page.
    unique_rows = {}
    for row in rows:
        key = normalize(row["arabic"])
        if key:
            unique_rows.setdefault(key, row)
    rows = list(unique_rows.values())
    rows = sorted(
        rows,
        key=lambda row: (-len(normalize(row["arabic"])), row["page"])
    )
    winner = rows[0]
    selected[number] = winner
    if len(rows) > 1:
        duplicates.append({
            "source_number": number,
            "selected": normalize(winner["arabic"]),
            "alternatives": [
                {
                    "normalized": normalize(row["arabic"]),
                    "page": row["page"]
                }
                for row in rows[1:]
            ]
        })

# One source page omits the numeric marker for 233 in machine parsing.
# The text is recovered only because it was independently verified in:
# - Ibn 'Ajiba index/digital edition: Najaf Desert Library
# - University of Ghardaia academic appendix of Matn al-Hikam
if 233 not in selected:
    selected[233] = {
        "page": None,
        "arabic": "العِلْمُ إِنْ قَارَنَتْهُ الخَشْيَةُ فَلَكَ، وَإِلَّا فَعَلَيْكَ.",
        "recovered_from_crosscheck": True,
        "recovery_sources": [
            "https://najafdesertlibrary.com/book/إيقاظ-الهمم-في-شرح-حكم-سيدي-أحمد-بن-عطاء-الله-السكندري/v/1/p/74",
            "https://dspace.univ-ghardaia.edu.dz/jspui/bitstream/123456789/4884/1/408.04.106.pdf"
        ]
    }

missing = [n for n in range(1, 265) if n not in selected]

payload = {
    "collection": "al_hikam_al_ataiyya",
    "source_title": "اللطائف الإلهية في شرح مختارات من الحكم العطائية",
    "digital_library": "مكتبة أهل البيت",
    "base_url": BASE,
    "expected_numbering_range_for_cross_check": "1-264",
    "detected_count": len(selected),
    "missing_numbers": missing,
    "fetch_failure_count": len(failures),
    "candidate_variant_numbers": duplicates,
    "entries": [
        {
            "source_number": n,
            "source_page": selected[n].get("page"),
            "arabic": selected[n]["arabic"],
            "normalized_arabic": normalize(selected[n]["arabic"]),
            "verification_status": "pending_secondary_crosscheck",
            "recovered_from_crosscheck": selected[n].get("recovered_from_crosscheck", False),
            "recovery_sources": selected[n].get("recovery_sources", [])
        }
        for n in sorted(selected)
    ]
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "detected": len(selected),
    "missing": missing,
    "variants": len(duplicates),
    "fetch_failures": len(failures)
}, ensure_ascii=False))

if missing:
    raise SystemExit("Primary extraction incomplete: missing " + ",".join(map(str, missing)))
