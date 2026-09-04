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
    "background:transparent",
    "A−",
    "A+",
)
FORBIDDEN_TEXT = (
    "#F7F2E8",
    "#F7FBF6",
    "Warm ivory only",
)
REQUIRED = tuple(text.encode("utf-8") for text in REQUIRED_TEXT)
FORBIDDEN = tuple(text.encode("utf-8") for text in FORBIDDEN_TEXT)

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("apk", type=Path)
    args = ap.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Plus APK not found: {args.apk}")
    seen = {label: False for label in REQUIRED_TEXT}
    forbidden_seen = {label: False for label in FORBIDDEN_TEXT}
    with zipfile.ZipFile(args.apk) as archive:
        for info in archive.infolist():
            if info.file_size > 32 * 1024 * 1024:
                continue
            payload = archive.read(info)
            for marker, label in zip(REQUIRED, REQUIRED_TEXT):
                if marker in payload:
                    seen[label] = True
            for marker, label in zip(FORBIDDEN, FORBIDDEN_TEXT):
                if marker in payload:
                    forbidden_seen[label] = True
    missing = [label for label, present in seen.items() if not present]
    if missing:
        raise SystemExit(f"Plus APK missing Taddabur/readability contract markers: {missing}")
    leaked = [label for label, present in forbidden_seen.items() if present]
    if leaked:
        raise SystemExit(f"Plus APK contains forbidden added reading background marker(s): {leaked}")
    print("Verified Plus APK: Taddabur, bookmark, reminder, neutral reading surface, readability controls and fixed deadline enforcement are packaged")

if __name__ == "__main__":
    main()
