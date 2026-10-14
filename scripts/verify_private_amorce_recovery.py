#!/usr/bin/env python3
"""Authenticate, decrypt and validate the private M7-B Amorces archive."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TOOL = ROOT / "tools/amorce-aead/AmorceAeadTool.java"
PUBLIC_MANIFEST = ROOT / "secure-corpus/ibn-kathir-amorces-v1/manifest.json"
EXPECTED_M7 = {
    "M7B_15_ARBITRAGES_VALIDES.csv",
    "M7B_CORPUS_FINAL_1903.csv",
    "M7B_DECISIONS_FINALES_473.csv",
    "M7B_MANIFEST_SHA256.txt",
    "M7B_RAPPORT_CLOTURE.md",
    "M7B_TITRES_FINAL_1903_PRIVES.json",
}


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def validate_archive(restored: Path, extract_chunks: Path | None) -> None:
    public = json.loads(PUBLIC_MANIFEST.read_text(encoding="utf-8"))
    if digest(restored.read_bytes()) != public["private_archive_sha256"]:
        raise ValueError("decrypted private archive SHA-256 mismatch")
    with zipfile.ZipFile(restored) as archive:
        names = set(archive.namelist())
        expected = {f"m7b/{name}" for name in EXPECTED_M7}
        expected |= {f"chunks/{name}" for name in public["source_chunks"]}
        expected.add("bundle-metadata.json")
        if names != expected:
            raise ValueError(f"private archive file set mismatch: {sorted(names ^ expected)}")

        corpus = json.loads(archive.read("m7b/M7B_TITRES_FINAL_1903_PRIVES.json"))
        cues = corpus.get("cues", [])
        if len(cues) != 1903 or corpus.get("titles_count") != 1708 or corpus.get("untitled_count") != 195:
            raise ValueError("M7-B counts differ from 1903/1708/195")
        identifiers = [cue["cue_id"] for cue in cues]
        if len(set(identifiers)) != 1903:
            raise ValueError("M7-B IDs are not unique")
        approved = {cue["cue_id"]: cue["final_title"] for cue in cues if cue.get("final_title")}

        bundled: dict[str, str] = {}
        for name, metadata in public["source_chunks"].items():
            data = archive.read(f"chunks/{name}")
            if len(data) != metadata["bytes"] or digest(data) != metadata["sha256"]:
                raise ValueError(f"private chunk integrity mismatch: {name}")
            obj = json.loads(data)
            if obj.get("schema") != "IK_APPROVED_FINAL_V2":
                raise ValueError(f"private chunk schema mismatch: {name}")
            if len(obj["groups"]) != metadata["titles"]:
                raise ValueError(f"private chunk title count mismatch: {name}")
            for cue_id, title in obj["groups"].items():
                if not re.fullmatch(r"IKEN\d{3}_\d{3}_\d{3}", cue_id) or cue_id in bundled:
                    raise ValueError(f"invalid or duplicate private ID: {cue_id}")
                bundled[cue_id] = title
            if extract_chunks is not None:
                extract_chunks.mkdir(parents=True, exist_ok=True)
                target = extract_chunks / name
                if target.exists():
                    raise ValueError(f"refusing to overwrite private output: {target}")
                target.write_bytes(data)
        if bundled != approved:
            raise ValueError("recovered chunks differ from the exact M7-B approved mapping")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--secret-file", required=True, type=Path)
    parser.add_argument("--container", required=True, type=Path)
    parser.add_argument("--extract-chunks", type=Path)
    args = parser.parse_args()
    public = json.loads(PUBLIC_MANIFEST.read_text(encoding="utf-8"))
    if digest(args.secret_file.read_bytes()) != public["secret_fingerprint_sha256"]:
        raise SystemExit("STOP: private secret fingerprint mismatch")
    if digest(args.container.read_bytes()) != public["ciphertext_sha256"]:
        raise SystemExit("STOP: public ciphertext SHA-256 mismatch")
    with tempfile.TemporaryDirectory() as raw:
        restored = Path(raw) / "amorces.private.zip"
        subprocess.run(
            ["java", str(TOOL), "decrypt", str(args.secret_file), str(args.container), str(restored)],
            cwd=ROOT,
            check=True,
            stdout=subprocess.DEVNULL,
        )
        validate_archive(restored, args.extract_chunks)
    print("PASS: authenticated reconstruction of 1,903 IDs, 1,708 exact titles, 195 absences and eight chunks")


if __name__ == "__main__":
    main()
