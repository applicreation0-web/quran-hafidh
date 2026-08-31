#!/usr/bin/env python3
import difflib
import io
import json
import re
import unicodedata
import urllib.request
from pathlib import Path

from pypdf import PdfReader

PDF_URL = "https://data.nur.nu/Kutub/Arabic/Ibn3AtaAllah_Hikam_000802_al-mostafa.pdf"
PRIMARY = Path("app/src/main/assets/hikam/primary_extraction.json")
OUT = Path("app/src/main/assets/hikam/secondary_arabic_crosscheck.json")

def fetch():
    req = urllib.request.Request(
        PDF_URL,
        headers={"User-Agent":"QuranSafeguard-HikamAudit/3.0"}
    )
    with urllib.request.urlopen(req, timeout=90) as response:
        return response.read()

def normalize(value):
    value = unicodedata.normalize("NFC", value or "")
    value = re.sub(r"[\ue000-\uf8ff]", "", value)
    value = value.replace("ـ", "").replace("ٱ", "ا")
    value = value.replace("االله", "الله")
    value = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", value)
    value = value.translate(str.maketrans({
        "إ":"ا","أ":"ا","آ":"ا","ى":"ي","ؤ":"و","ئ":"ي","ة":"ه"
    }))
    # Bibliographic Quran references are editorial and are not part of the matn.
    value = re.sub(r"\[[^\]]*\]", " ", value)
    value = re.sub(r"\([^)]*سورة[^)]*\)", " ", value)
    value = re.sub(r"[^\u0600-\u06ff]", "", value)
    return value

def extract_ordered(reader):
    blocks=[]
    current=[]
    started=False

    def flush():
        nonlocal current
        if started and current:
            text=" ".join(current).strip()
            if re.search(r"[\u0600-\u06ff]", text):
                blocks.append(text)
        current=[]

    for page in reader.pages:
        text=page.extract_text() or ""
        for raw_line in text.splitlines():
            line=re.sub(r"\s+", " ", raw_line).strip()
            if not line:
                continue
            if re.match(r"^الحكمة\s+", line):
                if started:
                    flush()
                started=True
                continue
            if not started:
                continue
            # Remove recurring page headers/footers and conversion credits.
            if "ابن عطاء" in line and "الحكم العطائية" in line:
                continue
            if line.startswith("Source:") or line.startswith("To Pdf:"):
                continue
            current.append(line)
    flush()
    return blocks

primary=json.loads(PRIMARY.read_text(encoding="utf-8"))
primary_entries=primary["entries"]

reader=PdfReader(io.BytesIO(fetch()))
secondary=extract_ordered(reader)

if len(secondary) != 264:
    raise SystemExit(f"Expected 264 secondary Hikam headings, extracted {len(secondary)}")

results=[]
verified=0
review=0
for index,(pentry,sraw) in enumerate(zip(primary_entries,secondary),start=1):
    assert int(pentry["source_number"]) == index
    p=normalize(pentry["arabic"])
    s=normalize(sraw)
    ratio=difflib.SequenceMatcher(None,p,s,autojunk=False).ratio()
    length_ratio=min(len(p),len(s))/max(1,max(len(p),len(s)))

    # High thresholds: tolerate OCR noise while refusing material textual drift.
    if ratio >= 0.92 and length_ratio >= 0.88:
        status="verified_against_secondary_arabic"
        verified += 1
        results.append({
            "source_number":index,
            "status":status,
            "similarity":round(ratio,4),
            "length_ratio":round(length_ratio,4)
        })
    else:
        status="needs_manual_textual_review"
        review += 1
        results.append({
            "source_number":index,
            "status":status,
            "similarity":round(ratio,4),
            "length_ratio":round(length_ratio,4),
            "primary_arabic":pentry["arabic"],
            "secondary_arabic":sraw
        })

payload={
    "secondary_source":{
        "title":"الحكم العطائية — ابن عطاء الله السكندري",
        "url":PDF_URL,
        "source_credit":"al-mostafa PDF; linked by Damas Cultural Society",
        "pages":len(reader.pages),
        "numbering_method":"264 sequential explicit 'الحكمة ...' headings"
    },
    "summary":{
        "secondary_detected":len(secondary),
        "auto_verified_high_similarity":verified,
        "needs_manual_textual_review":review
    },
    "results":results
}
OUT.write_text(json.dumps(payload,ensure_ascii=False,indent=2),encoding="utf-8")
print(json.dumps(payload["summary"],ensure_ascii=False))
