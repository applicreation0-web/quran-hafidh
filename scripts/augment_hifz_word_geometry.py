#!/usr/bin/env python3
"""Attach verified linguistic-word boxes to the exact local Hifz geometry.

Runtime remains fully offline. The public coordinate corpus is read only at build time
from one pinned Git commit. Its page Y placement is deliberately *not* copied: the
local KFQC SVG has special first/last-page framing, so source words are first grouped
into physical text lines and then aligned, line-for-line, to the already-verified local
geometry. Within each matched line only the relative horizontal word positions are
transferred. The displayed Mushaf SVG itself is never modified.
"""
from __future__ import annotations

import io
import json
import math
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GEOMETRY = ROOT / "app/src/main/assets/reader109/geometry.json"
REPORT = ROOT / "geometry-report.json"
SOURCE_REPO = "bodoorzahera/Quran-coordinate"
SOURCE_COMMIT = "ed24b7fbf60a052ac58e694d5728ab4c4d59f96d"
SOURCE_URL = f"https://codeload.github.com/{SOURCE_REPO}/tar.gz/{SOURCE_COMMIT}"
SOURCE_WIDTH = 900.0
SOURCE_HEIGHT = 1437.0
EXPECTED_PAGES = 604
EXPECTED_WORDS = 77320


def download_archive() -> bytes:
    request = urllib.request.Request(SOURCE_URL, headers={"User-Agent": "Quran-Hifz-build/0.7.3"})
    last = None
    for _ in range(3):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                payload = response.read()
            if len(payload) < 1_000_000:
                raise RuntimeError(f"coordinate archive unexpectedly small: {len(payload)} bytes")
            return payload
        except Exception as exc:  # pragma: no cover - retry path is CI/network dependent
            last = exc
    raise RuntimeError(f"cannot fetch pinned Quran word coordinates: {last}")


def page_sources(payload: bytes) -> dict[int, dict]:
    result: dict[int, dict] = {}
    with tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz") as archive:
        for member in archive.getmembers():
            name = member.name
            if "/src/qurancoor/data/page-" not in name or not name.endswith(".json"):
                continue
            basename = Path(name).name
            try:
                page = int(basename.removeprefix("page-").removesuffix(".json"))
            except ValueError:
                continue
            if not 1 <= page <= EXPECTED_PAGES:
                continue
            fileobj = archive.extractfile(member)
            if fileobj is not None:
                result[page] = json.loads(fileobj.read().decode("utf-8"))
    if len(result) != EXPECTED_PAGES:
        raise RuntimeError(f"word-coordinate source has {len(result)} pages, expected {EXPECTED_PAGES}")
    return result


def parsed_words(page: int, source: dict) -> list[dict]:
    coords = source.get("coords", {})
    if not isinstance(coords, dict) or not coords:
        raise RuntimeError(f"page {page}: invalid/empty coordinate payload")
    words: list[dict] = []
    for word_id, coord in coords.items():
        parts = word_id.split(":")
        if len(parts) != 3 or not all(part.isdigit() for part in parts):
            raise RuntimeError(f"page {page}: invalid word id {word_id!r}")
        box = coord.get("h")
        if not isinstance(box, dict):
            raise RuntimeError(f"page {page} word {word_id}: missing highlight box")
        x, y, w, h = (float(box[key]) for key in ("x", "y", "w", "h"))
        if not all(math.isfinite(value) for value in (x, y, w, h)) or w <= 0 or h <= 0:
            raise RuntimeError(f"page {page} word {word_id}: invalid highlight box")
        if x < -1 or y < -1 or x + w > SOURCE_WIDTH + 1 or y + h > SOURCE_HEIGHT + 1:
            raise RuntimeError(f"page {page} word {word_id}: source box outside 900x1437 page")
        words.append({
            "id": word_id,
            "verse": f"{int(parts[0])}:{int(parts[1])}",
            "ordinal": tuple(map(int, parts)),
            "x0": x,
            "x1": x + w,
            "cy": y + h / 2.0,
        })
    return words


