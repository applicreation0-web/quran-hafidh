#!/usr/bin/env python3
"""Source-only check of the eight Quran.com An-Najm groups, never a cue engine."""
import argparse
import base64
import hashlib
import json
import re
import zlib
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JSON=ROOT/"docs/ibn_kathir_najm_qurancom_editorial_evidence.json"
INDEX=ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirGroupIndex.java"
SOURCE=ROOT/"hifz-app/src/main/word-source/quran-ws-v1.1.2/word-boxes-451-604.json"
RANGES=[(1,4,526,526),(5,18,526,526),(19,26,526,526),
        (27,30,527,527),(31,32,527,527),(33,41,527,527),
        (42,55,527,528),(56,62,528,528)]

def require(ok, message):
    if not ok:raise ValueError("An-Najm Ibn Kathir audit: "+message)

def inspect(evidence,pages,index_ranges):
    require(evidence.get("schema")=="IK_NAJM_QURANCOM_EDITORIAL_EVIDENCE_V1"
            and evidence.get("surah")==53 and evidence.get("groups")==8
            and evidence.get("approved_keys")==0
            and evidence.get("runtime_ready") is False,"unapproved runtime or wrong schema")
    records=evidence["reviewed"]
    require(len(records)==8 and index_ranges==[(a,b) for a,b,_,_ in RANGES],
            "not the eight frozen source groups")
    seen=set()
    inside=0
    for (start,end,p0,p1),entry in zip(RANGES,records):
        identifier=f"IKEN053_{start:03d}_{end:03d}"
        require(entry["id"]==identifier
                and (entry["start_ayah"],entry["end_ayah"])==(start,end)
                and (entry["start_page"],entry["end_page"])==(p0,p1),
                "unexpected verse boundary/page")
        require(entry["quran_com_url"]==
                f"https://quran.com/an-najm/{start}/tafsirs/en-tafisr-ibn-kathir",
                "missing exact Quran.com reference")
        require(bool(entry["observed_original_heading_excerpt"])
                and entry["title_status"]=="READ_ON_QURANCOM_NOT_APPROVED_FOR_APP"
                and entry["candidate_status"]=="LOCATION_ONLY_NOT_APPROVED"
                and entry["rights_status"]=="NO_COMMENTARY_REPRODUCTION"
                and entry["runtime_enabled"] is False,identifier+" title status")
        require(entry["qcf_verse_start_candidates"],identifier+" candidates missing")
        for candidate in entry["qcf_verse_start_candidates"]:
            key=candidate["word_key"]
            surah,aya,word=[int(x) for x in key.split(":")]
            require(surah==53 and start<=aya<=end and word==1,
                    "invalid Quranic candidate reference")
            hits=[p for p in range(p0,p1+1) if key in pages[str(p)]]
            require(len(hits)==1,"QCF word reference absent or duplicate "+key)
            require(key not in seen,"duplicate candidate "+key)
            seen.add(key)
            require(candidate["exact_word_form"] is None
                    and candidate["selected_word_length"] is None
                    and candidate["start_word_status"]=="UNREVIEWED_MNEMONIC"
                    and candidate["live"] is False,
                    "draft illegally approved")
            inside+=1
    require("53:3:1" in seen and "53:38:1" in seen
            and "53:62:1" in seen and len(seen)>8,
            "the original multi-theme nature was flattened")
    return inside

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--negative-tests",action="store_true")
    args=ap.parse_args()
    raw=INDEX.read_text(encoding="utf-8")
    encoded=re.search(r'BOUNDARIES_ZLIB_BASE64\s*=\s*"([^"]+)"',raw)
    digest=re.search(r'ENDPOINTS_SHA256\s*=\s*"([^"]+)"',raw)
    require(encoded and digest,"missing verified source index")
    plain=zlib.decompress(base64.b64decode(encoded.group(1)))
    require(hashlib.sha256(plain).hexdigest()==digest.group(1),
            "mutated documentary source")
    lines=plain.decode("utf-8").splitlines()
    surah,ends=lines[52].split(":")
    require(surah=="53","source surah index drift")
    first=1
    grouped=[]
    for last in ends.split(","):
        final=int(last)
        grouped.append((first,final))
        first=final+1
    require(first==63,"incomplete An-Najm verse span")
    qcf=json.loads(SOURCE.read_text(encoding="utf-8"))["pages"]
    pages={str(p):{row[0] for row in qcf[str(p)]} for p in (526,527,528)}
    data=json.loads(JSON.read_text(encoding="utf-8"))
    total=inspect(data,pages,grouped)
    if args.negative_tests:
        tampered=json.loads(json.dumps(data))
        tampered["reviewed"][0]["observed_original_heading_excerpt"]=""
        try:inspect(tampered,pages,grouped)
        except ValueError:pass
        else:raise AssertionError("missing title evidence accepted")
        tampered=json.loads(json.dumps(data))
        tampered["reviewed"][6]["qcf_verse_start_candidates"][0]["live"]=True
        try:inspect(tampered,pages,grouped)
        except ValueError:pass
        else:raise AssertionError("unapproved runtime enabled")
        tampered=json.loads(json.dumps(data))
        tampered["reviewed"][7]["qcf_verse_start_candidates"][0]["word_key"]="53:99:1"
        try:inspect(tampered,pages,grouped)
        except ValueError:pass
        else:raise AssertionError("imaginary verse accepted")
        print("PASS Najm: three negative controls rejected")
    print(f"PASS Najm: eight Quran.com-linked groups, {total} genuine QCF "
          "verse-start candidate positions; 0 approved; Hifz/Quiz frozen; no APK")

if __name__=="__main__":main()
