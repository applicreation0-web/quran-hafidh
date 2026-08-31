#!/usr/bin/env python3
import html
import json
import re
import unicodedata
import urllib.request
from pathlib import Path

UA = {"User-Agent": "QuranSafeguard-HikamAudit/1.1"}

PRIMARY_URLS = [
    f"https://ablibrary.net/book_content/8787/{page}"
    for page in range(1, 116)
]

SECONDARY_URLS = [
    f"http://arabic-books.amuslim.org/%D8%A7%D9%84%D8%B1%D9%82%D8%A7%D9%82%20%D9%88%D8%A7%D9%84%D8%A2%D8%AF%D8%A7%D8%A8%20%D9%88%D8%A7%D9%84%D8%A3%D8%B0%D9%83%D8%A7%D8%B1/Web/8045/{page:03d}.htm"
    for page in range(1, 35)
]

OUT = Path("app/src/main/assets/hikam/source_audit.json")

ARABIC_BLOCK = re.compile(r"[\u0600-\u06ff]")

def fetch(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=45) as response:
        return response.read().decode("utf-8", errors="replace")

def textify(raw):
    raw = re.sub(r"<script\b[^>]*>.*?</script>", " ", raw, flags=re.S|re.I)
    raw = re.sub(r"<style\b[^>]*>.*?</style>", " ", raw, flags=re.S|re.I)
    raw = re.sub(r"<br\s*/?>", "\n", raw, flags=re.I)
    raw = re.sub(r"</p>|</li>|</div>|</h\d>", "\n", raw, flags=re.I)
    raw = re.sub(r"<[^>]+>", " ", raw)
    raw = html.unescape(raw)
    raw = raw.replace("\xa0", " ")
    raw = re.sub(r"[ \t]+", " ", raw)
    raw = re.sub(r"\n+", "\n", raw)
    return raw

def normalize_arabic(value):
    value = unicodedata.normalize("NFC", value)
    value = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", value)
    value = value.replace("ـ", "")
    value = value.replace("ٱ", "ا")
    value = re.sub(r"[«»“”\[\](){}،؛؟.,:;!?]", " ", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value

def clean_body(body):
    body = re.sub(r"\s+", " ", body).strip()
    body = re.sub(r"^[\]\[(){}\s]+|[\]\[(){}\s]+$", "", body)
    return body

def extract_numbered(text):
    entries = {}
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    for line in lines:
        m = re.match(r"^(\d{1,3})\s*[-–—]\s*(.+)$", line)
        if not m:
            continue
        num = int(m.group(1))
        body = clean_body(m.group(2))
        if 1 <= num <= 400 and ARABIC_BLOCK.search(body) and 8 <= len(body) <= 1600:
            entries.setdefault(num, body)

    compact = " ".join(lines)
    pattern = re.compile(
        r"(?<!\d)(\d{1,3})\s*[-–—]\s*(.+?)(?=(?:\s+\d{1,3}\s*[-–—])|$)"
    )
    for m in pattern.finditer(compact):
        num = int(m.group(1))
        body = clean_body(m.group(2))
        if 1 <= num <= 400 and ARABIC_BLOCK.search(body) and 8 <= len(body) <= 1600:
            entries.setdefault(num, body)
    return entries

def harvest(urls):
    all_entries = {}
    pages = {}
    failures = []
    for url in urls:
        try:
            raw = fetch(url)
        except Exception as exc:
            failures.append({"url": url, "error": str(exc)})
            continue
        found = extract_numbered(textify(raw))
        if found:
            pages[url] = sorted(found)
        for n, body in found.items():
            if 1 <= n <= 264:
                current = all_entries.get(n)
                if current is None or len(body) < len(current):
                    all_entries[n] = body
    return all_entries, pages, failures

primary, primary_pages, primary_failures = harvest(PRIMARY_URLS)
secondary, secondary_pages, secondary_failures = harvest(SECONDARY_URLS)

matches = []
variants = []
missing_secondary = []
for n in sorted(primary):
    p = normalize_arabic(primary[n])
    sraw = secondary.get(n)
    if sraw is None:
        missing_secondary.append(n)
        continue
    s = normalize_arabic(sraw)
    if p == s:
        matches.append(n)
    else:
        ratio = 0.0
        if p and s:
            common = min(len(p), len(s))
            ratio = sum(1 for a,b in zip(p,s) if a == b) / max(len(p), len(s))
        variants.append({
            "number": n,
            "primary": primary[n],
            "secondary": sraw,
            "primary_normalized": p,
            "secondary_normalized": s,
            "rough_prefix_similarity": round(ratio, 4),
        })

payload = {
    "primary": {
        "source_title": "اللطائف الإلهية في شرح مختارات من الحكم العطائية",
        "digital_library": "مكتبة أهل البيت",
        "base_url": "https://ablibrary.net/book_content/8787/",
        "detected_hikam_1_264": len(primary),
        "missing_1_264": [n for n in range(1,265) if n not in primary],
        "pages_with_numbered_entries": primary_pages,
        "fetch_failures": primary_failures,
    },
    "secondary": {
        "source_title": "إيقاظ الهمم في شرح الحكم - ابن عجيبة",
        "digital_library": "Shamela text mirror",
        "base_url": "http://arabic-books.amuslim.org/.../Web/8045/",
        "detected_hikam_1_264": len(secondary),
        "missing_1_264": [n for n in range(1,265) if n not in secondary],
        "pages_with_numbered_entries": secondary_pages,
        "fetch_failures": secondary_failures,
    },
    "bibliographic_reference": {
        "title": "إيقاظ الهمم في شرح الحكم",
        "commentator": "أحمد بن محمد بن عجيبة الحسني",
        "edition": "دار الكتب العلمية، بيروت، 1426/2005",
        "editor": "عاصم إبراهيم الكيالي الحسيني الشاذلي الدرقاوي",
        "note": "Bibliographic identity cross-checked independently; the digital text mirror is used for textual comparison, not to invent numbering."
    },
    "cross_check": {
        "exact_normalized_matches": len(matches),
        "exact_match_numbers": matches,
        "variant_count": len(variants),
        "variants": variants,
        "missing_secondary_count": len(missing_secondary),
        "missing_secondary": missing_secondary,
    },
    "primary_entries": [
        {"source_number": n, "arabic": primary[n]}
        for n in sorted(primary)
    ],
    "secondary_entries": [
        {"source_number": n, "arabic": secondary[n]}
        for n in sorted(secondary)
    ],
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "primary": len(primary),
    "secondary": len(secondary),
    "matches": len(matches),
    "variants": len(variants),
    "missing_primary": len(payload["primary"]["missing_1_264"]),
    "missing_secondary": len(payload["secondary"]["missing_1_264"]),
}, ensure_ascii=False))
