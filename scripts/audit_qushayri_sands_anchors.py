#!/usr/bin/env python3
"""Audit Sands' Sūras 1–4 verse/range anchors without emitting commentary text.

Input is text extracted from the user-supplied/authorized PDF. The script is deliberately
sequential because running headers contain marker-like ranges and Q 2:68 is an inline
next-verse anchor. It produces metadata only, not copyrighted commentary.
"""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path

VERSE_COUNTS = [7, 286, 200, 176]
PATTERN = re.compile(r"\[(\d{1,3}):(\d{1,3})(?:\s*[–—-]\s*(\d{1,3}))?\]")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("text", type=Path)
    args = parser.parse_args()
    lines = args.text.read_text(encoding="utf-8", errors="replace").splitlines()

    try:
        start = next(i for i, line in enumerate(lines) if "[Author’s Introduction]" in line)
    except StopIteration as error:
        raise SystemExit("Author introduction anchor not found") from error
    try:
        end = next(
            i for i, line in enumerate(lines[start + 1 :], start + 1)
            if line.strip() == "Bibliography"
        )
    except StopIteration as error:
        raise SystemExit("Bibliography boundary not found") from error

    expected = [
        (surah, ayah)
        for surah, count in enumerate(VERSE_COUNTS, start=1)
        for ayah in range(1, count + 1)
    ]
    expected_index = -1
    current: tuple[int, int] | None = None
    accepted: list[dict] = []

    for line_number, line in enumerate(lines[start:end], start=start + 1):
        # Running headers such as "Subtle Allusions [2:69] – [2:70]" are locators,
        # never content anchors.
        if ("Subtle Allusions" in line or "Laṭāʾif al-ishārāt" in line) and PATTERN.search(line):
            continue
        indent = len(line) - len(line.lstrip())
        for match in PATTERN.finditer(line):
            surah = int(match.group(1))
            verse_start = int(match.group(2))
            verse_end = int(match.group(3) or match.group(2))
            if not 1 <= surah <= 4:
                continue
            near_start = match.start() <= indent + 2
            next_expected = expected[expected_index + 1] if expected_index + 1 < len(expected) else None
            reason = None

            if current == (surah, verse_start) and near_start and verse_end == verse_start:
                reason = "repeat_segment"
            elif (surah, verse_start) == next_expected:
                sequence = [(surah, ayah) for ayah in range(verse_start, verse_end + 1)]
                if expected[expected_index + 1 : expected_index + 1 + len(sequence)] == sequence:
                    expected_index += len(sequence)
                    current = (surah, verse_end)
                    reason = "next_range" if verse_end > verse_start else (
                        "next_inline" if not near_start else "next"
                    )

            if reason:
                accepted.append(
                    {
                        "line": line_number,
                        "surah": surah,
                        "verse_start": verse_start,
                        "verse_end": verse_end,
                        "reason": reason,
                    }
                )

    mapped: Counter[tuple[int, int]] = Counter()
    for anchor in accepted:
        for ayah in range(anchor["verse_start"], anchor["verse_end"] + 1):
            mapped[(anchor["surah"], ayah)] += 1

    missing = [
        f"{surah}:{ayah}"
        for surah, count in enumerate(VERSE_COUNTS, start=1)
        for ayah in range(1, count + 1)
        if (surah, ayah) not in mapped
    ]
    report = {
        "status": "PASS" if not missing and len(mapped) == 669 else "FAIL",
        "expected_verses": 669,
        "distinct_verses_mapped": len(mapped),
        "accepted_anchor_blocks": len(accepted),
        "range_blocks": sum(1 for item in accepted if item["verse_end"] > item["verse_start"]),
        "inline_expected_anchors": sum(1 for item in accepted if item["reason"] == "next_inline"),
        "verses_with_multiple_segments": sum(1 for value in mapped.values() if value > 1),
        "missing": missing,
        "required_regressions": {
            "2:68_mapped": (2, 68) in mapped,
            "2:83_mapped": (2, 83) in mapped,
            "2:84_mapped": (2, 84) in mapped,
            "4:167_169_range_present": any(
                item["surah"] == 4 and item["verse_start"] == 167 and item["verse_end"] == 169
                for item in accepted
            ),
        },
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    if report["status"] != "PASS" or not all(report["required_regressions"].values()):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
