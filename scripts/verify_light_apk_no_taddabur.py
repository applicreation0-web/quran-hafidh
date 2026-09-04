#!/usr/bin/env python3
from __future__ import annotations
import argparse
import zipfile
from pathlib import Path

FORBIDDEN_TEXT = (
    "TaddaburActivity",
    "taddabur_prefs",
    "com.applicreation0.quransafeguard.TADDABUR_DEADLINE",
    "TADDABUR • PLUS",
    "pool fixe 1–60",
    "Blocage Taddabur actif jusqu’à minuit",
)
FORBIDDEN = tuple(text.encode("utf-8") for text in FORBIDDEN_TEXT)

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("apk", type=Path)
    args = ap.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Light APK not found: {args.apk}")
    with zipfile.ZipFile(args.apk) as archive:
        for info in archive.infolist():
            if info.file_size > 32 * 1024 * 1024:
                continue
            payload = archive.read(info)
            for marker, label in zip(FORBIDDEN, FORBIDDEN_TEXT):
                if marker in payload:
                    raise SystemExit(
                        f"Plus-only Taddabur marker {label!r} leaked into Light: {info.filename}"
                    )
    print("Verified Light APK: no Taddabur activity, storage, reminder or Plus UI payload")

if __name__ == "__main__":
    main()
