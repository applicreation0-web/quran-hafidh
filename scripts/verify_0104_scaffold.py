#!/usr/bin/env python3
"""Static fail-closed checks for the 0.10.4 pre-release scaffold.

This deliberately validates architecture and non-regression only. It does NOT certify
Qurtubi/Qushayri redistribution rights or the pending 264/264 Hikam matn re-audit.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


models = read("app/src/main/java/com/quranunlock/guard/TafsirModels.kt")
panel = read("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
repo = read("app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt")
light = read("app/src/light/java/com/quranunlock/guard/TafsirEdition.kt")
free_reader = read("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
hikam_repo = read("app/src/main/java/com/quranunlock/guard/HikamRepository.kt")
hikam_ui = read("app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt")
hikam_sharh = read("app/src/main/java/com/quranunlock/guard/HikamSharh.kt")
plus_sharh = read("app/src/plus/java/com/quranunlock/guard/HikamSharhEdition.kt")

# Jalalayn golden UX and byte-preserving adapter.
for token in (
    'Color(0xFFF7FBF6)',
    'Text("A−")',
    'Text("A+")',
    'notesExpanded',
    'verticalScroll(scrollState)',
    'Text("${editionId.displayName} ▾")',
):
    assert token in panel, f"Jalalayn golden UX token missing: {token}"

assert 'displayName = "Jalalayn"' in models
assert 'JALALAYN_EXPECTED_ENTRIES = 6_236' in repo
assert '26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56' in repo
assert 'verse_commentary' in repo and 'verse_note' in repo

# Multi-edition state must be request-keyed and fail closed.
for token in ('JALALAYN', 'QURTUBI', 'QUSHAYRI', 'TafsirRequestKey'):
    assert token in models
assert 'LaunchedEffect(selectedTafsirVerse, selectedTafsirEdition)' in free_reader
assert 'selectedTafsirEdition == requestKey.editionId' in free_reader
assert 'readyForDistribution' in repo
assert 'English commentary unavailable for this verse in this edition.' in panel

# Light must remain a true no-op and compile against both old/new facade signatures.
assert 'isEnabled: Boolean = false' in light
assert light.count('): TafsirEntry? = null') >= 2
assert light.count('fun Panel(') >= 2

manifest = json.loads(
    read("app/src/plus/assets/tafsir/tafsir_v2_manifest.json")
)
ids = {item["edition_id"] for item in manifest["editions"]}
assert ids == {"qurtubi_en_bewley", "qushayri_en_sands"}
for item in manifest["editions"]:
    assert item["ready_for_distribution"] is False, (
        "New copyrighted tafsir payload must stay fail-closed until rights/data audit"
    )
    assert item["database_sha256"] == ""
    assert item["asset_parts"] == []

# Hikam: main corpus/model stays distinct from sharh.
for forbidden in ('HikamSharhEntry', 'commentary_work_id', 'commentary_arabic'):
    assert forbidden not in hikam_repo, f"Sharh leaked into HikamRepository: {forbidden}"
for label in ('"ḤIKMA"', '"TRADUCTION FRANÇAISE"', '"COMMENTAIRE CLASSIQUE"'):
    assert label in hikam_ui, f"Missing explicit Hikam UI layer: {label}"
assert 'sharhAvailability.any { it.available }' in hikam_ui
assert 'expectedWorkId' in hikam_sharh
assert 'requireUnique' in hikam_sharh
assert 'singleOrNull' in plus_sharh
assert 'commentary_work_id' in plus_sharh

# Release-level matn gate exists but is intentionally not asserted true yet.
for token in (
    'matnBoundaryStatus',
    'matnSnapshotSha256',
    'translationMatnSnapshotSha256',
    'independentWitnessIds',
    'matnReleaseEligible',
):
    assert token in hikam_repo

print("0.10.4 scaffold audit: PASS")
print("Release payload/matn certification: intentionally PENDING")
