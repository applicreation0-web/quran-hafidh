#!/usr/bin/env python3
"""Attach pinned linguistic-word boxes to the exact local Hifz geometry.

The public coordinate corpus is used only at build time from one pinned commit.
Runtime remains offline and the canonical KFQC SVG pages are never modified.

Upstream words expose natural physical text-line Y bands. Those source lines are
matched monotonically to the already-verified local Quran lines by verse overlap and
rank. The local Hifz geometry remains authoritative: an occasional extra local band is
skipped, while an occasional detector-merged local band may receive two adjacent
source lines. Relative word geometry is transferred only into the matched local band.
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
            lines.append([word])
            centers.append(word["cy"])
            continue
        if abs(word["cy"] - centers[-1]) <= SOURCE_LINE_Y_TOLERANCE:
            lines[-1].append(word)
            centers[-1] = statistics.median(item["cy"] for item in lines[-1])
        else:
            lines.append([word])
            centers.append(word["cy"])
    return lines


def _source_verses(line: list[dict]) -> set[str]:
    return {str(word["verse"]) for word in line}


def _local_verses(line: dict) -> set[str]:
    return set(map(str, line.get("verses", [])))


def match_source_lines(source_lines: list[list[dict]], local_lines: list[dict]) -> list[int]:
    """Map every source word line monotonically onto existing local Hifz lines.

    The local geometry is never rewritten. If the local detector has one or more
    surplus bands, they may be skipped. If it has merged adjacent physical bands,
    adjacent source lines may share one local line. Verse overlap dominates the score;
    reading-order proximity only resolves otherwise equivalent choices.
    """
    n, m = len(source_lines), len(local_lines)
    if n == 0 or m == 0:
        raise RuntimeError("cannot align empty source/local Quran lines")

    source_sets = [_source_verses(line) for line in source_lines]
    local_sets = [_local_verses(line) for line in local_lines]

    def cost(i: int, j: int) -> float:
        source = source_sets[i]
        local = local_sets[j]
        common = len(source & local)
        missing = len(source - local)
        extra = len(local - source)
        target = 0.0 if n == 1 else i * (m - 1) / (n - 1)
        return missing * 10_000.0 - common * 1_000.0 + extra * 20.0 + abs(j - target)

    inf = float("inf")

    if n <= m:
        # Strictly increasing: source lines stay distinct and surplus local detector
        # bands are skipped rather than fabricating linguistic lines.
        dp = [[inf] * m for _ in range(n)]
        prev = [[-1] * m for _ in range(n)]
        for j in range(0, m - n + 1):
            dp[0][j] = cost(0, j)
        for i in range(1, n):
            j_min = i
            j_max = m - (n - i)
            for j in range(j_min, j_max + 1):
                best_value = inf
                best_prev = -1
                for k in range(i - 1, j):
                    value = dp[i - 1][k]
                    if value == inf:
                        continue
                    value += cost(i, j)
                    if value < best_value:
                        best_value = value
                        best_prev = k
                dp[i][j] = best_value
                prev[i][j] = best_prev
        end = min(range(n - 1, m), key=lambda j: dp[n - 1][j])
        if dp[n - 1][end] == inf:
            raise RuntimeError(f"cannot monotonically align {n} source lines to {m} local lines")
        mapping = [0] * n
        mapping[-1] = end
        for i in range(n - 1, 0, -1):
            mapping[i - 1] = prev[i][mapping[i]]
        if any(mapping[i] >= mapping[i + 1] for i in range(n - 1)):
            raise RuntimeError(f"non-monotonic source/local word-line mapping: {mapping}")
        return mapping

    # Source has more physical lines than the detector. Start at local 0 and end at
    # local m-1; each source step may stay on the current local band (a detector merge)
    # or advance by exactly one. This guarantees every existing Hifz line is preserved
    # and used at least once, with the fewest necessary merges.
    dp = [[inf] * m for _ in range(n)]
    prev = [[-1] * m for _ in range(n)]
    dp[0][0] = cost(0, 0)
    merge_penalty = 250.0
    for i in range(1, n):
        j_min = max(0, m - n + i)
        j_max = min(i, m - 1)
        for j in range(j_min, j_max + 1):
            stay = dp[i - 1][j] + merge_penalty if dp[i - 1][j] != inf else inf
            advance = dp[i - 1][j - 1] if j > 0 else inf
            if advance <= stay:
                dp[i][j] = advance + cost(i, j)
                prev[i][j] = j - 1
            else:
                dp[i][j] = stay + cost(i, j)
                prev[i][j] = j
    if dp[n - 1][m - 1] == inf:
        raise RuntimeError(f"cannot merge-align {n} source lines to {m} local lines")
    mapping = [0] * n
    mapping[-1] = m - 1
    for i in range(n - 1, 0, -1):
        mapping[i - 1] = prev[i][mapping[i]]
    if mapping[0] != 0 or mapping[-1] != m - 1:
        raise RuntimeError(f"merged mapping does not cover local geometry: {mapping}")
    if any(mapping[i] > mapping[i + 1] or mapping[i + 1] - mapping[i] > 1 for i in range(n - 1)):
        raise RuntimeError(f"invalid merged source/local word-line mapping: {mapping}")
    if len(set(mapping)) != m:
        raise RuntimeError(f"merged mapping skipped a local Hifz line: {mapping}")
    return mapping


def local_line_span(line: dict) -> tuple[float, float]:
    cells = line.get("cells", [])
    if not cells:
        raise RuntimeError(f"local line {line.get('id')} has no ink cells")
    left = min(float(cell[0]) for cell in cells)
    right = max(float(cell[1]) for cell in cells)
    if not right > left:
        raise RuntimeError(f"local line {line.get('id')} has invalid horizontal extent")
    return left, right


def attach_words(root: dict, sources: dict[int, dict]) -> tuple[int, float, float, int, int, int]:
    seen: set[str] = set()
    total = 0
    min_scale = math.inf
    max_scale = 0.0
    adjacent_boundary_tolerances = 0
    skipped_local_bands = 0
    merged_source_bands = 0

    for page in range(1, EXPECTED_PAGES + 1):
        page_geo = root["pages"][str(page)]
        local_lines = page_geo["lines"]
        for line in local_lines:
            line["words"] = []

        source_words = parsed_words(page, sources[page])
        source_lines = natural_source_lines(source_words)
        try:
            mapping = match_source_lines(source_lines, local_lines)
        except RuntimeError as exc:
            raise RuntimeError(
                f"page {page}: {exc}; sourceLineVerses="
                f"{[sorted(_source_verses(line)) for line in source_lines]}; "
                f"localLineVerses={[sorted(_local_verses(line)) for line in local_lines]}"
            ) from exc

        used_local = set(mapping)
        skipped_local_bands += len(local_lines) - len(used_local)
        merged_source_bands += len(source_lines) - len(used_local)
        source_indices_by_local: dict[int, list[int]] = {}
        for source_index, local_index in enumerate(mapping):
            source_indices_by_local.setdefault(local_index, []).append(source_index)

        source_page_verses = {word["verse"] for word in source_words}
        local_page_verses = {str(verse) for line in local_lines for verse in line.get("verses", [])}
        if source_page_verses != local_page_verses:
            missing = sorted(source_page_verses - local_page_verses)
            extra = sorted(local_page_verses - source_page_verses)
            raise RuntimeError(
                f"page {page}: source/local pagination mismatch; missing={missing[:6]} extra={extra[:6]}"
            )

        for source_index, source_line in enumerate(source_lines):
            line_index = mapping[source_index]
            local_line = local_lines[line_index]
            source_verses = _source_verses(source_line)
            current_verses = _local_verses(local_line)
            allowed_verses = set(current_verses)
            if line_index > 0:
                allowed_verses.update(_local_verses(local_lines[line_index - 1]))
            if line_index + 1 < len(local_lines):
                allowed_verses.update(_local_verses(local_lines[line_index + 1]))
            unexpected = sorted(source_verses - allowed_verses)
            if unexpected:
                raise RuntimeError(
                    f"page {page} line {local_line.get('id')}: non-adjacent verse-layout mismatch {unexpected}; "
                    f"source={sorted(source_verses)} local={sorted(current_verses)} mapping={mapping}"
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
            if not 0.08 <= scale <= 0.90:
                raise RuntimeError(
                    f"page {page} line {local_line.get('id')}: implausible x scale {scale:.4f}; mapping={mapping}"
                )

            full_top = float(local_line["top"]) + 0.6
            full_bottom = float(local_line["bottom"]) - 0.6
            full_height = full_bottom - full_top
            if full_height <= 1:
                raise RuntimeError(f"page {page} line {local_line.get('id')}: invalid local line height")

            group = source_indices_by_local[line_index]
            slot = group.index(source_index)
            group_count = len(group)
            # If the local detector merged two physical bands, preserve their vertical
            # order by subdividing only that existing local band. No Hifz line is added.
            line_top = full_top + full_height * slot / group_count
            line_bottom = full_top + full_height * (slot + 1) / group_count
            line_height = line_bottom - line_top
            if line_height <= 1:
                raise RuntimeError(
                    f"page {page} line {local_line.get('id')}: merged word sub-band too small "
                    f"({line_height:.3f}) for {group_count} source lines"
                )

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

        for local_line in local_lines:
            local_line["words"].sort(key=lambda item: tuple(map(int, item["id"].split(":"))))

    if total != EXPECTED_WORDS or len(seen) != EXPECTED_WORDS:
        raise RuntimeError(f"word count mismatch: {total} / unique {len(seen)}, expected {EXPECTED_WORDS}")
    return total, min_scale, max_scale, adjacent_boundary_tolerances, skipped_local_bands, merged_source_bands


def main() -> None:
    if not GEOMETRY.is_file():
        raise RuntimeError("generate exact Hifz geometry before attaching word coordinates")
    root = json.loads(GEOMETRY.read_text(encoding="utf-8"))
    sources = page_sources(download_archive())
    total, min_scale, max_scale, boundary_tolerances, skipped_bands, merged_bands = attach_words(root, sources)
    root["wordGeometrySource"] = {
        "repository": SOURCE_REPO,
        "commit": SOURCE_COMMIT,
        "license": "MIT coordinates; upstream Mushaf source assets retain their own terms",
        "sourceWidth": int(SOURCE_WIDTH),
        "sourceHeight": int(SOURCE_HEIGHT),
        "wordCount": total,
        "alignment": "natural source Y-bands -> monotonic verse/rank matched verified local lines; surplus local bands skipped; adjacent source bands may share detector-merged local line; per-source-line affine X",
        "adjacentBoundaryToleranceCount": boundary_tolerances,
        "skippedLocalDetectorBandCount": skipped_bands,
        "mergedSourceBandCount": merged_bands,
        "runtimeNetwork": False,
    }
    GEOMETRY.write_text(json.dumps(root, separators=(",", ":")), encoding="utf-8")

    report = json.loads(REPORT.read_text(encoding="utf-8")) if REPORT.is_file() else {}
    report.update({
        "wordMaskUnit": "linguistic words; random selection; verse markers excluded",
        "wordCoordinateSource": f"https://github.com/{SOURCE_REPO}@{SOURCE_COMMIT}",
        "wordCoordinateCount": total,
        "wordCoordinateAlignment": "monotonic verse/rank source-to-local alignment; surplus local bands skipped; detector-merged local bands subdivided for adjacent source lines",
        "wordCoordinateAdjacentBoundaryToleranceCount": boundary_tolerances,
        "wordCoordinateSkippedLocalDetectorBandCount": skipped_bands,
        "wordCoordinateMergedSourceBandCount": merged_bands,
        "wordCoordinateMinXScale": round(min_scale, 6),
        "wordCoordinateMaxXScale": round(max_scale, 6),
    })
    REPORT.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(
        f"HIFZ_WORD_GEOMETRY_OK pages={EXPECTED_PAGES} words={total} "
        f"skippedLocalBands={skipped_bands} mergedSourceBands={merged_bands} "
        f"adjacentBoundaryTolerances={boundary_tolerances} xScale={min_scale:.4f}..{max_scale:.4f}"
    )


if __name__ == "__main__":
    main()
