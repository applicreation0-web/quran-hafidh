#!/usr/bin/env python3
import json
import re
from pathlib import Path

PATH = Path("app/src/main/assets/hikam/primary_extraction.json")
data = json.loads(PATH.read_text(encoding="utf-8"))

digit_map = str.maketrans("٠١٢٣٤٥٦٧٨٩", "0123456789")
changed = []

for entry in data["entries"]:
    n = int(entry["source_number"])
    text = entry["arabic"].translate(digit_map)

    # A polluted compact candidate can append "14 - ..." to Hikma 13 etc.
    # Cut only at a later numbered-Hikma marker. Quran citations use ":" and
    # are therefore unaffected.
    marker = re.search(r"\s+(\d{1,3})\s*[-–—]\s*", text)
    if marker:
        following = int(marker.group(1))
        if following != n:
            text = text[:marker.start()].strip()
            changed.append({"source_number": n, "cut_before": following})

    for footer in (
        "تمّت بعونه تعالى الحكم العطائية الكبرى",
        "تمت بعونه تعالى الحكم العطائية الكبرى",
        "صفحات الكتاب",
        "الرئيسية /"
    ):
        if footer in text:
            text = text.split(footer, 1)[0].strip()
            changed.append({"source_number": n, "footer_removed": footer})

    entry["arabic"] = text

    # Keep normalized text in sync using a conservative display normalization.
    normalized = text
    normalized = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", normalized)
    normalized = normalized.replace("ـ", "")
    normalized = re.sub(r"[«»“”\[\](){}،؛؟.,:;!?]", " ", normalized)
    entry["normalized_arabic"] = re.sub(r"\s+", " ", normalized).strip()

numbers = [int(e["source_number"]) for e in data["entries"]]
assert numbers == list(range(1, 265)), "Numbering is not exactly 1..264"
assert all(e["arabic"].strip() for e in data["entries"])

data["post_extraction_cleanup"] = {
    "method": "cut only at embedded later numbered-Hikma markers and known site footers",
    "changed_count": len(changed),
    "changes": changed
}

PATH.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({
    "entries": len(data["entries"]),
    "changed": len(changed)
}, ensure_ascii=False))
