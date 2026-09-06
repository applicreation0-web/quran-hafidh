#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

def require(condition, message):
    if not condition:
        raise SystemExit("0.10.7 TAFSIR READING AUDIT FAILURE: " + message)

panel = read("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
reader = read("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
edition = read("app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt")
light_edition = read("app/src/light/java/com/quranunlock/guard/TafsirEdition.kt")
repo = read("app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt")
contract = read("docs/TAFSIR_READING_CONTRACT_0.10.7.md")

require("if (isPoetry) TextAlign.Start else TextAlign.Justify" in panel,
        "commentary must be justified while poetry remains Start-aligned")
require("COMMENTARY_LINE_HEIGHT_RATIO = 1.50f" in panel, "commentary line height changed")
require("NOTE_LINE_HEIGHT_RATIO = 1.45f" in panel, "note line height changed")
require("POETRY_LINE_HEIGHT_RATIO = 1.55f" in panel, "poetry line height changed")
require("run.text.replace(Regex" in panel and "n{2,}" in panel, "false blank-line collapse missing")
require("tapStart = (linkStart - 2)" in panel and "tapEnd = (linkEnd + 2)" in panel, "expanded Quran reference tap target missing")
require("onExpandedChange(!expanded)" in panel, "Tafsir expand/collapse control missing")
require("0.84f" in reader and "0.42f" in reader, "compact/expanded Tafsir heights missing")
require("expanded: Boolean = false" in edition and "onExpandedChange: (Boolean) -> Unit = {}" in edition, "Plus shared Panel defaults missing")
require("expanded: Boolean = false" in light_edition and "onExpandedChange: (Boolean) -> Unit = {}" in light_edition, "Light shared Panel defaults missing")
require("stroke: none !important" in edition and "#C8CEC8" in edition, "neutral no-outline verse highlight missing")
for marker in ('JALALAYN("jalalayn", "Jalalayn")', 'QURTUBI("qurtubi", "Qurtubi")', 'QUSHAYRI("qushayri", "Qushayri")'):
    require(marker in repo, "author label changed: " + marker)
for marker in ("source-backed English", "full justification", "Poetry is the exception", "compact and expanded", "grayscale"):
    require(marker in contract, "contract marker missing: " + marker)
print("0.10.7 Tafsir reading contract: PASS")
