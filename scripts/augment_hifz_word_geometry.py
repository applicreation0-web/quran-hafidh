#!/usr/bin/env python3
"""Attach linguistic-word mask boxes to the exact local Hifz geometry.

Two pinned build-time inputs are deliberately separated:

* Quran-coordinate supplies only per-word highlight widths. Its page breaks are not
  trusted because they differ from the QPC V2 Mushaf used by Quran Hifz.
* mushaf-layout supplies QPC V2 page/line/word order. This is the authoritative
  physical line layout for placing those words onto the existing local SVG geometry.

Runtime remains fully offline. The canonical 604 SVG pages and the already-verified
Hifz line geometry are never rewritten; this script only adds a `words` list to each
existing local line.
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

COORD_REPO = "bodoorzahera/Quran-coordinate"
COORD_COMMIT = "ed24b7fbf60a052ac58e694d5728ab4c4d59f96d"
COORD_URL = f"https://codeload.github.com/{COORD_REPO}/tar.gz/{COORD_COMMIT}"
COORD_WIDTH = 900.0
COORD_HEIGHT = 1437.0

LAYOUT_REPO = "zonetecde/mushaf-layout"
LAYOUT_COMMIT = "72116ce4d405d67823804f0eed795c1e6409b4af"
LAYOUT_URL = f"https://codeload.github.com/{LAYOUT_REPO}/tar.gz/{LAYOUT_COMMIT}"

EXPECTED_PAGES = 604
EXPECTED_WORDS = 77320


def download_archive(url: str, label: str, minimum_size: int) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "Quran-Hifz-build/0.7.3"})
    last = None
    for _ in range(3):
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                payload = response.read()
            if len(payload) < minimum_size:
                raise RuntimeError(f"{label} archive unexpectedly small: {len(payload)} bytes")
            return payload
        except Exception as exc:  # pragma: no cover
            last = exc
    raise RuntimeError(f"cannot fetch pinned {label}: {last}")


def coordinate_pages(payload: bytes) -> dict[int, dict]:
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


def coordinate_word_widths(sources: dict[int, dict]) -> dict[str, float]:
    """Return global word-id -> width; source page placement is intentionally ignored."""
    widths: dict[str, float] = {}
    for page, source in sources.items():
        coords = source.get("coords", {})
        if not isinstance(coords, dict) or not coords:
            raise RuntimeError(f"coordinate page {page}: invalid/empty coordinate payload")
        for word_id, coord in coords.items():
            parts = word_id.split(":")
            if len(parts) != 3 or not all(part.isdigit() for part in parts):
                raise RuntimeError(f"coordinate page {page}: invalid word id {word_id!r}")
            box = coord.get("h")
            if not isinstance(box, dict):
                raise RuntimeError(f"coordinate page {page} word {word_id}: missing highlight box")
            x, y, w, h = (float(box[key]) for key in ("x", "y", "w", "h"))
            if not all(math.isfinite(value) for value in (x, y, w, h)) or w <= 0 or h <= 0:
                raise RuntimeError(f"coordinate page {page} word {word_id}: invalid highlight box")
            if x < -1 or y < -1 or x + w > COORD_WIDTH + 1 or y + h > COORD_HEIGHT + 1:
                raise RuntimeError(f"coordinate page {page} word {word_id}: box outside 900x1437 page")
            if word_id in widths:
                raise RuntimeError(f"duplicate coordinate word id {word_id}")
            widths[word_id] = w
    if len(widths) != EXPECTED_WORDS:
        raise RuntimeError(f"coordinate source word count {len(widths)} != {EXPECTED_WORDS}")
    return widths


def layout_pages(payload: bytes) -> dict[int, list[list[dict]]]:
    """Parse pinned QPC V2 text-line layout; headers/basmala are never mask candidates."""
    result: dict[int, list[list[dict]]] = {}
    seen: set[str] = set()
    with tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz") as archive:
        for member in archive.getmembers():
            name = member.name
            if "/mushaf/page-" not in name or not name.endswith(".json"):
                continue
            basename = Path(name).name
            try:
                page = int(basename.removeprefix("page-").removesuffix(".json"))
            except ValueError:
                continue
            if not 1 <= page <= EXPECTED_PAGES:
                continue
            fileobj = archive.extractfile(member)
            if fileobj is None:
                continue
            document = json.loads(fileobj.read().decode("utf-8"))
            if int(document.get("page", page)) != page:
                raise RuntimeError(f"layout page number mismatch for file {basename}")
            text_lines: list[tuple[int, list[dict]]] = []
            for row in document.get("lines", []):
                if row.get("type") != "text":
                    continue
                line_number = int(row.get("line", 0))
                words: list[dict] = []
                for item in row.get("words", []):
                    word_id = str(item.get("location", ""))
                    parts = word_id.split(":")
                    if len(parts) != 3 or not all(part.isdigit() for part in parts):
                        raise RuntimeError(f"layout page {page}: invalid word id {word_id!r}")
                    if word_id in seen:
                        raise RuntimeError(f"duplicate layout word id {word_id}")
                    seen.add(word_id)
                    words.append({
                        "id": word_id,
                        "verse": f"{int(parts[0])}:{int(parts[1])}",
                        "ordinal": tuple(map(int, parts)),
                    })
                if words:
                    text_lines.append((line_number, words))
            text_lines.sort(key=lambda pair: pair[0])
            result[page] = [words for _line_number, words in text_lines]
    if len(result) != EXPECTED_PAGES:
        raise RuntimeError(f"QPC layout source has {len(result)} pages, expected {EXPECTED_PAGES}")
    if len(seen) != EXPECTED_WORDS:
        raise RuntimeError(f"QPC layout word count {len(seen)} != {EXPECTED_WORDS}")
    return result


def _source_verses(line: list[dict]) -> set[str]:
    return {str(word["verse"]) for word in line}


def _local_verses(line: dict) -> set[str]:
    return set(map(str, line.get("verses", [])))


def match_source_lines(source_lines: list[list[dict]], local_lines: list[dict]) -> list[int]:
    """Map QPC text lines monotonically onto the unchanged verified local Hifz lines."""
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


def _layout_word_ids(layout: dict[int, list[list[dict]]]) -> set[str]:
    return {word["id"] for lines in layout.values() for line in lines for word in line}


def attach_words(
    root: dict,
    coordinate_sources: dict[int, dict],
    qpc_layout: dict[int, list[list[dict]]],
) -> tuple[int, float, float, int, int, int]:
    widths = coordinate_word_widths(coordinate_sources)
    layout_ids = _layout_word_ids(qpc_layout)
    width_ids = set(widths)
    if layout_ids != width_ids:
        missing_width = sorted(layout_ids - width_ids)
        unused_width = sorted(width_ids - layout_ids)
        raise RuntimeError(
            "coordinate/QPC linguistic word sets differ; "
            f"missingWidth={missing_width[:8]} unusedWidth={unused_width[:8]} "
            f"layout={len(layout_ids)} coordinate={len(width_ids)}"
        )

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

        source_lines = qpc_layout[page]
        if not source_lines:
            raise RuntimeError(f"QPC layout page {page}: no text lines")

        local_page_verses = {str(verse) for line in local_lines for verse in line.get("verses", [])}
        source_page_verses = {word["verse"] for line in source_lines for word in line}
        if source_page_verses != local_page_verses:
            missing = sorted(source_page_verses - local_page_verses)
            extra = sorted(local_page_verses - source_page_verses)
            raise RuntimeError(
                f"page {page}: QPC/local pagination mismatch; missing={missing[:6]} extra={extra[:6]}"
            )

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

            dst_left, dst_right = local_line_span(local_line)
            source_total_width = sum(widths[word["id"]] for word in source_line)
            if source_total_width <= 0:
                raise RuntimeError(f"page {page} line {local_line.get('id')}: invalid source word widths")
            scale = (dst_right - dst_left) / source_total_width
            min_scale = min(min_scale, scale)
            max_scale = max(max_scale, scale)
            if not 0.08 <= scale <= 1.2:
                raise RuntimeError(
                    f"page {page} line {local_line.get('id')}: implausible word-width scale {scale:.4f}; mapping={mapping}"
                )

            full_top = float(local_line["top"]) + 0.8
            full_bottom = float(local_line["bottom"]) - 0.8
            full_height = full_bottom - full_top
            if full_height <= 1:
                raise RuntimeError(f"page {page} line {local_line.get('id')}: invalid local line height")

            group = source_indices_by_local[line_index]
            slot = group.index(source_index)
            group_count = len(group)
            line_top = full_top + full_height * slot / group_count
            line_bottom = full_top + full_height * (slot + 1) / group_count
            line_height = line_bottom - line_top
            if line_height <= 1:
                raise RuntimeError(
                    f"page {page} line {local_line.get('id')}: merged word sub-band too small "
                    f"({line_height:.3f}) for {group_count} source lines"
                )

            cursor = dst_right
            for word in source_line:
                word_id = word["id"]
                if word_id in seen:
                    raise RuntimeError(f"duplicate packaged word coordinate {word_id}")
                seen.add(word_id)
                raw_width = widths[word_id] * scale
                x0 = cursor - raw_width
                inset = min(0.35, raw_width * 0.035)
                box_x = x0 + inset
                box_width = max(0.5, raw_width - inset * 2)
                local_line["words"].append({
                    "id": word_id,
                    "verse": word["verse"],
                    "kind": "word",
                    "x": round(box_x, 3),
                    "y": round(line_top, 3),
                    "w": round(box_width, 3),
                    "h": round(line_height, 3),
                })
                cursor = x0
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

    coordinate_payload = download_archive(COORD_URL, "Quran word coordinates", 1_000_000)
    layout_payload = download_archive(LAYOUT_URL, "QPC V2 Mushaf layout", 1_000_000)
    coordinate_sources = coordinate_pages(coordinate_payload)
    qpc_layout = layout_pages(layout_payload)

    page120_ids = {word["id"] for line in qpc_layout[120] for word in line}
    if "5:77:1" not in page120_ids:
        raise RuntimeError("QPC V2 layout regression: 5:77:1 must be on page 120")

    total, min_scale, max_scale, boundary_tolerances, skipped_bands, merged_bands = attach_words(
        root, coordinate_sources, qpc_layout
    )
    root["wordGeometrySource"] = {
        "coordinateRepository": COORD_REPO,
        "coordinateCommit": COORD_COMMIT,
        "coordinateLicense": "MIT",
        "layoutRepository": LAYOUT_REPO,
        "layoutCommit": LAYOUT_COMMIT,
        "layoutBasis": "QPC V2 604-page Madani Mushaf page/line/word mapping",
        "layoutRightsNote": "Pinned public build-time layout metadata; raw upstream files are not bundled in the APK.",
        "wordCount": total,
        "alignment": "QPC V2 page/line order -> monotonic verse/rank matched verified local lines; Quran-coordinate used only for per-word width ratios",
        "adjacentBoundaryToleranceCount": boundary_tolerances,
        "skippedLocalDetectorBandCount": skipped_bands,
        "mergedSourceBandCount": merged_bands,
        "runtimeNetwork": False,
    }
    GEOMETRY.write_text(json.dumps(root, separators=(",", ":")), encoding="utf-8")

    report = json.loads(REPORT.read_text(encoding="utf-8")) if REPORT.is_file() else {}
    report.update({
        "wordMaskUnit": "linguistic words; deterministic-random selection; verse markers excluded",
        "wordCoordinateSource": f"https://github.com/{COORD_REPO}@{COORD_COMMIT}",
        "wordLayoutSource": f"https://github.com/{LAYOUT_REPO}@{LAYOUT_COMMIT}",
        "wordCoordinateCount": total,
        "wordCoordinateAlignment": "QPC V2 page/line mapping + coordinate-source word width ratios + unchanged local Hifz line bands",
        "wordCoordinateAdjacentBoundaryToleranceCount": boundary_tolerances,
        "wordCoordinateSkippedLocalDetectorBandCount": skipped_bands,
        "wordCoordinateMergedSourceBandCount": merged_bands,
        "wordCoordinateMinWidthScale": round(min_scale, 6),
        "wordCoordinateMaxWidthScale": round(max_scale, 6),
    })
    REPORT.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(
        f"HIFZ_WORD_GEOMETRY_OK pages={EXPECTED_PAGES} words={total} "
        f"skippedLocalBands={skipped_bands} mergedSourceBands={merged_bands} "
        f"adjacentBoundaryTolerances={boundary_tolerances} widthScale={min_scale:.4f}..{max_scale:.4f}"
    )


if __name__ == "__main__":
    main()
