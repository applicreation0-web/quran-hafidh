#!/usr/bin/env python3
"""J2: build source-linked editorial review cards (no approvals, no Android asset).

The 1,903 documentary boundaries come from the pinned Ibn Kathir index.
The Hafs lexical previews are CANDIDATES only. Neither is an editorial key.
"""
from __future__ import annotations
import argparse
import collections
import csv
import json
from pathlib import Path

GROUPS = 1903
SOURCE = "eb82bb6294efe30ad5c135c03b1864afaa70e855"
EXCEPTION = "IKEN002_047_047"
ORTHOGRAPHY = "IKEN015_006_009"

def require(test, why):
    if not test:
        raise ValueError("J2 source review queue FAIL: " + why)

def by_id(rows, label):
    result = {}
    for row in rows:
        key = row.get("id")
        require(key not in result and isinstance(key, str), label+" duplicate/missing ID")
        result[key] = row
    require(len(result) == GROUPS, label+" incomplete")
    return result

def generate(review, hafs, gate, j2):
    require(j2.get("schema") == "IK_J2_CONTRADICTORY_REVIEW_V1"
            and gate.get("schema") == "IK_AMORCE_EDITORIAL_GATE_V1",
            "unknown verification provenance")
    require(gate.get("ibn_kathir_commit") == SOURCE
            and gate.get("approved") == j2["counts"]["approved_keys"] == 0
            and gate.get("runtime_ready") is False
            and j2.get("runtime_ready") is False, "unauthorized approval")
    a = by_id(review["candidates"], "Tanzil")
    b = by_id(hafs["candidates"], "Hafs")
    c = by_id(gate["entries"], "Gate")
    d = by_id(j2["entries"], "Risk ledger")
    require(set(a) == set(b) == set(c) == set(d), "differing documentary IDs")
    require(len(j2["sample_ids"]) == 109, "contradictory sample changed")
    for field in ("source_index_sha256",):
        require(review[field] == gate["group_boundary_sha256"]
                == hafs["boundary_sha256"] == j2["provenance"]["ibn_kathir_boundaries_sha256"],
                "boundary SHA drift")
    require(hafs["source_sha256"] == gate["quran_ws_hafs_sha256"]
            == j2["provenance"]["quran_ws_hafs_sha256"], "Hafs SHA drift")

    first_words = collections.defaultdict(list)
    repeated_ayah = collections.defaultdict(list)
    for identifier in a:
        first_words[b[identifier]["exact_source_first_word"]].append(identifier)
        repeated_ayah[(a[identifier]["surah"],
                       a[identifier]["full_start_ayah_text_for_review"])].append(identifier)
    ordered = sorted(a.values(), key=lambda x: (x["surah"], x["start_ayah"]))
    all_cards = []
    for i, item in enumerate(ordered):
        identifier = item["id"]
        h, g, flags = b[identifier], c[identifier], d[identifier]
        require(g["semantic_amorce_status"] == "NOT_APPROVED"
                and flags["human_disposition"] == "UNREVIEWED"
                and not flags["approved_keys"]
                and not flags["extra_cues_within_group"]
                and flags["runtime_enabled"] is False,
                identifier+" illegitimate editorial approval")
        require((item["surah"], item["start_ayah"], item["end_ayah"])
                == (g["surah"],g["start_ayah"],g["end_ayah"]),
                identifier+" contradictory verse boundary")
        require(item["first_quran_word_key"] == h["quran_start_word_key"]
                == g["first_word_key"] == flags["first_word_key"],
                identifier+" QCF first-word mismatch")
        previous = ordered[i-1] if i and ordered[i-1]["surah"] == item["surah"] else None
        following = ordered[i+1] if i+1 < GROUPS and ordered[i+1]["surah"] == item["surah"] else None
        # The "same first ayah" relation deliberately uses exact original text,
        # never an assistant-generated paraphrase or thematic classification.
        duplicates = repeated_ayah[(item["surah"],item["full_start_ayah_text_for_review"])]
        first_word_peers = first_words[h["exact_source_first_word"]]
        card = {
            "id": identifier,"surah":item["surah"],"start_ayah":item["start_ayah"],
            "end_ayah":item["end_ayah"],"start_page":item["start_page"],
            "end_page":item["end_page"],"first_qcf_word_key":item["first_quran_word_key"],
            "source_commit":SOURCE,
            "source_relative_path":f"tafsir/en-tafisr-ibn-kathir/{item['surah']}.json",
            "source_verse_reference":f"{item['surah']}:{item['start_ayah']}-{item['end_ayah']}",
            "preceding_group_id":previous["id"] if previous else None,
            "following_group_id":following["id"] if following else None,
            "first_word_collisions":len(first_word_peers),
            "same_surah_same_start_ayah_text":sorted(duplicates),
            "risk_flags":flags["risk_flags"],
            "draft_lexical_length_not_a_key":h["distinctive_prefix_draft_words"],
            "draft_hafs_prefix_for_review_only":h["exact_quran_preview"],
            "source_start_ayah_tanzil_for_review_only":item["full_start_ayah_text_for_review"],
            "source_orthography_warning":item["review_text_warning"],
            "documentary_group_status":"VERIFIED_INDEX_ONLY",
            "editorial_decision":"UNREVIEWED",
            "approval_evidence":None,"minimality_rarity_context_evidence":None,
            "qcf_word_by_word_signoff":None,"rights_confirmation":None,
            "proposed_keys":[],"runtime_enabled":False
        }
        require(not card["runtime_enabled"] and not card["proposed_keys"],
                identifier+" preview used as selected key")
        all_cards.append(card)
    assert len(all_cards) == GROUPS
    assert set(j2["sample_ids"]).issubset({c["id"] for c in all_cards})
    cards_by_id = {card["id"]:card for card in all_cards}
    sample = [cards_by_id[key] for key in j2["sample_ids"]]
    require(cards_by_id[EXCEPTION]["draft_lexical_length_not_a_key"] is None,
            "2:47 exceptional status erased")
    require("TANZIL_QCF_15_7_DIFFERENCE" in cards_by_id[ORTHOGRAPHY]["risk_flags"],
            "15:7 source exception erased")
    qaf = [c for c in all_cards if c["surah"] == 50]
    require(qaf and all(c in sample for c in qaf),
            "Qaf must be completely present in contradictory first batch")
    return {
        "schema":"IK_J2_EDITORIAL_CARDS_V1", "runtime_ready":False,
        "ibn_kathir_source_commit":SOURCE,
        "group_boundary_sha256":review["source_index_sha256"],
        "quran_ws_hafs_sha256":hafs["source_sha256"],
        "total_groups":GROUPS,"editorially_approved":0,
        "sample_count":len(sample),"qaf_groups_in_sample":len(qaf),
        "notes":[
          "No quota: zero, one or several mnemonic cues may be appropriate per group.",
          "Tanzil orthography is a review aid, not proof of QCF-exact glyph content.",
          "Printed headings, exegesis and rights MUST be checked against the pinned primary edition.",
          "No copied English exegesis, invented heading or automatically approved key.",
          "All draft Quran words are reviewer-only and never packaged into the Android app."
        ],
        "cards":all_cards,"sample_cards":sample
    }

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("tanzil_review",type=Path)
    parser.add_argument("hafs_review",type=Path)
    parser.add_argument("approval_gate",type=Path)
    parser.add_argument("risk_review",type=Path)
    parser.add_argument("out_dir",type=Path)
    args=parser.parse_args()
    items=[json.loads(p.read_text(encoding="utf-8")) for p in
           (args.tanzil_review,args.hafs_review,args.approval_gate,args.risk_review)]
    result=generate(*items)
    args.out_dir.mkdir(parents=True,exist_ok=True)
    for name,rows in (("ik-j2-editorial-all-1903.json",result["cards"]),
                      ("ik-j2-editorial-sample-109.json",result["sample_cards"])):
        (args.out_dir/name).write_text(json.dumps({
            "schema":result["schema"],"source_commit":SOURCE,
            "group_boundary_sha256":result["group_boundary_sha256"],
            "quran_ws_hafs_sha256":result["quran_ws_hafs_sha256"],
            "runtime_ready":False,"editorially_approved":0,
            "cards":rows},ensure_ascii=False,indent=1)+"\n",encoding="utf-8")
    cols=["id","surah","start_ayah","end_ayah","start_page","end_page",
          "first_qcf_word_key","preceding_group_id","following_group_id",
          "first_word_collisions","risk_flags","draft_lexical_length_not_a_key",
          "editorial_decision","source_relative_path"]
    with (args.out_dir/"ik-j2-editorial-sample-109.tsv").open("w",encoding="utf-8",newline="") as file:
        w=csv.DictWriter(file,fieldnames=cols,delimiter="\t")
        w.writeheader()
        for row in result["sample_cards"]:
            w.writerow({k:";".join(row[k]) if k=="risk_flags" else row.get(k) for k in cols})
    report=["# Ibn Kathīr — J2 dossier de revue contradictoire",
            "","**NO GO / 0 APPROUVÉES** — références uniquement.","",
            "- Source : spa5k/tafsir_api à commit "+SOURCE,
            "- Groupes : "+str(result["total_groups"]),
            "- Échantillon contradictoire : "+str(result["sample_count"]),
            "- Qāf : "+str(result["qaf_groups_in_sample"])+" groupes, tous présents dans l'échantillon",
            "- Exception 2:47 : "+EXCEPTION+" (préfixe distinctif non établi)",
            "- Résidu 15:7 : "+ORTHOGRAPHY+" (orthographe Tanzil/QCF)",
            "","Décisions à documenter séparément par groupe : NEEDS_NO_KEY /",
            "APPROVE_AFTER_QCF_AND_CONTEXT_CHECK / BLOCKED. Le projet n'a actuellement",
            "aucune décision de ce type vérifiée.",
            "","Chaque fiche fournit voisinage documentaire, lien vers JSON épinglé,",
            "collisions lexicales et traces QCF/Hafs. Le texte d'exégèse",
            "anglaise n'est pas repris : source et droits à confirmer.",
            "","Pas d'APK, pas de fichier d'actifs Android, aucun runtime activé."]
    (args.out_dir/"ik-j2-editorial-review-summary.md").write_text(
        "\n".join(report)+"\n",encoding="utf-8")
    print("PASS J2 editorial packets: 1903 source-linked records, 109 contradictory"
          " sample cards, "+str(result["qaf_groups_in_sample"])+" Qaf groups;"
          " 0 approved; runtime NO GO.")
if __name__=="__main__":
    main()
