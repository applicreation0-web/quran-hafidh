#!/usr/bin/env python3
"""Validate the approved M7-B corpus and create a deterministic private ZIP."""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path


EXPECTED_CHUNKS = {
    "chunk-001-015.json", "chunk-016-030.json", "chunk-031-045.json",
    "chunk-046-060.json", "chunk-061-075.json", "chunk-076-090.json",
    "chunk-091-105.json", "chunk-106-114.json",
}
EXPECTED_M7_FILES = {
    "M7B_15_ARBITRAGES_VALIDES.csv",
    "M7B_CORPUS_FINAL_1903.csv",
    "M7B_DECISIONS_FINALES_473.csv",
    "M7B_MANIFEST_SHA256.txt",
    "M7B_RAPPORT_CLOTURE.md",
    "M7B_TITRES_FINAL_1903_PRIVES.json",
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def validate(m7b_zip: Path, chunks_dir: Path) -> tuple[dict[str, bytes], dict[str, object]]:
    with zipfile.ZipFile(m7b_zip) as archive:
        names = set(archive.namelist())
        if names != EXPECTED_M7_FILES:
            raise ValueError(f"M7-B file set mismatch: {sorted(names ^ EXPECTED_M7_FILES)}")
        m7_files = {name: archive.read(name) for name in sorted(names)}
    manifest_lines = m7_files["M7B_MANIFEST_SHA256.txt"].decode("utf-8").splitlines()
    declared = {}
    for line in manifest_lines:
        digest, name = line.split(None, 1)
        declared[name.strip().lstrip("*")] = digest
    for name, data in m7_files.items():
        if name == "M7B_MANIFEST_SHA256.txt":
            continue
        if declared.get(name) != sha256(data):
            raise ValueError(f"M7-B SHA-256 mismatch: {name}")

    corpus = json.loads(m7_files["M7B_TITRES_FINAL_1903_PRIVES.json"])
    cues = corpus.get("cues", [])
    if (corpus.get("total_cues"), corpus.get("titles_count"), corpus.get("untitled_count")) != (1903, 1708, 195):
        raise ValueError("M7-B declared counts differ from 1903/1708/195")
    if len(cues) != 1903:
        raise ValueError("M7-B cue count is not 1903")
    ids = [cue["cue_id"] for cue in cues]
    if len(set(ids)) != 1903:
        raise ValueError("M7-B cue IDs are not unique")
    approved = {cue["cue_id"]: cue["final_title"] for cue in cues if cue.get("final_title")}
    if len(approved) != 1708:
        raise ValueError("M7-B titled count is not 1708")

    chunk_paths = {path.name: path for path in chunks_dir.glob("*.json")}
    if set(chunk_paths) != EXPECTED_CHUNKS:
        raise ValueError(f"private chunk set mismatch: {sorted(set(chunk_paths) ^ EXPECTED_CHUNKS)}")
    chunks = {name: chunk_paths[name].read_bytes() for name in sorted(chunk_paths)}
    bundled: dict[str, str] = {}
    chunk_meta: dict[str, dict[str, object]] = {}
    for name, data in chunks.items():
        obj = json.loads(data)
        if obj.get("schema") != "IK_APPROVED_FINAL_V2" or obj.get("corpus") != "M7B_1903_2026-10-13":
            raise ValueError(f"wrong private chunk schema: {name}")
        first, last = obj["range"]
        if name != f"chunk-{first:03d}-{last:03d}.json":
            raise ValueError(f"range/filename mismatch: {name}")
        for cue_id, title in obj["groups"].items():
            if not re.fullmatch(r"IKEN\d{3}_\d{3}_\d{3}", cue_id):
                raise ValueError(f"invalid cue ID: {cue_id}")
            if cue_id in bundled or not isinstance(title, str) or not title.strip():
                raise ValueError(f"duplicate or blank private title: {cue_id}")
            bundled[cue_id] = title
        chunk_meta[name] = {"sha256": sha256(data), "titles": len(obj["groups"])}
    if bundled != approved:
        raise ValueError("eight chunks differ from the exact approved M7-B title mapping")

    metadata: dict[str, object] = {
        "schema": "QH_AMORCES_PRIVATE_ARCHIVE_V1",
        "corpus": "M7B_1903_2026-10-13",
        "total_groups": 1903,
        "titles": 1708,
        "untitled": 195,
        "chunks": chunk_meta,
        "m7b_zip_sha256": sha256(m7b_zip.read_bytes()),
    }
    files = {f"m7b/{name}": data for name, data in m7_files.items()}
    files.update({f"chunks/{name}": data for name, data in chunks.items()})
    files["bundle-metadata.json"] = (json.dumps(metadata, sort_keys=True, indent=2) + "\n").encode("utf-8")
    return files, metadata


def write_deterministic_zip(output: Path, files: dict[str, bytes]) -> None:
    if output.exists():
        raise ValueError(f"output already exists: {output}")
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "x", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in sorted(files):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = 0o100600 << 16
            archive.writestr(info, files[name], compresslevel=9)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--m7b", required=True, type=Path)
    parser.add_argument("--chunks-dir", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    files, _ = validate(args.m7b, args.chunks_dir)
    write_deterministic_zip(args.output, files)
    print("PASS: deterministic private archive matches 1903 groups, 1708 titles, 195 absences and eight chunks")


if __name__ == "__main__":
    main()
