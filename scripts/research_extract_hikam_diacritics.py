#!/usr/bin/env python3
"""
Research-only alignment of a fully vocalized Al-Hikam HTML transcription.

A candidate is eligible for later manual review only when its complete Arabic
base-letter skeleton is identical to one retained production Hikma. No
diacritics are generated and no fuzzy match is promoted.
"""

from __future__ import annotations

import difflib
import html
import json
import re
import unicodedata
import urllib.request
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
OUTDIR = ROOT / "build/research/hikam_diacritics"
OUTDIR.mkdir(parents=True, exist_ok=True)
OUT = OUTDIR / "hikam_diacritics_candidates.json"
REPORT = OUTDIR / "hikam_diacritics_report.json"

SOURCE_URL = "https://www.nafahat-tarik.com/2015/08/sufism22.html"
BLOCK_TAGS = {
    "article", "br", "div", "h1", "h2", "h3", "h4", "li", "main", "p",
    "section", "td", "tr",
}
ARABIC_MARKS = re.compile(
    "[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]"
)
NON_ARABIC = re.compile("[^\u0621-\u063A\u0641-\u064A]+")


class VisibleTextParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.ignore_depth = 0
        self.parts: list[str] = []

    def handle_starttag(self, tag, attrs):
        if tag in {"script", "style", "noscript"}:
            self.ignore_depth += 1
            return
        if not self.ignore_depth and tag in BLOCK_TAGS:
            self.parts.append("\n")

    def handle_endtag(self, tag):
        if tag in {"script", "style", "noscript"}:
            if self.ignore_depth:
                self.ignore_depth -= 1
            return
        if not self.ignore_depth and tag in BLOCK_TAGS:
            self.parts.append("\n")

    def handle_data(self, data):
        if not self.ignore_depth:
            self.parts.append(data)

    def text(self) -> str:
        return html.unescape("".join(self.parts))


def skeleton(value: str) -> str:
    value = unicodedata.normalize("NFKC", value)
    value = ARABIC_MARKS.sub("", value)
    for old, new in (
        ("أ", "ا"), ("إ", "ا"), ("آ", "ا"), ("ٱ", "ا"),
        ("ى", "ي"), ("ؤ", "و"), ("ئ", "ي"), ("ة", "ه"),
    ):
        value = value.replace(old, new)
    return NON_ARABIC.sub("", value)


def normalize_display(value: str) -> str:
    value = unicodedata.normalize("NFC", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value


def extract_numbered_segments(raw_html: str) -> list[dict]:
    parser = VisibleTextParser()
    parser.feed(raw_html)
    lines = [
        normalize_display(line)
        for line in parser.text().splitlines()
        if normalize_display(line)
    ]
    # The article's matn begins at its first numbered Hikma. Later site comments
    # are ignored after the first plausible final-number boundary.
    start = next(
        (i for i, line in enumerate(lines) if re.match(r"^1\)\s*[\u0600-\u06FF]", line)),
        None,
    )
    if start is None:
        raise SystemExit("Could not locate the first numbered Hikma")

    segments: list[dict] = []
    current_number: int | None = None
    current_parts: list[str] = []
    numbered = re.compile(r"^(\d{1,3})\)\s*(.*)$")

    for line in lines[start:]:
        match = numbered.match(line)
        if match:
            if current_number is not None and current_parts:
                segments.append({
                    "declared_number": current_number,
                    "arabic": normalize_display(" ".join(current_parts)),
                })
            current_number = int(match.group(1))
            current_parts = [match.group(2)]
            if current_number == 264 and len(segments) >= 240:
                # Keep collecting the final text until the next numbered block.
                continue
        elif current_number is not None:
            # Stop when the article has clearly ended after a plausible final
            # range. Site UI/comments must never become part of a candidate.
            if len(segments) >= 240 and re.search(
                r"(تعليقات|إرسال تعليق|الصفحة الرئيسية|آخر الأخبار)", line
            ):
                break
            current_parts.append(line)

    if current_number is not None and current_parts:
        segments.append({
            "declared_number": current_number,
            "arabic": normalize_display(" ".join(current_parts)),
        })

    return [
        item for item in segments
        if skeleton(item["arabic"]) and len(skeleton(item["arabic"])) >= 8
    ]


def main() -> None:
    req = urllib.request.Request(
        SOURCE_URL,
        headers={"User-Agent": "QuranSafeguardResearch/0.9.4 (+source audit)"},
    )
    with urllib.request.urlopen(req, timeout=30) as response:
        raw_html = response.read().decode("utf-8", errors="replace")

    corpus = json.loads(CORPUS.read_text(encoding="utf-8"))
    segments = extract_numbered_segments(raw_html)

    by_skeleton: dict[str, list[dict]] = {}
    for index, item in enumerate(segments):
        item["source_order"] = index + 1
        by_skeleton.setdefault(skeleton(item["arabic"]), []).append(item)

    candidates = []
    missing = []
    ambiguous = []
    near = []

    for item in corpus:
        source_number = int(item["source_number"])
        needle = skeleton(item["arabic"])
        exact = by_skeleton.get(needle, [])
        if len(exact) == 1:
            source = exact[0]
            candidates.append({
                "source_number": source_number,
                "source_declared_number": source["declared_number"],
                "source_order": source["source_order"],
                "arabic_vocalized": source["arabic"],
                "source_url": SOURCE_URL,
                "base_letter_skeleton_match": "exact",
                "status": "candidate_needs_manual_boundary_and_source_review",
                "automatic_diacritic_generation": False,
            })
        elif len(exact) > 1:
            ambiguous.append(source_number)
        else:
            missing.append(source_number)
            # Diagnostic only. Fuzzy text is never emitted as a candidate.
            best_score = 0.0
            best_declared = None
            for source in segments:
                observed = skeleton(source["arabic"])
                score = difflib.SequenceMatcher(None, needle, observed).ratio()
                if score > best_score:
                    best_score = score
                    best_declared = source["declared_number"]
            near.append({
                "source_number": source_number,
                "best_declared_number": best_declared,
                "score": round(best_score, 4),
            })

    payload = {
        "collection": "al_hikam_source_diacritics_candidates",
        "source_url": SOURCE_URL,
        "entries": candidates,
    }
    report = {
        "source_url": SOURCE_URL,
        "html_bytes": len(raw_html.encode("utf-8")),
        "numbered_segments": len(segments),
        "exact_skeleton_candidates": len(candidates),
        "missing_numbers": missing,
        "ambiguous_numbers": ambiguous,
        "near_match_diagnostics": near,
        "promotion_policy": {
            "automatic_diacritic_generation_forbidden": True,
            "exact_full_base_letter_skeleton_required": True,
            "manual_boundary_review_required": True,
            "source_locator_required": True,
        },
    }

    OUT.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    REPORT.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(report, ensure_ascii=False))

    if len(candidates) < 200:
        raise SystemExit(
            f"HTML vocalized-source exact alignment too low: {len(candidates)}/264"
        )


if __name__ == "__main__":
    main()
