#!/usr/bin/env python3
"""Extract the audited private-personal Qushayri English payload from the exact Sands PDF.

This parser is intentionally fail-closed. It uses the PDF typography/structure rather than
semantic inference. Quran Arabic and the printed English Quran translation are excluded.
Ambiguous multi-verse attribution is hidden rather than guessed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import tempfile
from pathlib import Path
from xml.etree import ElementTree as ET

VERSE_COUNTS = {1: 7, 2: 286, 3: 200, 4: 176}
ANCHOR_RE = re.compile(r"\[(\d{1,3}):(\d{1,3})(?:\s*[–—-]\s*(\d{1,3}))?\]")
ARABIC_RE = re.compile(r"[\u0600-\u06ff\u0750-\u077f\u08a0-\u08ff]")
HEADER_TOKENS = ("Subtle Allusions", "Laṭāʾif al-ishārāt")
BODY_MIN_SIZE = 15
BODY_MAX_SIZE = 20
EXPECTED_PDF_SHA256 = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
EXPECTED_AUDIT = {
    "structural_anchor_events": 806,
    "structural_distinct_verses": 669,
    "mapped_verses": 519,
    "emitted_entries": 644,
    "ambiguous_clusters_hidden": 76,
    "ambiguous_anchor_count": 162,
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def text_of(element: ET.Element) -> str:
    return (
        "".join(element.itertext())
        .replace("\u00ad", "")
        .replace("\ufeff", "")
        .replace("￾", "")
    )


def clean_line(text: str) -> str:
    text = text.replace("\u00ad", "").replace("￾", "")
    return re.sub(r"[ \t]+", " ", text).strip()


def serialized(element: ET.Element) -> str:
    return ET.tostring(element, encoding="unicode")


def has_styled_children(element: ET.Element) -> bool:
    xml = serialized(element)
    return "<i>" in xml or "<b>" in xml


def anchor_translation_line(element: ET.Element, raw: str, match: re.Match[str]) -> bool:
    """True for the edition's printed standalone verse/range translation heading."""
    return match.start() <= 3 and has_styled_children(element)


def styled_translation_continuation(element: ET.Element, raw: str) -> bool:
    """True for a continuation of the printed English Quran translation."""
    if not raw:
        return False
    if (element.text or "").strip():
        return False
    xml = serialized(element)
    return "<i>" in xml or "<b>" in xml


def global_font_map(root: ET.Element) -> dict[str, dict[str, object]]:
    result: dict[str, dict[str, object]] = {}
    for page in root.findall("page"):
        for font in page.findall("fontspec"):
            try:
                size = int(round(float(font.attrib.get("size", "0"))))
            except Exception:
                size = 0
            result[font.attrib["id"]] = {
                "size": size,
                "family": font.attrib.get("family", ""),
            }
    return result


