#!/usr/bin/env python3
"""Fail-closed verification of the final Qushayri footnote-call cross-check.

The upstream research script deliberately keeps uncertain cases out of production.
This verifier consumes its diagnostic JSON, which is generated from the pinned PDF,
and accepts the remaining eleven note-body keys only when their same-page call is
visible through one of three narrow source-layout rules. It never creates or edits
Tafsir text and it never promotes standalone numeric candidates that have no
same-page note body.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

EXPECTED_SOURCE_SHA256 = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
EXPECTED_UNION = 928
EXPECTED_PRIMARY = 917
EXPECTED_EXTRA_WITHOUT_BODY = {(173, 101), (173, 117)}

# The rule is frozen per still-unmatched key after inspection of the pinned source
# diagnostics. New or changed cases fail closed instead of being guessed.
EXPECTED_RULES = {
    (61, 30): "honorific",
    (87, 90): "honorific",
    (170, 246): "colon",
    (241, 374): "spaced-punctuation",
    (339, 133): "spaced-punctuation",
    (344, 149): "honorific",
    (349, 167): "honorific",
    (354, 176): "honorific",
    (366, 191): "spaced-punctuation",
    (370, 201): "honorific",
    (448, 72): "honorific",
}

PUA_RE = re.compile(r"[\ue000-\uf8ff]")


def fail(message: str) -> None:
    raise SystemExit(f"Qushayri note cross-check: {message}")


def starts_with_number(text: str, number: int) -> bool:
    stripped = text.lstrip(" \t\u00a0")
    wanted = str(number)
    if not stripped.startswith(wanted):
        return False
    tail = stripped[len(wanted):len(wanted) + 1]
    return not tail or tail not in "0123456789:%"


def matches_honorific(candidate: dict, number: int) -> bool:
    spans = candidate.get("spans") or []
    hits = 0
    for index, span in enumerate(spans[:-1]):
        if span.get("font") != "Honorifics":
            continue
        if not PUA_RE.search(span.get("text", "")):
            continue
        if starts_with_number(spans[index + 1].get("text", ""), number):
            hits += 1
    return hits == 1


def matches_colon(candidate: dict, number: int) -> bool:
    # Reject Quran references such as [4:72]: the character before ':' may not be
    # a digit. The source form '):246' is accepted.
    pattern = re.compile(rf"(?<!\d):\s*{number}(?![\d:%])")
    return len(pattern.findall(candidate.get("text", ""))) == 1


def matches_spaced_punctuation(candidate: dict, number: int) -> bool:
    # Narrowly cover the source forms ') 374', '. 133' and '? 191'.
    pattern = re.compile(rf"[\.\?\!\)\]”’\"]\s+{number}(?![\d:%])")
    return len(pattern.findall(candidate.get("text", ""))) == 1


def candidate_matches(candidate: dict, number: int, rule: str) -> bool:
    if rule == "honorific":
        return matches_honorific(candidate, number)
    if rule == "colon":
        return matches_colon(candidate, number)
    if rule == "spaced-punctuation":
        return matches_spaced_punctuation(candidate, number)
    fail(f"unknown verification rule {rule!r}")
    return False


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("report", type=Path)
    args = parser.parse_args()

    report = json.loads(args.report.read_text(encoding="utf-8"))
    if report.get("source_sha256") != EXPECTED_SOURCE_SHA256:
        fail(f"source SHA changed: {report.get('source_sha256')!r}")
    if report.get("production_eligible") is not False:
        fail("research report unexpectedly declares itself production-eligible")
    if report.get("note_body_union_keys") != EXPECTED_UNION:
        fail(f"note-body union changed: {report.get('note_body_union_keys')!r}")
    if report.get("matched_by_primary_attached_detector") != EXPECTED_PRIMARY:
        fail(f"primary detector count changed: {report.get('matched_by_primary_attached_detector')!r}")
    if report.get("matched_note_body_call_keys") != EXPECTED_PRIMARY:
        fail(f"pre-verification matched count changed: {report.get('matched_note_body_call_keys')!r}")
    if report.get("matched_by_geometry_fallback") != 0:
        fail("geometry fallback unexpectedly promoted a case; inspect before release")
    if report.get("duplicate_matched_call_keys") != []:
        fail(f"duplicate note-call matches exist: {report.get('duplicate_matched_call_keys')!r}")

    actual_unmatched = {tuple(item) for item in report.get("unmatched_note_body_keys", [])}
    expected_unmatched = set(EXPECTED_RULES)
    if actual_unmatched != expected_unmatched:
        fail(
            "unmatched key set changed; expected "
            f"{sorted(expected_unmatched)!r}, got {sorted(actual_unmatched)!r}"
        )

    extras = {tuple(item) for item in report.get("candidate_keys_without_body_sample", [])}
    if report.get("attached_candidates_without_same_page_note_body") != len(EXPECTED_EXTRA_WITHOUT_BODY):
        fail("standalone candidate count changed")
    if extras != EXPECTED_EXTRA_WITHOUT_BODY:
        fail(f"standalone candidates changed: {sorted(extras)!r}")

    diagnostics = {
        (int(item["page"]), int(item["number"])): item
        for item in report.get("unmatched_diagnostics", [])
    }
    if set(diagnostics) != expected_unmatched:
        fail("diagnostic key set does not exactly match the unresolved keys")

    verified = []
    for key in sorted(expected_unmatched):
        page, number = key
        rule = EXPECTED_RULES[key]
        candidates = diagnostics[key].get("main_text_candidates_containing_number") or []
        matches = [c for c in candidates if candidate_matches(c, number, rule)]
        if len(matches) != 1:
            fail(
                f"{page}:{number} expected exactly one {rule} source call, "
                f"found {len(matches)}"
            )
        if not diagnostics[key].get("note_bodies"):
            fail(f"{page}:{number} has no independently detected same-page note body")
        verified.append((page, number, rule))

    if EXPECTED_PRIMARY + len(verified) != EXPECTED_UNION:
        fail("verified total does not close the complete note-body inventory")

    print("0.10.5 Qushayri note-call cross-check PASS")
    print(f"- pinned source SHA-256: {EXPECTED_SOURCE_SHA256}")
    print(f"- independently detected note-body keys: {EXPECTED_UNION}")
    print(f"- primary unambiguous calls: {EXPECTED_PRIMARY}")
    print(f"- strictly resolved diagnostic calls: {len(verified)}")
    print(f"- verified note-body -> same-page call relations: {EXPECTED_UNION}/{EXPECTED_UNION}")
    print("- no duplicate call relation promoted")
    print("- standalone numeric candidates 173:101 and 173:117 remain non-promoted because no same-page note body was detected")


if __name__ == "__main__":
    main()
