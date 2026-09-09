#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []


def text(path: str) -> str:
    p = ROOT / path
    if not p.exists():
        errors.append(f"missing required file: {path}")
        return ""
    return p.read_text(encoding="utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def require_tokens(blob: str, tokens: tuple[str, ...], area: str) -> None:
    for token in tokens:
        require(token in blob, f"{area} missing {token}")


index = text("app/src/main/assets/reader109/index.html")
guard = text("app/src/main/assets/reader109/runtime_guard.js")
reader = text("app/src/main/assets/reader109/reader.js")
protocol = text("app/src/main/assets/reader109/protocol.js")
free_reader = text("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
timed_reader = text("app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt")
visibility = text("app/src/main/java/com/quranunlock/guard/MushafRuntimeVisibilityPolicy.kt")
namespaces = text("app/src/main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt")
protected = text("app/src/main/java/com/quranunlock/guard/ProtectedApps.kt")
schedule = text("app/src/main/java/com/quranunlock/guard/HifzSchedulePolicy.kt")
hifz_training = text("app/src/main/java/com/quranunlock/guard/HifzTrainingPolicy.kt")
canonical_bounds = text("app/src/main/java/com/quranunlock/guard/HifzCursor.kt")
audio_controller = text("app/src/main/java/com/quranunlock/guard/QuranAudioController.kt")
audio_source = text("app/src/main/java/com/quranunlock/guard/QuranAudioSource.kt")
reminders = text("app/src/main/java/com/quranunlock/guard/MindfulReminderScheduler.kt")
manifest = text("app/src/main/AndroidManifest.xml")
design = text("app/src/main/java/com/quranunlock/guard/SafeguardDesign.kt")
reader_sentinel = text("app/src/androidTest/java/com/quranunlock/guard/ReaderRuntimeSentinelAuditTest.kt")
eink_operational = text("app/src/androidTest/java/com/quranunlock/guard/EInkOperationalRuntimeTest.kt")
eink_adversarial = text("app/src/androidTest/java/com/quranunlock/guard/EInkAdversarialRuntimeTest.kt")
challenge_runtime = text("app/src/androidTest/java/com/quranunlock/guard/ChallengeDisplayRuntimeTest.kt")
reminder_runtime = text("app/src/androidTest/java/com/quranunlock/guard/ReminderRuntimeContractTest.kt")
audio_runtime = text("app/src/androidTest/java/com/quranunlock/guard/QuranAudioRuntimeTest.kt")
ui_scale_runtime = text("app/src/androidTest/java/com/quranunlock/guard/SettingsUiScaleRuntimeTest.kt")

# Mushaf runtime proof and recovery.
require('runtime_guard.js' in index and index.index('runtime_guard.js') < index.index('reader.js'),
        "runtime guard must load before reader.js")
require('body:not(.runtime-ready) #mushaf' in index, "Mushaf must stay hidden until runtime-ready")
require('id="readerRetry"' in index and 'Réessayer' in index, "reader retry control missing")
require_tokens(guard, (
    'strictPage', 'getBoundingClientRect', 'getBBox', 'runtime-ready', 'MutationObserver',
    'unhandledrejection', "classList.remove('booting'",
    "querySelectorAll('path,text,use,polygon,polyline,line,circle,ellipse,rect')"
), "reader runtime guard")
require('Reader109StateSanitizer.migrateOnReaderEntry(this)' in free_reader,
        "0.10.9 reader state sanitizer is not wired before reader boot")
require_tokens(reader_sentinel, ('1', '2', '304', '305', '499', '604', 'runtimeReady', 'booting'),
               "reader sentinel runtime audit")

# Challenge: real SVG readiness and active-foreground time only, Standard + E-Ink.
require_tokens(timed_reader, (
    'MushafRuntimeVisibilityPolicy.probeJavascript()', '.probeResultIsReady(result)',
    'takeIf(MushafRuntimeVisibilityPolicy::sourceLooksRenderable)', 'Text("Réessayer")'
), "Challenge reader")
probe_pos = timed_reader.find('MushafRuntimeVisibilityPolicy.probeJavascript()')
onready_pos = timed_reader.find('currentOnReady.value()', probe_pos)
require(probe_pos >= 0 and onready_pos > probe_pos, "Challenge timer must start only after DOM visibility proof")
require_tokens(visibility, ('getBoundingClientRect', 'getBBox', 'visibility', 'opacity', 'count >= 8'),
               "Challenge visibility policy")
require_tokens(challenge_runtime, (
    'DisplayProfilePreference.STANDARD', 'DisplayProfilePreference.EINK',
    'MushafRuntimeVisibilityPolicy.probeJavascript()', 'Lifecycle.State.CREATED',
    'Lifecycle.State.RESUMED', 'readingElapsedMs', 'Tafs', 'Audio Al-Husary'
), "Challenge Standard/E-Ink runtime audit")

# Tafsir/Memory/Light boundaries and one canonical Quran authority.
require("$('tafsir').hidden=!initial.plus||memory" in reader,
        "Tafsir must be hidden in Memorization and Light")
compact_free_reader = re.sub(r"\s+", "", free_reader)
require('!memoryMode&&TafsirEdition.isEnabled&&QuranCanonicalBounds.isValid(ref)' in compact_free_reader,
        "native Tafsir bridge must reject Memorization and noncanonical references")
require_tokens(canonical_bounds, ('SURAH_COUNT = 114', 'MUSHAF_PAGE_COUNT = 604', 'TOTAL_VERSES = 6236', 'fun isValid'),
               "canonical Quran bounds")
require('QsgNative.tafsir(1,8)' in eink_operational,
        "Plus E-Ink audit must attack impossible Tafsir reference 1:8")

# Private local Al-Husary audio; never bundled and never mandatory for Hifz/Memory.
audio_path = ROOT / "app/src/main/assets/reader109/audio.json"
try:
    audio = json.loads(audio_path.read_text(encoding="utf-8"))
except Exception as exc:
    errors.append(f"invalid audio.json: {exc}")
    audio = {}
require(audio.get('bundledInApk') is False, "audio must not be bundled in APK")
require(audio.get('redistributionApproved') is False, "must not claim audio redistribution rights")
require(audio.get('delivery') == 'user-initiated-local-download', "audio must be explicit local download")
require(audio.get('baseUrl') == 'https://everyayah.com/data/Husary_Muallim_128kbps', "audio source drifted")
require_tokens(audio_source, (
    'Husary_Muallim_128kbps', 'isAllowedHttpsUrl', 'fileName(', 'STORAGE_VERSION',
    'QuranCanonicalBounds.ayahCount', 'QuranCanonicalBounds.isValid'
), "audio source policy")
require_tokens(audio_controller, (
    'MediaPlayer', 'downloadVerse(', 'downloadSurah(', 'deleteSurah(', 'isDownloaded(',
    '.part', 'looksLikeMp3', 'QuranAudioSource.url'
), "audio controller")
require('localAudioReady' in reader and 'P.disableAudio(s)' in reader and 'function disableAudio(s)' in protocol,
        "missing audio must downgrade, never block")
require('audioAvailable: Boolean = false' in hifz_training,
        "structured Hifz default must remain audio-independent")
require_tokens(audio_runtime, ('downloadVerse(1, 1)', 'isDownloaded(1, 1)', 'playVerse(1, 1, 1)', 'MediaPlayer'),
               "real audio runtime proof")

# Hifz isolation and fixed weekly cadence.
require_tokens(namespaces, ('FREE_READER_MEMORIZATION', 'FREE_READER_LAST_PAGE', 'HIFZ'), "persistence namespaces")
require('check(HIFZ != FREE_READER_MEMORIZATION)' in namespaces, "Hifz must not share free-memory state")
require_tokens(schedule, ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'),
               "Hifz weekly schedule")
require_tokens(schedule.upper(), ('SABQI', 'ITQAN', 'MURAJAAH'), "Hifz tracks")

# E-Ink: independent operational and adversarial evidence.
require_tokens(eink_operational, (
    'DisplayProfilePreference.EINK', 'KEYCODE_PAGE_UP', 'KEYCODE_PAGE_DOWN',
    "document.getElementById('bookmark').click()", "document.getElementById('plus').click()",
    'Mémorisation', 'Parcours Hifz', 'TafsirEdition.isEnabled'
), "E-Ink operational audit")
require_tokens(eink_adversarial, (
    '1, 2, 304, 305, 499, 604', 'KEYCODE_PAGE_UP', 'KEYCODE_PAGE_DOWN',
    'for (page in 2..26)', 'runtimeReady', 'einkCleanerRuns', 'booting'
), "E-Ink adversarial audit")

# Narrow target-only protection; banks/security/identity are never target categories.
compact_protected = re.sub(r"\s+", "", protected)
require('enumclassSafeguardTargetCategory{SOCIAL,BROWSER}' in compact_protected,
        "target categories must remain SOCIAL/BROWSER only")
require('selectableTargets:List<SafeguardTarget>=socialTargets+browserTargets' in compact_protected,
        "selectable target boundary changed")
require('isSelectableTarget(packageName)' in protected, "protection must use explicit target list")
require('packageNameinGuardPrefs.protectedPackages(context)' in compact_protected,
        "protection must use user-selected targets")
require('SENSITIVE' not in protected and 'BANKING' not in protected,
        "sensitive/banking category must never be introduced")

# Free uninstall and calls boundary.
for forbidden in ('BIND_DEVICE_ADMIN', 'DEVICE_ADMIN_ENABLED', 'DeviceAdminReceiver', 'setLockTaskPackages'):
    require(forbidden not in manifest, f"free uninstall violated by {forbidden}")
require('android.permission.CALL_PHONE' in manifest and 'tools:node="remove"' in manifest,
        "CALL_PHONE must remain explicitly removed")

# Reminder/Adhkar contract.
require('atTime(ThoughtOfDayPolicy.REMINDER_HOUR, 0)' in reminders, "20:00 scheduler hook missing")
require('times.fajr' in reminders and 'times.sunrise' in reminders, "morning Adhkar window changed")
require('times.asr' in reminders and 'times.maghrib' in reminders, "evening Adhkar window changed")
require('setSound(null' in reminders and 'longArrayOf(0L, 55L)' in reminders,
        "quiet notification contract changed")
require_tokens(reminder_runtime, (
    'mindful_reminders_banner_v2', 'assertNull(channel.sound)', 'longArrayOf(0L, 55L)',
    'adhkarTransliterationEnabled'
), "reminder runtime audit")

# UI and visual identity.
require_tokens(ui_scale_runtime, ('"1.0"', '"1.3"', '"1.5"', 'DashboardActivity', 'HifzJourneyActivity'),
               "UI scale runtime audit")
require_tokens(design, ('0xFF1D5B47', '0xFFB48A3C', '0xFF76563C', 'SafeguardShapes'), "visual identity")

# Immutable Mushaf and Light/Plus binary boundary.
corpus = ROOT / "app/src/main/assets/mushaf/hafs/kfqc/svg-br"
if corpus.exists():
    pages = sorted(corpus.glob('*.svg.br'))
    require(len(pages) == 604, f"Mushaf corpus must contain 604 pages, got {len(pages)}")
    for n in (1, 2, 304, 305, 499, 604):
        require((corpus / f"{n:03d}.svg.br").is_file(), f"sentinel Mushaf page missing: {n}")
light_tafsir = ROOT / "app/src/light/assets/tafsir"
require(not light_tafsir.exists() or not any(light_tafsir.rglob('*')), "Light source set leaks Tafsir assets")

if errors:
    print("0.10.10 RELEASE CONTRACT: FAIL")
    for item in errors:
        print(f" - {item}")
    sys.exit(1)

print("0.10.10 RELEASE CONTRACT: PASS")
print("All release-critical surfaces are bound to explicit static/runtime evidence, including dual E-Ink and Challenge Standard/E-Ink coverage.")
