#!/usr/bin/env python3
"""Regression contract for source-word lines vs verified local Mushaf lines."""
from augment_hifz_word_geometry import match_source_lines


def source_line(*verses):
    return [{"verse": verse, "id": f"{verse}:1"} for verse in verses]


def local_line(line_id, *verses):
    return {"id": line_id, "verses": list(verses), "cells": [[0.0, 100.0]], "top": 0.0, "bottom": 10.0}


# A local detector may contain one extra verified physical band. Word geometry must
# still align all genuine linguistic source lines monotonically without fabricating
# a fifteenth source line or changing the existing local Hifz geometry.
source = [source_line(f"3:{195+i}") for i in range(14)]
local = [local_line(f"76:{i}", f"3:{195+i}") for i in range(7)]
local.append(local_line("76:7", "3:201"))  # extra adjacent local band
local.extend(local_line(f"76:{i+8}", f"3:{202+i}") for i in range(7))

mapping = match_source_lines(source, local)
assert len(mapping) == 14
assert mapping == sorted(mapping)
assert len(set(mapping)) == 14
assert 7 not in mapping, mapping

# The inverse mismatch is also real in the shipped geometry: the detector may merge
# two neighbouring physical text bands into one verified Hifz line. In that case the
# word mapper must preserve every source word while mapping adjacent source lines to
# the same local line; it must not rewrite or invent Hifz lines.
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

# Long verses spanning adjacent lines are valid; rank proximity must remain monotonic.
source2 = [source_line("2:282"), source_line("2:282"), source_line("2:282")]
local2 = [local_line("48:0", "2:282"), local_line("48:1", "2:282"), local_line("48:2", "2:282")]
assert match_source_lines(source2, local2) == [0, 1, 2]

print("HIFZ_WORD_LINE_ALIGNMENT_OK")
