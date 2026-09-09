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
eink_operational = text("app/src/androidTest/java/com/quranunlock/guard/EInkOperationalRuntimeTest.kt")
eink_adversarial = text("app/src/androidTest/java/com/quranunlock/guard/EInkAdversarialRuntimeTest.kt")
challenge_runtime = text("app/src/androidTest/java/com/quranunlock/guard/ChallengeDisplayRuntimeTest.kt")
reminder_runtime = text("app/src/androidTest/java/com/quranunlock/guard/ReminderRuntimeContractTest.kt")
reader_sentinel = text("app/src/androidTest/java/com/quranunlock/guard/ReaderRuntimeSentinelAuditTest.kt")
audio_runtime = text("app/src/androidTest/java/com/quranunlock/guard/QuranAudioRealRuntimeTest.kt")
ui_scale_runtime = text("app/src/androidTest/java/com/quranunlock/guard/SettingsUiScaleRuntimeTest.kt")

# Reader boot/rendition must be a runtime proof, not an asset-presence assertion.
require('runtime_guard.js' in index and index.index('runtime_guard.js') < index.index('reader.js'),
        "runtime guard must load before reader.js")
require('body:not(.runtime-ready) #mushaf' in index,
        "Mushaf must remain hidden until the runtime-ready proof")
require('id="readerRetry"' in index and 'Réessayer' in index,
        "reader must expose an explicit retry control")
for token in (
    'strictPage', 'getBoundingClientRect', 'getBBox', 'runtime-ready',
    'MutationObserver', 'unhandledrejection', "classList.remove('booting'",
    "querySelectorAll('path,text,use,polygon,polyline,line,circle,ellipse,rect')"
):
    require(token in guard, f"runtime guard missing proof/recovery token: {token}")
require('Reader109StateSanitizer.migrateOnReaderEntry(this)' in free_reader,
        "0.10.9 reader state sanitizer is not wired before reader boot")

# Runtime sentinel coverage must survive future workflow refactors.
for page in ('1', '2', '304', '305', '499', '604'):
    require(page in reader_sentinel, f"reader runtime sentinel audit lost page {page}")
for token in ('getBoundingClientRect', 'runtimeReady', 'nodes', 'booting'):
    require(token in reader_sentinel, f"reader runtime proof lost {token}")

# Timed Safeguard reader: load completion alone must not start the 60 s counter.
require('MushafRuntimeVisibilityPolicy.probeJavascript()' in timed_reader,
        "timed reader lacks DOM visibility probe")
require('.probeResultIsReady(result)' in timed_reader,
        "timed reader does not gate readiness on exact DOM probe result")
require('takeIf(MushafRuntimeVisibilityPolicy::sourceLooksRenderable)' in timed_reader,
        "timed reader does not reject empty/non-substantive SVG source")
require('Text("Réessayer")' in timed_reader,
        "timed reader failure path lacks Retry")
probe_pos = timed_reader.find('MushafRuntimeVisibilityPolicy.probeJavascript()')
onready_pos = timed_reader.find('currentOnReady.value()', probe_pos)
require(probe_pos >= 0 and onready_pos > probe_pos,
        "timed reader onReady must occur only after the visibility probe")
for token in ('getBoundingClientRect', 'getBBox', 'visibility', 'opacity', 'count >= 8'):
    require(token in visibility, f"timed reader visibility policy missing {token}")
for token in (
    'DisplayProfilePreference.STANDARD', 'DisplayProfilePreference.EINK',
    'MushafRuntimeVisibilityPolicy.probeJavascript()', 'Lifecycle.State.CREATED',
    'Lifecycle.State.RESUMED', 'readingElapsedMs', 'Tafs', 'Audio Al-Husary'
):
    require(token in challenge_runtime, f"Challenge Standard/E-Ink runtime audit missing {token}")

# Memorization / Tafsir boundary and canonical Quran references.
require("$('tafsir').hidden=!initial.plus||memory" in reader,
        "reader must hide Tafsir in Memorization and all Light journeys")
compact_free_reader = re.sub(r"\s+", "", free_reader)
require('!memoryMode&&TafsirEdition.isEnabled&&QuranCanonicalBounds.isValid(ref)' in compact_free_reader,
        "native Tafsir bridge must refuse Memorization and non-canonical verses")
