#!/usr/bin/env python3
import io
import json
import re
import unicodedata
import urllib.request
from pathlib import Path

from pypdf import PdfReader

PDF_URL = "https://dspace.univ-ghardaia.edu.dz/jspui/bitstream/123456789/4884/1/408.04.106.pdf"
OUT = Path("app/src/main/assets/hikam/academic_crosscheck.json")
UA = {"User-Agent": "QuranSafeguard-HikamAcademicAudit/1.0"}

def fetch_pdf():
    req = urllib.request.Request(PDF_URL, headers=UA)
    with urllib.request.urlopen(req, timeout=90) as response:
        return response.read()

def normalize(value):
    value = unicodedata.normalize("NFC", value or "")
    value = value.replace("ـ", "").replace("ٱ", "ا")
    value = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", value)
    value = value.translate(str.maketrans({
        "إ":"ا","أ":"ا","آ":"ا","ى":"ي","ؤ":"و","ئ":"ي","ة":"ه"
    }))
    value = re.sub(r"[^\u0600-\u06ff0-9 ]", " ", value)
    return re.sub(r"\s+", " ", value).strip()

def digit_fix(text):
    # OCR may use Arabic-Indic digits; normalize them to Latin.
    return text.translate(str.maketrans("٠١٢٣٤٥٦٧٨٩", "0123456789"))

def parse_numbered(text):
    text = digit_fix(text)
    rows = {}
    # The appendix uses patterns such as "-233 ..." and sometimes "233-".
    lines = [re.sub(r"\s+", " ", x).strip() for x in text.splitlines() if x.strip()]
    for i, line in enumerate(lines):
        for pat in (
            r"^[-–—]?\s*(\d{1,3})\s*[-–—.:)]\s*(.+)$",
            r"^(\d{1,3})\s+(.+)$",
            r"^[-–—]\s*(\d{1,3})\s+(.+)$"
        ):
            m = re.match(pat, line)
            if not m:
                continue
            n = int(m.group(1))
            if not 1 <= n <= 264:
                continue
            body = m.group(2).strip()
            if re.search(r"[\u0600-\u06ff]", body):
                rows.setdefault(n, body)
                break
    return rows

raw = fetch_pdf()
reader = PdfReader(io.BytesIO(raw))
pages = {}
all_entries = {}
# Appendix begins around physical PDF page 70 and ends around 89.
for index in range(max(0, 64), min(len(reader.pages), 92)):
    text = reader.pages[index].extract_text() or ""
    pages[index + 1] = text
    for n, body in parse_numbered(text).items():
        all_entries.setdefault(n, {
            "pdf_page": index + 1,
            "ocr_arabic": body,
            "normalized_ocr": normalize(body)
        })

payload = {
    "source": {
        "institution": "Université de Ghardaïa",
        "title": "جماليات البديع في الحكم العطائية - دراسة بلاغية",
        "appendix": "ملحق: متن الحكم العطائية",
        "url": PDF_URL,
        "pdf_page_window_scanned": [65, min(len(reader.pages), 92)],
        "pdf_total_pages": len(reader.pages),
        "note": "Secondary academic cross-check. OCR/extracted Arabic is not used to overwrite the primary matn."
    },
    "detected_count": len(all_entries),
    "detected_numbers": sorted(all_entries),
    "missing_1_264": [n for n in range(1,265) if n not in all_entries],
    "entries": [
        {
            "source_number": n,
            **all_entries[n]
        }
        for n in sorted(all_entries)
    ],
    "raw_pages": {
        str(page): text
        for page, text in pages.items()
    }
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "detected": len(all_entries),
    "missing": payload["missing_1_264"],
    "pdf_pages": len(reader.pages)
}, ensure_ascii=False))
