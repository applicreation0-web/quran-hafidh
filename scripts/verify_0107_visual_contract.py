#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def require(ok, message):
    if not ok:
        raise SystemExit("0.10.10 PRODUCT CONTRACT FAILURE: " + message)


design = read("app/src/main/java/com/quranunlock/guard/SafeguardDesign.kt")
theme = read("app/src/main/java/com/quranunlock/guard/MainActivity.kt")
reader = read("app/src/main/assets/reader109/reader.js")
hifz_guard = read("app/src/main/assets/reader109/hifz_guard.js")
hifz_policy = read("app/src/main/java/com/quranunlock/guard/HifzTrainingPolicy.kt")
eink = read("app/src/main/java/com/quranunlock/guard/EInkRefreshController.kt")
icon_light = read("app/src/main/res/drawable/ic_launcher_foreground.xml")
icon_plus = read("app/src/plus/res/drawable/ic_launcher_foreground.xml")
geometry_test = read("app/src/test/java/com/quranunlock/guard/HifzGeometryPolicyTest.kt")

# Final visual identity. Historical green/gold/brown values are explicitly obsolete.
for marker in (
    "SafeguardAppBackground = Color(0xFFFBF7EF)",
    "SafeguardReadingSurface = Color(0xFFF7F2E8)",
    "SafeguardInk = Color(0xFF171715)",
):
    require(marker in design, "final cream/black design token missing: " + marker)

for banned in ("#2C5D49", "#214B3B", "#18392E", "#B0823F", "#D8BA73", "#7A5337", "#1D5B47"):
    require(banned not in theme + reader + eink + icon_light + icon_plus,
            "obsolete coloured visual token remains active: " + banned)

require("primary = SafeguardInk" in theme and "tertiary = SafeguardInk" in theme,
        "Compose theme must be monochrome")
require("ElevatedCard" not in theme,
        "dashboard must not return to elevated-card-heavy presentation")
require(icon_light.count("#171715") >= 3 and icon_plus.count("#171715") >= 3,
        "adaptive launcher icons must use the black-ink contract")

# Audio is visible and callable only in memorization/Hifz, never normal reading.
require("const audioModeAllowed=()=>memory||!!initial.hifz" in reader,
        "reader audio scope gate missing")
require("$('audio').hidden=!allowAudio" in reader and "$('repeat').hidden=!allowAudio" in reader,
        "normal reader can still expose audio controls")
require("if(!audioModeAllowed())return" in reader,
        "audio actions must fail closed outside memorization/Hifz")
require("Audio Al-Husary Muʿallim" in hifz_guard,
        "structured Hifz audio control missing")
require("st.kind==='AUDIO_PASSIVE'||st.kind==='AUDIO_ACTIVE'" in hifz_guard,
        "structured Hifz audio repetitions are not connected to native progress")

# Hifz repetition/masking product rules.
require('HifzTrainingStep("sabqi-visible", "Texte visible", HifzTrainingKind.VISIBLE, 10, 0)' in hifz_policy,
        "Sabqi must start its reading protocol with 10 visible readings")
for marker in (
    'HifzTrainingStep("sabqi-mask-25", "Masquage 25 %", HifzTrainingKind.MASKED, 5, 25)',
    'HifzTrainingStep("sabqi-mask-50", "Masquage 50 %", HifzTrainingKind.MASKED, 5, 50)',
    'HifzTrainingStep("sabqi-mask-75", "Masquage 75 %", HifzTrainingKind.MASKED, 5, 75)',
    'HifzTrainingStep("sabqi-mask-100", "Masquage 100 %", HifzTrainingKind.MASKED, 7, 100)',
):
    require(marker in hifz_policy, "Sabqi progressive masking rule missing")
require('id = "itqan-visible-20"' in hifz_policy and "repetitions = 20" in hifz_policy,
        "Itqan must contain 20 visible repetitions")
require('HifzTrainingStep("itqan-mask-25", "Masquage 25 %", HifzTrainingKind.MASKED, 2, 25' in hifz_policy,
        "Itqan 10-masked protocol missing")
require('id = "murajaah-recall"' in hifz_policy and "maskPercent = 0" in hifz_policy,
        "Murajaah must not impose masking")
require("visibleReadings" in hifz_guard and "maskedReadings" in hifz_guard,
        "structured Hifz must expose separate visible/masked counters")
require("readingCounts" in reader and "avec masque" in reader and "sans masque" in reader,
        "free memorization must expose separate visible/masked counters")

# E-Ink: portable, deterministic and monochrome. Physical-device proof is separate.
require("com.onyx" not in eink and "EpdController" not in eink,
        "manufacturer-specific E-Ink API dependency remains")
require("profile == DisplayProfile.STANDARD" in eink,
        "Standard display profile must remain a strict no-op")
require("einkFullRefreshFallback" in reader and "#171715" in reader and "#F7F2E8" in reader,
        "portable monochrome E-Ink cleanup missing")

# Edge cases are product tests, not decorative tests.
require("midPageTargetKeepsOnlyTargetVersesActive" in geometry_test,
        "Itqan mid-page start regression test missing")
require("fifteenRealLinesSplitFiveByFiveButKeepOneCanonicalVerse" in geometry_test,
        "long Sabqi verse regression test missing")

print("Quran Safeguard 0.10.10 requirement-driven product contract: PASS")