for token in ('SURAH_COUNT = 114', 'MUSHAF_PAGE_COUNT = 604', 'TOTAL_VERSES = 6236', 'fun isValid'):
    require(token in canonical_bounds, f"canonical Quran bounds missing {token}")
require('QsgNative.tafsir(1,8)' in eink_operational,
        "Plus E-Ink adversarial audit must attack an impossible Tafsir reference")

# Private audio contract: no audio is bundled in the APK. Safeguard downloads
# original Al-Husary Muallim ayah files only on explicit user action, stores
# them privately, and never makes audio a mandatory memorization/Hifz dependency.
audio_path = ROOT / "app/src/main/assets/reader109/audio.json"
try:
    audio = json.loads(audio_path.read_text(encoding="utf-8"))
except Exception as exc:
    errors.append(f"invalid audio.json: {exc}")
    audio = {}
require(audio.get('bundledInApk') is False,
        "audio files must never be bundled in the APK")
require(audio.get('redistributionApproved') is False,
        "local-download mode must not falsely claim redistribution rights")
require(audio.get('delivery') == 'user-initiated-local-download',
        "audio delivery must be explicit user-initiated local download")
require(audio.get('baseUrl') == 'https://everyayah.com/data/Husary_Muallim_128kbps',
        "Al-Husary Muallim source path changed unexpectedly")
require('android.permission.INTERNET' in manifest,
        "explicit HTTPS audio download requires the normal INTERNET permission")
for token in ('Husary_Muallim_128kbps', 'isAllowedHttpsUrl', 'fileName(', 'STORAGE_VERSION',
              'QuranCanonicalBounds.ayahCount', 'QuranCanonicalBounds.isValid'):
    require(token in audio_source, f"audio source policy missing {token}")
for token in (
    'MediaPlayer', 'downloadVerse(', 'downloadSurah(', 'deleteSurah(',
    'isDownloaded(', '.part', 'looksLikeMp3', 'QuranAudioSource.url'
):
    require(token in audio_controller, f"local audio controller missing {token}")
for token in (
    'downloadVerse(surah:Int,ayah:Int)', 'downloadSurah(surah:Int,ayahCount:Int)',
    'deleteSurah(surah:Int,ayahCount:Int)', 'isDownloaded(surah:Int,ayah:Int)'
):
    require(token in compact_free_reader, f"reader bridge missing audio API {token}")
require('Audio Al-Husary Muʿallim' in reader and 'Télécharger la sourate' in reader,
        "reader must expose in-app audio download management")
require("classList.toggle('audio',playing&&p.dataset.verse===e.surah+':'+e.ayah)" in reader,
        "audio highlighting must stay at whole-verse polygon level")
require('localAudioReady' in reader and 'P.disableAudio(s)' in reader,
        "missing local audio must not block Memorization")
require('function disableAudio(s)' in protocol,
        "pedagogical protocol lacks non-blocking audio downgrade")
require('audioAvailable: Boolean = false' in hifz_training,
        "structured Hifz must remain usable without audio/network")
for token in ('download', 'MediaPlayer', 'playing', 'delete'):
    require(token.lower() in audio_runtime.lower(), f"real audio runtime proof missing {token}")

# Hifz and free memorization persistence must remain separate.
for name in ('FREE_READER_MEMORIZATION', 'FREE_READER_LAST_PAGE', 'HIFZ'):
    require(name in namespaces, f"missing persistence namespace {name}")
require('check(HIFZ != FREE_READER_MEMORIZATION)' in namespaces,
        "Hifz must never share free-memorization persistence")

# Hifz weekly contract must remain present; behavioural unit tests enforce transitions.
for day_kind in ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'):
    require(day_kind in schedule, f"Hifz schedule missing {day_kind}")
for track in ('SABQI', 'ITQAN', 'MURAJAAH'):
    require(track in schedule.upper(), f"Hifz schedule missing {track}")

# E-Ink is audited twice with different methodologies: normal activity and hostile boundaries.
for token in (
    'DisplayProfilePreference.EINK', 'KEYCODE_PAGE_UP', 'KEYCODE_PAGE_DOWN',
    "document.getElementById('bookmark').click()", "document.getElementById('plus').click()",
    'Mémorisation', 'Parcours Hifz', 'TafsirEdition.isEnabled'
):
    require(token in eink_operational, f"E-Ink operational audit missing {token}")
