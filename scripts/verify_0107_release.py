#!/usr/bin/env python3
"""Contradictory source gate for Quran Safeguard 0.10.7."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []


def text(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


build = text("app/build.gradle.kts")
manifest = text("app/src/main/AndroidManifest.xml")
light_config = text("app/src/main/res/xml/accessibility_service_config.xml")
plus_config = text("app/src/plus/res/xml/accessibility_service_config.xml")
protected = text("app/src/main/java/com/quranunlock/guard/ProtectedApps.kt")
service = text("app/src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt")
presence = text("app/src/main/java/com/quranunlock/guard/TargetPresenceScopePolicy.kt")
reader = text("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
comfort = text("app/src/main/java/com/quranunlock/guard/ReaderComfortPrefs.kt")
panel = text("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
parser = text("app/src/plus/java/com/quranunlock/guard/TafsirReferenceParser.kt")
nav = text("app/src/plus/java/com/quranunlock/guard/TafsirReferenceNavigation.kt")
exit_policy = text("app/src/main/java/com/quranunlock/guard/TargetWindowExitPolicy.kt")
exit_tests = text("app/src/test/java/com/quranunlock/guard/TargetWindowExitPolicyTest.kt")
parser_tests = text("app/src/testPlus/java/com/quranunlock/guard/TafsirReferenceParserTest.kt")

# Exact release metadata.
require('versionCode = 26' in build, "versionCode 26 missing")
require('versionName = "0.10.7"' in build, "Light 0.10.7 versionName missing")
require('versionNameSuffix = "-plus.1"' in build, "Plus .plus / -plus.1 continuity missing")
require('applicationId = "com.applicreation0.quransafeguard"' in build, "Light applicationId changed")

# Strict selected-target-only Accessibility. Deliberately search every compiled source
# for broad package/window APIs instead of trusting one policy function.
for forbidden in (
    "android.permission.QUERY_ALL_PACKAGES",
    "android.permission.PACKAGE_USAGE_STATS",
    "android.permission.READ_PHONE_STATE",
    "android.permission.INTERNET",
):
    require(forbidden not in manifest, f"forbidden manifest capability: {forbidden}")
require('android:canRetrieveWindowContent="false"' in light_config, "Light window-content retrieval must be false")
require('android:canRetrieveWindowContent="false"' in plus_config, "Plus window-content retrieval must be false")
require('android:packageNames="com.applicreation0.quransafeguard"' in light_config,
        "Light static Accessibility config must be self-only")
require('android:packageNames="com.applicreation0.quransafeguard.plus"' in plus_config,
        "Plus static Accessibility config must be self-only")
for forbidden in (
    "packageNames = null",
    "anonymousExitSentinel",
    "broad = true",
    "UsageStatsManager",
    "FLAG_RETRIEVE_INTERACTIVE_WINDOWS",
    "getWindows()",
):
    require(forbidden not in service, f"runtime broad-scope marker survived: {forbidden}")
require("TargetPresenceScopePolicy.requiresAnonymousExitSentinel" not in service,
        "historical sentinel must not be called by runtime")
require("): Boolean = false" in presence, "historical sentinel guard is not fail-closed")
require("GuardPrefs.protectedPackages(context)" in protected, "selected target set is not runtime source")
require("filterTo(linkedSetOf())(::isSelectableTarget)" in protected,
        "runtime event scope does not re-filter to selectable targets")
require("it += context.packageName" in protected, "Safeguard self package missing from runtime scope")
for outside_marker in ("SYSTEM_UI_PACKAGE", "launcherPackage", "transitionSignalPackages"):
    require(outside_marker not in protected, f"outside transition package still admitted: {outside_marker}")
require("info.packageNames = ProtectedApps.eventScopePackages(this).toTypedArray()" in service,
        "runtime package list is not strict selected targets + self")

# Target-owned exit proof: improves timer stopping without observing destination.
require("AccessibilityEvent.WINDOWS_CHANGE_REMOVED" in exit_policy,
        "target-window removal proof missing")
require("eventPackage == runningBudgetPackage" in exit_policy,
        "exit proof can be owned by the wrong package")
require("TargetWindowExitPolicy.provesSelectedTargetWindowRemoved" in service,
        "service does not apply target-owned exit proof")
require("TARGET_WINDOW_REMOVED" in service, "target exit is not diagnosable")
require("selectedTargetRemovalProvesExitWithoutKnowingDestination" in exit_tests,
        "target-only exit regression test missing")

# Fixed target catalogue = six socials + eight browsers. No dynamic category discovery.
expected_targets = {
    "com.whatsapp", "com.twitter.android", "com.instagram.android",
    "com.facebook.katana", "com.google.android.youtube", "com.zhiliaoapp.musically",
    "com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx",
    "com.brave.browser", "com.opera.browser", "com.sec.android.app.sbrowser",
    "com.duckduckgo.mobile.android", "com.vivaldi.browser",
}
for package in expected_targets:
    require(package in protected, f"approved target missing: {package}")
for package in (
    "org.telegram.messenger", "com.discord", "com.reddit.frontpage", "com.snapchat.android",
    "com.revolut.revolut", "com.barclays.android.barclaysmobilebanking",
    "com.google.android.apps.authenticator2", "com.x8bit.bitwarden",
):
    require(package not in protected, f"outside package classified by product scope: {package}")

# Reader field-test corrections.
for marker in ("statusBarsPadding()", "navigationBarsPadding()", "bottomActionsOpen"):
    require(marker in reader, f"free-reader correction missing: {marker}")
require("Options de lecture ▾" in reader and "Options de lecture ▴" in reader,
        "retractable lower controls missing")
require('"Qur’an"' in reader and '"Mushaf de Médine • p. $page/$LAST_PAGE' in reader,
        "compact reader header missing")
require("0.72f" not in reader, "fake Auto brightness position survived")
require("Luminosité : automatique (téléphone)" in reader, "honest Auto brightness label missing")
require("Manuel 55 %" in reader, "explicit Auto-to-manual transition missing")
require("0.08f" in comfort and "BRIGHTNESS_OVERRIDE_NONE" in comfort,
        "local brightness range/system reset contract missing")
require("Settings.System" not in comfort and "SCREEN_BRIGHTNESS" not in comfort,
        "reader must not persist global Android brightness")

# Tafsir reference reliability. Text remains source-driven; controls and fuzzy hit
# resolution only invoke canonically parsed annotations.
require("resolveTafsirAnnotation" in panel, "forgiving inline reference activation missing")
require("for (distance in 1..6)" in panel, "inline near-miss tolerance missing")
require("TafsirAccessibleReferenceRow" in panel, "explicit finger-size reference controls missing")
require("heightIn(min = 48.dp)" in panel, "Tafsir reference controls are below 48dp")
require('Text("Coran ${reference.label}")' in panel, "Quran reference control missing")
require("TafsirReferenceParser.find(it.text)" in panel, "reference controls bypass canonical parser")
require("run.style != TafsirRunStyle.BOLD_ITALIC" in panel,
        "current source verse anchor would be duplicated as a cross-reference")
require("TafsirRunStyle.POETRY -> SpanStyle()" in panel,
        "renderer still imposes a partial italic layer on source-indexed poetry")
require("block.kind == TafsirBlockKind.POETRY" in panel and "TextAlign.Start else TextAlign.Justify" in panel,
        "source poetry/prose block distinction was lost")
require("explicitReference" in parser and "isCanonical" in parser,
        "Tafsir links are not canonical-source parsed")
require("TafsirEdition.referencePage" in reader or "TafsirEdition.referencePage" in text("app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt"),
        "reader no longer resolves Tafsir Quran references")
require("MushafVerseIndex.fromSvg" in nav, "reference navigation no longer resolves exact Medina page")
require("bracketsPunctuationAndUnicodeRangesRemainClickable" in parser_tests,
        "punctuation/Unicode range parser regression test missing")
require("ordinaryNumbersAndInvalidVersesNeverBecomeLinks" in parser_tests,
        "false-link regression test missing")

# Invariants that 0.10.7 must not weaken.
require("private const val LAST_PAGE = 604" in reader, "free reader lost 604-page boundary")
require("QuranBookmarkStore" in reader, "bookmarks were lost")
require("ReadingValidationPolicy" in text("app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt"),
        "60-second validation path was disturbed")
require("MIN_ACTIVE_READING_MS = 60_000L" in text("app/src/main/java/com/quranunlock/guard/ReadingValidationPolicy.kt"),
        "60-second minimum changed")
require("INTERVAL_MINUTES = 15" in text("app/src/main/java/com/quranunlock/guard/UsageCyclePolicy.kt"),
        "15-minute cycle changed")
require("CUMULATIVE_MINUTES = 90" in text("app/src/main/java/com/quranunlock/guard/UsageCyclePolicy.kt"),
        "90-minute cycle changed")
require("AdhkarPeriod.MORNING" in text("app/src/main/java/com/quranunlock/guard/AdhkarActivity.kt") and
        "AdhkarPeriod.EVENING" in text("app/src/main/java/com/quranunlock/guard/AdhkarActivity.kt"),
        "morning/evening Adhkar regression")

if errors:
    raise SystemExit("0.10.7 CONTRADICTORY RELEASE GATE FAILURE:\n- " + "\n- ".join(errors))

print("0.10.7 contradictory source gate: PASS")
print("- Accessibility is strict selected-targets + Safeguard only; no outside sentinel")
print("- target-owned window-removal proof cannot identify/observe destination apps")
print("- Tafsir links have canonical parsing, forgiving inline hit resolution and 48dp controls")
print("- free reader has system insets, retractable secondary controls and honest local brightness")
print("- 60s reading / 15m / 90m / bookmarks / Adhkar invariants preserved")
