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
free_reader = text("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
timed_reader = text("app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt")
visibility = text("app/src/main/java/com/quranunlock/guard/MushafRuntimeVisibilityPolicy.kt")
namespaces = text("app/src/main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt")
protected = text("app/src/main/java/com/quranunlock/guard/ProtectedApps.kt")
schedule = text("app/src/main/java/com/quranunlock/guard/HifzSchedulePolicy.kt")

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

# Memorization / Tafsir boundary. Check both the JS presentation gate and the
# separate native bridge gate. The latter deliberately uses memoryMode, the
# Activity-owned state, rather than trusting a JavaScript argument.
require("$('tafsir').hidden=!initial.plus||memory" in reader,
        "reader must hide Tafsir in Memorization and all Light journeys")
require("if(!memoryMode&&TafsirEdition.isEnabled" in free_reader.replace(" ", ""),
        "native Tafsir bridge must refuse Memorization")

# Audio must stay honest while a real downloadable catalogue is not yet validated.
audio_path = ROOT / "app/src/main/assets/reader109/audio.json"
try:
    audio = json.loads(audio_path.read_text(encoding="utf-8"))
except Exception as exc:
    errors.append(f"invalid audio.json: {exc}")
    audio = {}
require(audio.get('redistributionApproved') is False,
        "audio redistribution must not be marked approved without evidence")
require(audio.get('productionHost', '') == '',
        "pending audio must not invent a production host")
require(audio.get('surahs', []) == [],
        "pending audio must not invent a 114-surah catalogue")
require('source de diffusion non encore validée' in reader,
        "pending audio state must be explicit in the user-facing reader")

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

# Protection boundary: preserve the proven 0.10.8 target-only methodology.
# Safeguard observes/protects only explicitly selectable social/browser targets;
# do not introduce a second classifier for banking/security/identity applications.
compact_protected = re.sub(r"\s+", "", protected)
require('enumclassSafeguardTargetCategory{SOCIAL,BROWSER}' in compact_protected,
        "0.10.8 target categories must remain SOCIAL/BROWSER only")
require('selectableTargets:List<SafeguardTarget>=socialTargets+browserTargets' in compact_protected,
        "0.10.8 selectable target boundary changed")
require('isSelectableTarget(packageName)' in protected,
        "protection must remain gated by the explicit target list")
require('packageNameinGuardPrefs.protectedPackages(context)' in compact_protected,
        "protection must remain gated by the user's selected targets")
require('SENSITIVE' not in protected and 'BANKING' not in protected,
        "do not add a separate sensitive/banking target category")

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
print("Runtime visibility, 10.9 migration, Hifz separation, audio honesty, 0.10.8 target-only protection and edition source boundaries verified.")
