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
    body = body.strip("[](){} ـ")
    return body

def normalize(value):
    value = unicodedata.normalize("NFC", value)
    value = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", value)
    value = value.replace("ـ", "").replace("ٱ", "ا")
    value = re.sub(r"[«»“”\[\](){}،؛؟.,:;!?]", " ", value)
    return re.sub(r"\s+", " ", value).strip()

def extract(text):
    entries = []
    lines = [x.strip() for x in text.splitlines() if x.strip()]
    for line in lines:
        m = re.match(r"^(\d{1,3})\s*[-–—]\s*(.+)$", line)
        if m:
            n = int(m.group(1))
            body = clean(m.group(2))
            if 1 <= n <= 264 and re.search(r"[\u0600-\u06ff]", body):
                entries.append((n, body))
    compact = " ".join(lines)
    pat = re.compile(r"(?<!\d)(\d{1,3})\s*[-–—]\s*(.+?)(?=(?:\s+\d{1,3}\s*[-–—])|$)")
    for m in pat.finditer(compact):
        n = int(m.group(1))
        body = clean(m.group(2))
        if 1 <= n <= 264 and re.search(r"[\u0600-\u06ff]", body) and len(body) <= 1800:
            entries.append((n, body))
    return entries

pages = {}
failures = []
with ThreadPoolExecutor(max_workers=32) as pool:
    futures = [pool.submit(fetch_page, p) for p in range(1, 116)]
    for fut in as_completed(futures):
        try:
            page, raw = fut.result()
            pages[page] = raw
        except Exception as exc:
            failures.append(str(exc))

candidates = {}
for page in sorted(pages):
    for number, body in extract(textify(pages[page])):
        candidates.setdefault(number, []).append({"page": page, "arabic": body})

selected = {}
duplicates = []
for number, rows in sorted(candidates.items()):
    # Prefer the shortest numbered line; commentary pages can repeat the hikma.
    rows = sorted(rows, key=lambda row: (len(normalize(row["arabic"])), row["page"]))
    winner = rows[0]
    selected[number] = winner
    normalized = {}
    for row in rows:
        normalized.setdefault(normalize(row["arabic"]), []).append(row["page"])
    if len(normalized) > 1:
        duplicates.append({
            "source_number": number,
            "variants": [
                {"normalized": key, "pages": value}
                for key, value in normalized.items()
            ]
        })

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
            "source_page": selected[n]["page"],
            "arabic": selected[n]["arabic"],
            "normalized_arabic": normalize(selected[n]["arabic"]),
            "verification_status": "pending_secondary_crosscheck"
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
