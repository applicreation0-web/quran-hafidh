#!/usr/bin/env python3
"""Static fail-closed checks for the 0.10.4 pre-release scaffold.

This validates architecture/non-regression only. It deliberately does NOT certify new
English payload rights or the still-pending 264/264 Hikam source comparison.
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
plus_edition = read("app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt")
hikam_repo = read("app/src/main/java/com/quranunlock/guard/HikamRepository.kt")
hikam_ui = read("app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt")
hikam_sharh = read("app/src/main/java/com/quranunlock/guard/HikamSharh.kt")
plus_sharh = read("app/src/plus/java/com/quranunlock/guard/HikamSharhEdition.kt")
builder = read("scripts/build_tafsir_v2.py")
plus_apk_verifier = read("scripts/verify_plus_apk_tafsir.py")
light_apk_verifier = read("scripts/verify_light_apk_no_tafsir.py")

# Jalalayn golden UX and frozen legacy adapter.
for token in (
    'Color(0xFFF7FBF6)',
    'Text("A−")',
    'Text("A+")',
    'notesExpanded',
    'verticalScroll(scrollState)',
    'editionId.displayName',
):
    assert token in panel, f"Jalalayn golden UX token missing: {token}"
assert 'displayName = "Jalalayn"' in models
assert 'JALALAYN_EXPECTED_ENTRIES = 6_236' in repo
assert '26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56' in repo
assert 'verse_commentary' in repo and 'verse_note' in repo
assert 'sourceLabel: String? = null' in models
assert 'note.displayLabel' in panel

# Verse-specific options: absent partial tafsir editions are hidden and Jalalayn wins fallback.
for token in (
    'fun availableFor(verse: VerseRef)',
    'fun effectiveFor(',
    'preferred.takeIf { it.covers(verse) } ?: JALALAYN',
):
    assert token in models, f"Missing verse-specific tafsir option rule: {token}"
assert 'TafsirEditionId.availableFor(verse)' in panel
assert 'availableEditions.forEach' in panel
assert 'enabled = availableEditions.size > 1' in panel
assert 'TafsirEditionId.effectiveFor(' in free_reader
assert 'TafsirEditionId.effectiveFor(' in plus_edition

# Multi-edition state is request-keyed and stale results are rejected.
for token in ('JALALAYN', 'QURTUBI', 'QUSHAYRI', 'TafsirRequestKey'):
    assert token in models
assert 'LaunchedEffect(selectedTafsirVerse, selectedTafsirEdition)' in free_reader
assert 'selectedTafsirEdition == requestKey.editionId' in free_reader
assert 'English commentary unavailable for this verse in this edition.' in panel
assert 'sourceRanges' in models
assert 'blocks.map { it.verseStart..it.verseEnd }.distinct()' in repo

# V2 payloads fail closed independently of a single boolean.
manifest = json.loads(read("app/src/plus/assets/tafsir/tafsir_v2_manifest.json"))
assert manifest.get("manifest_schema") == 2
ids = {item["edition_id"] for item in manifest["editions"]}
assert ids == {"qurtubi_en_bewley", "qushayri_en_sands"}
for item in manifest["editions"]:
    assert item["schema_version"] == "tafsir-v2"
    assert item["ready_for_distribution"] is False
    assert item["rights_status"] == "unresolved"
    assert item["content_audit_status"] == "pending"
    assert item["database_sha256"] == ""
    assert item["asset_parts"] == []

for token in (
    'APPROVED_RIGHTS_STATUSES',
    'sourceAuditStatus == "verified"',
    'contentAuditStatus == "verified"',
    'validateV2Metadata',
    'CancellationException',
    'StandardCopyOption.ATOMIC_MOVE',
):
    assert token in repo, f"Missing runtime v2 fail-closed control: {token}"

# Builder must reject unreviewed, contaminated or structurally ambiguous input.
for token in (
    '--allow-unreleased',
    'expected_mapped_verse_count',
    'Forbidden third-party contamination marker',
    'VERSE_RANGE_COMMENTARY must span >1 verse',
    'must stay structurally separate from verse taps',
    'Duplicate verse range/segment',
    'Mapped verses outside declared coverage',
):
    assert token in builder, f"Missing v2 builder gate: {token}"
assert 'manifest-driven' in plus_apk_verifier.lower() or 'packaged manifest' in plus_apk_verifier
assert 'EXPECTED_V2_IDS' in plus_apk_verifier
assert 'qurtubi' in light_apk_verifier and 'qushayri' in light_apk_verifier

# Light remains a true no-op and compiles against old/new facade signatures.
assert 'isEnabled: Boolean = false' in light
assert light.count('): TafsirEntry? = null') >= 2
assert light.count('fun Panel(') >= 2

# Hikam main corpus/model stays distinct from sharh.
for forbidden in ('HikamSharhEntry', 'commentary_work_id', 'commentary_arabic'):
    assert forbidden not in hikam_repo, f"Sharh leaked into HikamRepository: {forbidden}"
for label in ('"ḤIKMA"', '"TRADUCTION FRANÇAISE"', '"COMMENTAIRE CLASSIQUE"'):
    assert label in hikam_ui, f"Missing explicit Hikam UI layer: {label}"
assert 'sharhAvailability.any { it.available }' in hikam_ui

# Grouped classical sharh is represented once and mapped, never split/duplicated by inference.
for token in (
    'canonicalHikmaNumbers',
    'sourceHikmaLocator',
    'commentaryGroupId',
    'expectedWorkId',
    'requireUnique',
):
    assert token in hikam_sharh, f"Missing Hikam sharh integrity field: {token}"
for token in (
    'canonical_hikma_numbers',
    'source_hikma_locator',
    'commentary_group_id',
    'commentary_work_id',
    'singleOrNull',
):
    assert token in plus_sharh, f"Missing Plus sharh parser gate: {token}"

# Canonical matn, vocalization and French translation fingerprints are distinct.
for token in (
    'matnBoundaryStatus',
    'matnPrimarySourceId',
    'matnPrimarySourceKind',
    'matnSnapshotSha256',
    'vocalizedSnapshotSha256',
    'translationMatnSnapshotSha256',
    'independentWitnessIds',
    'matnReleaseEligible',
):
    assert token in hikam_repo, f"Missing Hikam release provenance field: {token}"

registry = json.loads(read("editorial/hikam_0104_source_registry.json"))
assert registry.get("schema_version") == 1
source_by_id = {item["source_id"]: item for item in registry["sources"]}
assert source_by_id["matheson_2014_original_arabic"]["source_kind"] == "matn_only"
assert source_by_id["matheson_2014_original_arabic"]["boundary_authority"] is True
assert source_by_id["al_mostafa_unresolved_edition"]["counts_as_independent_witness"] is False
eligible = {
    source_id for source_id, item in source_by_id.items()
    if item.get("counts_as_independent_witness") is True
}
assert eligible == {"dar_al_iman_1986_abd_al_rahim", "emir_abdelkader_ms_4267"}

print("0.10.4 scaffold audit: PASS")
print("Release payload/matn certification: intentionally PENDING")
