#!/usr/bin/env python3
import html
import json
import re
import unicodedata
import urllib.request
from pathlib import Path

UA = {"User-Agent": "QuranSafeguard-HikamAudit/1.0"}

PRIMARY_URL = (
    "https://najafdesertlibrary.com/book/"
    "%D8%A5%D9%8A%D9%82%D8%A7%D8%B8-%D8%A7%D9%84%D9%87%D9%85%D9%85-%D9%81%D9%8A-%D8%B4%D8%B1%D8%AD-"
    "%D8%AD%D9%83%D9%85-%D8%B3%D9%8A%D8%AF%D9%8A-%D8%A3%D8%AD%D9%85%D8%AF-%D8%A8%D9%86-%D8%B9%D8%B7%D8%A7%D8%A1-"
    "%D8%A7%D9%84%D9%84%D9%87-%D8%A7%D9%84%D8%B3%D9%83%D9%86%D8%AF%D8%B1%D9%8A/v/1/p/614"
)
SECONDARY_URLS = [
    f"https://ablibrary.net/book_content/8787/{page}"
    for page in range(1, 116)
]

OUT = Path("app/src/main/assets/hikam/source_audit.json")

ARABIC_BLOCK = re.compile(r"[\u0600-\u06ff]")
NUMBERED = re.compile(
    r"(?<!\d)(\d{1,3})\s*[-–—]\s*([^\n\r<>]{8,900}?)(?=(?:\s+\d{1,3}\s*[-–—])|$)"
)

def fetch(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=45) as response:
        return response.read().decode("utf-8", errors="replace")

def textify(raw):
    raw = re.sub(r"<script\b[^>]*>.*?</script>", " ", raw, flags=re.S|re.I)
    raw = re.sub(r"<style\b[^>]*>.*?</style>", " ", raw, flags=re.S|re.I)
    raw = re.sub(r"<[^>]+>", "\n", raw)
    raw = html.unescape(raw)
    raw = raw.replace("\xa0", " ")
    raw = re.sub(r"[ \t]+", " ", raw)
    raw = re.sub(r"\n+", "\n", raw)
    return raw

def normalize_arabic(value):
    value = unicodedata.normalize("NFC", value)
    value = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", value)
    value = value.replace("ـ", "")
    value = re.sub(r"[«»“”\[\](){}،؛؟.,:؛!?]", " ", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value

def extract_numbered(text):
    entries = {}
    # line-oriented first
    for line in text.splitlines():
        line = line.strip()
        m = re.match(r"^(\d{1,3})\s*[-–—]\s*(.+)$", line)
        if not m:
            continue
        num = int(m.group(1))
        body = m.group(2).strip()
        if 1 <= num <= 400 and ARABIC_BLOCK.search(body):
            entries.setdefault(num, body)
    # fallback over compact text
    compact = re.sub(r"\s+", " ", text)
    for m in NUMBERED.finditer(compact):
        num = int(m.group(1))
        body = m.group(2).strip()
        if 1 <= num <= 400 and ARABIC_BLOCK.search(body):
            entries.setdefault(num, body)
    return entries

primary_html = fetch(PRIMARY_URL)
primary_text = textify(primary_html)
primary_all = extract_numbered(primary_text)
primary_hikam = {n:t for n,t in primary_all.items() if 1 <= n <= 264}

secondary_all = {}
secondary_pages = {}
for url in SECONDARY_URLS:
    try:
        raw = fetch(url)
    except Exception:
        continue
    found = extract_numbered(textify(raw))
    if not found:
        continue
    page = int(url.rsplit("/",1)[1])
    secondary_pages[str(page)] = sorted(found)
    for n,t in found.items():
        if 1 <= n <= 264:
            secondary_all.setdefault(n, t)

matches = []
variants = []
missing_secondary = []
for n in sorted(primary_hikam):
    p = normalize_arabic(primary_hikam[n])
    sraw = secondary_all.get(n)
    if sraw is None:
        missing_secondary.append(n)
        continue
    s = normalize_arabic(sraw)
    if p == s:
        matches.append(n)
    else:
        # allow one normalized text to contain the other when a source carries
        # a short editorial suffix/prefix; keep it as variant for human review.
        variants.append({
            "number": n,
            "primary": primary_hikam[n],
            "secondary": sraw,
            "primary_normalized": p,
            "secondary_normalized": s,
        })

payload = {
    "primary": {
        "url": PRIMARY_URL,
        "source_title": "إيقاظ الهمم في شرح الحكم",
        "commentator": "أحمد بن عجيبة الحسني",
        "edition_note": "Dār al-Kutub al-ʿIlmiyya, Beirut, 1426/2005, ed. ʿĀṣim Ibrāhīm al-Kayyālī (bibliographic cross-check)",
        "detected_numbered_entries": len(primary_all),
        "detected_hikam_1_264": len(primary_hikam),
        "min": min(primary_hikam) if primary_hikam else None,
        "max": max(primary_hikam) if primary_hikam else None,
        "missing_1_264": [n for n in range(1,265) if n not in primary_hikam],
    },
    "secondary": {
        "base": "https://ablibrary.net/book_content/8787/",
        "detected_hikam_1_264": len(secondary_all),
        "missing_1_264": [n for n in range(1,265) if n not in secondary_all],
        "pages_with_numbered_entries": secondary_pages,
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
        {"source_number": n, "arabic": primary_hikam[n]}
        for n in sorted(primary_hikam)
    ],
    "secondary_entries": [
        {"source_number": n, "arabic": secondary_all[n]}
        for n in sorted(secondary_all)
    ],
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "primary_hikam": len(primary_hikam),
    "secondary_hikam": len(secondary_all),
    "matches": len(matches),
    "variants": len(variants),
    "missing_primary": payload["primary"]["missing_1_264"],
    "missing_secondary_count": len(missing_secondary),
}, ensure_ascii=False))
