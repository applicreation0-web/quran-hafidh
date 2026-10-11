#!/usr/bin/env python3
"""READ-ONLY independent verifier for the PRIVATE Quran.com 1903-row Work ZIP.

Usage: python3 scripts/verify_qurancom_private_title_payload.py /path/to/unzipped/private
Runs without internet; the dataset is NEVER committed to the public repo.
"""
import argparse
import base64
import csv
import hashlib
import json
import pathlib
import re
import zipfile
import zlib

ROOT=pathlib.Path(__file__).resolve().parents[1]
INDEX=ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirGroupIndex.java"
EXPECTED_BOUNDARY_SHA="deffec6e775965d7b1f717a63fc4d0d60c1933540aacefc13e1333d30c49be62"
EXPECTED_SOURCE_SHA="8b07799cbe02a7f907be84bbff2b09f95e19a319dffcf3a5458fd8365fa755b1"
EXPECTED_STATUS={"à contrôler":1689,"absent":163,"discordant":51}

def canonical_groups():
    text=INDEX.read_text(encoding="utf8")
    b64=re.search(r'BOUNDARIES_ZLIB_BASE64\s*=\s*"([^"]+)"',text)
    digest=re.search(r'ENDPOINTS_SHA256\s*=\s*"([a-f0-9]{64})"',text)
    assert b64 and digest, "Pinned source index changed"
    data=zlib.decompress(base64.b64decode(b64.group(1)))
    assert hashlib.sha256(data).hexdigest()==EXPECTED_BOUNDARY_SHA==digest.group(1)
    result={}
    verse_count=0
    for line in data.decode("utf8").splitlines():
        surah_str,ends=line.split(":")
        surah=int(surah_str)
        start=1
        for end in map(int,ends.split(",")):
            key=f"IKEN{surah:03d}_{start:03d}_{end:03d}"
            assert key not in result
            result[key]=(surah,start,end)
            verse_count+=end-start+1
            start=end+1
    assert len(result)==1903 and verse_count==6236
    return result

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("private_dir",type=pathlib.Path)
    args=parser.parse_args()
    folder=args.private_dir
    srczip=folder/"SOURCE_QURANCOM_H2_114_SOURATES.zip"
    assert hashlib.sha256(srczip.read_bytes()).hexdigest()==EXPECTED_SOURCE_SHA, "Wrong Quran.com ZIP"
    groups=canonical_groups()
    with zipfile.ZipFile(srczip) as source:
        manifest=json.loads(source.read("SOURCE_ET_CONTROLES.json"))
        assert manifest["surahs_checked"]==114 and manifest["ayah_records_checked"]==6236
        with source.open("QURAN_COM_IBN_KATHIR_TITRES_ANGLAIS_ORIGINAUX.csv") as stream:
            raw=list(csv.DictReader(__import__("io").TextIOWrapper(stream,encoding="utf-8-sig")))
    assert len(raw)==2951, "Unmatched raw Quran.com record count"
    exact_by_group={key:[] for key in groups}
    by_ayah={}
    for key,(s,a,b) in groups.items():
        for ayah in range(a,b+1):by_ayah[s,ayah]=key
    for r in raw:
        key=by_ayah[int(r["surah"]),int(r["ayah"])]
        assert r["quran_com_page"].startswith("https://quran.com/")
        assert r["quran_com_api_source"].startswith("https://api.quran.com/api/v4/tafsirs/169/")
        exact_by_group[key].append(r["quran_com_heading_exact"])
    table=folder/"IBN_KATHIR_QURANCOM_TITRES_1903.csv"
    with table.open(encoding="utf-8-sig",newline="") as stream:
        rows=list(csv.DictReader(stream))
    assert len(rows)==1903
    found=set()
    status={}
    staged={}
    for r in rows:
        key=r["identifiant"]
        assert key in groups and key not in found, "Duplicate/unknown group "+key
        found.add(key)
        assert (int(r["sourate"]),int(r["verset_debut"]),int(r["verset_fin"]))==groups[key]
        source_titles=json.loads(r["titres_anglais_exacts"])
        assert source_titles==exact_by_group[key], "Quran.com source mismatch "+key
        chosen=json.loads(r["titres_eligibles_provisoires"])
        # Exact source match OR explicit substring with documentary split flag.
        for title in chosen:
            if title not in source_titles:
                assert "QURANCOM_H2_SPLIT_EDITORIAL_REVIEW" in r["anomalies"]
                assert any(candidate.startswith(title+" ") for candidate in source_titles)
            assert 1<=len(title)<=145 and title==title.strip(), "Non-original or oversized heading"
        state=r["statut"]
        assert state in EXPECTED_STATUS
        status[state]=status.get(state,0)+1
        if state=="à contrôler":
            assert chosen, "Staged group missing headings"
            staged[key]=chosen
        else:
            assert not chosen, "Unapproved group has runtime title"
    assert found==set(groups) and status==EXPECTED_STATUS, str(status)

    chunks=folder/"IBN_KATHIR_QURANCOM_PRIVATE_ASSETS/hifz-app/src/main/assets/ibn_kathir_qurancom_private"
    restored={}
    for start in range(1,115,15):
        end=min(114,start+14)
        path=chunks/f"chunk-{start:03d}-{end:03d}.json"
        chunk=json.loads(path.read_text(encoding="utf8"))
        assert chunk["schema"]=="IK_QURANCOM_PRIVATE_V1"
        assert chunk["source_resource_id"]==169 and chunk["range"]==[start,end]
        for key,title_list in chunk["groups"].items():
            assert key not in restored and start<=groups[key][0]<=end
            restored[key]=title_list
    assert staged==restored, "Quran.com staging chunks differ from CSV"
    import subprocess
    tracked=subprocess.check_output(
        ["git","ls-files","--","hifz-app/src/main/assets/ibn_kathir_qurancom_private"],
        cwd=ROOT,text=True).strip()
    assert not tracked, "Private content is tracked in the public repository"
    assert "The Discussion of the Individual Letters" in staged["IKEN002_001_001"]
    assert "IKEN114_001_006" not in staged, "Do not fabricate An-Nas title"
    print("PASS: 1903 exact canonical groups, 6236 verses, 2951 Quran.com h2 records;")
    print("      staged=1689, absent=163, discordant=51; all 8 private chunks verified.")
    print("WARNING: editorial h2 validation, legal offline sync, and real BOOX pixels NOT YET VERIFIED.")
if __name__=="__main__":
    main()
