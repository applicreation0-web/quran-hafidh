#!/usr/bin/env python3
"""Regression contract for QPC word lines vs verified local Mushaf lines."""
import augment_hifz_word_geometry as aug
from augment_hifz_word_geometry import match_source_lines


def source_line(*verses):
    return [{"verse": verse, "id": f"{verse}:1"} for verse in verses]


def local_line(line_id, *verses):
    return {"id": line_id, "verses": list(verses), "cells": [[0.0, 100.0]], "top": 0.0, "bottom": 10.0}


# A local detector may contain one extra verified physical band. Word geometry must
# still align genuine linguistic source lines without changing the Hifz geometry.
source = [source_line(f"3:{195+i}") for i in range(14)]
local = [local_line(f"76:{i}", f"3:{195+i}") for i in range(7)]
local.append(local_line("76:7", "3:201"))
local.extend(local_line(f"76:{i+8}", f"3:{202+i}") for i in range(7))

mapping = match_source_lines(source, local)
assert len(mapping) == 14
assert mapping == sorted(mapping)
assert len(set(mapping)) == 14
assert 7 not in mapping, mapping

# The inverse mismatch is also real: the detector may merge two adjacent physical
# text bands into one local Hifz line. Preserve all source words without adding lines.
source_merged = [source_line(f"4:{1+i}") for i in range(14)]
local_merged = [local_line(f"77:{i}", f"4:{1+i}") for i in range(6)]
local_merged.append(local_line("77:6", "4:7", "4:8"))
local_merged.extend(local_line(f"77:{i+7}", f"4:{9+i}") for i in range(6))

mapping_merged = match_source_lines(source_merged, local_merged)
assert len(mapping_merged) == 14
assert mapping_merged == sorted(mapping_merged)
assert len(set(mapping_merged)) == 13
assert mapping_merged[6] == mapping_merged[7] == 6, mapping_merged
assert mapping_merged[0] == 0 and mapping_merged[-1] == 12

# Long verses spanning adjacent lines remain monotonic.
source2 = [source_line("2:282"), source_line("2:282"), source_line("2:282")]
local2 = [local_line("48:0", "2:282"), local_line("48:1", "2:282"), local_line("48:2", "2:282")]
assert match_source_lines(source2, local2) == [0, 1, 2]

# Cross-page regression: coordinate boxes can use a different page break, while QPC
# V2 correctly places 5:77 on the local page. The width source page must be ignored.
def coord(x, y, w=30.0, h=20.0):
    return {"h": {"x": x, "y": y, "w": w, "h": h}}

old_pages, old_words = aug.EXPECTED_PAGES, aug.EXPECTED_WORDS
try:
    aug.EXPECTED_PAGES = 2
    aug.EXPECTED_WORDS = 5
    synthetic_root = {"pages": {
        "1": {"lines": [
            {"id": "1:0", "verses": ["5:76"], "cells": [[10.0, 90.0]], "top": 10.0, "bottom": 30.0},
            {"id": "1:1", "verses": ["5:77"], "cells": [[10.0, 90.0]], "top": 35.0, "bottom": 55.0},
        ]},
        "2": {"lines": [
            {"id": "2:0", "verses": ["5:78"], "cells": [[10.0, 90.0]], "top": 10.0, "bottom": 30.0},
        ]},
    }}
    coordinate_sources = {
        1: {"coords": {
            "5:76:1": coord(700, 20),
            "5:76:2": coord(600, 20),
        }},
        2: {"coords": {
            "5:77:1": coord(700, 20),
            "5:77:2": coord(600, 20),
            "5:78:1": coord(700, 100),
        }},
    }
    qpc_layout = {
        1: [[
            {"id": "5:76:1", "verse": "5:76", "ordinal": (5, 76, 1)},
            {"id": "5:76:2", "verse": "5:76", "ordinal": (5, 76, 2)},
        ], [
            {"id": "5:77:1", "verse": "5:77", "ordinal": (5, 77, 1)},
            {"id": "5:77:2", "verse": "5:77", "ordinal": (5, 77, 2)},
        ]],
        2: [[
            {"id": "5:78:1", "verse": "5:78", "ordinal": (5, 78, 1)},
        ]],
    }
    result = aug.attach_words(synthetic_root, coordinate_sources, qpc_layout)
    page1_words = [word["id"] for line in synthetic_root["pages"]["1"]["lines"] for word in line.get("words", [])]
    page2_words = [word["id"] for line in synthetic_root["pages"]["2"]["lines"] for word in line.get("words", [])]
    assert page1_words == ["5:76:1", "5:76:2", "5:77:1", "5:77:2"], page1_words
    assert page2_words == ["5:78:1"], page2_words
    assert result[0] == 5
finally:
    aug.EXPECTED_PAGES = old_pages
    aug.EXPECTED_WORDS = old_words

print("HIFZ_WORD_LINE_ALIGNMENT_OK")
