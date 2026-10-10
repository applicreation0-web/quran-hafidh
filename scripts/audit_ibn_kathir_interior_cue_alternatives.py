#!/usr/bin/env python3
"""Independent source/QCF check of real mid-verse alternatives for Qaf/Najm.

Crucial: measures INTERNAL Quran word positions, not just verse-opening words.
The source English Tafsir is linked, never copied into Android. NO APPROVAL.
"""
from pathlib import Path
import json
from analyze_ibn_kathir_qaf_najm_global_rarity import (
    derive_inventory, derive_occurrence_index, find_hits,
    normalized, ROOT, fail_unless, source_reference
)

PROPOSALS=ROOT/"docs/ibn_kathir_qaf_najm_interior_alternatives.json"
GROUPS={
"IKEN050_012_015":(50,12,15),
"IKEN050_016_022":(50,16,22),
"IKEN050_030_035":(50,30,35),
"IKEN050_036_040":(50,36,40),
"IKEN050_041_045":(50,41,45),
"IKEN053_027_030":(53,27,30),
"IKEN053_031_032":(53,31,32)}

def run(hafs,out):
    words,keys,idx,offsets,ends=derive_inventory(hafs)
    nwords,byfirst=derive_occurrence_index(words)
    doc=json.loads(PROPOSALS.read_text(encoding="utf-8"))
    fail_unless(doc.get("schema")=="IK_J2_QAF_NAJM_INTERIOR_ALTERNATIVES_V1"
       and doc.get("approved")==0 and doc.get("runtime_ready") is False
       and doc.get("independent_semantic_review") is False,
       "not staging-only review")
    srcs=doc["suggestions"]
    fail_unless(len(srcs)==9 and len({s["start_word_key"] for s in srcs})==9,
      "missing/duplicated source alternatives")
    real=[]
    for s in srcs:
        key=s["start_word_key"]
        group=s["source_group_id"]
        chapter,verse,w=source_reference(key)
        fail_unless(group in GROUPS and GROUPS[group][0]==chapter
                    and GROUPS[group][1]<=verse<=GROUPS[group][2],
            key+" crosses original source group")
        fail_unless(s["word_count"]>=1 and key in idx,
                    key+" missing QCF coordinate")
        at=idx[key]
        n=s["word_count"]
        fail_unless(idx[f"{chapter}:{verse}:1"]==offsets[(chapter,verse)]
                and at+n<=ends[offsets[(chapter,verse)]],
             key+" candidate spills beyond actual verse")
        ids=[f"{chapter}:{verse}:{i}" for i in range(w,w+n)]
        fail_unless(keys[at:at+n]==ids,key+" noncontiguous QCF words")
        page_source=(518 if chapter==50 and verse<=15
                     else 519 if chapter==50 and verse<=35
                     else 520 if chapter==50
                     else 527)
        fail_unless(s["page"]==page_source,key+" wrong physical page")
        fail_unless(s["approved"] is False and
                    s["runtime_enabled"] is False and
                    s["editorial_status"]=="ALTERNATIVE_TO_REVIEW_NOT_APPROVED",
                    "attempted activation")
        phrase=tuple(nwords[at:at+n])
        # Compare only within the same verse, never across an ayah break.
        hits=[h for h in find_hits(phrase,nwords,byfirst)
              if h+n<=len(keys) and
              keys[h].split(":")[:2]==keys[h+n-1].split(":")[:2]]
        real.append({
         "source_group_id":group,
         "first_qcf_word_key":key,
         "last_qcf_word_key":ids[-1],
         "page":s["page"],
         "exact_hafs_source_words":" ".join(words[at:at+n]),
         "word_count":n,
         "whole_quran_phrase_occurrences":len(hits),
         "example_other_qcf_word_keys":[keys[h] for h in hits if h!=at][:12],
         "semantic_rationale_for_review_only":s["semantic_review_reason_fr"],
         "tafsir_source":s["primary_tafsir_reference"],
         "status":"LEXICAL_EVIDENCE_ONLY",
         "editorially_approved":False,
         "runtime_enabled":False
        })
    fail_unless(len(real)==9,"not nine alternatives")
    require_pair={x["first_qcf_word_key"] for x in real}
    fail_unless({"50:39:5","50:36:7","53:31:8","53:27:6"}.issubset(require_pair),
                "critical mid-verse alternatives missing")
    output={
      "schema":"IK_J2_INTERIOR_ALTERNATIVE_RARITY_V1",
      "reviewed":len(real),"approved":0,"runtime_ready":False,
      "entries":real
    }
    out.mkdir(parents=True,exist_ok=True)
    (out/"ik-j2-interior-cue-rarity.json").write_text(
      json.dumps(output,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    lines=["# J2 — propositions internes aux versets Qāf et An-Najm",
       "","**Candidats à discuter, zéro amorce approuvée.**","",
       "| Référence QCF de départ | Extrait Hafs vérifié | Répétitions dans le Coran | Contexte |",
       "|---|---|---:|---|"]
    for e in real:
        lines.append("| "+e["first_qcf_word_key"]+" | "+e["exact_hafs_source_words"]+
            " | "+str(e["whole_quran_phrase_occurrences"])+" | "+
            e["semantic_rationale_for_review_only"]+" |")
    lines+=["","Chaque position est un mot QCF exact et chaque suite de mots reste à l'intérieur du verset.",
            "Une phrase rare n'est pas automatiquement une bonne amorce : validation contradictoire obligatoire.",
            "Aucun titre Tafsir inventé, aucune donnée éditoriale activée, aucun changement Hifz/Quiz."]
    (out/"ik-j2-interior-cue-rarity.md").write_text("\n".join(lines)+"\n",encoding="utf-8")
    print(f"PASS J2 interior alternatives: {len(real)} QCF-verified source spans; "
          "whole-Quran occurrence counts computed; 0 active keys, no APK")

if __name__=="__main__":
    import argparse
    p=argparse.ArgumentParser()
    p.add_argument("pinned_hafs",type=Path)
    p.add_argument("out",type=Path)
    args=p.parse_args()
    run(args.pinned_hafs,args.out)
