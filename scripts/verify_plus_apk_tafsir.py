#!/usr/bin/env python3
"""Fail closed unless a Plus APK embeds exactly the approved tafsir payloads.

Jalalayn remains a frozen legacy corpus and is checked byte-for-byte. New v2 editions
are driven by the packaged manifest and are accepted only when every distribution,
rights, source-audit, content-audit and database-integrity gate is explicit.
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import re
import sqlite3
import tempfile
import zipfile
from pathlib import Path

JALALAYN_DATABASE_SHA256 = (
    "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56"
)
JALALAYN_ARCHIVE_SHA256 = (
    "824fa202ad2b47aabdc6910f4792e0c8951a5cc8a641f47a2bab70de73b90680"
)
JALALAYN_PARTS = [
    f"assets/tafsir/al_jalalayn_en.sqlite.gz.part{index:02d}" for index in range(4)
]
V2_MANIFEST = "assets/tafsir/tafsir_v2_manifest.json"
EXPECTED_V2_IDS = {"qurtubi_en_bewley", "qushayri_en_sands"}
APPROVED_RIGHTS = {"licensed", "public_domain", "permission_documented"}
SHA256 = re.compile(r"^[0-9a-f]{64}$")
PART_PATH = re.compile(r"^assets/tafsir/[A-Za-z0-9._-]+\.sqlite\.gz\.part\d{2}$")
FORBIDDEN_ASSET_SUFFIXES = (".pdf", ".epub", ".txt", ".ocr", ".doc", ".docx")
FORBIDDEN_DB_MARKERS = (b"sunniconnect.com", b"Downloaded via sunniconnect")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def open_database(database: bytes):
    temporary = tempfile.NamedTemporaryFile(suffix=".sqlite")
    temporary.write(database)
    temporary.flush()
    connection = sqlite3.connect(f"file:{temporary.name}?mode=ro", uri=True)
    return temporary, connection


def metadata(connection: sqlite3.Connection, key: str) -> str | None:
    row = connection.execute(
        "SELECT value FROM source_metadata WHERE key = ?", (key,)
    ).fetchone()
    return None if row is None else str(row[0])


def verify_jalalayn(archive: zipfile.ZipFile, names: set[str]) -> set[str]:
    missing = [name for name in JALALAYN_PARTS if name not in names]
    if missing:
        raise SystemExit(f"Frozen Jalalayn parts missing: {missing}")
    compressed = b"".join(archive.read(name) for name in JALALAYN_PARTS)
    if sha256(compressed) != JALALAYN_ARCHIVE_SHA256:
        raise SystemExit("Frozen Jalalayn archive checksum mismatch")
    database = gzip.decompress(compressed)
    if sha256(database) != JALALAYN_DATABASE_SHA256:
        raise SystemExit("Frozen Jalalayn database checksum mismatch")

    temporary, connection = open_database(database)
    try:
        if connection.execute("PRAGMA quick_check").fetchone()[0] != "ok":
            raise SystemExit("Jalalayn SQLite integrity check failed")
        comments = connection.execute("SELECT COUNT(*) FROM verse_commentary").fetchone()[0]
        notes = connection.execute("SELECT COUNT(*) FROM verse_note").fetchone()[0]
        verse_count = metadata(connection, "verse_count")
    finally:
        connection.close()
        temporary.close()
    if comments != 6_236 or notes != 427 or verse_count != "6236":
        raise SystemExit(
            f"Frozen Jalalayn corpus changed: comments={comments}, notes={notes}, "
            f"metadata verse_count={verse_count!r}"
        )
    return set(JALALAYN_PARTS)


def parse_manifest(archive: zipfile.ZipFile, names: set[str]) -> list[dict]:
    if V2_MANIFEST not in names:
        raise SystemExit("Plus tafsir v2 manifest is missing")
    try:
        document = json.loads(archive.read(V2_MANIFEST).decode("utf-8"))
    except Exception as error:
        raise SystemExit(f"Invalid tafsir v2 manifest: {error}") from error
    if document.get("manifest_schema") != 2:
        raise SystemExit("Unexpected tafsir v2 manifest schema")
    editions = document.get("editions")
    if not isinstance(editions, list):
        raise SystemExit("Manifest editions must be a list")
    ids = [str(item.get("edition_id", "")) for item in editions if isinstance(item, dict)]
    if set(ids) != EXPECTED_V2_IDS or len(ids) != len(EXPECTED_V2_IDS):
        raise SystemExit(f"Unexpected/duplicate v2 edition ids: {ids}")
    return editions


def verify_v2(
    archive: zipfile.ZipFile,
    names: set[str],
    editions: list[dict],
) -> set[str]:
    approved_parts: set[str] = set()
    for spec in editions:
        edition_id = str(spec["edition_id"])
        parts = spec.get("asset_parts")
        if not isinstance(parts, list) or any(not isinstance(item, str) for item in parts):
            raise SystemExit(f"{edition_id}: asset_parts must be a string list")
        if len(parts) != len(set(parts)):
            raise SystemExit(f"{edition_id}: duplicate asset part")
        if any(not PART_PATH.fullmatch(f"assets/{part}" if part.startswith("tafsir/") else part) for part in parts):
            raise SystemExit(f"{edition_id}: unsafe asset part path")
        apk_parts = [f"assets/{part}" if part.startswith("tafsir/") else part for part in parts]

        ready = spec.get("ready_for_distribution") is True
        if not ready:
            if parts or str(spec.get("database_sha256", "")).strip():
                raise SystemExit(
                    f"{edition_id}: non-distributable edition must not carry payload/hash"
                )
            continue

        if spec.get("schema_version") != "tafsir-v2":
            raise SystemExit(f"{edition_id}: wrong schema gate")
        rights = str(spec.get("rights_status", ""))
        if rights not in APPROVED_RIGHTS:
            raise SystemExit(f"{edition_id}: redistribution rights not approved")
        if spec.get("source_audit_status") != "verified":
            raise SystemExit(f"{edition_id}: source audit not verified")
        if spec.get("content_audit_status") != "verified":
            raise SystemExit(f"{edition_id}: content audit not verified")
        expected_db_sha = str(spec.get("database_sha256", ""))
        if not SHA256.fullmatch(expected_db_sha):
            raise SystemExit(f"{edition_id}: invalid database SHA-256")
        if not apk_parts:
            raise SystemExit(f"{edition_id}: ready edition has no asset parts")
        missing = [name for name in apk_parts if name not in names]
        if missing:
            raise SystemExit(f"{edition_id}: manifest payload parts missing: {missing}")

        compressed = b"".join(archive.read(name) for name in apk_parts)
        try:
            database = gzip.decompress(compressed)
        except Exception as error:
            raise SystemExit(f"{edition_id}: invalid gzip payload: {error}") from error
        if sha256(database) != expected_db_sha:
            raise SystemExit(f"{edition_id}: database checksum mismatch")
        folded = database.lower()
        for marker in FORBIDDEN_DB_MARKERS:
            if marker.lower() in folded:
                raise SystemExit(f"{edition_id}: third-party contamination marker present")

        temporary, connection = open_database(database)
        try:
            if connection.execute("PRAGMA quick_check").fetchone()[0] != "ok":
                raise SystemExit(f"{edition_id}: SQLite quick_check failed")
            if connection.execute("PRAGMA foreign_key_check").fetchall():
                raise SystemExit(f"{edition_id}: SQLite foreign_key_check failed")
            if metadata(connection, "schema_version") != "tafsir-v2":
                raise SystemExit(f"{edition_id}: DB schema metadata mismatch")
            if metadata(connection, "edition_id") != edition_id:
                raise SystemExit(f"{edition_id}: DB edition metadata mismatch")
            if metadata(connection, "rights_status") != rights:
                raise SystemExit(f"{edition_id}: DB rights metadata mismatch")
            if metadata(connection, "source_audit_status") != "verified":
                raise SystemExit(f"{edition_id}: DB source audit metadata mismatch")
            if metadata(connection, "content_audit_status") != "verified":
                raise SystemExit(f"{edition_id}: DB content audit metadata mismatch")
            expected_mapped = int(metadata(connection, "expected_mapped_verse_count") or "0")
            actual_mapped = connection.execute(
                "SELECT COUNT(*) FROM (SELECT DISTINCT surah, ayah FROM entry_verse_map)"
            ).fetchone()[0]
            if expected_mapped <= 0 or actual_mapped != expected_mapped:
                raise SystemExit(
                    f"{edition_id}: mapped verse count mismatch "
                    f"{actual_mapped}/{expected_mapped}"
                )
        finally:
            connection.close()
            temporary.close()
        approved_parts.update(apk_parts)

    return approved_parts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"Plus APK not found: {args.apk}")

    with zipfile.ZipFile(args.apk) as archive:
        names = set(archive.namelist())
        forbidden = [
            name for name in names
            if name.casefold().endswith(FORBIDDEN_ASSET_SUFFIXES)
            and name.startswith("assets/tafsir/")
        ]
        if forbidden:
            raise SystemExit(f"Raw/source tafsir material leaked into Plus: {forbidden[:8]}")

        approved_parts = verify_jalalayn(archive, names)
        editions = parse_manifest(archive, names)
        approved_parts |= verify_v2(archive, names, editions)

        actual_tafsir_parts = {name for name in names if PART_PATH.fullmatch(name)}
        unexpected_parts = actual_tafsir_parts - approved_parts
        if unexpected_parts:
            raise SystemExit(
                "Unlisted/unapproved tafsir payload leaked into Plus: "
                + ", ".join(sorted(unexpected_parts))
            )

    ready_ids = [
        str(item["edition_id"])
        for item in editions
        if item.get("ready_for_distribution") is True
    ]
    print(
        "Verified Plus APK tafsir corpus: frozen Jalalayn 6236/427; "
        f"approved v2 editions={ready_ids or 'none (fail-closed scaffold)'}"
    )


if __name__ == "__main__":
    main()
