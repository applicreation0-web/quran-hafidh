#!/usr/bin/env python3
"""Attach pinned linguistic-word boxes to the exact local Hifz geometry.

The public coordinate corpus is used only at build time from one pinned commit.
Runtime remains offline and the canonical KFQC SVG pages are never modified.

Upstream words already share a common Y value for each physical Mushaf text line.
We therefore recover those natural source lines, require their count to match the
verified local Quran-line count, pair lines in reading order, and transfer only the
relative horizontal word geometry into each exact local line band. This avoids using
absolute page margins (which differ on framed/surah pages) and avoids inventing line
breaks from generic gap heuristics.
"""
from __future__ import annotations

import io
import json
import math
import statistics
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
SOURCE_LINE_Y_TOLERANCE = 6.0


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
        except Exception as exc:  # pragma: no cover
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
            "y": y,
            "cy": y + h / 2.0,
        })
    return words


def natural_source_lines(words: list[dict]) -> list[list[dict]]:
    """Recover physical source lines from the coordinate data's own Y bands."""
    ordered = sorted(words, key=lambda word: (word["cy"], -word["x0"], word["ordinal"]))
    lines: list[list[dict]] = []
    centers: list[float] = []
    for word in ordered:
        if not lines:
            lines.append([word]); centers.append(word["cy"]); continue
        if abs(word["cy"] - centers[-1]) <= SOURCE_LINE_Y_TOLERANCE:
            lines[-1].append(word)
            centers[-1] = statistics.median(item["cy"] for item in lines[-1])
        else:
            lines.append([word]); centers.append(word["cy"])
    return lines


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
    min_scale = math.inf
    max_scale = 0.0
    adjacent_boundary_tolerances = 0

    for page in range(1, EXPECTED_PAGES + 1):
        page_geo = root["pages"][str(page)]
        local_lines = page_geo["lines"]
        for line in local_lines:
            line["words"] = []

        source_words = parsed_words(page, sources[page])
        source_lines = natural_source_lines(source_words)
        if len(source_lines) != len(local_lines):
            raise RuntimeError(
                f"page {page}: natural source/local line-count mismatch "
                f"{len(source_lines)} != {len(local_lines)}"
            )

        source_page_verses = {word["verse"] for word in source_words}
        local_page_verses = {str(verse) for line in local_lines for verse in line.get("verses", [])}
        if source_page_verses != local_page_verses:
            missing = sorted(source_page_verses - local_page_verses)
            extra = sorted(local_page_verses - source_page_verses)
            raise RuntimeError(
                f"page {page}: source/local pagination mismatch; missing={missing[:6]} extra={extra[:6]}"
            )

        for line_index, (source_line, local_line) in enumerate(zip(source_lines, local_lines)):
            source_verses = {word["verse"] for word in source_line}
            current_verses = set(map(str, local_line.get("verses", [])))
            allowed_verses = set(current_verses)
            if line_index > 0:
                allowed_verses.update(map(str, local_lines[line_index - 1].get("verses", [])))
            if line_index + 1 < len(local_lines):
                allowed_verses.update(map(str, local_lines[line_index + 1].get("verses", [])))
            unexpected = sorted(source_verses - allowed_verses)
            if unexpected:
                raise RuntimeError(
                    f"page {page} line {local_line.get('id')}: non-adjacent verse-layout mismatch {unexpected}; "
                    f"source={sorted(source_verses)} local={sorted(current_verses)}"
                )
            adjacent_boundary_tolerances += len(source_verses - current_verses)

            src_left = min(word["x0"] for word in source_line)
            src_right = max(word["x1"] for word in source_line)
            if not src_right > src_left:
                raise RuntimeError(f"page {page} line {local_line.get('id')}: collapsed source extent")
            dst_left, dst_right = local_line_span(local_line)
            scale = (dst_right - dst_left) / (src_right - src_left)
            min_scale = min(min_scale, scale)
            max_scale = max(max_scale, scale)
            # A real line mapping may be strongly compressed on framed opening pages,
            # but a value outside this broad guard indicates a wrong physical-line pair.
            if not 0.08 <= scale <= 0.90:
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

            local_line["words"].sort(key=lambda item: tuple(map(int, item["id"].split(":"))))

    if total != EXPECTED_WORDS or len(seen) != EXPECTED_WORDS:
        raise RuntimeError(f"word count mismatch: {total} / unique {len(seen)}, expected {EXPECTED_WORDS}")
    return total, min_scale, max_scale, adjacent_boundary_tolerances


def main() -> None:
    if not GEOMETRY.is_file():
        raise RuntimeError("generate exact Hifz geometry before attaching word coordinates")
    root = json.loads(GEOMETRY.read_text(encoding="utf-8"))
    sources = page_sources(download_archive())
    total, min_scale, max_scale, boundary_tolerances = attach_words(root, sources)
    root["wordGeometrySource"] = {
        "repository": SOURCE_REPO,
        "commit": SOURCE_COMMIT,
        "license": "MIT coordinates; upstream Mushaf source assets retain their own terms",
        "sourceWidth": int(SOURCE_WIDTH),
        "sourceHeight": int(SOURCE_HEIGHT),
        "wordCount": total,
        "alignment": "natural source Y-bands -> verified local line rank; adjacent verse-boundary gate; per-line affine X; local Y band",
        "adjacentBoundaryToleranceCount": boundary_tolerances,
        "runtimeNetwork": False,
    }
    GEOMETRY.write_text(json.dumps(root, separators=(",", ":")), encoding="utf-8")

    report = json.loads(REPORT.read_text(encoding="utf-8")) if REPORT.is_file() else {}
    report.update({
        "wordMaskUnit": "linguistic words; random selection; verse markers excluded",
        "wordCoordinateSource": f"https://github.com/{SOURCE_REPO}@{SOURCE_COMMIT}",
        "wordCoordinateCount": total,
        "wordCoordinateAlignment": "natural source Y-bands + local line rank + per-line affine X",
        "wordCoordinateAdjacentBoundaryToleranceCount": boundary_tolerances,
        "wordCoordinateMinXScale": round(min_scale, 6),
        "wordCoordinateMaxXScale": round(max_scale, 6),
    })
    REPORT.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(
        f"HIFZ_WORD_GEOMETRY_OK pages={EXPECTED_PAGES} words={total} "
        f"adjacentBoundaryTolerances={boundary_tolerances} xScale={min_scale:.4f}..{max_scale:.4f}"
    )


if __name__ == "__main__":
    main()
