#!/usr/bin/env python3
"""J2 Quran.com Qaf editorial checkpoint: 8 exact groups, QCF-only review hints.

All English title excerpts are evidenced by their cited Quran.com pages and
NEVER turned into runtime titles or into self-approved Arabic mnemonic keys.
No Quran text is recreated. This script validates source geometry and refuses
changes to runtime/editorial status.
"""
from __future__ import annotations
import argparse
import base64
import hashlib
import json
import re
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirGroupIndex.java"
BOXES = ROOT / "hifz-app/src/main/word-source/quran-ws-v1.1.2/word-boxes-451-604.json"
DATA = ROOT / "docs/ibn_kathir_qaf_qurancom_editorial_evidence.json"
GROUP_ENDS = [(1,5),(6,11),(12,15),(16,22),(23,29),(30,35),(36,40),(41,45)]
PAGES = [518,518,518,519,519,519,520,520]

def require(value, name):
    if not value:
        raise ValueError("Qaf / Ibn Kathir source audit FAIL: " + name)

def boundaries_from_pinned_index():
    source = SRC.read_text(encoding="utf-8")
    match = re.search(r'BOUNDARIES_ZLIB_BASE64\s*=\s*"([^"]+)"', source)
    require(match is not None, "missing pinned index")
    raw = zlib.decompress(base64.b64decode(match.group(1)))
    digest = hashlib.sha256(raw).hexdigest()
    expected = re.search(r'ENDPOINTS_SHA256\s*=\s*"([^"]+)"', source)
    require(expected is not None and digest == expected.group(1),
            "Ibn Kathir boundaries changed or SHA invalid")
    lines = raw.decode("utf-8").splitlines()
    require(len(lines) == 114, "not all surahs present")
    surah,tail = lines[49].split(":")
    require(surah == "50", "wrong surah index")
    starts = 1
    ranges = []
    for text in tail.split(","):
        end = int(text)
        ranges.append((starts,end))
        starts = end+1
    require(ranges == GROUP_ENDS and starts == 46,
            "Qaf boundaries differ from pinned source")
    return ranges

def verify(evidence, word_pages):
    groups = boundaries_from_pinned_index()
    require(evidence.get("schema") == "IK_QAF_QURANCOM_EDITORIAL_EVIDENCE_V1",
            "wrong evidence schema")
    require(evidence.get("group_count") == 8 and evidence.get("approved_keys") == 0
            and evidence.get("runtime_ready") is False, "premature approval")
    rows = evidence["group_entries"]
    require(len(rows) == len(groups) == 8, "missing or extra Qaf groups")
    checked = 0
    candidates = []
    for i,row in enumerate(rows):
        start,end = groups[i]
        group_id = f"IKEN050_{start:03d}_{end:03d}"
        require(row["id"] == group_id and row["surah"] == 50
                and (row["start_ayah"], row["end_ayah"]) == (start,end),
                "Qaf verse boundaries or ID drift")
        require(row["verified_qcf_page"] == PAGES[i],group_id+" page drift")
        require(row["quran_com_url"] ==
                f"https://quran.com/qaf/{start}/tafsirs/en-tafisr-ibn-kathir",
                group_id+" source URL drift")
        require(row["source_git_commit"] ==
                "eb82bb6294efe30ad5c135c03b1864afaa70e855",
                "source commit drift")
        require(row["editorial_status"] == "SOURCE_HEADINGS_READ_KEY_NOT_APPROVED"
                and row["rights_status"] == "TEXT_NOT_REDISTRIBUTED",
                group_id+" source status changed")
        require(bool(row["observed_english_heading_excerpts"]),group_id+" no headings")
        require(row["title_evidence"] ==
                "EXCERPTS_FROM_VISIBLE_HEADINGS_NOT_FULL_COMMENTARY",
                group_id+" full text unexpectedly packaged")
        seen=set()
        for review in row["review_start_word_keys"]:
            key=review["key"]
            require(key not in seen,group_id+" duplicate review key")
            seen.add(key)
            components=key.split(":")
            require(len(components)==3 and components[0]=="50"
                    and start <= int(components[1]) <= end
                    and components[2]=="1",group_id+" ungrounded Quranic start")
            require(key in word_pages[str(PAGES[i])],group_id+" no exact QCF word "+key)
            require(review["selected_arabic_text"] is None
                    and review["selected_length_words"] is None
                    and review["editorial_status"]=="UNREVIEWED"
                    and review["mnemonic_approval"] is False
                    and review["runtime_enabled"] is False,
                    group_id+" lexical review became a live cue")
            checked+=1
            candidates.append(key)
    require(len(candidates)==len(set(candidates)),"duplicated cross-group candidate")
    require("50:15:1" in candidates and "50:45:1" in candidates
            and "50:19:1" in candidates and "50:27:1" in candidates,
            "missing clearly observed intra-group transitions")
    require(len(candidates)>len(groups),"one-key-per-group quota inadvertently imposed")
    return checked

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--negative-tests", action="store_true")
    opts=parser.parse_args()
    evidence=json.loads(DATA.read_text(encoding="utf-8"))
    source=json.loads(BOXES.read_text(encoding="utf-8"))
    require(source["schema"] == "quran-haafidh-word-boxes-v1",
            "incorrect QCF source format")
    word_pages={}
    for p in ("518","519","520"):
        lines=source["pages"][p]
        word_pages[p]={row[0] for row in lines}
        require(len(word_pages[p])==len(lines), "QCF repeated keys on page "+p)
    count=verify(evidence,word_pages)
    if opts.negative_tests:
        altered=json.loads(json.dumps(evidence))
        altered["group_entries"][2]["review_start_word_keys"][1]["mnemonic_approval"]=True
        try:verify(altered,word_pages)
        except ValueError:pass
        else:raise AssertionError("fake approval passed review gate")
        altered=json.loads(json.dumps(evidence))
        altered["group_entries"][2]["review_start_word_keys"][1]["key"]="50:99:1"
        try:verify(altered,word_pages)
        except ValueError:pass
        else:raise AssertionError("wrong verse passed QCF gate")
        altered=json.loads(json.dumps(evidence))
        altered["group_entries"][7]["verified_qcf_page"]=518
        try:verify(altered,word_pages)
        except ValueError:pass
        else:raise AssertionError("false Mushaf page passed")
        altered=json.loads(json.dumps(evidence))
        altered["group_entries"].pop()
        try:verify(altered,word_pages)
        except ValueError:pass
        else:raise AssertionError("lost Qaf group passed")
        print("PASS: 4 source/QCF negative controls rejected")
    print(f"PASS Qaf J2: 8/8 source-linked Ibn Kathir groups, "
          f"{count} verse-start review positions exist in exact QCF geometry; "
          "zero approved/activated cue words; no APK")

if __name__=="__main__":
    main()
