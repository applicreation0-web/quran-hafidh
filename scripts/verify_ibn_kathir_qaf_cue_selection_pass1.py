#!/usr/bin/env python3
"""J2 pass-1 Quran.com context selections: fail-closed QCF span verification.

These are PRESELECTED Quranic word positions, not semantically approved mnemonics.
The original source snippets are documentary references, not app titles.
"""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/"docs/ibn_kathir_qaf_cue_selection_pass1.json"
EVIDENCE=ROOT/"docs/ibn_kathir_qaf_qurancom_editorial_evidence.json"
GEOMETRY=ROOT/"hifz-app/src/main/word-source/quran-ws-v1.1.2/word-boxes-451-604.json"
EXPECTED={518,519,520}
def check(condition,message):
    if not condition:raise RuntimeError("J2 cue first-pass NO GO: "+message)
def verify(main,review,pages):
    check(main.get("schema")=="IK_QAF_SEMANTIC_CUE_SELECTION_PASS1_V1"
          and main.get("approved")==0 and main.get("runtime_ready") is False,
          "premature semantic approval")
    check(main.get("group_count")==8 and main.get("first_pass_candidate_count")==14,
          "unreviewed group/cue change")
    original={x["id"]:{r["key"] for r in x["review_start_word_keys"]}
              for x in review["group_entries"]}
    check(len(original)==8,"missing source groups")
    seen=set()
    counts={}
    for cue in main["cues"]:
        group=cue["source_group_id"]
        key=cue["start_word_key"]
        page=cue["page"]
        count=cue["proposed_word_count"]
        check(key in original.get(group,set()) and key not in seen,
              "unverified/duplicated source key "+key)
        seen.add(key)
        check(page in EXPECTED and 1<=count<=5,"bad QCF span or page")
        words=pages[str(page)]
        positions=[i for i,row in enumerate(words) if row[0]==key]
        check(len(positions)==1,"missing/ambiguous first word "+key)
        i=positions[0]
        check(i+count<=len(words),"QCF cue exits physical page "+key)
        ayah=key.split(":")[1]
        # Selecting initial words only: do not pass word boxes from another verse
        check(all(row[0].split(":")[0:2]==["50",ayah]
                  for row in words[i:i+count]),
              "selected preview extends outside its source ayah "+key)
        check([row[0] for row in words[i:i+count]]==
              [f"50:{ayah}:{word}" for word in range(1,count+1)],
              "noncontiguous canonical QCF word IDs "+key)
        check(cue["independent_rarity_check"] is False
              and cue["independent_semantic_approval"] is False
              and cue["original_arabic_glyph_match_reviewed"] is False
              and cue["qcf_word_boxes_verified_by_ci"] is False
              and cue["status"]=="PENDING_CONTRADICTORY_REVIEW"
              and cue["runtime_enabled"] is False,
              "staging data masquerades as approved")
        check(cue["quran_com_display_preview"].strip()
              and cue["reason_for_candidate_fr"].strip()
              and cue["review_risk_fr"].strip(),
              "undocumented preselection "+key)
        counts[group]=counts.get(group,0)+1
    check(seen==set.union(*original.values()),"source review candidates omitted/added")
    check(set(counts)==set(original) and any(n>1 for n in counts.values()),
          "one-cue-per-group quota imposed")
    check(len(seen)==14,"wrong first-pass candidate count")
    return len(seen)
def main():
    main=json.loads(DATA.read_text(encoding="utf-8"))
    review=json.loads(EVIDENCE.read_text(encoding="utf-8"))
    raw=json.loads(GEOMETRY.read_text(encoding="utf-8"))
    check(raw["schema"]=="quran-haafidh-word-boxes-v1","unexpected geometry")
    n=verify(main,review,raw["pages"])
    # Test the gate: false activation and shifted word both MUST fail.
    bad=json.loads(json.dumps(main))
    bad["cues"][1]["runtime_enabled"]=True
    try:verify(bad,review,raw["pages"])
    except RuntimeError:pass
    else:raise AssertionError("false runtime flag passed")
    bad=json.loads(json.dumps(main))
    bad["cues"][1]["proposed_word_count"]=50
    try:verify(bad,review,raw["pages"])
    except RuntimeError:pass
    else:raise AssertionError("fake QCF word span passed")
    print(f"PASS J2 FIRST PASS: {n} contextual candidate locations, 8 source groups,"
          " exact contiguous QCF word keys; zero approvals/activation; NO APK")
if __name__=="__main__":main()
