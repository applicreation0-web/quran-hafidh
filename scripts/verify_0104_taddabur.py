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
free_reader = read("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
design = read("app/src/main/java/com/quranunlock/guard/SafeguardDesign.kt")
light_tafsir = read("app/src/light/java/com/quranunlock/guard/TafsirEdition.kt")
plus_tafsir = read("app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt")

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

# P0 regression gate: changing the logical page must recreate the Mushaf WebView,
# rebuild the page-specific Tafsir mapping, and ignore stale callbacks from an old page.
require(
    reader,
    "androidx.compose.runtime.key(pageNumber)",
    "onReady: (Int) -> Unit",
    "onFailure: (Int) -> Unit",
    "currentOnReady.value(pageNumber)",
    "currentOnFailure.value(pageNumber)",
    "if (readyPage == page) pageReady = true",
    "if (failedPage == page) pageReady = false",
    "TafsirEdition.prepareHtml(svgContent, pageNumber)",
    "pageNumber = pageNumber",
    "verseIndex = MushafVerseIndex.fromSvg(svgContent)",
)
forbid(reader, "onReady: () -> Unit", "onFailure: () -> Unit")

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

# Final user override for sustained reading comfort: use one warm low-glare surface
# across both flavors' Mushaf rendering, Taddabur, Tafsir and the voluntary Quran reader.
require(
    design,
    "SafeguardReadingSurface = Color(0xFFF4F0E6)",
    ".heightIn(min = 52.dp)",
)
require(light_tafsir, "html, body, svg { background: #F4F0E6 !important; }")
require(plus_tafsir, "html, body, svg { background: #F4F0E6 !important; }")
require(
    reader,
    "color = SafeguardReadingSurface",
    "Color.rgb(244, 240, 230)",
    "background:#F4F0E6",
    "settings.builtInZoomControls = true",
    "settings.displayZoomControls = false",
)
require(
    free_reader,
    "color = SafeguardReadingSurface",
    "Color.rgb(244, 240, 230)",
    "background: #F4F0E6",
)
require(
    tafsir_panel,
    "color = SafeguardReadingSurface",
    'Text("A−")',
    'Text("A+")',
    "lineHeight = (fontSize * 1.42f).sp",
    "preferences.edit().putFloat(FONT_SIZE_KEY, next).apply()",
)
forbid(
    reader + free_reader + tafsir_panel + light_tafsir + plus_tafsir,
    "#F7F2E8",
    "#F7FBF6",
    "background:#000",
    "background: #000",
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

print("Taddabur 0.10.5 source audit PASS: Plus-only, fixed Hizb 1-60, 90 s/page, bookmark, page-keyed WebView reload, stale-callback rejection, Tafsir remapping, warm reading surface, 20:00-midnight enforcement")