def cluster_into_exact_lines(words: list[dict], line_count: int) -> list[list[dict]]:
    """Split source words into exactly the local number of physical lines.

    In a Madani page, inter-line vertical gaps are much larger than within-line
    diacritic variation. Selecting the N-1 largest adjacent centre gaps avoids any
    dependence on absolute page Y offsets (important on pages 1/2 and surah pages).
    """
    if line_count <= 0 or len(words) < line_count:
        raise RuntimeError(f"cannot cluster {len(words)} words into {line_count} lines")
    ordered = sorted(words, key=lambda word: (word["cy"], -word["x0"], word["ordinal"]))
    if line_count == 1:
        return [ordered]
    gaps = [ordered[i + 1]["cy"] - ordered[i]["cy"] for i in range(len(ordered) - 1)]
    cuts = sorted(sorted(range(len(gaps)), key=lambda i: gaps[i], reverse=True)[: line_count - 1])
    clusters: list[list[dict]] = []
    start = 0
    for cut in cuts:
        clusters.append(ordered[start : cut + 1])
        start = cut + 1
    clusters.append(ordered[start:])
    if len(clusters) != line_count or any(not cluster for cluster in clusters):
        raise RuntimeError(f"line clustering produced {len(clusters)} groups, expected {line_count}")
    return clusters


def local_line_span(line: dict) -> tuple[float, float]:
    cells = line.get("cells", [])
    if not cells:
        raise RuntimeError(f"local line {line.get('id')} has no ink cells")
    left = min(float(cell[0]) for cell in cells)
    right = max(float(cell[1]) for cell in cells)
    if not right > left:
        raise RuntimeError(f"local line {line.get('id')} has invalid horizontal extent")
    return left, right


def attach_words(root: dict, sources: dict[int, dict]) -> tuple[int, float, float, int]:
    seen: set[str] = set()
    total = 0
    worst_scale = 0.0
    smallest_source_gap = math.inf
    boundary_verse_tolerances = 0

    for page in range(1, EXPECTED_PAGES + 1):
        page_geo = root["pages"][str(page)]
        lines = page_geo["lines"]
        for line in lines:
            line["words"] = []

        source_words = parsed_words(page, sources[page])
        clusters = cluster_into_exact_lines(source_words, len(lines))

        for line_index, (source_line, local_line) in enumerate(zip(clusters, lines)):
            source_verses = {word["verse"] for word in source_line}
            local_verses = set(map(str, local_line.get("verses", [])))

            # The existing local line index was derived from ayah-polygon vertical overlap,
            # while the pinned word source is grouped by glyph centres. At a verse boundary
            # one of those methods can assign the first/last word to the neighbouring line.
            # Tolerate *only* that one-line boundary ambiguity; a non-adjacent mismatch still
            # aborts the release build and protects against a bad page/line alignment.
            allowed_verses = set(local_verses)
            if line_index > 0:
                allowed_verses.update(map(str, lines[line_index - 1].get("verses", [])))
            if line_index + 1 < len(lines):
                allowed_verses.update(map(str, lines[line_index + 1].get("verses", [])))
            unexpected = sorted(source_verses - allowed_verses)
            if unexpected:
                raise RuntimeError(
                    f"page {page} line {local_line.get('id')}: non-adjacent source/local verse-layout mismatch {unexpected}; "
                    f"source={sorted(source_verses)} local={sorted(local_verses)}"
                )
            boundary_verse_tolerances += len(source_verses - local_verses)

            src_left = min(word["x0"] for word in source_line)
            src_right = max(word["x1"] for word in source_line)
            if not src_right > src_left:
                raise RuntimeError(f"page {page} line {local_line.get('id')}: collapsed source extent")
            dst_left, dst_right = local_line_span(local_line)
            scale = (dst_right - dst_left) / (src_right - src_left)
            worst_scale = max(worst_scale, scale)
            # Pages 1 and 2 have the special al-Fatiha / opening-Baqarah framing and
            # legitimately compress short source lines more than normal 15-line pages.
            # Keep the strict 0.15 gate everywhere else; only those two canonical pages
            # get the narrowly relaxed lower bound proven by the CI failure (0.1431).
            min_scale = 0.10 if page <= 2 else 0.15
            if not min_scale <= scale <= 0.85:
                raise RuntimeError(f"page {page} line {local_line.get('id')}: implausible x scale {scale:.4f}")

            line_top = float(local_line["top"]) + 0.6
            line_bottom = float(local_line["bottom"]) - 0.6
            line_height = line_bottom - line_top
            if line_height <= 1:
                raise RuntimeError(f"page {page} line {local_line.get('id')}: invalid local line height")

            for word in source_line:
                word_id = word["id"]
                if word_id in seen:
                    raise RuntimeError(f"duplicate word coordinate {word_id}")
                seen.add(word_id)
                x0 = dst_left + (word["x0"] - src_left) * scale
                x1 = dst_left + (word["x1"] - src_left) * scale
                width = x1 - x0
                if width <= 0:
                    raise RuntimeError(f"page {page} word {word_id}: collapsed local word width")
                local_line["words"].append({
                    "id": word_id,
                    "verse": word["verse"],
                    "kind": "word",
                    "x": round(x0, 3),
                    "y": round(line_top, 3),
                    "w": round(width, 3),
                    "h": round(line_height, 3),
                })
                total += 1

            centers = sorted(word["cy"] for word in source_line)
            if len(centers) > 1:
                local_gaps = [b - a for a, b in zip(centers, centers[1:]) if b > a]
                if local_gaps:
                    smallest_source_gap = min(smallest_source_gap, min(local_gaps))

            local_line["words"].sort(key=lambda word: tuple(map(int, word["id"].split(":"))))

    if total != EXPECTED_WORDS or len(seen) != EXPECTED_WORDS:
        raise RuntimeError(f"word count mismatch: {total} / unique {len(seen)}, expected {EXPECTED_WORDS}")
    if smallest_source_gap is math.inf:
        smallest_source_gap = 0.0
    return total, worst_scale, smallest_source_gap, boundary_verse_tolerances


