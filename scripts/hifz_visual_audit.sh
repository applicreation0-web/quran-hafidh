#!/usr/bin/env bash
# Visual audit of Quran Haafidh (debug APK) on a real Android emulator: walks the UI by its own
# labels/content descriptions and captures every screen at a phone and a BOOX-like geometry.
set -uo pipefail

apk="${1:?APK path}"
out="${2:?output dir}"
pkg="com.quransafeguard.hifz"
main="${pkg}/com.quransafeguard.hifz.preview.MainActivity"
mkdir -p "$out"
log="$out/audit.log"
: > "$log"

note() { echo "$*" | tee -a "$log"; }

dump_ui() {
  timeout 20s adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || return 1
  adb shell cat /sdcard/ui.xml > /tmp/ui.xml 2>/dev/null
}

# find <mode> <value>: mode = text | desc | descprefix | textprefix | switchdesc; prints "x y".
find_node() {
  dump_ui || return 1
  python3 - "$1" "$2" <<'PY'
import re, sys, html
mode, value = sys.argv[1], sys.argv[2]
xml = open('/tmp/ui.xml', encoding='utf-8', errors='replace').read()
for node in re.finditer(r'<node [^>]*>', xml):
    n = node.group(0)
    def attr(k):
        m = re.search(k + r'="([^"]*)"', n)
        return html.unescape(m.group(1)) if m else ''
    text, desc, cls = attr('text'), attr('content-desc'), attr('class')
    ok = (mode == 'text' and text == value) or (mode == 'desc' and desc == value) \
        or (mode == 'descprefix' and desc.startswith(value)) or (mode == 'textprefix' and text.startswith(value)) \
        or (mode == 'switchdesc' and desc == value and cls.endswith('Switch'))
    if ok:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            x0, y0, x1, y1 = map(int, b.groups())
            print((x0 + x1) // 2, (y0 + y1) // 2)
            sys.exit(0)
sys.exit(1)
PY
}

tap() { # tap <mode> <value>
  local xy
  if xy="$(find_node "$1" "$2")"; then
    adb shell input tap $xy; sleep "${3:-3}"; return 0
  fi
  note "  ! not found: $1 '$2'"; return 1
}

shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$out/$profile-$1.png"; note "  shot $profile-$1"; }
back() { adb shell input keyevent 4; sleep "${1:-2}"; }

dismiss_anr() {
  for _ in 1 2 3; do
    dump_ui || { sleep 3; continue; }
    if grep -Eq 'aerr_wait|isn.t responding' /tmp/ui.xml; then
      xy="$(find_node text 'Wait')" && adb shell input tap $xy
      sleep 3
    else
      return 0
    fi
  done
}

launch_home() {
  adb shell am force-stop "$pkg"; sleep 1
  adb shell am start -W -n "$main" >/dev/null; sleep 6
  dismiss_anr
}

set_lecture_page() {
  adb shell am force-stop "$pkg"
  printf '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>\n<map>\n    <int name="page" value="%s" />\n</map>\n' "$1" > /tmp/hifz_study.xml
  adb push /tmp/hifz_study.xml /data/local/tmp/hifz_study.xml >/dev/null
  adb shell chmod 644 /data/local/tmp/hifz_study.xml
  adb shell run-as "$pkg" mkdir -p shared_prefs
  adb shell run-as "$pkg" cp /data/local/tmp/hifz_study.xml shared_prefs/hifz_study.xml \
    || note "  ! could not preset Lecture page $1"
}

resumed() { adb shell dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity' | tr -d '\r'; }

# Opens a Parcours line; screenshots the session and comes back only if one really opened
# (Apprentissage/Stabilisation are greyed out on the other cadence days).
open_mode() {
  launch_home; tap text "Parcours Hifz" || return
  tap text "$1" 6 || return
  if resumed | grep -q HifzSessionActivity; then shot "session-$2"; else note "  - $1 not open today (cadence)"; shot "parcours-$2-closed"; fi
}

tap_mushaf_center() {
  local size w h
  size="$(adb shell wm size | tail -n1 | grep -oE '[0-9]+x[0-9]+')"
  w="${size%x*}"; h="${size#*x}"
  adb shell input tap $((w / 2)) $((h * 45 / 100)); sleep 3
}

audit_profile() {
  profile="$1"
  note "== profile $profile ($(adb shell wm size | tail -n1 | tr -d '\r'), $(adb shell wm density | tail -n1 | tr -d '\r'))"

  if [[ "$profile" == boox ]]; then
    launch_home
    tap desc "Paramètres" && tap text "Audio & affichage" && {
      tap switchdesc "Optimisation E‑Ink / BOOX" 2 || true
      shot settings-audio-eink
    }
  fi

  launch_home; shot home
  tap text "Parcours Hifz" && shot parcours
  open_mode Apprentissage apprentissage
  open_mode Stabilisation stabilisation
  open_mode Renforcement renforcement
  open_mode Consolidation consolidation
  launch_home; tap text "Parcours Hifz" && tap text "Révision" && {
    shot revision-selector
    tap textprefix "Révision active" 6 && shot session-revision-active
  }
  launch_home; tap text "Parcours Hifz" && tap text "Révision" && tap textprefix "Entretien" 6 && shot session-revision-passive

  set_lecture_page 106
  launch_home
  tap text "Lecture" 6 && {
    find_node desc "Annoter" >/dev/null || tap_mushaf_center
    shot lecture-106-half-rub
    tap desc "Annoter" && shot lecture-pen-open && tap descprefix "Désactiver le crayon"
    tap_mushaf_center
    tap descprefix "Tafsir" 5 && shot lecture-tafsir && back
  }
  set_lecture_page 515
  launch_home
  tap text "Lecture" 6 && shot lecture-515-hujurat

  launch_home
  tap text "Mémorisation libre" 6 && {
    shot freemem-empty
    tap_mushaf_center
    tap desc "Masque 50 %" && tap desc "Ajouter une répétition" && shot freemem-50
  }

  launch_home
  tap text "Quiz" 4 && {
    shot quiz-setup
    tap desc "Commencer" 6 && {
      shot quiz-question
      tap desc "Vérifier" 4 && shot quiz-correction
      tap desc "À revoir" 4
      for _ in $(seq 2 10); do tap desc "Vérifier" 3 && tap desc "À revoir" 3 || true; done
      shot quiz-summary
    }
  }

  launch_home
  tap text "Progression" 6 && shot progression

  launch_home
  tap desc "Paramètres" && {
    shot settings-root
    for rubric in "Parcours" "Plages & corpus" "Révision" "Audio & affichage" "Sauvegarde" "Avancé"; do
      tap text "$rubric" && { shot "settings-${rubric// /_}"; back; }
    done
    tap text "Avancé" && tap text "À propos du parcours" && shot settings-about && back
    tap text "Repères faibles" 4 && shot weak-verses && back
  }
}

adb wait-for-device
for _ in $(seq 1 90); do [[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == 1 ]] && break; sleep 2; done
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r -g "$apk" | tee -a "$log"

adb shell wm size 1080x2340; adb shell wm density 440
audit_profile phone
adb shell wm size 1404x1872; adb shell wm density 300
audit_profile boox
adb shell wm size reset; adb shell wm density reset
adb logcat -d -s AndroidRuntime:E QuranHifz:E chromium:E > "$out/logcat-errors.txt" || true
note "done: $(ls "$out"/*.png 2>/dev/null | wc -l) screenshots"
