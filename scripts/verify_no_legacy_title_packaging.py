#!/usr/bin/env python3
"""Fail if superseded public Ibn Kathir title sources can enter an app package."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "hifz-app" / "build.gradle.kts"


def require(fragment: str, source: str) -> None:
    if fragment not in source:
        raise SystemExit(f"FAIL: missing packaging exclusion: {fragment}")


def main() -> None:
    source = GRADLE.read_text(encoding="utf-8")
    require('assets.exclude("ibn_kathir_headings/**")', source)
    require('java.exclude("**/IbnKathirOfflineTitles.java")', source)
    require('java.exclude("**/IbnKathirTitlePack.java")', source)
    print("PASS: superseded title assets and classes are excluded from main packaging")


if __name__ == "__main__":
    main()
