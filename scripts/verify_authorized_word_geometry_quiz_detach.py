#!/usr/bin/env python3
"""Verify only two exact Quiz interface detachments in Quran QCF word geometry.

All 604 pages / 77432 words, their loading/validation/order and coordinate
methods must remain byte-identical to the official 1.17.1 implementation.
"""
from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parents[1]
BASE="7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21"
PATH="hifz-app/src/main/java/com/quransafeguard/hifz/preview/WordGeometryRepository.java"
old=subprocess.check_output(["git","show",f"{BASE}:{PATH}"],cwd=ROOT).decode("utf-8")
for before,after in [
    ("final class WordGeometryRepository implements QuizCorpus.WordCounts {","final class WordGeometryRepository {"),
    ("@Override public synchronized int wordCountForVerse(int page, VerseRef verse) {","public synchronized int wordCountForVerse(int page, VerseRef verse) {")]:
    assert old.count(before)==1,"Baseline Quiz interface location changed"
    old=old.replace(before,after)
assert (ROOT/PATH).read_text(encoding="utf-8")==old,"QCF geometry differs beyond two Quiz detachments"
assert "QuizCorpus" not in old
print("PASS: QCF geometry and word counts identical to 1.17.1 aside from retired Quiz interface")
