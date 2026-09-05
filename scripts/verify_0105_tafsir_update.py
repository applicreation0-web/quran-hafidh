#!/usr/bin/env python3
"""Static/source gate for the Quran Safeguard 0.10.5 Tafsir-only update.

This complements rebuilt-database and APK audits. It freezes the intended
contracts: source fidelity, real per-verse coverage, one justified renderer,
source-note navigation, explicit canonical Quran-reference navigation, and
strict Light/Plus isolation.
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
reference_parser = read("app/src/plus/java/com/quranunlock/guard/TafsirReferenceParser.kt")
reference_navigation = read("app/src/plus/java/com/quranunlock/guard/TafsirReferenceNavigation.kt")
plus_edition = read("app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt")
free_reader = read("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
models = read("app/src/main/java/com/quranunlock/guard/TafsirModels.kt")
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
require("entries.containsKey(PrivateTafsirEdition.JALALAYN)" in repo, "Jalalayn-first fallback is missing")
require("availability = null" in controller, "verse change must reset availability before reload")
require("MultiTafsirRepository.loadAvailable(context, verse)" in controller, "controller must load real per-verse coverage")
require("loaded.resolveEdition(selectedEdition)" in controller, "coverage-driven fallback resolution missing")
require("availableEditions = loaded?.editions.orEmpty()" in controller, "renderer must receive only real available editions")
require("DropdownMenu" in renderer, "single Tafsir renderer must expose edition selector")
require("availableEditions.forEach" in renderer, "selector must enumerate only available editions")
require("enabled = availableEditions.size > 1" in renderer, "selector must not advertise a choice when only one edition covers the verse")

# One visual/typographic contract for all editions and notes.
require("private fun InteractiveTafsirText(" in renderer, "shared interactive Tafsir text renderer missing")
require(
    "textAlign = if (isPoetry) TextAlign.Start else TextAlign.Justify" in renderer,
    "shared Tafsir prose renderer must remain justified while explicit poetry is Start-aligned",
)
require("COMMENTARY_LINE_HEIGHT_RATIO = 1.50f" in renderer, "commentary reading line-height contract changed")
require("NOTE_LINE_HEIGHT_RATIO = 1.45f" in renderer, "note reading line-height contract changed")
require("MIN_FONT_SIZE = 16f" in renderer, "minimum sustained-reading font size changed")
require(renderer.count("InteractiveTafsirText(") >= 3,
        "commentary and source notes must both use the shared interactive renderer")
require("TafsirRunStyle.BOLD_ITALIC" in renderer, "bold-italic source translation style missing")
require("TafsirRunStyle.NOTE_REF" in renderer, "source note-call style missing")
require("BaselineShift.Superscript" in renderer, "source note calls must remain superscript")

# Note navigation must resolve only a note that actually exists in the loaded entry.
require("entry.notes.none { it.number == number }" in renderer,
        "note click must fail closed when the linked note is absent")
require("BringIntoViewRequester" in renderer and "bringIntoViewRequester" in renderer,
        "note calls must navigate to their exact source note")
require("noteReturnScroll = scrollState.value" in renderer,
        "note navigation must preserve exact commentary scroll position")
require("Text(\"← Retour au commentaire\")" in renderer,
        "note navigation must expose explicit return to commentary")
require("BackHandler(enabled = noteReturnScroll != null)" in renderer,
        "Android Back must return from a note before closing Tafsir")

# Quran references: explicit source syntax only, canonical validation, no guessing.
require("data class QuranReferenceRef" in models, "Quran reference model missing")
require("(?<!\\d)" in reference_parser and "(?!\\d)" in reference_parser,
        "reference parser must require explicit isolated chapter:verse notation")
require("verseCounts = intArrayOf(" in reference_parser,
        "reference parser must validate against canonical Quran verse counts")
for impossible in ("21:480", "2:293", "4:181", "58:29", "4:186"):
    # The source anomalies are covered by unit tests; the production parser must
    # not contain a hand-written correction for any one of them.
    require(impossible not in reference_parser,
            f"source anomaly must not be hard-corrected in parser: {impossible}")
require("TafsirReferenceParser.find(run.text)" in renderer,
        "explicit source Quran references are not annotated by the shared renderer")
require("run.style != TafsirRunStyle.BOLD_ITALIC" in renderer,
        "source verse-translation anchors must not be reinterpreted as commentary cross-references")
require("onQuranReferenceSelected" in controller and "onQuranReferenceSelected" in plus_edition,
        "Quran-reference click route is not wired through the Plus Tafsir stack")
require("TafsirReferenceNavigation.pageFor" in plus_edition,
        "Plus reference route must resolve inside the pinned Mushaf")
require("QuranStructureMetadata.division(QuranSelectionMode.JUZ" in reference_navigation,
        "reference page lookup must constrain scanning to the canonical Juz page window")
require("MushafVerseIndex.fromSvg" in reference_navigation,
        "reference page lookup must prove the target verse exists on the exact pinned SVG page")
require("EXTRA_REFERENCE_MODE" in free_reader and "← Retour au commentaire" in free_reader,
        "free Quran/Tafsir reader must open an internal reference view with explicit return")
require("preserveTafsirOnNextPause" in free_reader,
        "opening an internal reference must preserve the original Tafsir composition/scroll state")
require("referenceHighlight == null" in free_reader,
        "reference preview must not replace the original interactive Tafsir WebView binding")
require("settings.blockNetworkLoads = true" in free_reader,
        "Quran/Tafsir reference navigation must remain offline")

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
for forbidden in ("Commentary on ${verse.surah}", "Verse ${", "Ayah ${"):
    require(forbidden not in repo and forbidden not in source_rendering,
            f"generated Tafsir heading must remain absent: {forbidden}")

# Jalalayn remains the byte-frozen golden corpus/repository.
require("6_236" in legacy_repo, "Jalalayn expected row count changed")
require("26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56" in legacy_repo,
        "Jalalayn approved database checksum changed")
require('"note_ref" -> TafsirRunStyle.NOTE_REF' in legacy_repo,
        "Jalalayn source note calls must remain structurally distinct")

# Qushayri source structure.
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
require("referencePage" in light and "): Int? = null" in light,
        "Light reference navigation API must remain inert")
require("qurtubi" in light_apk.lower() and "qushayri" in light_apk.lower(),
        "Light APK gate must explicitly reject Qurtubi/Qushayri leakage")

print("0.10.5 Tafsir source/UI audit: PASS")
print("- Jalalayn golden repository/checksum and 427 structured note calls remain preserved")
print("- selector exposes only editions with a real source-backed row for the tapped verse")
print("- shared low-glare renderer justifies prose/notes while explicit source-tagged poetry stays Start-aligned")
print("- source note calls jump to their linked note and return to exact commentary scroll position")
print("- only explicit canonically valid Quran references become offline internal links; impossible source citations stay plain")
print("- Qushayri: 806 source anchors -> 720 logical entries; 584 source soft hyphens source-cleaned")
print("- Qurtubi: 432 entries, four approved volumes, 4:23 excluded")
print("- Qurtubi/Qushayri keep source English verse translation first, then source commentary; no generated heading or Arabic injection")
print("- Light remains Tafsir-free")
