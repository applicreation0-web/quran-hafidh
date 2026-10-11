#!/usr/bin/env python3
"""READ-ONLY Ibn Kathir title-coverage evidence for the final Work audit.

This does NOT claim first-word documentary starts are approved mnemonic keys;
it does NOT extract or reproduce copyrighted Darussalam commentary or invent
headings. It merely lists which EXACT group ranges can return a verified
English heading from the current offline Java file.

Usage: python3 scripts/audit_ibn_kathir_verified_title_coverage.py
           --out /tmp/ibn-kathir-title-coverage.csv
"""
import argparse
import base64
import csv
import hashlib
import pathlib
import re
import zlib
from collections import Counter

ROOT = pathlib.Path(__file__).resolve().parents[1]
PKG = ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview"
INDEX = PKG / "IbnKathirGroupIndex.java"
TITLE = PKG / "IbnKathirOfflineTitles.java"

def intervals_from_index():
    source = INDEX.read_text(encoding="utf8")
    packed = re.search(r'BOUNDARIES_ZLIB_BASE64\s*=\s*"([^"]+)"', source)
    checksum = re.search(r'ENDPOINTS_SHA256\s*=\s*"([0-9a-f]{64})"', source)
    assert packed and checksum, "Immutable Ibn Kathir documentary index changed shape"
    raw = zlib.decompress(base64.b64decode(packed.group(1)))
    assert hashlib.sha256(raw).hexdigest() == checksum.group(1), "Group boundary hash mismatch"
    result = []
    for row in raw.decode("utf8").splitlines():
        surah_str, ends_str = row.split(":", 1)
        surah = int(surah_str)
        beginning = 1
        for end in map(int, ends_str.split(",")):
            assert end >= beginning, (surah, beginning, end)
            result.append((surah, beginning, end))
            beginning = end + 1
    assert len(result) == 1903, ("Expected 1,903 source groups", len(result))
    assert {s for s, _, _ in result} == set(range(1, 115))
    return result

def approved_title_ranges():
    text = TITLE.read_text(encoding="utf8")
    # A closed whitelist is deliberate. Only these two chapters currently have
    # source-attested English title arrays; extra chapters demand their own audit.
    result = set()
    for method, surah in [("verifiedQafHeadings", 50), ("verifiedNajmHeadings", 53)]:
        i = text.index("private static String[] " + method)
        # Qaf begins with a null guard returning an empty array; start at
        # the actual switch rather than stopping at that preliminary return.
        switch = text.index("switch(group.startAyah)", i)
        block = text[switch:].split("\\n        return new String[0];", 1)[0]
        for case in re.finditer(r"case\s+(\d+)\s*:", block):
            following = block[case.end():]
            next_case = re.search(r"\bcase\s+\d+\s*:", following)
            clause = following[:next_case.start()] if next_case else following
            eq = re.search(r"group\.endAyah\s*==\s*(\d+)", clause)
            neq = re.search(r"group\.endAyah\s*!=\s*(\d+)", clause)
            boundary = eq or neq
            if not boundary or "return new String[]" not in clause:
                continue
            result.add((surah, int(case.group(1)), int(boundary.group(1))))
    return result

def audit(out):
    source_groups = intervals_from_index()
    titled = approved_title_ranges()
    missing = titled - set(source_groups)
    assert not missing, ("Headings do not match a real commentary interval", missing)
    assert len(titled) == 16, ("Expected current source-attested checkpoint: Qaf/Najm", len(titled))
    with pathlib.Path(out).open("w", encoding="utf8", newline="") as fp:
        writer = csv.writer(fp)
        writer.writerow(("group_id", "surah", "start_ayah", "end_ayah",
                         "english_title_status", "mnemonic_key_approved", "required_evidence"))
        for s, start, end in source_groups:
            writer.writerow((f"IKEN{s:03d}_{start:03d}_{end:03d}", s, start, end,
                "VERIFIED_QAF_NAJM" if (s, start, end) in titled else "MISSING_VERIFIED_TITLE",
                "NO",
                "original English chapter heading, edition, source location and rights review"
                if (s, start, end) not in titled else "retain exact verified English wording"))
    print(f"GROUPS={len(source_groups)}; VERIFIED_TITLES={len(titled)}; "
          f"MISSING={len(source_groups)-len(titled)}; "
          f"EDITORIALLY_APPROVED_MNEMONIC_KEYS=0; CSV={out}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="/tmp/quran-haafidh-ibn-kathir-coverage.csv")
    args = parser.parse_args()
    audit(args.out)
