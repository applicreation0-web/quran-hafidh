#!/usr/bin/env bash
# Single-shell command for reactivecircus/android-emulator-runner.
# Its multiline script input is executed ONE LINE PER shell command; a
# multiline for/if block there breaks with "expecting done".
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <wm-size> <wm-density> <surface>" >&2
  exit 2
fi
size="$1"
density="$2"
surface="$3"
case "$surface" in phone|boox-portrait-emulation) ;; *) echo "Invalid surface" >&2; exit 2;; esac
if [[ ! "$size" =~ ^[0-9]+x[0-9]+$ || ! "$density" =~ ^[0-9]+$ ]]; then
  echo "Invalid display dimensions" >&2
  exit 2
fi

adb shell wm size "$size"
adb shell wm density "$density"
mkdir -p hifz-app/build/android-proof

# Reproduce the diagnosed home lifecycle boundary before the full matrix.
# Each old failing class is repeated with a fresh process and preferences.
regressions=com.quransafeguard.hifz.preview.HomeLoaderLifecycleInstrumentedTest
for repetition in 1 2 3; do
  regressions+=,com.quransafeguard.hifz.preview.PostFinalRecallInstrumentedTest
  regressions+=,com.quransafeguard.hifz.preview.ItqanCarryoverNavigation11711InstrumentedTest
done
bash scripts/run_11711_hifz_parcours_isolated.sh "$regressions" "$surface"

# Collect evidence and audit Hifz even when the correspondence test fails.
test_status=0
if gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.CorrespondenceContextInstrumentedTest; then
  echo "CORRESPONDENCES_ANDROID_CATALOGUE_TESTS_PASS: $surface"
else
  test_status=$?
  echo "CORRESPONDENCES_ANDROID_TEST_FAILURE: $surface exit=$test_status" >&2
fi

capture_status=0
for capture in progression repetition opposition-85 crossed-93 agrandissement lecture-reperes lecture-deux-familles similitudes tafsir retour-lecture; do
  if ! adb pull "/data/local/tmp/haafidh-$capture.png" hifz-app/build/android-proof/ >/dev/null 2>&1; then
    echo "NO_VERIFIED_CAPTURE: $capture"
    capture_status=1
  fi
done

# Reuse PR8's disposable, class-isolated BOOX/phone non-regression suite.
# The list comes from the established 1.17.11 workflow, not a new guess.
classes="$(awk -v surface="$surface" '
  $1=="-" && $2=="surface:" { active=($3==surface) }
  active && $1=="classes:" {print $2; exit}
' .github/workflows/haafidh-11711-hifz-parcours-exhaustive.yml | tr -d "'")"
if [[ -z "$classes" || "$classes" != com.quransafeguard.hifz.preview.* ]]; then
  echo "Cannot resolve established Hifz classes for $surface" >&2
  exit 3
fi
adb logcat -d -s HaafidhGutterTouch > hifz-app/build/android-proof/gutter-touch.log || true
# The two process-death fixtures need separate instrumentations and a real
# external force-stop. Running the whole class provides no such boundary.
classes="$(printf '%s\n' "$classes" | tr ',' '\n' |
  sed '/\.ProcessDeathPersistenceInstrumentedTest$/d; /\.SundayFinalProcessDeathInstrumentedTest$/d' |
  paste -sd, -)"
audit_status=0
bash scripts/run_11711_hifz_parcours_isolated.sh "$classes" "$surface" || audit_status=$?
death_status=0
bash scripts/run_1175_process_death_audit.sh --active-replay || death_status=$?
if (( test_status != 0 )); then exit "$test_status"; fi
if (( audit_status != 0 )); then exit "$audit_status"; fi
if (( death_status != 0 )); then exit "$death_status"; fi
exit "$capture_status"
