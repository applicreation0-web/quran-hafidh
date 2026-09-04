#!/usr/bin/env python3
from __future__ import annotations
import argparse
import zipfile
from pathlib import Path

REQUIRED = (
    b"TaddaburActivity",
    b"taddabur_prefs",
    b"TADDABUR_DEADLINE",
    b"TADDABUR âTADDABUR \xe2TADDABUR \xe2\x80¢TADDABUR \xe2\x80\xa2 PLUS",
    b"pool fixe 1âpool fixe 1\xe2pool fixe 1\xe2\x80pool fixe 1\xe2\x80\x9360",
    b"Fermer âFermer \xe2Fermer \xe2\x80¢Fermer \xe2\x80\xa2 garder le marque-page",
    b"applications protÃapplications prot\xc3©applications prot\xc3\xa9gÃapplications prot\xc3\xa9g\xc3©applications prot\xc3\xa9g\xc3\xa9es bloquÃapplications prot\xc3\xa9g\xc3\xa9es bloqu\xc3©applications prot\xc3\xa9g\xc3\xa9es bloqu\xc3\xa9es jusquâapplications prot\xc3\xa9g\xc3\xa9es bloqu\xc3\xa9es jusqu\xe2applications prot\xc3\xa9g\xc3\xa9es bloqu\xc3\xa9es jusqu\xe2\x80applications prot\xc3\xa9g\xc3\xa9es bloqu\xc3\xa9es jusqu\xe2\x80\x99Ãapplications prot\xc3\xa9g\xc3\xa9es bloqu\xc3\xa9es jusqu\xe2\x80\x99\xc3 applications prot\xc3\xa9g\xc3\xa9es bloqu\xc3\xa9es jusqu\xe2\x80\x99\xc3\xa0 minuit",
)

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("apk", type=Path)
    args = ap.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Plus APK not found: {args.apk}")
    seen = {marker: False for marker in REQUIRED}
    with zipfile.ZipFile(args.apk) as archive:
        for info in archive.infolist():
            if info.file_size > 32 * 1024 * 1024:
                continue
            payload = archive.read(info)
            for marker in REQUIRED:
                if marker in payload:
                    seen[marker] = True
    missing = [marker for marker, present in seen.items() if not present]
    if missing:
        raise SystemExit(f"Plus APK missing Taddabur contract markers: {missing}")
    print("Verified Plus APK: Taddabur activity, bookmark, reminder and fixed deadline enforcement are packaged")

if __name__ == "__main__":
    main()
