#!/usr/bin/env python3
"""Cross-check candidate Ibn Kathir retrieval keys with REAL Quran Arabic and KFQC geometry.

Everything produced here is documentary/audit-only, NEVER runtime-approved.
Shortest distinct verse prefixes are suggestions, not proof of mnemonic relevance.
No Al-Munir segmentation, invented Quran word, original tafsir text or AI heading.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import re
import unicodedata
from pathlib import Path
from build_ibn_kathir_amorce_review import CHUNKS, WORDS, build, require, read_groups, load_verified_verse_text

QURAN_VERSES = 6236
TASHKIL = re.compile(r"[\u064b-\u065f\u0670\u06d6-\u06ed]")
FORMAT = re.compile(r"^\s*(\d+)\|(\d+)\|(.*)$")

def normalized_word(raw):
    cleaned = unicodedata.normalize("NFC", TASHKIL.sub("", raw))
    return cleaned.replace("ٱ", "ا").replace("أ", "ا").replace("إ", "ا")

def load_verses(path, groups):
    verses, _ = load_verified_verse_text(path, groups)
    return {key: value.split() for key, value in verses.items()}

def box_word_counts(folder):
    by_verse = collections.Counter()
    page_start_words = {}
    for chunk in CHUNKS:
        doc = json.loads((folder/f"word-boxes-{chunk}.json").read_text(encoding="utf-8"))
        for raw_page, entries in doc["pages"].items():
            page = int(raw_page)
            for row in entries:
                s,a,w = map(int, row[0].split(":"))
                by_verse[(s,a)] += 1
                if w == 1:
                    page_start_words[(s,a)] = page
    require(sum(by_verse.values()) == WORDS, "different word-count source")
    return by_verse, page_start_words

def shortest_distinct_prefix(tokens, rivals):
    """No artificial 1–6 word limit; return distinct prefix or null."""
    for length in range(1,len(tokens)+1):
        prefix=tuple(tokens[:length])
        if all(tuple(other[:length]) != prefix for other in rivals):
            return length
    return None

def draft(index_java, word_dir, tanzil_path):
    validated = build(index_java, word_dir, tanzil_path)
    ayahs = load_verses(tanzil_path, read_groups(index_java)[0])
    box_counts, page_starts = box_word_counts(word_dir)
    wrong=[]
    for verse,words in ayahs.items():
        n=box_counts.get(verse,0)
        if len(words) != n:
            wrong.append({"ref":f"{verse[0]}:{verse[1]}","text_words":len(words),"geometry_words":n})
    by_surah=collections.defaultdict(list)
    starts=[]
    for c in validated["candidates"]:
        surah, start, end = (c["surah"],c["start_ayah"],c["end_ayah"])
        tokens = []
        for a in range(start, end+1):
            require((surah,a) in ayahs, f"missing text for {surah}:{a}")
            tokens.extend(ayahs[(surah,a)])
        assert tokens
        entry = (c, tokens, tuple(normalized_word(w) for w in tokens))
        starts.append(entry)
        by_surah[surah].append(entry)
    for c,tokens,normalized in starts:
        surah=c["surah"]
        rivals=[e[2] for e in by_surah[surah] if e[0]["id"]!=c["id"]]
        candidate=shortest_distinct_prefix(normalized,rivals)
        # All words in the suggested prefix must match pinned QCF word indices;
        # a text/box discrepancy in any participating verse prohibits approval.
        count_at_first_page=0
        for ayah in range(c["start_ayah"],c["end_ayah"]+1):
            if page_starts.get((surah,ayah),c["start_page"]) != c["start_page"]:
                break
            count_at_first_page+=box_counts[(surah,ayah)]
        c["suggested_distinct_prefix_words"]=candidate
        c["suggested_prefix_quran_arabic"] = " ".join(tokens[:candidate]) if candidate else None
        c["prefix_status"]="DRAFT_ONLY_NOT_SEMANTICALLY_APPROVED"
        c["geometry_and_text_word_counts_agree"]=not bool(wrong)
        c["one_page_word_capacity"]=count_at_first_page
        c["preview_exact_on_one_page"]=bool(candidate and candidate <= count_at_first_page and not wrong)
        c["candidate_anchor_word_count"]=None  # NEVER activate a draft!
        c["selection_status"]="NOT_REVIEWED"
        c["review_required"]="Semantic trigger, rarity, contextual cue and KFQC exact-word confirmation"
    validated["quran_text_file_sha256"]=hashlib.sha256(tanzil_path.read_bytes()).hexdigest()
    validated["tanzil_geometry_token_count_discrepancies"]=len(wrong)
    validated["sample_token_discrepancies"]=wrong[:12]
    validated["unique_prefixes_within_surah"]=sum(c["suggested_distinct_prefix_words"] is not None for c in validated["candidates"])
    validated["prefixes_fit_in_one_page"]=sum(c["preview_exact_on_one_page"] for c in validated["candidates"])
    validated["approved_semantic_amorces"]=0
    validated["runtime_ready"]=False
    return validated

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("index_java",type=Path)
    parser.add_argument("word_dir",type=Path)
    parser.add_argument("tanzil",type=Path)
    parser.add_argument("output",type=Path)
    args=parser.parse_args()
    data=draft(args.index_java,args.word_dir,args.tanzil)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    assert data["groups"]==1903 and len(data["candidates"])==1903
    assert data["approved_semantic_amorces"]==0 and not data["runtime_ready"]
    print(f"IBN KATHIR CUE AUDIT: 1903 groups; "
          f"{data['unique_prefixes_within_surah']} distinct-prefix proposals; "
          f"{data['prefixes_fit_in_one_page']} proposals fit same KFQC page; "
          f"{data['tanzil_geometry_token_count_discrepancies']} verse text/box count anomalies; "
          "ZERO SEMANTIC APPROVAL. No runtime use.")
    for item in data["sample_token_discrepancies"][:5]:
        print("REQUIRES REVIEW:",item)

if __name__=="__main__":
    main()
