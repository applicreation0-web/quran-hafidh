#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def path(rel: str) -> Path:
    return ROOT / rel


def replace_once(rel: str, old: str, new: str) -> None:
    p = path(rel)
    text = p.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{rel}: expected exactly one source match, got {count}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


panel = "app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt"
multi = "app/src/plus/java/com/quranunlock/guard/MultiTafsirPanel.kt"
edition = "app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt"
light_edition = "app/src/light/java/com/quranunlock/guard/TafsirEdition.kt"
reader = "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt"

# Narrow mobile/e-reader layouts must not stretch English words to fill the line.
replace_once(
    panel,
    "textAlign = if (isPoetry) TextAlign.Start else TextAlign.Justify",
    "textAlign = TextAlign.Start",
)

# Add a compact/expanded Tafsir state without duplicating the reading controls.
replace_once(
    panel,
    """    maxPanelHeight: Dp,
    onPanelTopInWindow: (Int) -> Unit
) {""",
    """    maxPanelHeight: Dp,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPanelTopInWindow: (Int) -> Unit
) {""",
)
replace_once(
    panel,
    """                Row {
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = "Réduire la taille du commentaire"
                        },""",
    """                Row {
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = if (expanded) {
                                "Réduire le panneau du Tafsîr"
                            } else {
                                "Agrandir le panneau du Tafsîr"
                            }
                            stateDescription = if (expanded) "Tafsîr agrandi" else "Tafsîr compact"
                        },
                        onClick = { onExpandedChange(!expanded) }
                    ) {
                        Text(
                            if (expanded) "⤡" else "⤢",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = "Réduire la taille du commentaire"
                        },""",
)

# Thread the state through the Plus-only controller.
replace_once(
    multi,
    """    maxPanelHeight: Dp,
    onPanelTopInWindow: (Int) -> Unit,
    onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)? = null""",
    """    maxPanelHeight: Dp,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPanelTopInWindow: (Int) -> Unit,
    onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)? = null""",
)
replace_once(
    multi,
    """        modifier = modifier,
        maxPanelHeight = maxPanelHeight,
        onPanelTopInWindow = onPanelTopInWindow""",
    """        modifier = modifier,
        maxPanelHeight = maxPanelHeight,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onPanelTopInWindow = onPanelTopInWindow""",
)

# Keep the shared reader API source-set compatible. Defaults preserve existing
# Safeguard/Mushaf call sites, while FreeQuranReader can pass the expansion state.
replace_once(
    edition,
    """        maxPanelHeight: Dp,
        onPanelTopInWindow: (Int) -> Unit,
        onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)? = null""",
    """        maxPanelHeight: Dp,
        expanded: Boolean = false,
        onExpandedChange: (Boolean) -> Unit = {},
        onPanelTopInWindow: (Int) -> Unit,
        onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)? = null""",
)
replace_once(
    edition,
    """            modifier = modifier,
            maxPanelHeight = maxPanelHeight,
            onPanelTopInWindow = onPanelTopInWindow,""",
    """            modifier = modifier,
            maxPanelHeight = maxPanelHeight,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            onPanelTopInWindow = onPanelTopInWindow,""",
)
replace_once(
    light_edition,
    """        maxPanelHeight: Dp,
        onPanelTopInWindow: (Int) -> Unit,
        onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)? = null""",
    """        maxPanelHeight: Dp,
        expanded: Boolean = false,
        onExpandedChange: (Boolean) -> Unit = {},
        onPanelTopInWindow: (Int) -> Unit,
        onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)? = null""",
)

# Compact by default for consultation; expand to 84% for sustained Tafsir reading.
replace_once(
    reader,
    """                var pureReading by remember { mutableStateOf(false) }
                var visualMode by remember {""",
    """                var pureReading by remember { mutableStateOf(false) }
                var tafsirExpanded by remember { mutableStateOf(false) }
                var visualMode by remember {""",
)
replace_once(
    reader,
    """                LaunchedEffect(selectedTafsirVerse) {
                    val requestedVerse = selectedTafsirVerse ?: return@LaunchedEffect""",
    """                LaunchedEffect(selectedTafsirVerse) {
                    tafsirExpanded = false
                    val requestedVerse = selectedTafsirVerse ?: return@LaunchedEffect""",
)
replace_once(
    reader,
    """                                    maxPanelHeight = maxHeight * 0.42f,
                                    onPanelTopInWindow = { top ->""",
    """                                    maxPanelHeight = maxHeight * if (tafsirExpanded) 0.84f else 0.42f,
                                    expanded = tafsirExpanded,
                                    onExpandedChange = { tafsirExpanded = it },
                                    onPanelTopInWindow = { top ->""",
)

# Selected verse: muted neutral fill, no enclosing stroke. This remains readable in grayscale.
replace_once(
    edition,
    """                  .ayahPolygon.qsg-selected {
                    fill: #BFE8C8 !important;
                    fill-opacity: .48 !important;
                  }""",
    """                  .ayahPolygon.qsg-selected {
                    fill: #C8CEC8 !important;
                    fill-opacity: .44 !important;
                    stroke: none !important;
                  }""",
)

contract = path("docs/TAFSIR_READING_CONTRACT_0.10.7.md")
contract.write_text(
    """# Quran Safeguard 0.10.7 — Tafsîr reading contract

Status: blocking 0.10.7 presentation contract.

- The displayed Tafsîr remains source-backed English. No generated translation or paraphrase is inserted.
- Primary edition labels remain the author names: **Jalalayn**, **Qurtubi**, **Qushayri**.
- Commentary, source translation, notes and metadata use logical Start/left alignment on Latin text. Full justification is disabled to avoid stretched word spacing on narrow screens.
- Established sizes remain: 18sp default commentary, 16–26sp user range; commentary 1.50×, notes 1.45×, poetry 1.55×.
- Poetry is source-semantic only, never inferred from italics. Source line breaks and stanza breaks are preserved. False PDF/OCR blank lines outside source poetry/stanzas are removed.
- Qushayri source-note calls are removed only when source verification establishes them as unexposed footnote calls. Arbitrary numbers must never be stripped.
- Explicit canonical Qur’an cross-references remain interactive with a tap target larger than the visible underlined reference. Back returns to the originating Tafsîr state.
- The Tafsîr panel has compact and expanded reading states; expansion must not duplicate controls or alter source text.
- Selected Mushaf verses use a muted neutral highlight with no enclosing outline. Selection must not imply neighboring verse fragments.
- Reading surfaces use cream/neutral colors and remain understandable in grayscale; color is not the only carrier of meaning.
""",
    encoding="utf-8",
)

verifier = path("scripts/verify_0107_tafsir_reading_contract.py")
verifier.write_text(
    '''#!/usr/bin/env python3
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

require("textAlign = TextAlign.Start" in panel, "Latin Tafsir text must use Start alignment")
require("TextAlign.Justify" not in panel, "full justification must not survive in the 0.10.7 renderer")
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
for marker in ("source-backed English", "Full justification is disabled", "compact and expanded", "grayscale"):
    require(marker in contract, "contract marker missing: " + marker)
print("0.10.7 Tafsir reading contract: PASS")
''',
    encoding="utf-8",
)

# Static invariants before CI compilation.
panel_text = path(panel).read_text(encoding="utf-8")
repo_text = path("app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt").read_text(encoding="utf-8")
assert "TextAlign.Justify" not in panel_text
assert 'QUSHAYRI("qushayri", "Qushayri")' in repo_text
assert "stroke: none !important" in path(edition).read_text(encoding="utf-8")
print("0.10.7 reading-focus patch applied")