for token in ('1, 2, 304, 305, 499, 604', 'KEYCODE_PAGE_UP', 'KEYCODE_PAGE_DOWN',
              'for (page in 2..26)', 'runtimeReady', 'einkCleanerRuns', 'booting'):
    require(token in eink_adversarial, f"E-Ink adversarial audit missing {token}")

# Protection boundary: preserve the narrow target-only methodology.
compact_protected = re.sub(r"\s+", "", protected)
require('enumclassSafeguardTargetCategory{SOCIAL,BROWSER}' in compact_protected,
        "target categories must remain SOCIAL/BROWSER only")
require('selectableTargets:List<SafeguardTarget>=socialTargets+browserTargets' in compact_protected,
        "selectable target boundary changed")
require('isSelectableTarget(packageName)' in protected,
        "protection must remain gated by the explicit target list")
require('packageNameinGuardPrefs.protectedPackages(context)' in compact_protected,
        "protection must remain gated by the user's selected targets")
require('SENSITIVE' not in protected and 'BANKING' not in protected,
        "do not add a separate sensitive/banking target category")

# Uninstall must remain free: no device-admin/owner mechanism is allowed.
for forbidden in ('BIND_DEVICE_ADMIN', 'DEVICE_ADMIN_ENABLED', 'DeviceAdminReceiver', 'setLockTaskPackages'):
    require(forbidden not in manifest, f"free uninstall boundary violated by {forbidden}")
require('android.permission.CALL_PHONE' in manifest and 'tools:node="remove"' in manifest,
        "direct call permission must remain explicitly removed")

# Reminder/Adhkar delivery contract.
require('atTime(ThoughtOfDayPolicy.REMINDER_HOUR, 0)' in reminders,
        "daily reminder must be scheduled at the configured local hour")
require('times.fajr' in reminders and 'times.sunrise' in reminders,
        "morning Adhkar window must remain Fajr to sunrise")
require('times.asr' in reminders and 'times.maghrib' in reminders,
        "evening Adhkar window must remain Asr to Maghrib")
require('setSound(null' in reminders and 'longArrayOf(0L, 55L)' in reminders,
        "reminders must remain silent with one gentle vibration")
for token in ('mindful_reminders_banner_v2', 'assertNull(channel.sound)',
              'longArrayOf(0L, 55L)', 'adhkarTransliterationEnabled'):
    require(token in reminder_runtime, f"reminder runtime proof missing {token}")

# UI scale proof must not disappear.
for token in ('1.0f', '1.3f', '1.5f'):
    require(token in ui_scale_runtime, f"UI scale runtime audit missing {token}")

# Visual identity requested for 10.10.
for token in ('0xFF1D5B47', '0xFFB48A3C', '0xFF76563C', 'SafeguardShapes'):
    require(token in design, f"Safeguard visual identity missing {token}")

# If corpus has been restored in CI, require all sentinel assets and exactly 604 pages.
corpus = ROOT / "app/src/main/assets/mushaf/hafs/kfqc/svg-br"
if corpus.exists():
    pages = sorted(corpus.glob('*.svg.br'))
    require(len(pages) == 604, f"restored Mushaf corpus must contain 604 pages, got {len(pages)}")
    for n in (1, 2, 304, 305, 499, 604):
        require((corpus / f"{n:03d}.svg.br").is_file(), f"sentinel Mushaf page missing: {n}")

# Light source set must not contain Tafsir assets. Plus is allowed/expected to.
light_tafsir = ROOT / "app/src/light/assets/tafsir"
require(not light_tafsir.exists() or not any(light_tafsir.rglob('*')),
        "Light source set leaks Tafsir assets")

if errors:
    print("0.10.10 RELEASE CONTRACT: FAIL")
    for item in errors:
        print(f" - {item}")
    sys.exit(1)

print("0.10.10 RELEASE CONTRACT: PASS")
print("Runtime visibility, Challenge Standard/E-Ink, migration, Hifz/audio non-blocking, canonical Tafsir/audio bounds, dual E-Ink runtime coverage, quiet reminders, target-only protection, free uninstall, UI scale, visual identity and edition boundaries verified.")
