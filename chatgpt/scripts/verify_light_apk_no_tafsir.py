#!/usr/bin/env python3
"""Fail closed if a Light APK contains any private Plus tafsir material."""

from __future__ import annotations

import argparse
import zipfile
from pathlib import Path


FORBIDDEN_NAMES = (
    "tafsir",
    "jalalayn",
    "qurtubi",
    "qushayri",
    ".sqlite",
    ".pdf",
)
FORBIDDEN_PAYLOADS = (
    b"Tafsir al-Jalalayn",
    b"al_jalalayn_en.sqlite",
    b"qurtubi_en.sqlite",
    b"qushayri_en.sqlite",
    b"Qurtubi",
    b"Qushayri",
    b"quranSafeguardVerse",
    b"Quran Safeguard Plus",
    b"jalalayn-styled-runs-v1",
    b"english_verse_translation_included",
)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Light APK not found: {args.apk}")

    with zipfile.ZipFile(args.apk) as archive:
        names = archive.namelist()
        leaked_names = [
            name for name in names
            if any(token in name.casefold() for token in FORBIDDEN_NAMES)
        ]
        if leaked_names:
            raise SystemExit(f"Private corpus path leaked into Light APK: {leaked_names[:8]}")

        for info in archive.infolist():
            if info.file_size > 32 * 1024 * 1024:
                continue
            payload = archive.read(info)
            for marker in FORBIDDEN_PAYLOADS:
                if marker in payload:
                    raise SystemExit(
                        f"Private Plus marker {marker!r} leaked into {info.filename}"
                    )

    print(f"Verified Light APK isolation: {args.apk} ({len(names)} entries)")


if __name__ == "__main__":
    main()
