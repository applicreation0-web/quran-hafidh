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
  -e class 'com.quransafeguard.hifz.preview.ReaderSevenDayRuntimeInstrumentedTest,com.quransafeguard.hifz.preview.HifzAndMapRuntimeSmokeInstrumentedTest' \
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

# AndroidJUnitRunner is authoritative: exactly SIX independent runtime tests.
# A bare Android shell exit code is not sufficient to prove test success.
grep -Fxq 'OK (6 tests)' audit-runtime-instrumentation.txt
grep -Fxq 'INSTRUMENTATION_CODE: -1' audit-runtime-instrumentation.txt
test "$(grep -Fc 'INSTRUMENTATION_STATUS_CODE: 0' audit-runtime-instrumentation.txt)" -eq 6
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|FATAL EXCEPTION|INSTRUMENTATION_STATUS_CODE: -[0-9]+' audit-runtime-instrumentation.txt; then
  echo "::error::Android instrumented tests contain a failure; no debug allowed"
  exit 1
fi

# Screen evidence is mandatory. Keep test APK installed until copying pixels.
mkdir -p audit-runtime-screens
device_dir=/sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files
adb shell ls -la "$device_dir"
adb pull "$device_dir/." audit-runtime-screens/
count="$(find audit-runtime-screens -type f -name 'audit-*.png' | wc -l | tr -d ' ')"
test "$count" -ge 14
find audit-runtime-screens -type f -name 'audit-*.png' -size 0 -print | grep -q . && {
  echo "::error::At least one required screenshot is empty"; exit 1;
} || true
# Independent visual screens are checked; no Quiz or numbered Carte screens.
for file in audit-lecture-mushaf.png audit-lecture-amorce-anglais.png \
            audit-accueil-sans-quiz.png audit-calendrier-sept-jours.png; do
  test -s "audit-runtime-screens/$file"
done
if cmp -s audit-runtime-screens/audit-lecture-mushaf.png audit-runtime-screens/audit-lecture-amorce-anglais.png; then
  echo "::error::Amorce title popup has no visible UI transition"; exit 1
fi
echo "PASS: six genuine Android runtime tests + $count screenshot PNGs"