def load_xml(pdf: Path, xml_cache: Path | None) -> ET.Element:
    if xml_cache and xml_cache.is_file():
        return ET.parse(xml_cache).getroot()
    with tempfile.TemporaryDirectory() as directory:
        output = Path(directory) / "lataif"
        subprocess.run(
            ["pdftohtml", "-xml", "-hidden", "-i", str(pdf), str(output)],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        return ET.parse(str(output) + ".xml").getroot()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--xml-cache", type=Path)
    args = parser.parse_args()

    actual_sha = sha256_file(args.pdf)
    if actual_sha != EXPECTED_PDF_SHA256:
        raise SystemExit(f"Unexpected Sands PDF SHA256: {actual_sha}")

    root = load_xml(args.pdf, args.xml_cache)
    fonts = global_font_map(root)

    # Pre-scan printed standalone verse/range translation anchors. Inline references are not
    # allowed to advance the canonical sequence when a real standalone source anchor exists.
    standalone_verses: set[tuple[int, int]] = set()
    for page in root.findall("page"):
        for element in page.findall("text"):
            raw = clean_line(text_of(element))
            if not raw:
                continue
            top = int(round(float(element.attrib.get("top", "0"))))
            info = fonts.get(element.attrib.get("font", ""), {"size": 0, "family": ""})
            size = int(info["size"])
            family = str(info["family"])
            if top < 70 or size < BODY_MIN_SIZE or size > BODY_MAX_SIZE:
                continue
            if "Uthmanic" in family or "KFGQPC" in family or ARABIC_RE.search(raw):
                continue
            for match in ANCHOR_RE.finditer(raw):
                surah = int(match.group(1))
                start = int(match.group(2))
                end = int(match.group(3) or start)
                if 1 <= surah <= 4 and anchor_translation_line(element, raw, match):
                    for ayah in range(start, end + 1):
                        standalone_verses.add((surah, ayah))

    expected = [
        (surah, ayah)
        for surah, count in VERSE_COUNTS.items()
        for ayah in range(1, count + 1)
    ]
    expected_index = -1
    current: tuple[int, int] | None = None
    events: list[dict[str, object]] = []
    skipping_translation = False
    translation_page: int | None = None
    translation_last_top: int | None = None

    for page in root.findall("page"):
        page_number = int(page.attrib["number"])
        page_elements = sorted(
            page.findall("text"),
            key=lambda element: (
                int(round(float(element.attrib.get("top", "0")))) // 4,
                int(round(float(element.attrib.get("left", "0")))),
                int(round(float(element.attrib.get("top", "0")))),
            ),
        )
        for element in page_elements:
            raw = clean_line(text_of(element))
            if not raw:
                continue
            top = int(round(float(element.attrib.get("top", "0"))))
            left = int(round(float(element.attrib.get("left", "0"))))
            info = fonts.get(element.attrib.get("font", ""), {"size": 0, "family": ""})
            size = int(info["size"])
            family = str(info["family"])

            # Ignore headers/page numbers and small translator/editor footnotes in the body pass.
            if top < 70 or size < BODY_MIN_SIZE or size > BODY_MAX_SIZE:
                continue
            if "Uthmanic" in family or "KFGQPC" in family:
                continue
            if ARABIC_RE.search(raw) or re.fullmatch(r"\d+", raw):
                continue

            accepted: tuple[re.Match[str], str] | None = None
            for match in ANCHOR_RE.finditer(raw):
                surah = int(match.group(1))
                start = int(match.group(2))
                end = int(match.group(3) or start)
                if not 1 <= surah <= 4:
                    continue
                if any(token in raw for token in HEADER_TOKENS) and (top < 95 or left < 120):
                    continue
                next_expected = (
                    expected[expected_index + 1]
                    if expected_index + 1 < len(expected)
                    else None
                )
                if current == (surah, start) and match.start() <= 3 and end == start:
                    accepted = (match, "repeat_segment")
                    break
                if (surah, start) == next_expected:
                    standalone_here = anchor_translation_line(element, raw, match)
                    if (surah, start) in standalone_verses and not standalone_here:
                        continue
                    sequence = [(surah, ayah) for ayah in range(start, end + 1)]
                    if expected[
                        expected_index + 1 : expected_index + 1 + len(sequence)
                    ] == sequence:
                        expected_index += len(sequence)
                        current = (surah, end)
                        reason = (
                            "next_range"
                            if end > start
                            else ("next_inline" if not standalone_here else "next")
                        )
                        accepted = (match, reason)
                        break

            if accepted:
                match, reason = accepted
                is_translation = anchor_translation_line(element, raw, match)
                events.append(
                    {
                        "kind": "anchor",
                        "page": page_number,
                        "top": top,
                        "left": left,
                        "surah": int(match.group(1)),
                        "start": int(match.group(2)),
                        "end": int(match.group(3) or match.group(2)),
                        "reason": reason,
                        "raw": raw,
                        "translation_anchor": is_translation,
                    }
                )
                skipping_translation = is_translation
                translation_page = page_number if is_translation else None
                translation_last_top = top if is_translation else None
                if reason == "next_inline":
                    skipping_translation = False
                    # The text before a true inline canonical anchor belongs to the previous
                    # source block. Keep only commentary after the canonical marker.
                    inline_tail = clean_line(raw[match.end() :]).lstrip(" ,;:–—-")
                    if inline_tail:
                        events.append(
                            {
                                "kind": "body",
                                "page": page_number,
                                "top": top,
                                "left": left,
                                "raw": inline_tail,
                            }
                        )
                continue

            if skipping_translation and styled_translation_continuation(element, raw):
                same_page = translation_page == page_number
                close_line = (
                    same_page
                    and translation_last_top is not None
                    and (top - translation_last_top) <= 22
                )
                page_wrap = (
                    translation_page is not None
                    and page_number == translation_page + 1
                    and translation_last_top is not None
                    and translation_last_top > 700
                    and top < 180
                )
                if close_line or page_wrap:
                    translation_page = page_number
                    translation_last_top = top
                    continue
                skipping_translation = False
                translation_page = None
                translation_last_top = None
            elif skipping_translation:
                skipping_translation = False
                translation_page = None
                translation_last_top = None

            if any(token in raw for token in HEADER_TOKENS) and ANCHOR_RE.search(raw):
                continue
            events.append(
                {
                    "kind": "body",
                    "page": page_number,
                    "top": top,
                    "left": left,
                    "raw": raw,
                }
            )

    # Merge pdftohtml fragments that occupy the same visual line.
    merged_events: list[dict[str, object]] = []
    for event in events:
        if (
            event["kind"] == "body"
            and merged_events
            and merged_events[-1]["kind"] == "body"
            and merged_events[-1]["page"] == event["page"]
            and int(merged_events[-1]["top"]) // 4 == int(event["top"]) // 4
        ):
            previous = str(merged_events[-1]["raw"])
            following = str(event["raw"])
            if (
                len(previous.strip()) == 1
                and previous.strip().islower()
                and following[:1].islower()
            ):
                merged_events[-1]["raw"] = previous + following
            else:
                merged_events[-1]["raw"] = (
                    previous.rstrip() + " " + following.lstrip()
                ).strip()
        else:
            merged_events.append(event)
    events = merged_events

    structural_mapped: list[tuple[int, int]] = []
    for event in events:
        if event["kind"] == "anchor" and event["reason"] != "repeat_segment":
            structural_mapped.extend(
                (int(event["surah"]), ayah)
                for ayah in range(int(event["start"]), int(event["end"]) + 1)
            )
    distinct_struct = set(structural_mapped)
    missing = [
        f"{surah}:{ayah}"
        for surah, count in VERSE_COUNTS.items()
        for ayah in range(1, count + 1)
        if (surah, ayah) not in distinct_struct
    ]
    if missing:
        raise SystemExit(
            f"Structural scan incomplete: {len(missing)} missing; first={missing[:10]}"
        )

    entries: list[dict[str, object]] = []
    ambiguous: list[dict[str, object]] = []
    pending: list[dict[str, object]] = []
    current_anchor: dict[str, object] | None = None
    body: list[str] = []
    body_pages: list[int] = []

    def close_current() -> None:
        nonlocal current_anchor, body, body_pages, pending
        if current_anchor is None:
            return
        body_text = "\n".join(value for value in body if value).strip()
        if body_text:
            if pending:
                ambiguous.append(
                    {
                        "anchors": pending + [current_anchor],
                        "reason": "multiple sequential verse anchors precede one commentary block",
                    }
                )
            else:
                entries.append(
                    {
                        "anchor": current_anchor,
                        "body": body_text,
                        "page_end": max(body_pages)
                        if body_pages
                        else int(current_anchor["page"]),
                    }
                )
            pending = []
        else:
            pending.append(current_anchor)
        current_anchor = None
        body = []
        body_pages = []

    for event in events:
        if event["kind"] == "anchor":
            close_current()
            current_anchor = event
            body = []
            body_pages = []
        elif current_anchor is not None:
            raw = str(event["raw"])
            if "sunniconnect" in raw.casefold() or ARABIC_RE.search(raw):
                continue
            if re.match(r"^(Subtle Allusions|Laṭāʾif al-ishārāt)", raw):
                continue
            body.append(raw)
            body_pages.append(int(event["page"]))
    close_current()

    if pending:
        ambiguous.append(
            {"anchors": pending, "reason": "anchor(s) without a following commentary block"}
        )

    segment_count: dict[tuple[int, int, int], int] = {}
    output_entries: list[dict[str, object]] = []
    mapped: set[tuple[int, int]] = set()
    for item in entries:
        anchor = item["anchor"]
        assert isinstance(anchor, dict)
        surah = int(anchor["surah"])
        start = int(anchor["start"])
        end = int(anchor["end"])
        key = (surah, start, end)
        segment_count[key] = segment_count.get(key, 0) + 1
        entry_type = "VERSE_RANGE_COMMENTARY" if end > start else "VERSE_COMMENTARY"
        source_id = f"qs-{surah}-{start}-{end}-s{segment_count[key]}"
        output_entries.append(
            {
                "source_entry_id": source_id,
                "entry_type": entry_type,
                "surah": surah,
                "verse_start": start,
                "verse_end": end,
                "segment_no": segment_count[key],
                "body_runs": [{"style": "regular", "text": str(item["body"])}],
                "source_page_start": int(anchor["page"]),
                "source_page_end": int(item["page_end"]),
                "notes": [],
            }
        )
        for ayah in range(start, end + 1):
            mapped.add((surah, ayah))

    coverage: list[dict[str, object]] = []
    for surah in range(1, 5):
        verses = sorted(ayah for candidate_surah, ayah in mapped if candidate_surah == surah)
        if not verses:
            continue
        range_start = previous = verses[0]
        for ayah in verses[1:]:
            if ayah == previous + 1:
                previous = ayah
            else:
                coverage.append(
                    {
                        "surah": surah,
                        "ayah_start": range_start,
                        "ayah_end": previous,
                        "status": "available",
                    }
                )
                range_start = previous = ayah
        coverage.append(
            {
                "surah": surah,
                "ayah_start": range_start,
                "ayah_end": previous,
                "status": "available",
            }
        )

    audit = {
        "structural_anchor_events": sum(1 for event in events if event["kind"] == "anchor"),
        "structural_distinct_verses": len(distinct_struct),
        "mapped_verses": len(mapped),
        "emitted_entries": len(output_entries),
        "ambiguous_clusters_hidden": len(ambiguous),
        "ambiguous_anchor_count": sum(len(cluster["anchors"]) for cluster in ambiguous),
        "arabic_source_text_included": False,
        "source_translation_lines_excluded": True,
        "notes_policy": "translator/editor footnotes omitted until exact reference binding is audited",
        "regressions": {
            "2:68": (2, 68) in mapped,
            "2:83_structural": (2, 83) in distinct_struct,
            "2:84_structural": (2, 84) in distinct_struct,
            "4:167": (4, 167) in mapped,
            "4:168": (4, 168) in mapped,
            "4:169": (4, 169) in mapped,
            "4:167-169_range": any(
                entry["surah"] == 4
                and entry["verse_start"] == 167
                and entry["verse_end"] == 169
                for entry in output_entries
            ),
            "4:120_false_inline_not_exposed": (4, 120) not in mapped,
        },
    }

    document = {
        "metadata": {
            "schema_version": "tafsir-v2",
            "edition_id": "qushayri_en_sands",
            "source_title": "Lataif al-Isharat — Subtle Allusions, Suras 1–4",
            "source_sha256": actual_sha,
            "translator": "Kristin Zahra Sands",
            "rights_status": "private_personal_only",
            "distribution_scope": "private_personal",
            "private_personal_build_authorized": True,
            "source_audit_status": "verified",
            "content_audit_status": "verified",
            "expected_mapped_verse_count": len(mapped),
            "content_language": "en",
            "arabic_source_text_included": False,
            "parser_policy": "fail_closed_no_semantic_inference",
        },
        "entries": output_entries,
        "coverage": coverage,
        "audit": audit,
        "ambiguous_clusters": [
            {
                "anchors": [
                    f"{anchor['surah']}:{anchor['start']}"
                    if anchor["start"] == anchor["end"]
                    else f"{anchor['surah']}:{anchor['start']}-{anchor['end']}"
                    for anchor in cluster["anchors"]
                ],
                "reason": cluster["reason"],
            }
            for cluster in ambiguous
        ],
    }

    args.output.write_text(
        json.dumps(document, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(json.dumps(audit, indent=2, ensure_ascii=False))

    if any(
        ARABIC_RE.search(str(run["text"]))
        for entry in output_entries
        for run in entry["body_runs"]
    ):
        raise SystemExit("Arabic script leaked into output")
    for key, expected_value in EXPECTED_AUDIT.items():
        if audit[key] != expected_value:
            raise SystemExit(
                f"Qushayri audit drift for {key}: expected {expected_value}, got {audit[key]}"
            )
    if not all(audit["regressions"].values()):
        raise SystemExit(f"Required regression failed: {audit['regressions']}")


if __name__ == "__main__":
    main()
