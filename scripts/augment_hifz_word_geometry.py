#!/usr/bin/env python3
"""Attach verified per-word Madani Mushaf boxes to the exact local Hifz geometry.

Runtime remains fully offline. Word coordinates are fetched only while producing the
release asset, from one pinned public Git commit, then scaled to the local SVG viewBox.
The displayed Mushaf SVG is never modified.
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
MAX_LINE_DISTANCE = 7.0


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
            if not 1 <= page <= 604:
                continue
            fileobj = archive.extractfile(member)
            if fileobj is None:
                continue
            result[page] = json.loads(fileobj.read().decode("utf-8"))
    if len(result) != EXPECTED_PAGES:
        raise RuntimeError(f"word-coordinate source has {len(result)} pages, expected {EXPECTED_PAGES}")
    return result


def interval_distance(value: float, low: float, high: float) -> float:
    if low <= value <= high:
        return 0.0
    return low - value if value < low else value - high


def attach_words(root: dict, sources: dict[int, dict]) -> tuple[int, float]:
    seen: set[str] = set()
    total = 0
    worst_distance = 0.0
    for page in range(1, 605):
        page_geo = root["pages"][str(page)]
        view = page_geo["viewBox"]
        vx, vy, vw, vh = map(float, view)
        sx, sy = vw / SOURCE_WIDTH, vh / SOURCE_HEIGHT
        lines = page_geo["lines"]
        for line in lines:
            line["words"] = []

        coords = sources[page].get("coords", {})
        if not isinstance(coords, dict):
            raise RuntimeError(f"page {page}: invalid coordinate payload")
        for word_id, coord in coords.items():
            parts = word_id.split(":")
            if len(parts) != 3 or not all(part.isdigit() for part in parts):
                raise RuntimeError(f"page {page}: invalid word id {word_id!r}")
            if word_id in seen:
                raise RuntimeError(f"duplicate word coordinate {word_id}")
            seen.add(word_id)
            verse = f"{int(parts[0])}:{int(parts[1])}"
            box = coord.get("h")
            if not isinstance(box, dict):
                raise RuntimeError(f"page {page} word {word_id}: missing highlight box")
            x = vx + float(box["x"]) * sx
            y = vy + float(box["y"]) * sy
            w = float(box["w"]) * sx
            h = float(box["h"]) * sy
            if not all(math.isfinite(v) for v in (x, y, w, h)) or w <= 0 or h <= 0:
                raise RuntimeError(f"page {page} word {word_id}: invalid box")
            if x < vx - 1 or y < vy - 1 or x + w > vx + vw + 1 or y + h > vy + vh + 1:
                raise RuntimeError(f"page {page} word {word_id}: box outside local viewBox")

            cy = y + h / 2.0
            verse_lines = [line for line in lines if verse in line.get("verses", [])]
            if not verse_lines:
                raise RuntimeError(f"page {page} word {word_id}: verse absent from local geometry")
            best = min(verse_lines, key=lambda line: interval_distance(cy, float(line["top"]), float(line["bottom"])))
            distance = interval_distance(cy, float(best["top"]), float(best["bottom"]))
            worst_distance = max(worst_distance, distance)
            if distance > MAX_LINE_DISTANCE:
                raise RuntimeError(
                    f"page {page} word {word_id}: source/local line mismatch {distance:.3f} SVG units"
                )
            best["words"].append({
                "id": word_id,
                "verse": verse,
                "kind": "word",
                "x": round(x, 3),
                "y": round(y, 3),
                "w": round(w, 3),
                "h": round(h, 3),
            })
            total += 1

        # Canonical word order inside every physical line: verse ordinal then word position.
        for line in lines:
            line["words"].sort(key=lambda word: tuple(map(int, word["id"].split(":"))))

    if total != EXPECTED_WORDS or len(seen) != EXPECTED_WORDS:
        raise RuntimeError(f"word count mismatch: {total} / unique {len(seen)}, expected {EXPECTED_WORDS}")
    return total, worst_distance


def main() -> None:
    if not GEOMETRY.is_file():
        raise RuntimeError("generate exact Hifz geometry before attaching word coordinates")
    root = json.loads(GEOMETRY.read_text(encoding="utf-8"))
    sources = page_sources(download_archive())
    total, worst = attach_words(root, sources)
    root["wordGeometrySource"] = {
        "repository": SOURCE_REPO,
        "commit": SOURCE_COMMIT,
        "license": "MIT coordinates; upstream Mushaf source assets retain their own terms",
        "sourceWidth": int(SOURCE_WIDTH),
        "sourceHeight": int(SOURCE_HEIGHT),
        "wordCount": total,
        "runtimeNetwork": False,
    }
    GEOMETRY.write_text(json.dumps(root, separators=(",", ":")), encoding="utf-8")

    report = json.loads(REPORT.read_text(encoding="utf-8")) if REPORT.is_file() else {}
    report.update({
        "wordMaskUnit": "linguistic words; random cumulative selection; verse markers excluded",
        "wordCoordinateSource": f"https://github.com/{SOURCE_REPO}@{SOURCE_COMMIT}",
        "wordCoordinateCount": total,
        "wordCoordinateWorstLineDistance": round(worst, 3),
    })
    REPORT.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(f"HIFZ_WORD_GEOMETRY_OK pages={EXPECTED_PAGES} words={total} worstLineDistance={worst:.3f}")


if __name__ == "__main__":
    main()
