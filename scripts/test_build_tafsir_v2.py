#!/usr/bin/env python3
"""Adversarial regression tests for build_tafsir_v2.py.

Uses only synthetic text. No tafsir payload is embedded in the test suite.
"""
from __future__ import annotations

import copy
import json
import sqlite3
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILDER = ROOT / "scripts/build_tafsir_v2.py"


def base_document() -> dict:
    return {
        "metadata": {
            "schema_version": "tafsir-v2",
            "edition_id": "qushayri_en_sands",
            "source_title": "Synthetic audit source",
            "source_sha256": "a" * 64,
            "translator": "Synthetic translator",
            "rights_status": "unresolved",
            "source_audit_status": "pending",
            "content_audit_status": "pending",
            "expected_mapped_verse_count": 1,
            "content_language": "en",
            "arabic_source_text_included": False,
        },
        "entries": [
            {
                "source_entry_id": "synthetic-2-3-a",
                "entry_type": "VERSE_COMMENTARY",
                "surah": 2,
                "verse_start": 3,
                "verse_end": 3,
                "segment_no": 1,
                "body_runs": [{"style": "regular", "text": "synthetic commentary"}],
                "source_page_start": 10,
                "source_page_end": 10,
                "notes": [
                    {
                        "label": "A",
                        "runs": [{"style": "italic", "text": "synthetic note"}],
                        "source_page": 10,
                    }
                ],
            }
        ],
        "coverage": [
            {"surah": 2, "ayah_start": 3, "ayah_end": 3, "status": "available"}
        ],
    }


def run_builder(
    document: dict,
    *,
    allow_unreleased: bool,
) -> tuple[subprocess.CompletedProcess, Path, tempfile.TemporaryDirectory]:
    tmp = tempfile.TemporaryDirectory()
    root = Path(tmp.name)
    source = root / "input.json"
    output = root / "output.sqlite"
    source.write_text(json.dumps(document, ensure_ascii=False), encoding="utf-8")
    command = [sys.executable, str(BUILDER), str(source), str(output)]
    if allow_unreleased:
        command.append("--allow-unreleased")
    result = subprocess.run(command, capture_output=True, text=True)
    return result, output, tmp


def expect_failure(
    document: dict,
    phrase: str,
    *,
    allow_unreleased: bool = True,
) -> None:
    result, output, tmp = run_builder(document, allow_unreleased=allow_unreleased)
    try:
        assert result.returncode != 0, result.stdout
        assert phrase in (result.stdout + result.stderr), result.stdout + result.stderr
        assert not output.exists(), "failed build left a partial SQLite database behind"
    finally:
        tmp.cleanup()


def test_valid_audit_build() -> None:
    result, output, tmp = run_builder(base_document(), allow_unreleased=True)
    try:
        assert result.returncode == 0, result.stdout + result.stderr
        assert output.is_file()
        db = sqlite3.connect(output)
        try:
            assert db.execute(
                "SELECT value FROM source_metadata WHERE key='schema_version'"
            ).fetchone()[0] == "tafsir-v2"
            assert db.execute(
                "SELECT value FROM source_metadata WHERE key='content_language'"
            ).fetchone()[0] == "en"
            assert db.execute(
                "SELECT value FROM source_metadata "
                "WHERE key='arabic_source_text_included'"
            ).fetchone()[0] == "false"
            assert db.execute("SELECT source_entry_id FROM tafsir_entry").fetchone()[0] == (
                "synthetic-2-3-a"
            )
            assert db.execute("SELECT surah, ayah FROM entry_verse_map").fetchall() == [(2, 3)]
            assert db.execute("SELECT label FROM entry_note").fetchone()[0] == "A"
            assert db.execute("PRAGMA quick_check").fetchone()[0] == "ok"
        finally:
            db.close()
    finally:
        tmp.cleanup()


def test_release_requires_rights_and_audits() -> None:
    expect_failure(base_document(), "Redistribution rights are not cleared", allow_unreleased=False)


def test_release_can_build_only_when_all_distribution_gates_are_explicit() -> None:
    doc = base_document()
    doc["metadata"].update(
        rights_status="permission_documented",
        source_audit_status="verified",
        content_audit_status="verified",
    )
    result, output, tmp = run_builder(doc, allow_unreleased=False)
    try:
        assert result.returncode == 0, result.stdout + result.stderr
        assert output.is_file()
    finally:
        tmp.cleanup()


def test_non_english_payload_is_rejected() -> None:
    doc = base_document()
    doc["metadata"]["content_language"] = "ar"
    expect_failure(doc, "content_language must be 'en'")


def test_arabic_source_flag_must_be_false() -> None:
    doc = base_document()
    doc["metadata"]["arabic_source_text_included"] = True
    expect_failure(doc, "Arabic source text must not be included")


def test_arabic_script_commentary_is_rejected() -> None:
    doc = base_document()
    doc["entries"][0]["body_runs"][0]["text"] = "نص عربي"
    expect_failure(doc, "Arabic-script source text is forbidden")


def test_arabic_script_note_is_rejected() -> None:
    doc = base_document()
    doc["entries"][0]["notes"][0]["runs"][0]["text"] = "ملاحظة عربية"
    expect_failure(doc, "Arabic-script source text is forbidden")


def test_third_party_contamination_is_rejected() -> None:
    doc = base_document()
    doc["entries"][0]["body_runs"][0]["text"] = "Downloaded via sunniconnect.com"
    expect_failure(doc, "Forbidden third-party contamination marker")


def test_range_type_cannot_masquerade_as_single_verse() -> None:
    doc = base_document()
    doc["entries"][0].update(
        entry_type="VERSE_RANGE_COMMENTARY",
        verse_start=3,
        verse_end=3,
    )
    expect_failure(doc, "VERSE_RANGE_COMMENTARY must span >1 verse")


def test_sura_introduction_cannot_be_injected_into_verse_tap_mapping() -> None:
    doc = base_document()
    doc["entries"][0]["entry_type"] = "SURA_INTRODUCTION"
    expect_failure(doc, "must stay structurally separate from verse taps")


def test_duplicate_range_segment_is_rejected() -> None:
    doc = base_document()
    duplicate = copy.deepcopy(doc["entries"][0])
    duplicate["source_entry_id"] = "synthetic-2-3-b"
    doc["entries"].append(duplicate)
    expect_failure(doc, "Duplicate verse range/segment")


def test_mapped_verse_count_must_match_reviewed_metadata() -> None:
    doc = base_document()
    doc["metadata"]["expected_mapped_verse_count"] = 2
    expect_failure(doc, "Mapped verse count mismatch")


def test_mapped_verse_must_be_inside_declared_coverage() -> None:
    doc = base_document()
    doc["coverage"] = [
        {"surah": 2, "ayah_start": 4, "ayah_end": 4, "status": "available"}
    ]
    expect_failure(doc, "Mapped verses outside declared coverage")


def main() -> None:
    tests = [
        value
        for name, value in sorted(globals().items())
        if name.startswith("test_") and callable(value)
    ]
    for test in tests:
        test()
        print("PASS", test.__name__)
    print(f"build_tafsir_v2 adversarial tests: PASS ({len(tests)})")


if __name__ == "__main__":
    main()
