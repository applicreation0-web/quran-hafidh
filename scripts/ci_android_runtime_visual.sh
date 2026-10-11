#!/usr/bin/env bash
# Real Android UI gate on the independently named 1.17.1 Ibn Kathir test package.
# No production data, signed release, or original Quran/Hifz engine is modified.
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
  -e class 'com.quransafeguard.hifz.preview.QuizLaunchRegressionInstrumentedTest,com.quransafeguard.hifz.preview.HifzAndMapRuntimeSmokeInstrumentedTest' \
  'com.quransafeguard.hifz.ibnkathirtest.test/androidx.test.runner.AndroidJUnitRunner' \
  > audit-runtime-instrumentation.txt 2>&1
status=$?
set -e
cat audit-runtime-instrumentation.txt
test "$status" -eq 0
grep -Eq '^OK ([0-9]+ tests?)' audit-runtime-instrumentation.txt
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|FATAL EXCEPTION' audit-runtime-instrumentation.txt

# Unlike Gradle connectedDebugAndroidTest, manual instrumentation keeps the
# target test package installed until the evidence has been copied out.
mkdir -p audit-runtime-screens
adb shell ls -la /sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files
adb pull /sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files/. audit-runtime-screens/
test "$(find audit-runtime-screens -type f -name 'audit-*.png' | wc -l)" -ge 12
echo "PASS: instrumented Quiz and all Hifz/Map tests plus >=12 real emulator screenshots"
