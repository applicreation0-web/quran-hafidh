#!/usr/bin/env python3
from __future__ import annotations
import argparse
import zipfile
from pathlib import Path

REQUIRED_TEXT = (
    "TaddaburActivity",
    "taddabur_prefs",
    "com.applicreation0.quransafeguard.TADDABUR_DEADLINE",
    "TADDABUR • PLUS",
    "pool fixe 1–60",
    "Fermer • garder le marque-page",
    "applications protégées bloquées jusqu’à minuit",
)
REQUIRED = tuple(text.encode("utf-8") for text in REQUIRED_TEXT)

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("apk", type=Path)
    args = ap.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Plus APK not found: {args.apk}")
    seen = {label: False for label in REQUIRED_TEXT}
    with zipfile.ZipFile(args.apk) as archive:
        for info in archive.infolist():
            if info.file_size > 32 * 1024 * 1024:
                continue
            payload = archive.read(info)
            for marker, label in zip(REQUIRED, REQUIRED_TEXT):
                if marker in payload:
                    seen[label] = True
    missing = [label for label, present in seen.items() if not present]
    if missing:
        raise SystemExit(f"Plus APK missing Taddabur contract markers: {missing}")
    print("Verified Plus APK: Taddabur activity, bookmark, reminder and fixed deadline enforcement are packaged")

if __name__ == "__main__":
    main()
