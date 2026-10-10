#!/usr/bin/env python3
"""J2 round-1 editorial PREFERENCES, anchored to immutable Hafs/QCF evidence.

This stage makes explicit editorial tradeoffs; never automatically APPROVES a
cue. Reviewer's reasoning != the original English heading of Ibn Kathir.
"""
from pathlib import Path
import copy
import json
from analyze_ibn_kathir_qaf_najm_global_rarity import (
  ROOT,derive_inventory,derive_occurrence_index,find_hits,
  source_reference,fail_unless
)
import importlib.util

MANIFEST=ROOT/"docs/ibn_kathir_qaf_najm_editorial_preferences_round1.json"
INDEX=ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirGroupIndex.java"

def verify(manifest, hafs, old, interior):
    fail_unless(manifest.get("schema")=="IK_J2_EDITORIAL_PREFERENCE_ROUND1_V1"
       and manifest.get("decision_scope")=="NON_RUNTIME_REVIEW_PREFERENCES"
       and manifest.get("approved")==0 and manifest.get("runtime_ready") is False
       and manifest.get("reviewed_positions")==30,
       "manifest is not a review-only round")
    fail_unless(old["approved"]==0 and old["runtime_ready"] is False
       and len(old["entries"])==30 and
       interior["approved"]==0 and interior["runtime_ready"] is False,
       "upstream stage was activated")
    words,keys,idx,offsets,ends=derive_inventory(hafs)
    normalized,byfirst=derive_occurrence_index(words)
    old_refs={e["key"]:e for e in old["entries"]}
    internal_refs={e["first_qcf_word_key"]:e for e in interior["entries"]}
    allowed=set(old_refs)|set(internal_refs)
    selected=[]
    seen=set()
    all_groups=set()
    for row in manifest["preferences"]:
        key=row["start_word_key"]
        group=row["source_group_id"]
        s,a,w=source_reference(key)
        count=row["word_count"]
        fail_unless(key in allowed and key not in seen and 1<=count<=30,
                    "missing, repeated or suspicious lexical recommendation "+key)
        seen.add(key)
        fail_unless(s in (50,53) and key in idx and row["action"] in
                    ("RETAIN","EXTEND","SHORTEN","MOVE_INSIDE_VERSE"),
                    "unsupported semantic selection "+key)
        # Source group proof is separate from presentation spelling.
        seg=group.split("_")
        fail_unless(len(seg)==3 and seg[0]==f"IKEN{s:03d}" and
                    int(seg[1])<=a<=int(seg[2]),
                    "requested cue outside frozen Ibn Kathir group "+key)
        first=offsets[(s,a)]
        at=idx[key]
        fail_unless(at>=first and at+count<=ends[first],
                    "exceeded Quran verse "+key)
        fail_unless(keys[at:at+count]==[f"{s}:{a}:{i}" for i in range(w,w+count)],
                    "noncontiguous QCF word positions "+key)
        fail_unless(row["approved"] is False and
                    row["runtime_enabled"] is False and
                    row["independent_second_reader_verification"]=="PENDING" and
                    row["qcf_glyph_identity_signoff"]=="PENDING" and
                    row["memory_recall_quality_test"]=="PENDING" and
                    row["editorial_status"]=="RECOMMENDED_FOR_CONTRADICTORY_REVIEW",
                    "editorial preference used as runtime approval "+key)
        phrase=tuple(normalized[at:at+count])
        hits=[pos for pos in find_hits(phrase,normalized,byfirst)
              if pos+count<=len(keys) and keys[pos].split(":")[:2]==
                 keys[pos+count-1].split(":")[:2]]
        fail_unless(at in hits,"self occurrence missing")
        selected.append({
          "source_group_id":group,"start_word_key":key,
          "last_word_key":f"{s}:{a}:{w+count-1}",
          "word_count":count,"source_words_exact":" ".join(words[at:at+count]),
          "whole_quran_occurrences":len(hits),
          "other_verse_examples":[keys[h] for h in hits if h!=at][:15],
          "decision":row["action"],"semantic_reason_fr":row["reason_fr"],
          "recommendation_status":"CONTEXTUAL_FIRST_PASS_ONLY",
          "approved":False,"runtime_enabled":False
        })
        all_groups.add(group)
    fail_unless(len(selected)==30 and len(all_groups)==16
        and len({x["start_word_key"] for x in selected})==30,
        "8 Qaf + 8 Najm groups not covered")
    require_names={"50:23:1","50:27:1","50:36:10","50:39:5","53:27:6","53:31:8"}
    fail_unless(require_names.issubset(seen),"critical semantic divergence silently erased")
    return {
      "schema":"IK_J2_SOURCE_GROUNDED_EDITORIAL_PREFERENCES_V1",
      "groups":len(all_groups),"positions":len(selected),
      "approved":0,"runtime_ready":False,
      "model_editorial_preferences_not_final_approvals":True,
      "entries":selected}

def main():
    import argparse
    p=argparse.ArgumentParser()
    p.add_argument("hafs",type=Path)
    p.add_argument("global_rarity",type=Path)
    p.add_argument("interior_rarity",type=Path)
    p.add_argument("output_folder",type=Path)
    a=p.parse_args()
    preference=json.loads(MANIFEST.read_text(encoding="utf-8"))
    global_doc=json.loads(a.global_rarity.read_text(encoding="utf-8"))
    interior=json.loads(a.interior_rarity.read_text(encoding="utf-8"))
    result=verify(preference,a.hafs,global_doc,interior)
    # Strong negative test: an attempted approval must fail closed.
    malicious=copy.deepcopy(preference)
    malicious["preferences"][0]["approved"]=True
    try:verify(malicious,a.hafs,global_doc,interior)
    except ValueError:pass
    else:raise AssertionError("illegally approved key was accepted")
    a.output_folder.mkdir(parents=True,exist_ok=True)
    (a.output_folder/"ik-j2-contextual-preferences-30.json").write_text(
      json.dumps(result,ensure_ascii=False,indent=1)+"\n",encoding="utf-8")
    lines=[
      "# J2 — 30 recommandations contextuelles Qāf et An-Najm",
      "",
      "**16 groupes couverts ; 30 positions coraniques choisies pour revue ; 0 approbation finale.**",
      "Le texte arabe ci-dessous est extrait de la source Hafs épinglée et vérifié par"
      " des clés QCF contiguës, sans création de versets ni de titres d'exégèse.",
      "",
      "| Groupe | Position | Extrait coranique | Répétitions dans le Coran | Décision de pré-revue |",
      "|---|---|---|---:|---|"
    ]
    for e in result["entries"]:
       lines.append(f"| {e['source_group_id']} | {e['start_word_key']} | "
         f"{e['source_words_exact']} | {e['whole_quran_occurrences']} | {e['decision']} |")
    lines+=["", "Une phrase répétée ailleurs peut être un meilleur déclic qu'une phrase",
            "lexicalement unique mais longue ou dépourvue de sens.",
            "Deux lecteurs indépendants et un test de rappel sur la page BOOX doivent",
            "valider les choix avant la migration de SemanticPassageRepository.",
            "Aucun moteur, Quiz, réglage ou APK touché.", ""]
    (a.output_folder/"ik-j2-contextual-preferences-30.md").write_text(
      "\n".join(lines),encoding="utf-8")
    print("PASS J2: 30 contextual preference decisions across 16 groups, "
          "genuine QCF/Hafs contiguous words; semantic approval ZERO; NO APK")
if __name__=="__main__":main()
