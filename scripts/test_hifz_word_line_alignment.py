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

# Long verses spanning adjacent lines are valid; rank proximity must remain monotonic.
source2 = [source_line("2:282"), source_line("2:282"), source_line("2:282")]
local2 = [local_line("48:0", "2:282"), local_line("48:1", "2:282"), local_line("48:2", "2:282")]
assert match_source_lines(source2, local2) == [0, 1, 2]

print("HIFZ_WORD_LINE_ALIGNMENT_OK")
