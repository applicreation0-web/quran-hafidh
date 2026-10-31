#!/usr/bin/env python3
"""Verify the advisory Quranic mutashabihat-pivot review without mutating runtime.

Audit of source identity, exact canonical words, verified one-based indices,
non-promotion of quarantined/excluded cases, and explicit omission handling.
The review file is NOT a permission to change Quran highlighting or Hifz data.
"""
from __future__ import annotations
import json
import unicodedata
from pathlib import Path
from build_alabbad_catalogue import load_inputs

ROOT = Path(__file__).resolve().parents[1]
REVIEW = ROOT / "research/mutash_pivot_review_v2_20261031.json"
AUDIT = ROOT / "research/alabbad_audit_294.json"
APPROVED = ROOT / "research/alabbad_catalogue_approved.json"


def normalized(word: str) -> str:
    decomposed = unicodedata.normalize("NFKD", word)
    return (
        "".join(c for c in decomposed if not unicodedata.category(c).startswith("M"))
        .replace("\u200a", "").replace("\u2060", "").replace("\u0640", "")
        .replace("ٱ", "ا").replace("إ", "ا").replace("أ", "ا").replace("آ", "ا")
        .replace("ى", "ي").replace("ی", "ي")
    )


def verse_words(ref: str, verses: dict) -> list[str]:
    surah, ayah = map(int, ref.split(":"))
    return verses[(surah, ayah)]


def check_indices(indices: list[int], expected: list[str], words: list[str], label: str):
    assert len(indices) == len(expected), (label, "index/token count")
    assert len(indices) == len(set(indices)), (label, "duplicate word index")
    for index, word in zip(indices, expected):
        assert 1 <= index <= len(words), (label, "out-of-bounds", index)
        assert normalized(words[index - 1]) == normalized(word), (
            label, "canonical word mismatch", index, word, words[index - 1]
        )


def main():
    review = json.loads(REVIEW.read_text(encoding="utf-8"))
    audit = json.loads(AUDIT.read_text(encoding="utf-8"))
    approved = json.loads(APPROVED.read_text(encoding="utf-8"))
    assert review["schemaVersion"] == 1
    assert review["summary"]["automaticallyApproved"] == 0
    assert review["summary"]["canonicalMismatchIds"] == []
    rows = review["rows"]
    source = audit["entries"]
    assert len(rows) == len(source) == 294
    assert [r["auditId"] for r in rows] == [r["auditId"] for r in source] == list(range(1, 295))
    decisions = {r["auditId"]: r for r in source}
    admitted = [e for e in rows if e["decision"] in ("retenu", "corrigé")]
    pending = [e for e in rows if e["decision"] == "à arbitrer"]
    excluded = [e for e in rows if e["decision"] == "exclu"]
    assert (len(admitted), len(pending), len(excluded)) == (279, 9, 6)
    assert approved["approvedCount"] == len(admitted)
    approved_ids = {r["auditId"] for r in approved["entries"]}
    assert approved_ids == {r["auditId"] for r in admitted}
    geometry, verses, _hashes = load_inputs()
    additional = 0
    manual = 0
    for row in rows:
        source_row = decisions[row["auditId"]]
        assert (row["first"], row["second"], row["decision"]) == (
            source_row["first"], source_row["second"], source_row["decision"]
        )
        left = verse_words(row["first"], verses)
        right = verse_words(row["second"], verses)
        old = row["existing"]
        check_indices(old["leftIndices"], old["leftWords"], left, (row["auditId"], "original left"))
        check_indices(old["rightIndices"], old["rightWords"], right, (row["auditId"], "original right"))
        assert old["agreeWithCanonicalQuran"]
        for candidate in row["commonAnchorCandidates"]:
            check_indices(
                [candidate["firstIndex"]], [candidate["firstWord"]],
                left, (row["auditId"], "candidate left")
            )
            check_indices(
                [candidate["secondIndex"]], [candidate["secondWord"]],
                right, (row["auditId"], "candidate right")
            )
            if candidate["matchesExistingPivot"]:
                assert (
                    candidate["firstIndex"] in old["leftIndices"]
                    and candidate["secondIndex"] in old["rightIndices"]
                )
        proposal = row.get("manualKeywordProposal")
        if proposal is not None:
            manual += 1
            for side, words in enumerate([left, right]):
                for idx in proposal["i"][side]:
                    assert 1 <= idx <= len(words), (row["auditId"], "manual index", idx)
                    assert idx in geometry[tuple(map(int, row[["first", "second"][side]].split(":")))]
        curated = row.get("curatedAdditionalReview")
        if curated is not None:
            additional += 1
            assert row["decision"] in ("retenu", "corrigé")
            for segment in curated["segments"]:
                li, ri = segment["leftIndices"], segment["rightIndices"]
                assert li or ri, (row["auditId"], "empty contrast")
                check_indices(li, segment["leftCanonicalWords"], left, (row["auditId"], "curated left"))
                check_indices(ri, segment["rightCanonicalWords"], right, (row["auditId"], "curated right"))
                for i in li:
                    assert i in geometry[tuple(map(int, row["first"].split(":")))]
                for i in ri:
                    assert i in geometry[tuple(map(int, row["second"].split(":")))]
            assert curated["changeNotAppliedToRuntime"]
    assert (manual, additional) == (10, 12)
    assert review["summary"]["manuallyPrioritized"] == manual
    assert review["summary"]["additionalCuratedApprovedCases"] == additional
    print(f"PASS — 294 source decisions; 279 approved preserved; 9 pending; 6 excluded; "
          f"{manual} pending proposals; {additional} approved-case reviews; "
          "zero automatic admission, no runtime modification")


if __name__ == "__main__":
    main()
