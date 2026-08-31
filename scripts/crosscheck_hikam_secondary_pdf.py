#!/usr/bin/env python3
import io
import json
import re
import unicodedata
import urllib.request
from pathlib import Path

from pypdf import PdfReader

PDF_URL = "https://data.nur.nu/Kutub/Arabic/Ibn3AtaAllah_Hikam_themathesontrust.pdf"
PRIMARY = Path("app/src/main/assets/hikam/primary_extraction.json")
OUT = Path("app/src/main/assets/hikam/secondary_arabic_crosscheck.json")

def normalize(value):
    value = unicodedata.normalize("NFC", value or "")
    value = value.replace("ـ", "").replace("ٱ", "ا")
    value = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", value)
    value = value.translate(str.maketrans({"إ":"ا","أ":"ا","آ":"ا","ى":"ي","ؤ":"و","ئ":"ي"}))
    value = re.sub(r"[^\u0600-\u06ff ]", " ", value)
    return re.sub(r"\s+", " ", value).strip()

def fetch():
    req = urllib.request.Request(PDF_URL, headers={"User-Agent":"QuranSafeguard-HikamAudit/2.0"})
    with urllib.request.urlopen(req, timeout=90) as response:
        return response.read()

def extract(reader):
    raw = "\n".join((p.extract_text() or "") for p in reader.pages)
    raw = raw.translate(str.maketrans("٠١٢٣٤٥٦٧٨٩", "0123456789"))
    raw = re.sub(r"[\u0000-\u001f]+", " ", raw)
    raw = re.sub(r"\s+", " ", raw)

    # The PDF text layer alternates between "1 ◊" and "◊ 14".
    marker_re = re.compile(r"(?:(?P<a>\d{1,3})\s*[◊◇]|[◊◇]\s*(?P<b>\d{1,3}))")
    markers = []
    for m in marker_re.finditer(raw):
        token = m.group("a") or m.group("b")
        n = int(token)
        if 1 <= n <= 264:
            markers.append((m, n))

    entries = {}
    for i, (marker, n) in enumerate(markers):
        end = markers[i+1][0].start() if i+1 < len(markers) else len(raw)
        body = raw[marker.end():end].strip()
        if re.search(r"[\u0600-\u06ff]", body):
            # First occurrence wins; later page furniture can repeat numbers.
            entries.setdefault(n, body)
    return entries

primary = json.loads(PRIMARY.read_text(encoding="utf-8"))
primary_by_num = {int(e["source_number"]): e for e in primary["entries"]}

reader = PdfReader(io.BytesIO(fetch()))
secondary = extract(reader)

results = []
exact = 0
contains = 0
variants = 0
missing = []

for n in range(1, 265):
    p = normalize(primary_by_num[n]["arabic"])
    sraw = secondary.get(n)
    if not sraw:
        missing.append(n)
        results.append({"source_number":n,"status":"missing_secondary"})
        continue
    s = normalize(sraw)
    if p == s:
        status = "exact_normalized_match"
        exact += 1
    elif p and s and (p in s or s in p):
        status = "normalized_containment_match"
        contains += 1
    else:
        # Character token overlap provides an audit aid only; it never auto-verifies.
        pwords=set(p.split()); swords=set(s.split())
        overlap=(len(pwords & swords)/max(1,len(pwords | swords)))
        status="textual_variant_or_extraction_difference"
        variants += 1
        results.append({
            "source_number": n,
            "status": status,
            "word_jaccard": round(overlap,4),
            "primary_normalized": p,
            "secondary_normalized": s
        })
        continue
    results.append({"source_number":n,"status":status})

payload={
    "secondary_source":{
        "title":"Al-Hikam — Ibn Ata Allah al-Iskandari",
        "url":PDF_URL,
        "host":"Damas Cultural Society / data.nur.nu",
        "pages":len(reader.pages)
    },
    "summary":{
        "secondary_detected":len(secondary),
        "exact_normalized_matches":exact,
        "normalized_containment_matches":contains,
        "variants_or_extraction_differences":variants,
        "missing_secondary":missing
    },
    "results":results
}
OUT.write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding="utf-8")
print(json.dumps(payload["summary"],ensure_ascii=False))

# Require complete numbering from secondary before it can be used as corpus evidence.
if missing:
    raise SystemExit("Secondary source missing numbers: "+",".join(map(str,missing)))
