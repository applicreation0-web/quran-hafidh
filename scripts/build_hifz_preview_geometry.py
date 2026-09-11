#!/usr/bin/env python3
"""Build lazy Hifz geometry views from the exact reader109 geometry.

No Quran glyph/text data is modified. This script only splits/compacts the read-only
geometric index so BOOX does not parse geometry for all 604 pages at startup.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
src = ROOT / "app/src/main/assets/reader109/geometry.json"
out_dir = ROOT / "app/src/main/assets/reader109/hifz-pages"
out_dir.mkdir(parents=True, exist_ok=True)

root = json.loads(src.read_text())
pages = root["pages"]
index_pages = {}
line_count = 0
for page in range(1, 605):
    p = pages[str(page)]
    lines = p.get("lines", [])
    compact_lines = []
    for line in lines:
        compact_lines.append({
            "id": line["id"],
            "page": int(line.get("page", page)),
            "verses": line.get("verses", []),
        })
        line_count += 1
    index_pages[str(page)] = {"lines": compact_lines}
    # Mask renderer needs full cells/top/bottom, but only for the visible page.
    payload = {"viewBox": p.get("viewBox"), "lines": lines}
    (out_dir / f"{page:03d}.json").write_text(json.dumps(payload, separators=(",", ":")))

index = {
    "schema": 1,
    "pages": index_pages,
    "pageCount": 604,
    "lineCount": line_count,
}
(ROOT / "app/src/main/assets/reader109/hifz-index.json").write_text(
    json.dumps(index, separators=(",", ":"))
)
print(json.dumps({"pages": 604, "lines": line_count, "index": "hifz-index.json"}))