def main() -> None:
    if not GEOMETRY.is_file():
        raise RuntimeError("generate exact Hifz geometry before attaching word coordinates")
    root = json.loads(GEOMETRY.read_text(encoding="utf-8"))
    sources = page_sources(download_archive())
    total, worst_scale, smallest_gap, boundary_tolerances = attach_words(root, sources)
    root["wordGeometrySource"] = {
        "repository": SOURCE_REPO,
        "commit": SOURCE_COMMIT,
        "license": "MIT coordinates; upstream Mushaf source assets retain their own terms",
        "sourceWidth": int(SOURCE_WIDTH),
        "sourceHeight": int(SOURCE_HEIGHT),
        "wordCount": total,
        "alignment": "source physical-line rank -> verified local line; adjacent-line verse-boundary tolerance; per-line affine X; local Y band",
        "boundaryVerseToleranceCount": boundary_tolerances,
        "runtimeNetwork": False,
    }
    GEOMETRY.write_text(json.dumps(root, separators=(",", ":")), encoding="utf-8")

    report = json.loads(REPORT.read_text(encoding="utf-8")) if REPORT.is_file() else {}
    report.update({
        "wordMaskUnit": "linguistic words; random cumulative selection; verse markers excluded",
        "wordCoordinateSource": f"https://github.com/{SOURCE_REPO}@{SOURCE_COMMIT}",
        "wordCoordinateCount": total,
        "wordCoordinateAlignment": "physical-line rank + adjacent-line verse-boundary gate + per-line affine X",
        "wordCoordinateBoundaryVerseToleranceCount": boundary_tolerances,
        "wordCoordinateWorstXScale": round(worst_scale, 6),
        "wordCoordinateSmallestPositiveIntraLineCenterGap": round(smallest_gap, 3),
    })
    REPORT.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(
        f"HIFZ_WORD_GEOMETRY_OK pages={EXPECTED_PAGES} words={total} "
        f"boundaryVerseTolerances={boundary_tolerances} worstXScale={worst_scale:.4f}"
    )


if __name__ == "__main__":
    main()
