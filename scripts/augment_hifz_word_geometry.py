#!/usr/bin/env python3
"""Attach pinned linguistic-word boxes to the exact local Hifz geometry.

The coordinate corpus is fetched only at build time from one pinned Git commit.
Runtime remains fully offline and the shipped KFQC SVG pages are never modified.

The upstream boxes are expressed on a 900x1437 image of the same 604-page Madani
pagination. We therefore scale each word directly into the local SVG viewBox instead
of stretching source lines to local ink spans. Each scaled word is then associated
with the nearest verified local physical line only for Hifz 5-line filtering. This
keeps word rectangles true to the page coordinate system and avoids line-width
warping on short/surah-opening lines.
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
            "x": x,
            "y": y,
            "w": w,
            "h": h,
        })
    return words


def scale_word(word: dict, view_box: list[float]) -> dict:
    vx, vy, vw, vh = map(float, view_box)
    sx = vw / SOURCE_WIDTH
    sy = vh / SOURCE_HEIGHT
    return {
        "id": word["id"],
        "verse": word["verse"],
        "kind": "word",
        "x": vx + word["x"] * sx,
        "y": vy + word["y"] * sy,
        "w": word["w"] * sx,
        "h": word["h"] * sy,
    }


def choose_line(lines: list[dict], word: dict) -> tuple[int, float]:
    top = float(word["y"])
    bottom = top + float(word["h"])
    center = (top + bottom) / 2.0
    best_index = -1
    best_overlap = -1.0
    best_distance = math.inf
    for index, line in enumerate(lines):
        line_top = float(line["top"])
        line_bottom = float(line["bottom"])
        overlap = max(0.0, min(bottom, line_bottom) - max(top, line_top))
        line_center = (line_top + line_bottom) / 2.0
        distance = abs(center - line_center)
        if overlap > best_overlap + 1e-9 or (abs(overlap - best_overlap) <= 1e-9 and distance < best_distance):
            best_index = index
            best_overlap = overlap
            best_distance = distance
    if best_index < 0:
        raise RuntimeError("cannot associate scaled word with a local line")
    return best_index, best_distance


def attach_words(root: dict, sources: dict[int, dict]) -> tuple[int, float, int]:
    seen: set[str] = set()
    total = 0
    max_line_center_distance = 0.0
    adjacent_boundary_tolerances = 0

    for page in range(1, EXPECTED_PAGES + 1):
        page_geo = root["pages"][str(page)]
        lines = page_geo["lines"]
        view_box = page_geo.get("viewBox")
        if not isinstance(view_box, list) or len(view_box) != 4:
            raise RuntimeError(f"page {page}: missing local SVG viewBox")
        if not lines:
            raise RuntimeError(f"page {page}: local geometry has no Quran lines")
        for line in lines:
            line["words"] = []

        source_words = parsed_words(page, sources[page])
        source_page_verses = {word["verse"] for word in source_words}
        local_page_verses = {str(verse) for line in lines for verse in line.get("verses", [])}
        if source_page_verses != local_page_verses:
            missing = sorted(source_page_verses - local_page_verses)
            extra = sorted(local_page_verses - source_page_verses)
            raise RuntimeError(
                f"page {page}: source/local pagination mismatch; missing={missing[:6]} extra={extra[:6]}"
            )

        vx, vy, vw, vh = map(float, view_box)
        for raw_word in source_words:
            word_id = raw_word["id"]
            if word_id in seen:
                raise RuntimeError(f"duplicate word coordinate {word_id}")
            seen.add(word_id)
            word = scale_word(raw_word, view_box)
            if (
                word["x"] < vx - 0.5
                or word["y"] < vy - 0.5
                or word["x"] + word["w"] > vx + vw + 0.5
                or word["y"] + word["h"] > vy + vh + 0.5
            ):
                raise RuntimeError(f"page {page} word {word_id}: scaled box outside local SVG viewBox")

            line_index, distance = choose_line(lines, word)
            max_line_center_distance = max(max_line_center_distance, distance)
            current_verses = set(map(str, lines[line_index].get("verses", [])))
            if word["verse"] not in current_verses:
                adjacent = set()
                if line_index > 0:
                    adjacent.update(map(str, lines[line_index - 1].get("verses", [])))
                if line_index + 1 < len(lines):
                    adjacent.update(map(str, lines[line_index + 1].get("verses", [])))
                if word["verse"] not in adjacent:
                    raise RuntimeError(
                        f"page {page} word {word_id}: scaled Y maps to non-adjacent local verse line; "
                        f"line={lines[line_index].get('id')} verse={word['verse']}"
                    )
                adjacent_boundary_tolerances += 1

            lines[line_index]["words"].append({
                "id": word_id,
                "verse": word["verse"],
                "kind": "word",
                "x": round(word["x"], 3),
                "y": round(word["y"], 3),
                "w": round(word["w"], 3),
                "h": round(word["h"], 3),
            })
            total += 1

        for line in lines:
            line["words"].sort(key=lambda item: tuple(map(int, item["id"].split(":"))))

    if total != EXPECTED_WORDS or len(seen) != EXPECTED_WORDS:
        raise RuntimeError(f"word count mismatch: {total} / unique {len(seen)}, expected {EXPECTED_WORDS}")
    return total, max_line_center_distance, adjacent_boundary_tolerances


def main() -> None:
    if not GEOMETRY.is_file():
        raise RuntimeError("generate exact Hifz geometry before attaching word coordinates")
    root = json.loads(GEOMETRY.read_text(encoding="utf-8"))
    sources = page_sources(download_archive())
    total, max_distance, boundary_tolerances = attach_words(root, sources)
    root["wordGeometrySource"] = {
        "repository": SOURCE_REPO,
        "commit": SOURCE_COMMIT,
        "license": "MIT coordinates; upstream Mushaf source assets retain their own terms",
        "sourceWidth": int(SOURCE_WIDTH),
        "sourceHeight": int(SOURCE_HEIGHT),
        "wordCount": total,
        "alignment": "direct source-page box -> local SVG viewBox scaling; nearest verified local line only for Hifz filtering",
        "adjacentBoundaryToleranceCount": boundary_tolerances,
        "runtimeNetwork": False,
    }
    GEOMETRY.write_text(json.dumps(root, separators=(",", ":")), encoding="utf-8")

    report = json.loads(REPORT.read_text(encoding="utf-8")) if REPORT.is_file() else {}
    report.update({
        "wordMaskUnit": "linguistic words; random selection; verse markers excluded",
        "wordCoordinateSource": f"https://github.com/{SOURCE_REPO}@{SOURCE_COMMIT}",
        "wordCoordinateCount": total,
        "wordCoordinateAlignment": "direct 900x1437 source-page scaling into exact local SVG viewBox",
        "wordCoordinateAdjacentBoundaryToleranceCount": boundary_tolerances,
        "wordCoordinateMaxNearestLineCenterDistance": round(max_distance, 3),
    })
    REPORT.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(
        f"HIFZ_WORD_GEOMETRY_OK pages={EXPECTED_PAGES} words={total} "
        f"adjacentBoundaryTolerances={boundary_tolerances} maxLineCenterDistance={max_distance:.3f}"
    )


if __name__ == "__main__":
    main()
