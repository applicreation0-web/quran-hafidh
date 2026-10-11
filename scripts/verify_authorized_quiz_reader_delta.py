#!/usr/bin/env python3
"""Exact immutable exception to the 1.17.1 QCF renderer freeze: Quiz prompt holes ONLY.

This gate forbids ANY reader.js drift beyond the SHA-pinned, visually
instrumented Android fix for the blank 100%-masked Quiz screen.
"""
import pathlib
import subprocess

ROOT=pathlib.Path(__file__).resolve().parents[1]
FILE="hifz-app/src/main/assets/hifzreader/reader.js"
BASE="7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21"
ORIGINAL="8a9611a8bbd0e5983f0f9ee4e98ddfd9484efa44"
APPROVED="3dbac04861730d982da8a8f87c25a4415cb352dc"

def blob(path):
    return subprocess.check_output(["git","hash-object",str(path)],cwd=ROOT,text=True).strip()

assert subprocess.check_output(["git","rev-parse",f"{BASE}:{FILE}"],cwd=ROOT,text=True).strip()==ORIGINAL, "Official Quiz renderer baseline changed"
assert blob(ROOT/FILE)==APPROVED, "Quiz-only mask code diverged from byte-audited and Android-tested implementation"
source=(ROOT/FILE).read_text(encoding="utf-8")
assert source.count("function quizMaskElement(segment,svg,protectedRects)")==1
assert source.count("semanticCues.some(cue=>cue.id==='quiz-prompt')")==1
assert source.count("if(!quizPrompt)applyProtectedWordHoles(layer,svg);")==1
assert source.count("paintMask(segment)")==3
print("PASS: only exact approved Quiz 100% hole-rendering delta; other QCF masks unchanged")
