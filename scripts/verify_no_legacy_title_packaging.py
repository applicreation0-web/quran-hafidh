#!/usr/bin/env python3
"""Fail if superseded public Ibn Kathir title sources can enter an app package."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "hifz-app" / "build.gradle.kts"
MAIN = ROOT / "hifz-app" / "src" / "main"
AUDIT = ROOT / "hifz-app" / "src" / "audit" / "legacy-ibn-kathir"


def main() -> None:
    source = GRADLE.read_text(encoding="utf-8")
    if (MAIN / "assets" / "ibn_kathir_headings").exists():
        raise SystemExit("FAIL: legacy title assets still live in src/main")
    for name in ("IbnKathirOfflineTitles.java", "IbnKathirTitlePack.java"):
        if (MAIN / "java" / "com" / "quransafeguard" / "hifz" / "preview" / name).exists():
            raise SystemExit(f"FAIL: legacy title provider still lives in src/main: {name}")
    chunks = sorted((AUDIT / "assets" / "ibn_kathir_headings").glob("chunk-*.json"))
    if len(chunks) != 8:
        raise SystemExit(f"FAIL: expected 8 audit-only chunks, got {len(chunks)}")
    for name in ("IbnKathirOfflineTitles.java", "IbnKathirTitlePack.java"):
        if not (AUDIT / "java" / name).is_file():
            raise SystemExit(f"FAIL: missing audit-only provider: {name}")
    if "assets.exclude(" in source or "java.exclude(" in source:
        raise SystemExit("FAIL: unsupported AGP source-set exclusion remains")
    print("PASS: superseded title assets and classes exist only under src/audit")


if __name__ == "__main__":
    main()
