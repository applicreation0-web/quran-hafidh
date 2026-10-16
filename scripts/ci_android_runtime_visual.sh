#!/usr/bin/env bash
# Quran Haafidh 1.17.1: fail-closed real Android Hifz/Amorces/7-day UI audit.
# Compiles app + test apk for emulator ONLY. No user data or release publishing.
set -euo pipefail

gradle --no-daemon :hifz-app:assembleDebug :hifz-app:assembleDebugAndroidTest --stacktrace

mapfile -t debug_apks < <(find hifz-app/build/outputs/apk/debug -type f -name '*.apk' | sort)
mapfile -t test_apks < <(find hifz-app/build/outputs/apk/androidTest/debug -type f -name '*.apk' | sort)
test "${#debug_apks[@]}" -eq 1
test "${#test_apks[@]}" -eq 1
adb install -r "${debug_apks[0]}"
adb install -r "${test_apks[0]}"

set +e
adb shell am instrument -w -r \
  'com.quransafeguard.hifz.ibnkathirtest.test/androidx.test.runner.AndroidJUnitRunner' \
  > audit-runtime-instrumentation.txt 2>&1
shell_status=$?
set -e
cat audit-runtime-instrumentation.txt
echo "ADB instrumentation shell status: $shell_status"

# Preserve actual pixels even if an instrumented test fails. The old fail-fast
# grep discarded the very screenshots needed to diagnose a blank Quiz.
mkdir -p audit-runtime-screens
device_dir=/sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files
adb pull "$device_dir/." audit-runtime-screens/ || true

# AndroidJUnitRunner is authoritative: the complete checked-in suite has 99 tests.
# A bare Android shell exit code is not sufficient to prove test success.
grep -Fxq 'OK (99 tests)' audit-runtime-instrumentation.txt
grep -Fxq 'INSTRUMENTATION_CODE: -1' audit-runtime-instrumentation.txt
test "$(grep -Fc 'INSTRUMENTATION_STATUS_CODE: 0' audit-runtime-instrumentation.txt)" -eq 99
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|FATAL EXCEPTION|INSTRUMENTATION_STATUS_CODE: -[0-9]+' audit-runtime-instrumentation.txt; then
  echo "::error::Android instrumented tests contain a failure; no debug allowed"
  exit 1
fi

# A same-process pair of JUnit methods is not process-death evidence. Prepare
# committed Itqan, Sabqi and active-review state in one instrumentation process,
# launch the real activity, force-stop the target package from adb, then verify
# the exact values in a fresh instrumentation process.
runner='com.quransafeguard.hifz.ibnkathirtest.test/androidx.test.runner.AndroidJUnitRunner'
process_death_class='com.quransafeguard.hifz.preview.ProcessDeathPersistenceInstrumentedTest'
run_process_death_phase() {
  local method="$1"
  local output="$2"
  adb shell am instrument -w -r -e class "${process_death_class}#${method}" "$runner" \
    > "$output" 2>&1
  cat "$output"
  grep -Fxq 'OK (1 test)' "$output"
  grep -Fxq 'INSTRUMENTATION_CODE: -1' "$output"
  test "$(grep -Fc 'INSTRUMENTATION_STATUS_CODE: 0' "$output")" -eq 1
}
run_process_death_phase a_prepareFixtureForExternalForceStop audit-process-death-prepare.txt
# The prepare phase leaves an actually committed NEW main store and a durable
# rollback journal, with other stores still OLD. Kill the real app before
# Application.onCreate gets a chance to recover; only the fresh Android process
# is permitted to resolve the interrupted import.
adb shell am force-stop com.quransafeguard.hifz.ibnkathirtest
adb shell am start -W -n \
  'com.quransafeguard.hifz.ibnkathirtest/com.quransafeguard.hifz.preview.MainActivity' \
  > audit-process-death-launch.txt
grep -Eq 'Status: ok|Status: timeout' audit-process-death-launch.txt
adb shell am force-stop com.quransafeguard.hifz.ibnkathirtest
run_process_death_phase b_verifyFixtureAfterExternalForceStop audit-process-death-verify.txt

# Mandatory screenshots now target the 10-stage Mawdudi pilot, not retired 13-themes cards.
# Screen evidence is mandatory. Keep test APK installed until copying pixels.
mkdir -p audit-runtime-screens
device_dir=/sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files
adb shell ls -la "$device_dir"
adb pull "$device_dir/." audit-runtime-screens/
count="$(find audit-runtime-screens -type f -name 'audit-*.png' | wc -l | tr -d ' ')"
test "$count" -ge 21
find audit-runtime-screens -type f -name 'audit-*.png' -size 0 -print | grep -q . && {
  echo "::error::At least one required screenshot is empty"; exit 1;
} || true
# Independent visual screens are checked; no Quiz or numbered Carte screens.
# The public GitHub checkout has NO private Quran.com title sidecar.
# This job must prove all seven independent live behaviors and 14 available
# screens, but must NOT claim private title pixels were exercised.
for file in audit-lecture-mushaf.png \
            audit-accueil-sans-quiz.png audit-calendrier-sept-jours.png \
            audit-itqan-4dates.png \
            audit-cp03-carte-courte.png \
            audit-cp03-carte-multipage.png \
            audit-cp03-carte-lecture.png \
            audit-cp03-carte-retour.png \
            audit-mawdudi-baqara-10-continuous-stages.png \
            audit-mawdudi-baqara-compact-map-commentary-return.png \
            audit-cp04-memo-libre-avant.png \
            audit-cp04-memo-libre-apres.png; do
  test -s "audit-runtime-screens/$file"
done
private_dir=hifz-app/src/main/assets/ibn_kathir_qurancom_private
if [ -d "$private_dir" ]; then
  # Work's independent private-checkout test is stricter: actual title pixels.
  test -s audit-runtime-screens/audit-qurancom-qaf-private.png
  test -s audit-runtime-screens/audit-qurancom-baqarah-private.png
  if cmp -s audit-runtime-screens/audit-lecture-mushaf.png \
            audit-runtime-screens/audit-qurancom-qaf-private.png; then
    echo "::error::Private Quran.com heading created no visible UI transition"
    exit 1
  fi
  echo "PASS: 99 live Android tests + $count screenshots, including PRIVATE title pixels"
else
  echo "PASS: 99 live Android tests + external force-stop persistence + $count screenshots, PUBLIC no-title fail-closed only"
  echo "::notice::Quran.com English title pixels remain UNVERIFIED until private Work audit"
fi
