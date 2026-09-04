#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path: str) -> str:
    p = ROOT / path
    if not p.is_file():
        raise SystemExit(f"Missing Taddabur release file: {path}")
    return p.read_text(encoding="utf-8")

def require(text: str, *needles: str) -> None:
    for needle in needles:
        if needle not in text:
            raise SystemExit(f"Missing Taddabur release invariant: {needle}")

def forbid(text: str, *needles: str) -> None:
    for needle in needles:
        if needle in text:
            raise SystemExit(f"Forbidden Taddabur release signal: {needle}")

plus = read("app/src/plus/java/com/quranunlock/guard/TaddaburEdition.kt")
reader = read("app/src/plus/java/com/quranunlock/guard/TaddaburActivity.kt")
light = read("app/src/light/java/com/quranunlock/guard/TaddaburEdition.kt")
main_manifest = read("app/src/main/AndroidManifest.xml")
plus_manifest = read("app/src/plus/AndroidManifest.xml")
dashboard = read("app/src/main/java/com/quranunlock/guard/DashboardActivity.kt")
service = read("app/src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt")
gate = read("app/src/main/java/com/quranunlock/guard/GateActivity.kt")
reminders = read("app/src/main/java/com/quranunlock/guard/MindfulReminderScheduler.kt")
tests = read("app/src/testPlus/java/com/quranunlock/guard/TaddaburPolicyTest.kt")
tafsir_panel = read("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")

# Plus-only flavor boundary.
require(light, "const val isEnabled: Boolean = false", "fun DashboardCard() = Unit")
forbid(light, "taddabur_prefs", "TADDABUR_DEADLINE", "TaddaburActivity::class.java")
forbid(main_manifest, "TaddaburActivity")
require(plus_manifest, 'android:name=".TaddaburActivity"', 'android:exported="false"')
require(dashboard, "TaddaburEdition.DashboardCard()")

# Fixed 1..60 pool, no dependency on the user's selectable Hizb pool.
require(
    plus,
    "const val FIRST_HIZB = 1",
    "const val LAST_HIZB = 60",
    "if (current >= LAST_HIZB) FIRST_HIZB else current + 1",
    'private const val FILE = "taddabur_prefs"',
)
forbid(plus, "selected_hizb", "GuardPrefs.selectedHizb", "selectionMode")

# Daily window, page minimum and after-20 fixed penalty.
require(
    plus,
    "const val START_HOUR = 7",
    "const val DEADLINE_HOUR = 20",
    "const val MIN_PAGE_MS = 90_000L",
    "PENALTY_DAY",
    "if (state.penaltyActive) return true",
    "if (state.complete && state.completedAtEpochMs != null) return false",
    "Blocage Taddabur actif jusqu’à minuit.",
    "Aucun joker et aucune lecture de déblocage ne contournent cette règle.",
)
require(reader, "TaddaburPolicy.MIN_PAGE_MS", "TaddaburPolicy.mayAccumulate(LocalTime.now())")

# Bookmark and all-day cumulative reading.
require(
    plus,
    "BOOKMARK_PAGE",
    "fun setBookmark",
    "bookmarkPage",
    "history(context, 7)",
    "writeSnapshot",
)
require(
    reader,
    "TaddaburPrefs.setBookmark",
    "TaddaburPrefs.elapsedMs",
    "TaddaburPrefs.recordActiveMs",
    "Fermer • garder le marque-page",
)

# Tafsir is available inside the Taddabur reader and must not pause the active-page timer.
require(
    reader,
    "TafsirEdition.prepareHtml",
    "TafsirEdition.configureWebView",
    "TafsirEdition.Panel",
    "selectedTafsirVerse",
    "mayCountActiveReading()",
)
forbid(reader, "GuardPrefs.completeReadingAndUnlock", "consumeJokerAndUnlock")

# Reading comfort contract: no added coloured reading background. Keep practical
# readability controls without imposing a light/dark/ivory/green reader surface.
require(
    reader,
    "Color.TRANSPARENT",
    "background:transparent",
    "settings.builtInZoomControls = true",
    "settings.displayZoomControls = false",
)
forbid(
    reader,
    "#F7F2E8",
    "Color.rgb(247, 242, 232)",
    "background:#000",
    "background: #000",
)
require(
    tafsir_panel,
    'Text("A−")',
    'Text("A+")',
    "lineHeight = (fontSize * 1.42f).sp",
    "preferences.edit().putFloat(FONT_SIZE_KEY, next).apply()",
)
forbid(
    tafsir_panel,
    "Color(0xFFF7FBF6)",
    "androidx.compose.ui.graphics.Color",
)

# 20:00 reminder and reboot/time-change scheduling route through the flavor hook.
require(
    plus,
    "ACTION_REMINDER",
    "TADDABUR_DEADLINE",
    "setContentTitle(\"Taddabur • Hizb ${state.hizb}\")",
    "applications protégées bloquées jusqu’à minuit",
)
require(
    reminders,
    "TaddaburEdition.scheduleReminder(context)",
    "TaddaburEdition.handlesReminder(intent?.action)",
    "TaddaburEdition.handleReminder(context, intent?.action)",
)

# Protected-app enforcement overrides remaining 15-minute credit without consuming it.
require(
    service,
    "triggerTaddaburGateIfNeeded",
    "TaddaburEdition.shouldBlockNow(this@QuranAccessibilityService)",
    "TADDABUR_DEADLINE_GATE",
    "!TaddaburEdition.shouldBlockNow(this)",
    "val taddaburBlocked = TaddaburEdition.shouldBlockNow(this)",
    "if (!taddaburBlocked && GuardPrefs.isUnlocked(this, packageName)) return",
)
require(
    gate,
    "!TaddaburEdition.shouldBlockNow(this)",
    "TaddaburEdition.renderBlockingGate(this, challengeKey)",
    "if (TaddaburEdition.shouldBlockNow(this@GateActivity))",
)

# Regression tests must encode the literal contract.
require(
    tests,
    "poolIsFixedFromOneToSixtyAndWrapsWithoutSelection",
    "pageRequiresExactlyNinetySecondsMinimum",
    "activeReadingStartsAtSevenAndCanContinueAfterDeadline",
    "deadlineStartsAtTwenty",
)

print("Taddabur 0.10.4 source audit PASS: Plus-only, fixed Hizb 1-60, 90 s/page, bookmark, Tafsir, neutral reading surface, readability controls, 20:00-midnight enforcement")
