#!/usr/bin/env python3
"""Fail closed unless a Plus APK embeds the exact approved tafsir corpus."""

from __future__ import annotations

import argparse
import gzip
import hashlib
import sqlite3
import tempfile
import zipfile
from pathlib import Path


DATABASE_SHA256 = "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56"
ARCHIVE_SHA256 = "824fa202ad2b47aabdc6910f4792e0c8951a5cc8a641f47a2bab70de73b90680"
PARTS = [f"assets/tafsir/al_jalalayn_en.sqlite.gz.part{index:02d}" for index in range(4)]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Plus APK not found: {args.apk}")

    with zipfile.ZipFile(args.apk) as archive:
        names = set(archive.namelist())
        missing = [name for name in PARTS if name not in names]
        if missing:
            raise SystemExit(f"Plus tafsir parts missing: {missing}")
        if any(name.casefold().endswith(".pdf") for name in names):
            raise SystemExit("The source PDF must never be embedded in Plus")
        compressed = b"".join(archive.read(name) for name in PARTS)

    if hashlib.sha256(compressed).hexdigest() != ARCHIVE_SHA256:
        raise SystemExit("Plus tafsir archive checksum mismatch")
    database = gzip.decompress(compressed)
    if hashlib.sha256(database).hexdigest() != DATABASE_SHA256:
        raise SystemExit("Plus tafsir database checksum mismatch")

    with tempfile.NamedTemporaryFile(suffix=".sqlite") as temporary:
        temporary.write(database)
        temporary.flush()
        connection = sqlite3.connect(f"file:{temporary.name}?mode=ro", uri=True)
        try:
            if connection.execute("PRAGMA quick_check").fetchone()[0] != "ok":
                raise SystemExit("Plus tafsir database integrity check failed")
            comments = connection.execute(
                "SELECT COUNT(*) FROM verse_commentary"
            ).fetchone()[0]
            notes = connection.execute("SELECT COUNT(*) FROM verse_note").fetchone()[0]
        finally:
            connection.close()
    if comments != 6_236 or notes != 427:
        raise SystemExit(f"Unexpected Plus corpus counts: {comments} / {notes}")
    print(f"Verified Plus APK corpus: {comments} comments, {notes} notes")


if __name__ == "__main__":
    main()
