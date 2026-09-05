#!/usr/bin/env python3
"""Static/source gate for the Quran Safeguard 0.10.5 Tafsir-only update.

This complements the rebuilt-database and APK audits. It freezes the intended
0.10.5 contracts in source: Jalalayn untouched, Qurtubi four-volume boundary,
Qushayri 806 source anchors -> 720 logical entries, coverage-driven edition
selection, and source-only English-first justified rendering.
"""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit("0.10.5 TAFSIR SOURCE AUDIT FAILURE: " + message)


repo = read("app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt")
renderer = read("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
controller = read("app/src/plus/java/com/quranunlock/guard/MultiTafsirPanel.kt")
source_rendering = read("app/src/plus/java/com/quranunlock/guard/TafsirSourceRendering.kt")
legacy_repo = read("app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt")
light = read("app/src/light/java/com/quranunlock/guard/TafsirEdition.kt")
light_apk = read("scripts/verify_light_apk_no_tafsir.py")
prep = read("scripts/prepare_multitafsir_assets.py")
qsh = read("scripts/build_qushayri.py")
qur = read("scripts/build_qurtubi.py")
corpus_audit = read("scripts/audit_0105_tafsir_cleanup.py")
apk_audit = read("scripts/verify_plus_apk_multitafsir.py")

# Shared UI / routing invariants.
require("PrivateTafsirEdition.JALALAYN" in repo, "Jalalayn must remain the runtime fallback/default")
require('QURTUBI("qurtubi", "Qurtubi")' in repo, "Qurtubi edition id missing")
require('QUSHAYRI("qushayri", "Qushayri")' in repo, "Qushayri edition id missing")
require("data class MultiTafsirAvailability" in repo, "coverage model missing")
require("suspend fun loadAvailable(" in repo, "per-verse coverage lookup missing")
require("entries.containsKey(preferred)" in repo, "remembered edition must be kept only when it covers the verse")
require(
    "entries.containsKey(PrivateTafsirEdition.JALALAYN)" in repo,
    "Jalalayn-first fallback is missing",
)
require("availability = null" in controller, "verse change must reset availability before reload")
require("MultiTafsirRepository.loadAvailable(context, verse)" in controller, "controller must load real per-verse coverage")
require("loaded.resolveEdition(selectedEdition)" in controller, "coverage-driven fallback resolution missing")
require("availableEditions = loaded?.editions.orEmpty()" in controller, "renderer must receive only real available editions")
require("DropdownMenu" in renderer, "single Tafsir renderer must expose edition selector")
require("availableEditions.forEach" in renderer, "selector must enumerate only available editions")
require("enabled = availableEditions.size > 1" in renderer, "selector must not advertise a choice when only one edition covers the verse")
require("TextAlign.Justify" in renderer, "Tafsir renderer is not configured for justified prose")
require(
    renderer.count("textAlign = TextAlign.Justify") >= 2,
    "both commentary and note prose must remain justified",
)
main_marker = "text = runsToAnnotatedString(state.entry.commentaryRuns)"
require(main_marker in renderer, "main Tafsir commentary renderer missing")
main_window = renderer[renderer.index(main_marker):renderer.index(main_marker) + 500]
require("textAlign = TextAlign.Justify" in main_window, "main Tafsir commentary is not justified")
note_marker = "append(runsToAnnotatedString(note.runs))"
require(note_marker in renderer, "Jalalayn note renderer missing")
note_window = renderer[renderer.index(note_marker):renderer.index(note_marker) + 700]
require("textAlign = TextAlign.Justify" in note_window, "Tafsir notes are not justified")

# Source-only Qurtubi/Qushayri rendering. Structural verse numbers are provenance
# only: the app must not synthesize a displayed verse title or explanatory label.
require("data class SourceBackedTafsirSegment" in source_rendering, "source-backed rendering model missing")
require("fun renderSourceBackedTafsirSegments(" in source_rendering, "source-only rendering function missing")
require("TafsirRunStyle.BOLD_ITALIC, row.translation.trim()" in source_rendering,
        "source English verse translation must remain first and visually distinct")
require("TafsirRunStyle.REGULAR, row.commentary.trim()" in source_rendering,
        "source commentary rendering missing")
require("renderSourceBackedTafsirSegments(rows)" in repo,
        "Qurtubi/Qushayri runtime must use the source-only renderer")
require('value("arabic_included") == "false"' in repo,
        "Qurtubi/Qushayri runtime must reject a corpus that injects Arabic verse text")
for forbidden in (
    "Commentary on ${verse.surah}",
    "Verse ${",
    "Ayah ${",
):
    require(forbidden not in repo and forbidden not in source_rendering,
            f"generated Tafsir heading must remain absent: {forbidden}")

# Jalalayn remains the byte-frozen golden corpus/repository.
require("6_236" in legacy_repo, "Jalalayn expected row count changed")
require("26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56" in legacy_repo,
        "Jalalayn approved database checksum changed")

# Qushayri source structure: no source anchor is discarded merely to eliminate
# the 86 empty-commentary rows produced by the previous extractor.
for marker in (
    "EXPECTED_RAW_SEGMENTS = 806",
    "EXPECTED_LOGICAL_ENTRIES = 720",
    "EXPECTED_TRANSLATION_ONLY_ANCHORS = 86",
    "EXPECTED_GROUPED_RANGES = 76",
    'SOFT_HYPHEN_MARKER = "__QSH_SOFT_HYPHEN__"',
    "resolve_discretionary_hyphens",
    "preserve-marker-then-source-driven-join",
    "group_shared_commentaries",
    "Qushayri orphan translation-only anchor",
    "source_page_end",
):
    require(marker in qsh, f"Qushayri 0.10.5 grouping/cleanup invariant missing: {marker}")
require("expectedEntries = 720" in repo, "runtime Qushayri logical count must be 720")
require("'qushayri': 720" in prep, "asset packager still expects pre-cleanup Qushayri row count")
for marker in (
    "raw_segment_count",
    "translation_only_anchor_count",
    "grouped_source_range_count",
    "source_soft_hyphen_count",
    "soft_hyphen_policy",
):
    require(marker in prep, f"asset preparation does not preserve Qushayri source-structure proof: {marker}")
    require(marker in corpus_audit, f"corpus audit does not verify Qushayri source-structure proof: {marker}")
    require(marker in apk_audit, f"APK audit does not verify Qushayri source-structure proof: {marker}")
require("'source_soft_hyphen_count': '584'" in prep, "asset packager must pin the observed 584 Qushayri source soft hyphens")
require('"source_soft_hyphen_count": "584"' in corpus_audit, "corpus audit must pin the observed 584 Qushayri source soft hyphens")
require("'source_soft_hyphen_count':'584'" in apk_audit, "APK audit must pin the observed 584 Qushayri source soft hyphens")
require('"entries": 720' in corpus_audit, "0.10.5 corpus audit must expect 720 Qushayri logical entries")
require("'entries':720" in apk_audit, "Plus APK audit must expect 720 Qushayri logical entries")
require("raw_anchor_equivalent" in corpus_audit and "raw_anchor_equivalent" in apk_audit,
        "DB and APK audits must recount actual Qushayri source anchors")
require("non-breaking-space PDF debris" in corpus_audit, "corpus audit must reject residual NBSP extraction debris")
require("non-breaking-space PDF debris" in apk_audit, "APK audit must reject residual NBSP extraction debris")
require("blank commentary rows" in apk_audit, "Plus APK audit must reject empty commentary rows")

# Qurtubi remains the approved four-volume corpus, with volume 5 excluded.
require("expectedEntries = 432" in repo, "runtime Qurtubi count must remain 432")
for marker in (
    "a791ec1313fa2401abe7ca25ac7ccb4bedb1afcb51f2c779160a71e98a6f04cb",
    "466e72af70ad6c9c9ddccb418f87df6012c3078ef7947fdc88ab00c86c15645e",
    "e69818ce49f79d7de2bb5cef37c82e7e1f4431f7117a550fa33259da7dc6b583",
    "eb71cb2ed8c2497cc8a5d3634b3eeb7788fdc7caee9de5d6b50349fb8619965c",
):
    require(marker in qur or marker in prep, "one approved Qurtubi source-volume checksum is missing")
require("replace('\\u00a0',' ')" in qur, "Qurtubi builder must normalize residual PDF non-breaking spaces")
require("Qurtubi non-breaking-space extraction debris" in qur, "Qurtubi builder must fail closed if an NBSP survives normalization")
require("4:23" in qur, "Qurtubi volume-4/volume-5 boundary is not documented in builder")
require("4:23 must remain" in apk_audit, "APK audit does not enforce Qurtubi 4:23 exclusion")

# Edition isolation remains strict.
require("isEnabled: Boolean = false" in light, "Light Tafsir must remain disabled")
require("qurtubi" in light_apk.lower() and "qushayri" in light_apk.lower(),
        "Light APK gate must explicitly reject Qurtubi/Qushayri leakage")

print("0.10.5 Tafsir source/UI audit: PASS")
print("- Jalalayn golden repository/checksum preserved and remains first fallback")
print("- selector exposes only editions with a real source-backed row for the tapped verse")
print("- Qushayri: 806 source anchors -> 720 logical entries, 86 translation-only anchors grouped into 76 shared-commentary ranges; 584 source soft hyphens are preserved until source-driven joining")
print("- Qurtubi: 432 entries, four approved volumes, 4:23 excluded; residual PDF NBSPs normalized")
print("- Qurtubi/Qushayri keep source English verse translation first, then source commentary; no generated verse heading or Arabic injection")
print("- one shared Plus renderer keeps all three Tafsir commentaries and notes justified")
print("- Light remains Tafsir-free")
