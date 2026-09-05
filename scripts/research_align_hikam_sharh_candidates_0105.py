#!/usr/bin/env python3
"""Build RESEARCH-ONLY alignment candidates for the 0.10.5 Hikam Sharh project.

This script is intentionally incapable of creating the production asset. It
locates the frozen 264 Arabic matn strings inside one commentator source and
proposes documentary boundaries for human/independent verification.

Outputs are always marked research_candidate / unverified. They must never be
renamed or copied into app/src/plus/assets/hikam/hikam_sharh_dual.json without
passing the separate documentary workflow and release gate.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CANONICAL = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
FORBIDDEN_PRODUCTION = (
    ROOT / "app/src/plus/assets/hikam/hikam_sharh_dual.json"
).resolve()

ARABIC_DIACRITICS = re.compile(r"[\u0610-\u061a\u064b-\u065f\u0670\u06d6-\u06ed]")
NON_ARABIC_ALNUM = re.compile(r"[^\u0621-\u063a\u0641-\u064a0-9]+")
SPACE = re.compile(r"\s+")


@dataclass(frozen=True)
class NormalizedText:
    text: str
    original_indexes: list[int]


def normalize_char(ch: str) -> str:
    if ch == "ـ" or ARABIC_DIACRITICS.match(ch):
        return ""
    replacements = {
        "أ": "ا",
        "إ": "ا",
        "آ": "ا",
        "ٱ": "ا",
        "ى": "ي",
        "ؤ": "و",
        "ئ": "ي",
    }
    ch = replacements.get(ch, ch)
    if ("\u0621" <= ch <= "\u063a") or ("\u0641" <= ch <= "\u064a") or ch.isdigit():
        return ch
    if ch.isspace() or unicodedata.category(ch).startswith("P"):
        return " "
    return " "


def normalize_with_map(text: str) -> NormalizedText:
    out: list[str] = []
    indexes: list[int] = []
    pending_space = False
    for original_index, ch in enumerate(text):
        norm = normalize_char(ch)
        if not norm:
            continue
        if norm == " ":
            pending_space = bool(out)
            continue
        if pending_space and out and out[-1] != " ":
            out.append(" ")
            indexes.append(original_index)
        pending_space = False
        out.append(norm)
        indexes.append(original_index)
    return NormalizedText("".join(out).strip(), indexes)


def normalize_matn(text: str) -> str:
    return normalize_with_map(text).text


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def all_occurrences(haystack: str, needle: str) -> list[int]:
    if not needle:
        return []
    result: list[int] = []
    offset = 0
    while True:
        index = haystack.find(needle, offset)
        if index < 0:
            return result
        result.append(index)
        offset = index + 1


def original_index(mapped: NormalizedText, normalized_index: int) -> int:
    if not mapped.original_indexes:
        return 0
    normalized_index = max(0, min(normalized_index, len(mapped.original_indexes) - 1))
    return mapped.original_indexes[normalized_index]


def page_for_offset(source: str, offset: int) -> int:
    # Form-feed is preserved by PyMuPDF/pdftotext-style source extraction.
    return source.count("\f", 0, max(0, offset)) + 1


def compact_excerpt(text: str, limit: int = 260) -> str:
    clean = SPACE.sub(" ", text).strip()
    return clean if len(clean) <= limit else clean[:limit].rstrip() + "…"


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", type=Path, required=True, help="UTF-8 source text; form-feed may delimit pages")
    ap.add_argument("--output", type=Path, required=True)
    ap.add_argument("--canonical", type=Path, default=DEFAULT_CANONICAL)
    ap.add_argument("--commentator", required=True, choices=("sharnubi", "ibn_abbad"))
    ap.add_argument("--source-label", required=True)
    ap.add_argument("--source-url", required=True)
    args = ap.parse_args()

    output = args.output.resolve()
    if output == FORBIDDEN_PRODUCTION or FORBIDDEN_PRODUCTION in output.parents:
        raise SystemExit("Research candidate output may not target the Plus production asset path")

    canonical = json.loads(args.canonical.read_text(encoding="utf-8"))
    if len(canonical) != 264:
        raise SystemExit(f"Frozen canonical corpus must contain 264 Hikam, got {len(canonical)}")
    if {int(item["source_number"]) for item in canonical} != set(range(1, 265)):
        raise SystemExit("Frozen canonical numbering must be exactly 1..264")

    source = args.source.read_text(encoding="utf-8", errors="strict")
    mapped = normalize_with_map(source)
    if len(mapped.text) < 1000:
        raise SystemExit("Source text is implausibly short")

    results: list[dict] = []
    previous_start = -1
    for item in canonical:
        number = int(item["source_number"])
        matn = str(item["arabic"]).strip()
        needle = normalize_matn(matn)
        hits = all_occurrences(mapped.text, needle)

        match_type = "unmatched"
        normalized_start = None
        normalized_end = None
        if len(hits) == 1:
            normalized_start = hits[0]
            normalized_end = hits[0] + len(needle)
            match_type = "exact_normalized_unique"
        elif len(hits) > 1:
            ordered = [hit for hit in hits if hit > previous_start]
            if len(ordered) == 1:
                normalized_start = ordered[0]
                normalized_end = ordered[0] + len(needle)
                match_type = "exact_normalized_order_disambiguated"
            else:
                match_type = "ambiguous"

        source_start = None
        source_end = None
        page = None
        if normalized_start is not None and normalized_end is not None:
            source_start = original_index(mapped, normalized_start)
            source_end = original_index(mapped, normalized_end - 1) + 1
            page = page_for_offset(source, source_start)
            previous_start = normalized_start

        results.append(
            {
                "source_number": number,
                "commentator_id": args.commentator,
                "canonical_hikma_arabic_sha256": sha256(matn),
                "research_status": "research_candidate",
                "verification_status": "unverified",
                "match_type": match_type,
                "source_label": args.source_label,
                "source_url": args.source_url,
                "source_page_candidate": page,
                "source_offset_start": source_start,
                "source_offset_end": source_end,
                "match_excerpt": (
                    compact_excerpt(source[source_start:source_end])
                    if source_start is not None and source_end is not None
                    else None
                ),
            }
        )

    # Boundary candidates are mechanically inferred only when consecutive unique
    # matches are monotonic. They remain explicitly unverified.
    for index, current in enumerate(results[:-1]):
        nxt = results[index + 1]
        current_end = current.get("source_offset_end")
        next_start = nxt.get("source_offset_start")
        if (
            isinstance(current_end, int)
            and isinstance(next_start, int)
            and next_start > current_end
        ):
            candidate = source[current_end:next_start].strip()
            current["commentary_candidate"] = candidate
            current["boundary_candidate"] = {
                "start_after_matn_offset": current_end,
                "end_before_next_matn_offset": next_start,
                "start_page": page_for_offset(source, current_end),
                "end_page": page_for_offset(source, next_start),
                "status": "mechanically_inferred_unverified",
            }
        else:
            current["commentary_candidate"] = None
            current["boundary_candidate"] = None

    results[-1]["commentary_candidate"] = None
    results[-1]["boundary_candidate"] = None

    unique = sum(r["match_type"].startswith("exact_normalized") for r in results)
    ambiguous = sum(r["match_type"] == "ambiguous" for r in results)
    unmatched = sum(r["match_type"] == "unmatched" for r in results)
    monotonic_boundaries = sum(r.get("boundary_candidate") is not None for r in results)

    payload = {
        "schema": "quran-safeguard-hikam-sharh-research-candidates-v1",
        "production_eligible": False,
        "warning": (
            "RESEARCH ONLY. Mechanical alignment is not documentary verification "
            "and must never be copied to the Plus production asset as verified content."
        ),
        "commentator_id": args.commentator,
        "source_label": args.source_label,
        "source_url": args.source_url,
        "source_sha256": sha256(source),
        "summary": {
            "canonical_hikam": 264,
            "unique_or_order_disambiguated_matches": unique,
            "ambiguous_matches": ambiguous,
            "unmatched": unmatched,
            "mechanical_boundary_candidates": monotonic_boundaries,
        },
        "entries": results,
    }

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(payload["summary"], ensure_ascii=False))


if __name__ == "__main__":
    main()
