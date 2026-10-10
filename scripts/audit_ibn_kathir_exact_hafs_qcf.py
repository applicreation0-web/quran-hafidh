#!/usr/bin/env python3
"""CI-only Quran.ws Hafs ↔ frozen KFQC/Quranic grouping verification.

DO NOT embed downloaded text in APK or approve semantic keys. No font downloads.
"""
import argparse, hashlib, json, unicodedata, urllib.request
from collections import defaultdict
from pathlib import Path
from build_ibn_kathir_amorce_review import CHUNKS,read_groups,require

URL="https://text.quran.ws/files/mushaf/hafs.json"
SHA="9b9eb07ff5cff144bf964400924e593d97115e4b937db3c61783f782a5168075"

def norm(text):
    return "".join(x for x in unicodedata.normalize("NFD",text)
                   if not unicodedata.combining(x)).replace("ٱ","ا")

def run(index,source,output):
    request=urllib.request.Request(URL,headers={"User-Agent":"Quran-Haafidh-documentary-audit/1.0"})
    with urllib.request.urlopen(request,timeout=50) as stream:
        raw=stream.read(6_000_000)
    require(hashlib.sha256(raw).hexdigest()==SHA,"external Quran text SHA-256 mismatch")
    doc=json.loads(raw)
    words=doc["words"]
    require(doc["format"]=="quran-mushaf" and doc["mushaf"]["key"]=="hafs","wrong source edition")
    require(doc["counting"]["system"]=="kufi" and doc["counting"]["ayah_count"]==6236,"wrong 6236-ayah count")
    require(len(words)==77432,"mismatch in Quran word count")
    pages={}
    for chunk in CHUNKS:
        material=json.loads((source/f"word-boxes-{chunk}.json").read_text(encoding="utf-8"))
        for label,rows in material["pages"].items():
            p=int(label)
            require(p not in pages,"duplicated KFQC page")
            pages[p]=rows
    require(set(pages)==set(range(1,605)),"non-complete 604 pages")
    qcf=[]
    page_offsets=[]
    for page in range(1,605):
        page_offsets.append(len(qcf))
        qcf.extend(tuple(map(int,row[0].split(":"))) for row in pages[page])
    page_offsets.append(len(qcf))
    require(len(qcf)==77432 and doc["page_starts"]==page_offsets[:604],"word/page alignment failed")
    offsets={key:index for index,key in enumerate(qcf) if key[2]==1}
    require(len(offsets)==6236,"missing Quran first word")
    metadata=doc["surahs"]
    require(len(metadata)==114,"wrong surah count")
    canonical=[]
    for chapter,meta in enumerate(metadata,1):
        require(meta["number"]==chapter and isinstance(meta["first_ayah"],int),"surah numbering mismatch")
        canonical.extend((chapter,a) for a in range(1,meta["ayah_count"]+1))
    ayah_offsets=list(doc["ayah_starts"])
    require(len(canonical)==6236 and len(ayah_offsets)==6236,"invalid ayah offsets")
    ayah_offsets.append(77432)
    for k,ref in enumerate(canonical):
        first=ayah_offsets[k]
        last=ayah_offsets[k+1]
        require(offsets[ref]==first and last>first,"verse boundary mismatch "+str(ref))
        require(all(qcf[p]==(ref[0],ref[1],p-first+1) for p in range(first,last)),
                "QCF word identity differs "+str(ref))
    groups,boundary_sha=read_groups(index)
    require(len(groups)==1903,"incomplete Ibn Kathir index")
    by_surah=defaultdict(list)
    for surah,start,end in groups:
        first=offsets[(surah,start)]
        meta=metadata[surah-1]
        end_i=ayah_offsets[meta["first_ayah"]+end]
        page=next(p for p in range(1,605) if page_offsets[p-1]<=first<page_offsets[p])
        room=min(end_i,page_offsets[page])-first
        require(room>0,"group starts outside source page")
        row={"id":f"IKEN{surah:03d}_{start:03d}_{end:03d}",
             "surah":surah,"start_ayah":start,"end_ayah":end,
             "quran_start_word_key":f"{surah}:{start}:1","qcf_page":page,
             "exact_source_first_word":words[first],
             "review":"NOT_SEMANTICALLY_APPROVED","runtime_enabled":False}
        by_surah[surah].append((row,tuple(norm(w) for w in words[first:end_i]),words[first:end_i],room))
    output_rows=[]
    for surah,entries in by_surah.items():
        for row,normalized,original,room in entries:
            others=[other[1] for other in entries if other[0]["id"]!=row["id"]]
            n=None
            for length in range(1,min(len(normalized),room)+1):
                if all(other[:length]!=normalized[:length] for other in others):
                    n=length
                    break
            row["distinctive_prefix_draft_words"]=n
            row["exact_quran_preview"]= " ".join(original[:n]) if n else None
            row["draft_not_approved"]=True
            output_rows.append(row)
    good=sum(x["distinctive_prefix_draft_words"] is not None for x in output_rows)
    report={"source_url":URL,"source_sha256":SHA,"boundary_sha256":boundary_sha,
        "ayahs_exactly_aligned":6236,"pages_exactly_aligned":604,"words_exactly_aligned":77432,
        "ibn_kathir_groups":1903,"draft_one_page_distinct_keys":good,
        "unresolved":1903-good,"approved_semantic_amorces":0,
        "runtime_ready":False,"copyright":"KFGQPC rights check needed before reproduction",
        "candidates":output_rows}
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,ensure_ascii=False,indent=1)+"\n",encoding="utf-8")
    print("PASS: exact Quran-to-QCF join, 6236 ayahs, 604 pages, 77432 words.")
    print(f"IBN KATHIR: {good}/1903 same-page uniquely identifiable lexical drafts; 0 editorial approvals; NO APK.")
    return report

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("group_index",type=Path)
    p.add_argument("word_geometry",type=Path)
    p.add_argument("output",type=Path)
    a=p.parse_args()
    run(a.group_index,a.word_geometry,a.output)
