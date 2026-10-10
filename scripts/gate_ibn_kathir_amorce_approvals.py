#!/usr/bin/env python3
"""Join the two independently checked Ibn Kathir review reports into a NO-GO ledger.

This script does NOT approve semantic keys. It records exact references, provenance,
documentary discrepancies and the absence of editorial/rights approval for EVERY
group. Nothing produced here may be shipped as active runtime amorces.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

GROUPS = 1903
QCF_WORDS = 77432
QCF_PAGES = 604
VERSE_COUNT = 6236
IBN_KATHIR_COMMIT = "eb82bb6294efe30ad5c135c03b1864afaa70e855"
EXCEPTION_UNIQUE_PREFIX = "IKEN002_047_047"
TEXT_ORTHOGRAPHY_EXCEPTION = "15:7"

def require(test, message):
    if not test:
        raise ValueError("Ibn Kathir approval gate: " + message)

def generate(draft, exact):
    require(draft.get("schema_version") == "IK_QURANIC_AMORCE_REVIEW_V1",
            "unexpected draft schema")
    require(draft.get("groups") == GROUPS and draft.get("covered_verses") == VERSE_COUNT,
            "missing groups or verses in geometry review")
    require(draft.get("verified_word_boxes") == QCF_WORDS
            and draft.get("verified_604_pages") is True, "unverified geometry")
    require(draft.get("selected_anchors") == 0 and draft.get("runtime_ready") is False,
            "draft must not contain activated keys")
    require(exact.get("ibn_kathir_groups") == GROUPS
            and exact.get("ayahs_exactly_aligned") == VERSE_COUNT
            and exact.get("pages_exactly_aligned") == QCF_PAGES
            and exact.get("words_exactly_aligned") == QCF_WORDS,
            "exact source/QCF join was not successful")
    require(exact.get("approved_semantic_amorces") == 0
            and exact.get("runtime_ready") is False,
            "premature approval/activation in exact-source report")
    a = {x["id"]: x for x in draft["candidates"]}
    b = {x["id"]: x for x in exact["candidates"]}
    require(len(a) == len(b) == GROUPS and set(a) == set(b),
            "different or duplicate group IDs across independent audits")
    require(exact.get("unresolved_prefix_ids") == [EXCEPTION_UNIQUE_PREFIX],
            "unexpected unique-prefix exception set")
    require(exact.get("draft_one_page_distinct_keys") == GROUPS - 1,
            "draft distinctiveness count changed")
    rows = []
    for identifier in sorted(a):
        left, right = a[identifier], b[identifier]
        for field in ("surah", "start_ayah", "end_ayah"):
            require(left[field] == right[field], identifier + ": incompatible " + field)
        require(left["start_page"] == right["qcf_page"],
                identifier + ": QCF page mismatch")
        require(left["first_quran_word_key"] == right["quran_start_word_key"],
                identifier + ": first word key mismatch")
        require(left.get("selection_status") == "NOT_REVIEWED"
                and left.get("candidate_anchor_word_count") is None,
                identifier + ": undocumented review status")
        require(right.get("draft_not_approved") is True
                and right.get("runtime_enabled") is False,
                identifier + ": draft activation detected")
        exceptions = []
        if identifier == EXCEPTION_UNIQUE_PREFIX:
            require(right["distinctive_prefix_draft_words"] is None,
                    "the known unresolved prefix was silently filled")
            exceptions.append("NO_DISTINCT_ONE_PAGE_PREFIX")
        else:
            require(isinstance(right["distinctive_prefix_draft_words"], int)
                    and right["distinctive_prefix_draft_words"] >= 1,
                    identifier + ": undocumented lexical proposal")
        if left["surah"] == 15 and left["start_ayah"] <= 7 <= left["end_ayah"]:
            exceptions.append("TANZIL_QCF_15_7_ORTHOGRAPHY_NEEDS_REVIEW")
        rows.append({
            "id": identifier,
            "surah": left["surah"],
            "start_ayah": left["start_ayah"],
            "end_ayah": left["end_ayah"],
            "start_page": left["start_page"],
            "end_page": left["end_page"],
            "first_word_key": left["first_quran_word_key"],
            "documentary_geometry": "QCF_COORDINATES_VERIFIED",
            "quranic_word_text_equivalence": "NOT_INDEPENDENTLY_APPROVED",
            "minimality_rarity_recall": "NOT_REVIEWED",
            "rights_clearance": "NOT_CONFIRMED",
            "semantic_amorce_status": "NOT_APPROVED",
            "runtime_enabled": False,
            "exceptions": exceptions,
        })
    require(len(rows) == GROUPS
            and all(r["semantic_amorce_status"] == "NOT_APPROVED" for r in rows),
            "editorial approval gate bypassed")
    require(any("TANZIL_QCF_15_7_ORTHOGRAPHY_NEEDS_REVIEW" in r["exceptions"]
                for r in rows), "missing documented 15:7 source mismatch")
    return {
        "schema": "IK_AMORCE_EDITORIAL_GATE_V1",
        "group_boundary_sha256": draft["source_index_sha256"],
        "quran_ws_hafs_sha256": exact["source_sha256"],
        "ibn_kathir_commit": IBN_KATHIR_COMMIT,
        "groups": GROUPS,
        "approved": 0,
        "not_approved": GROUPS,
        "source_discrepancy": TEXT_ORTHOGRAPHY_EXCEPTION,
        "unique_prefix_exception": EXCEPTION_UNIQUE_PREFIX,
        "runtime_ready": False,
        "purpose": "Human/independent editorial review queue only, NEVER a runtime cue corpus",
        "entries": rows,
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("geometry_review", type=Path)
    parser.add_argument("exact_hafs_review", type=Path)
    parser.add_argument("output_json", type=Path)
    args = parser.parse_args()
    draft = json.loads(args.geometry_review.read_text(encoding="utf-8"))
    exact = json.loads(args.exact_hafs_review.read_text(encoding="utf-8"))
    ledger = generate(draft, exact)
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(
        json.dumps(ledger, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"PASS J1: {GROUPS} immutable Ibn Kathir IDs, all NOT_APPROVED; "
          f"exception {EXCEPTION_UNIQUE_PREFIX}; spelling {TEXT_ORTHOGRAPHY_EXCEPTION}.")
    print("J2 NO GO: zero semantic approvals, no rights clearance, no runtime activation.")

if __name__ == "__main__":
    main()
