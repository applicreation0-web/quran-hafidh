#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

def require(ok, message):
    if not ok:
        raise SystemExit("0.10.7 VISUAL CONTRACT FAILURE: " + message)

reader = read("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
design = read("app/src/main/java/com/quranunlock/guard/SafeguardDesign.kt")
theme = read("app/src/main/java/com/quranunlock/guard/MainActivity.kt")
hub = read("app/src/main/java/com/quranunlock/guard/QuranHubActivity.kt")
panel = read("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
repo = read("app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt")
edition = read("app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt")
contract = read("docs/VISUAL_CONTRACT_0.10.7.md")

require("statusBarsPadding()" in reader and "navigationBarsPadding()" in reader,
        "reader must respect Android system bars")
require("Navigation • page $quickNavPage / $LAST_PAGE" in reader,
        "persistent free-reading page slider label missing")
require(reader.count("valueRange = FIRST_PAGE.toFloat()..LAST_PAGE.toFloat()") >= 2,
        "both quick and persistent page navigation must cover 1..604")
require("onValueChangeFinished" in reader,
        "page navigation must commit on drag finish")
require("steps = LAST_PAGE - FIRST_PAGE - 1" not in reader,
        "hundreds of slider tick marks must not clutter the reader")
require(".height(28.dp)" in reader,
        "persistent page slider must stay visually compact")
require("chromeHidden" in reader and "pureReading" in reader,
        "clean reading mode must remain available")
require("0.84f" in reader and "0.42f" in reader,
        "Tafsir compact/expanded reading states missing")

for marker in (
    "SafeguardAppBackground = Color(0xFFFBF7EF)",
    "SafeguardSurface = Color(0xFFFFFDF8)",
    "SafeguardReadingSurface = Color(0xFFF7F2E8)",
    "SafeguardDeepGreen = Color(0xFF214B3B)",
    "SafeguardTextGreen = Color(0xFF18392E)",
    "SafeguardSecondaryText = Color(0xFF514A43)",
    "SafeguardGold = Color(0xFFB0823F)",
):
    require(marker in design, "0.10.7 palette token missing: " + marker)

for old in ("sahelianButtonOrnament", "drawDiamond"):
    require(old not in design, "ornamental button drawing returned: " + old)

for color in (
    "primary = SafeguardDeepGreen",
    "background = SafeguardAppBackground",
    "surface = SafeguardSurface",
    "onBackground = SafeguardTextGreen",
    "onSurfaceVariant = SafeguardSecondaryText",
    "tertiary = SafeguardGold",
):
    require(color in theme, "premium palette marker missing: " + color)

require("ElevatedCard" not in hub and "QuranHubRow" in hub,
        "Qur'an hub must remain a light list rather than elevated-card grid")
require("if (isPoetry) TextAlign.Start else TextAlign.Justify" in panel,
        "Tafsir prose must be justified while poetry stays start-aligned")
require("run.text.replace(Regex(\"\\\\n{2,}\"), \"\\n\")" in panel,
        "non-poetry double blank lines must stay collapsed")
require("stroke: none !important" in edition and "#C8CEC8" in edition,
        "neutral no-outline verse selection changed")

for marker in ('JALALAYN("jalalayn", "Jalalayn")',
               'QURTUBI("qurtubi", "Qurtubi")',
               'QUSHAYRI("qushayri", "Qushayri")'):
    require(marker in repo, "author label changed: " + marker)

for marker in (
    "curseur horizontal permanent",
    "niveaux de gris",
    "Tafsîr reste en anglais",
    "#214B3B",
    "#B0823F",
    "Aucun mode noir",
    "prose du commentaire est justifiée",
    "réellement orphelins",
):
    require(marker in contract, "visual contract marker missing: " + marker)

print("0.10.7 visual publication contract: PASS")
