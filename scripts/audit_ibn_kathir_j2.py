#!/usr/bin/env python3
"""J2: contradictory, documentary-only review of 1,903 Ibn Kathir group starts.
No proposed lexical prefix is an approved mnemonic, no group is forced to have a
cue, and no generated JSON from this script is part of the Android runtime.
"""
from __future__ import annotations
import argparse, collections, copy, csv, json, sys
from pathlib import Path

GROUPS = 1903
QURAN_SHA = "9b9eb07ff5cff144bf964400924e593d97115e4b937db3c61783f782a5168075"
BOUNDARY_SHA = "deffec6e775965d7b1f717a63fc4d0d60c1933540aacefc13e1333d30c49be62"
EXCEPTION = "IKEN002_047_047"

def require(condition, message):
    if not condition:
        raise ValueError("IBN KATHIR J2 FAIL: " + message)

def as_index(rows, title):
    result = {}
    for row in rows:
        key = row.get("id")
        require(isinstance(key,str) and key.startswith("IKEN"), title+" invalid key")
        require(key not in result, title+" duplicate "+key)
        result[key] = row
    require(len(result)==GROUPS, title+" incomplete/oversized")
    return result

def inspect(raw, exact, gate):
    require(raw.get("schema_version")=="IK_QURANIC_AMORCE_REVIEW_V1", "wrong source review schema")
    require(gate.get("schema")=="IK_AMORCE_EDITORIAL_GATE_V1", "wrong gate schema")
    require(raw.get("source_index_sha256")==exact.get("boundary_sha256")
            ==gate.get("group_boundary_sha256")==BOUNDARY_SHA, "wrong boundary SHA")
    require(exact.get("source_sha256")==gate.get("quran_ws_hafs_sha256")
            ==QURAN_SHA, "wrong pinned Quran.ws Hafs SHA")
    require(raw.get("groups")==exact.get("ibn_kathir_groups")
            ==gate.get("groups")==GROUPS, "incomplete group corpus")
    require(raw.get("covered_verses")==exact.get("ayahs_exactly_aligned")==6236, "ayah count")
    require(raw.get("verified_word_boxes")==exact.get("words_exactly_aligned")==77432, "word count")
    require(raw.get("verified_604_pages") is True
            and exact.get("pages_exactly_aligned")==604, "page geometry")
    require(exact.get("draft_one_page_distinct_keys")==GROUPS-1
            and exact.get("unresolved_prefix_ids")==[EXCEPTION], "lexical anomaly set changed")
    require(exact.get("pages_without_documentary_block_start")==[], "missing page block start")
    require(raw.get("runtime_ready") is False and exact.get("runtime_ready") is False
            and gate.get("runtime_ready") is False, "premature runtime activation")
    require(raw.get("selected_anchors")==0 and exact.get("approved_semantic_amorces")==0
            and gate.get("approved")==0 and gate.get("not_approved")==GROUPS,
            "unsubstantiated semantic approval")
    a,b,c=(as_index(doc[key],tag) for doc,key,tag in
        ((raw,"candidates","word geometry"),
         (exact,"candidates","pinned Hafs"),
         (gate,"entries","J1 gate")))
    require(set(a)==set(b)==set(c), "independent sources have different group IDs")
    require(a["IKEN002_047_047"]["full_start_ayah_text_for_review"]
            ==a["IKEN002_122_123"]["full_start_ayah_text_for_review"],
            "2:47/2:122 exact repeated source text discrepancy")
    require("IKEN050_012_015" in a and "IKEN050_012_014" not in a,
            "Qaf boundaries altered or invented")
    first_counts=collections.Counter(r["exact_source_first_word"] for r in b.values())
    start_text_counts=collections.Counter(r["full_start_ayah_text_for_review"] for r in a.values())
    entries=[]
    for key in sorted(a):
        l,r,g=a[key],b[key],c[key]
        require(all(l[f]==r[f]==g[f] for f in ("surah","start_ayah","end_ayah")),
                key+" interval mismatch")
        require(l["start_page"]==r["qcf_page"]==g["start_page"]
                and l["end_page"]==g["end_page"], key+" page mismatch")
        require(l["first_quran_word_key"]==r["quran_start_word_key"]==g["first_word_key"],
                key+" QCF word key mismatch")
        require(l.get("selection_status")=="NOT_REVIEWED"
                and l.get("candidate_anchor_word_count") is None
                and g.get("semantic_amorce_status")=="NOT_APPROVED"
                and g.get("runtime_enabled") is False
                and r.get("draft_not_approved") is True
                and r.get("runtime_enabled") is False, key+" draft activated")
        n=r.get("distinctive_prefix_draft_words")
        sample=r.get("exact_quran_preview")
        if n is None:
            require(key==EXCEPTION and sample is None, key+" unresolved prefix changed")
        else:
            require(type(n) is int and n>=1 and isinstance(sample,str),
                    key+" invalid suggested length")
            tokens=sample.split()
            require(len(tokens)==n and tokens[0]==r["exact_source_first_word"],
                    key+" lexical preview contradicts pinned Hafs words")
        flags=[]
        if n is None: flags.append("NO_SAME_PAGE_DISTINCT_PREFIX")
        if n==1: flags.append("SINGLE_WORD_DRAFT_NOT_MNEMONIC_APPROVAL")
        if n is not None and n>=7: flags.append("LONG_DISTINCT_PREFIX_REVIEW")
        if first_counts[r["exact_source_first_word"]]>=10:
            flags.append("COMMON_START_WORD_ACROSS_GROUPS_10_PLUS")
        if start_text_counts[l["full_start_ayah_text_for_review"]]>1:
            flags.append("REPEATED_TANZIL_START_AYAH")
        if l["start_page"]!=l["end_page"]: flags.append("MULTIPAGE_GROUP")
        if l["start_ayah"]==l["end_ayah"]: flags.append("SINGLE_AYAH_GROUP")
        if l["surah"]==15 and l["start_ayah"]<=7<=l["end_ayah"]:
            flags.append("TANZIL_QCF_15_7_DIFFERENCE")
        entries.append({
            "id":key,"surah":l["surah"],"start_ayah":l["start_ayah"],
            "end_ayah":l["end_ayah"],"start_page":l["start_page"],
            "end_page":l["end_page"],"first_word_key":l["first_quran_word_key"],
            "candidate_lexical_length_only":n,"risk_flags":flags,
            "human_disposition":"UNREVIEWED",
            "permitted_outcomes":["NO_KEY_REQUIRED","ONE_OR_MORE_VERIFIED_KEYS","BLOCKED"],
            "approved_keys":[],"extra_cues_within_group":[],
            "word_by_word_qcf_verified_for_selected_cue":False,
            "mnemonic_context_minimality_approved":False,
            "text_reuse_rights_confirmed":False,"runtime_enabled":False
        })
    require(len(entries)==GROUPS
            and all(not e["runtime_enabled"] and not e["approved_keys"] for e in entries),
            "unreviewed keys must stay inactive")
    counts=collections.Counter(f for e in entries for f in e["risk_flags"])
    require(counts["NO_SAME_PAGE_DISTINCT_PREFIX"]==1
            and counts["TANZIL_QCF_15_7_DIFFERENCE"]==1,
            "known exceptions silently disappeared")
    sample_ids={EXCEPTION,"IKEN002_122_123","IKEN002_040_041",
                "IKEN002_256_256","IKEN015_006_009"}
    sample_ids.update(x["id"] for x in entries if x["surah"] in (1,50,51,53,114))
    sample_ids.update(x["id"] for x in entries if x["candidate_lexical_length_only"] is not None
                      and x["candidate_lexical_length_only"]>=7)
    sample_ids.update(x["id"] for x in entries if "REPEATED_TANZIL_START_AYAH" in x["risk_flags"])
    multi=[e for e in entries if "MULTIPAGE_GROUP" in e["risk_flags"]]
    sample_ids.update(e["id"] for e in multi[::max(1,len(multi)//24)])
    require(len(sample_ids)>50, "representative sample incomplete")
    return {"schema":"IK_J2_CONTRADICTORY_REVIEW_V1","runtime_ready":False,
            "provenance":{"ibn_kathir_boundaries_sha256":BOUNDARY_SHA,
                          "quran_ws_hafs_sha256":QURAN_SHA},
            "counts":{"groups":GROUPS,"approved_keys":0,
                      "groups_pending_editorial_decision":GROUPS,
                      "sample_group_count":len(sample_ids)},
            "nonexclusive_risk_counts":dict(sorted(counts.items())),
            "sample_ids":sorted(sample_ids),
            "constraints":["No quota: a group may require zero, one or several keys.",
                           "A lexical prefix is NOT an approved mnemonic.",
                           "No invented titles, group boundaries or religious text.",
                           "Tanzil text is a review source; QCF word-by-word cue approval is pending.",
                           "No runtime use, progress changes or APK."],
            "entries":entries}

def main():
    p=argparse.ArgumentParser()
    for key in ("review_geometry","review_hafs","approval_gate","out_dir"):
        p.add_argument(key,type=Path)
    p.add_argument("--negative-tests",action="store_true")
    args=p.parse_args()
    raw,exact,gate=(json.loads(f.read_text(encoding="utf-8")) for f in
                    (args.review_geometry,args.review_hafs,args.approval_gate))
    report=inspect(raw,exact,gate)
    if args.negative_tests:
        for label,mutate in (
            ("runtime_activation",lambda d:d[2].update(runtime_ready=True)),
            ("source_drift",lambda d:d[1].update(source_sha256="0"*64)),
            ("verse_drift",lambda d:d[2]["entries"][1].update(start_ayah=999)),
            ("false_prefix",lambda d:d[1]["candidates"][0].update(distinctive_prefix_draft_words=999)),
            ("missing_group",lambda d:d[0]["candidates"].pop()),
        ):
            corrupted=copy.deepcopy([raw,exact,gate])
            mutate(corrupted)
            try:inspect(*corrupted)
            except ValueError: continue
            raise RuntimeError("J2 negative control was wrongly accepted: "+label)
        print("PASS: five negative controls rejected")
    args.out_dir.mkdir(parents=True,exist_ok=True)
    (args.out_dir/"ik-j2-review.json").write_text(
        json.dumps(report,ensure_ascii=False,indent=1)+"\n",encoding="utf-8")
    cols=("id","surah","start_ayah","end_ayah","start_page","end_page",
          "first_word_key","candidate_lexical_length_only",
          "risk_flags","human_disposition","runtime_enabled")
    with (args.out_dir/"ik-j2-review.tsv").open("w",encoding="utf-8",newline="") as f:
        writer=csv.DictWriter(f,fieldnames=cols,delimiter="\t")
        writer.writeheader()
        for row in report["entries"]:
            writer.writerow({k:";".join(row[k]) if k=="risk_flags" else row.get(k) for k in cols})
    summary=["# Ibn Kathir J2 — audit contradictoire",
             "","**NO GO : 0 clé approuvée, 1 903 groupes en attente de décision.**",
             "", "Il n'existe aucune obligation d'une amorce par groupe.",
             "Les catégories de risques se recoupent. Les propositions lexicales ne sont PAS des clés.",
             ""]
    summary.extend("- "+key+": "+str(value) for key,value in report["nonexclusive_risk_counts"].items())
    summary += ["", "Cas vérifiés : 2:47 et 2:122 même verset de départ (Tanzil) ;",
                "Qaf 50:12–15 est UN SEUL groupe selon l'index ;",
                "15:7 présente la discordance Tanzil/QCF déjà documentée.",
                "", "Échantillon contradictoire préparé : "+str(len(report["sample_ids"]))+" groupes.",
                "Voir le TSV/JSON généré pour l'intégralité des IDs, pages, longueurs et risques.",
                "Aucune approbation mnémotechnique, licence ou activation runtime implicite.",
                "J3/J5 et APK : NO GO jusqu'à validation des données et absence de régression."]
    (args.out_dir/"ik-j2-summary.md").write_text("\n".join(summary)+"\n",encoding="utf-8")
    print("PASS documentary J2:",GROUPS,"groups; no invented/activated keys.",
          "Audit sample:",len(report["sample_ids"]),"; risks:",report["nonexclusive_risk_counts"])
    print("J2 editorial/runtime release decision: NO GO")

if __name__=="__main__":
    main()
