#!/usr/bin/env python3
"""Fail-closed non-regression gate for Tafsir typography/alignment conventions."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit("0.10.5 TAFSIR TYPOGRAPHY AUDIT FAILURE: " + message)


contract = read("docs/TAFSIR_TYPOGRAPHY_ALIGNMENT_CONTRACT_0.10.5.md")
renderer = read("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
source_rendering = read("app/src/plus/java/com/quranunlock/guard/TafsirSourceRendering.kt")
models = read("app/src/main/java/com/quranunlock/guard/TafsirModels.kt")
repository = read("app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt")

# Freeze every special role requested for the Tafsir reader.
for marker in (
    "Commentary prose",
    "Source Qur’an translation inside Tafsīr",
    "Poetry / verse / strophe",
    "Scholarly transliteration",
    "Technical term in commentary",
    "Technical explanatory text / metadata",
    "Lexicon headword",
    "Lexicon definition",
    "Arabic lexicon headword",
    "Qur’an cross-reference",
    "Note call",
    "Note body",
    "Honorific / special source glyph",
):
    require(marker in contract, f"missing typography role: {marker}")

# Freeze alignment decisions.
for marker in (
    "Commentary prose is justified",
    "Poetry is never justified",
    "RTL / logical start",
    "Inherits surrounding paragraph",
    "Justified when multi-line",
):
    require(marker in contract, f"missing alignment convention: {marker}")

# Freeze source-fidelity conventions that were previously regressed.
for marker in (
    "never add by person name",
    "scholarly diacritics",
    "ṣ, ḍ, ḥ, ṭ, ẓ, ā, ī, ū, ʿ and ʾ",
    "preserve source line breaks and stanza boundaries",
    "no AI-authored definition presented as source",
):
    require(marker in contract, f"missing source-fidelity typography rule: {marker}")

# Existing executable renderer invariants must remain in sync with the contract.
require("MIN_FONT_SIZE = 16f" in renderer, "minimum Tafsir reading size changed")
require("MAX_FONT_SIZE = 26f" in renderer, "maximum Tafsir reading size changed")
require("DEFAULT_FONT_SIZE = 18f" in renderer, "default Tafsir reading size changed")
require("COMMENTARY_LINE_HEIGHT_RATIO = 1.50f" in renderer, "commentary line-height changed")
require("NOTE_LINE_HEIGHT_RATIO = 1.45f" in renderer, "note line-height changed")
require(
    "textAlign = if (isPoetry) TextAlign.Start else TextAlign.Justify" in renderer,
    "commentary/note prose must remain justified while poetry stays Start-aligned",
)
require("TafsirRunStyle.BOLD_ITALIC" in renderer, "bold-italic source-translation styling missing")
require("TafsirRunStyle.NOTE_REF" in renderer and "BaselineShift.Superscript" in renderer,
        "source note-call typography missing")
require("TextDecoration.Underline" in renderer, "interactive source references must remain visually distinct")
require("POETRY" in models and "TECHNICAL_TERM" in models and "TRANSLITERATION" in models,
        "semantic Tafsir run roles are missing from the executable model")
require("splitTafsirRenderBlocks" in models and "run.style == TafsirRunStyle.POETRY" in models,
        "source-tagged poetry is not separated from prose at block level")
require("POETRY_LINE_HEIGHT_RATIO = 1.55f" in renderer, "poetry line-height rule is not executable")
require("block.kind == TafsirBlockKind.POETRY" in renderer, "renderer does not branch on poetry semantics")
require("TextAlign.Start else TextAlign.Justify" in renderer,
        "poetry must render Start while prose remains justified")
require("TafsirRunStyle.POETRY -> SpanStyle" in renderer, "poetry style rendering missing")
require("TafsirRunStyle.TECHNICAL_TERM -> SpanStyle" in renderer, "technical term style missing")
require("TafsirRunStyle.TRANSLITERATION -> SpanStyle" in renderer, "transliteration style missing")
for marker in (
    '"poetry" -> TafsirRunStyle.POETRY',
    '"technical_term" -> TafsirRunStyle.TECHNICAL_TERM',
    '"transliteration" -> TafsirRunStyle.TRANSLITERATION',
):
    require(marker in repository, f"repository cannot decode semantic source role: {marker}")
require("TafsirRunStyle.BOLD_ITALIC, row.translation.trim()" in source_rendering,
        "Qushayri/Qurtubi source translation must remain bold-italic and source-first")

print("0.10.5 Tafsir typography/alignment contract: PASS")
print("- special roles and alignments are frozen in a blocking non-regression contract")
print("- prose remains justified; explicitly source-tagged poetry is Start-aligned at 1.55x and never inferred from italics")
print("- technical-term and transliteration roles are executable without rewriting source wording")
