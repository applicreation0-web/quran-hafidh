#!/usr/bin/env python3
"""Independent reproducible audit of pinned Ibn Kathir source-heading assets.

Only verified editorial titles from Qaf/Najm are declared editorially verified.
Other records are literal heading CANDIDATES extracted from the English source
and must pass independent source-context and copyright/rights review before
the next debug is approved.

Optional --source-dir: 114 pinned original JSON files, to confirm exact
verbatim text and document-group boundaries without internet at runtime.
"""
import argparse
import base64
import csv
import hashlib
import json
import pathlib
import re
import zlib

ROOT=pathlib.Path(__file__).resolve().parents[1]
DIR=ROOT/"hifz-app/src/main/assets/ibn_kathir_headings"
INDEX=ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirGroupIndex.java"
SHA="eb82bb6294efe30ad5c135c03b1864afaa70e855"

def canonical_groups():
    src=INDEX.read_text(encoding="utf8")
    packed=re.search(r'BOUNDARIES_ZLIB_BASE64\s*=\s*"([^"]+)"',src)
    expected=re.search(r'ENDPOINTS_SHA256\s*=\s*"([0-9a-f]{64})"',src)
    assert packed and expected, "Canonical index was changed"
    raw=zlib.decompress(base64.b64decode(packed.group(1)))
    assert hashlib.sha256(raw).hexdigest()==expected.group(1)
    rows=[]
    for line in raw.decode("utf8").splitlines():
        surah_str,ends=line.split(":",1)
        surah=int(surah_str)
        start=1
        for end in map(int,ends.split(",")):
            assert end>=start, (surah,start,end)
            rows.append((f"IKEN{surah:03d}_{start:03d}_{end:03d}",surah,start,end))
            start=end+1
    assert len(rows)==1903
    assert sum(end-start+1 for _,_,start,end in rows)==6236
    return rows

def verify(raws=None):
    groups=canonical_groups()
    canonical={id for id,_,_,_ in groups}
    titles={}
    covered_sura=set()
    for first in range(1,115,15):
        last=min(114,first+14)
        path=DIR/f"chunk-{first:03d}-{last:03d}.json"
        data=json.loads(path.read_text(encoding="utf8"))
        assert data["schema"]=="IK_TITLES_SOURCE_EXTRACT_V1"
        assert data["source_sha"]==SHA
        assert data["range"]==[first,last]
        for key,items in data["groups"].items():
            assert key in canonical and key not in titles,("Unknown or duplicate range",key)
            surah=int(key[4:7])
            assert first<=surah<=last
            assert 1<=len(items)<=3
            for title in items:
                assert title==title.strip()
                assert 9<=len(title)<=145 and "\n" not in title
                assert not re.search(r'[\u0600-\u06ff\d]',title)
                assert "English heading not yet verified" not in title
            titles[key]=items
            covered_sura.add(surah)
    assert len(titles)==1415,("Unexpected source heading coverage",len(titles))
    checked=(ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirOfflineTitles.java").read_text(encoding="utf8")
    assert 'group.surah==2 && group.startAyah==1 && group.endAyah==1' in checked
    assert 'The Discussion of the Individual Letters' in checked
    missing=canonical-set(titles)
    assert len(missing)==488
    if raws:
        for key,surah,start,end in groups:
            raw=pathlib.Path(raws)/f"{surah}.json"
            if not raw.exists():raise FileNotFoundError(raw)
            verses=json.loads(raw.read_text(encoding="utf8"))
            assert len(verses)>=end and verses[start-1]["ayah"]==start
            first_text=verses[start-1]["text"]
            assert end==len(verses) or verses[end]["text"]!=first_text
            for verse in verses[start-1:end]:
                assert verse["text"]==first_text, (key,"not an exact commentary group")
            if key in titles:
                individual_lines={s.strip() for s in first_text.splitlines()}
                for title in titles[key]:
                    assert title in individual_lines,(key,"title is NOT a verbatim source line",title)
    return groups,titles,covered_sura

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--source-dir",help="Pinned upstream JSON 1.json..114.json")
    parser.add_argument("--out",help="Write per-group Work audit CSV outside GitHub repo")
    args=parser.parse_args()
    groups,titles,sura=verify(args.source_dir)
    if args.out:
        with open(args.out,"w",encoding="utf8",newline="") as f:
            writer=csv.writer(f)
            writer.writerow(["group","surah","start_ayah","end_ayah","source_heading_status",
                             "heading_count","candidate_heading_1","editorial_status","rights_status"])
            for key,s,start,end in groups:
                h=titles.get(key,[])
                writer.writerow([key,s,start,end,"SOURCE_LINE" if h else "NO_VERIFIABLE_HEADING",
                                 len(h),h[0] if h else "","PENDING_WORK" if h else "NOT_APPLICABLE",
                                 "UNVERIFIED_DARUSSALAM_ENGLISH_RIGHTS" if h else ""])
    print(f"PASS: 114 surahs; canonical groups={len(groups)}; source heading candidates={len(titles)};"
          f" no heading candidate={len(groups)-len(titles)}; groups with content span={len(sura)} surahs;"
          " 17 source-checked checkpoints (Qaf/Najm + Al-Baqarah 2:1) preserved; rights NOT CLEARED")

if __name__=="__main__":
    main()
