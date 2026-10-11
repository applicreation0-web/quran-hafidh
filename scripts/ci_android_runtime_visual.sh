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
# The ADB instrument command can use a nonzero shell code for the Android
# success sentinel INSTRUMENTATION_CODE: -1. The authoritative check is the
# actual terminal result from AndroidJUnitRunner (and all 5 tests).
echo "ADB instrumentation shell status: $status"
grep -Eq '^OK \(5 tests\)' audit-runtime-instrumentation.txt
grep -Eq '^INSTRUMENTATION_CODE: -1

# Unlike Gradle connectedDebugAndroidTest, manual instrumentation keeps the
# target test package installed until the evidence has been copied out.
mkdir -p audit-runtime-screens
adb shell ls -la /sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files
adb pull /sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files/. audit-runtime-screens/
test "$(find audit-runtime-screens -type f -name 'audit-*.png' | wc -l)" -ge 12
echo "PASS: instrumented Quiz and all Hifz/Map tests plus >=12 real emulator screenshots"
 audit-runtime-instrumentation.txt
test "$(grep -c '^INSTRUMENTATION_STATUS_CODE: 0

# Unlike Gradle connectedDebugAndroidTest, manual instrumentation keeps the
# target test package installed until the evidence has been copied out.
mkdir -p audit-runtime-screens
adb shell ls -la /sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files
adb pull /sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files/. audit-runtime-screens/
test "$(find audit-runtime-screens -type f -name 'audit-*.png' | wc -l)" -ge 12
echo "PASS: instrumented Quiz and all Hifz/Map tests plus >=12 real emulator screenshots"
 audit-runtime-instrumentation.txt)" -eq 5
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|FATAL EXCEPTION|INSTRUMENTATION_STATUS_CODE: -[0-9]+' audit-runtime-instrumentation.txt

# Unlike Gradle connectedDebugAndroidTest, manual instrumentation keeps the
# target test package installed until the evidence has been copied out.
mkdir -p audit-runtime-screens
adb shell ls -la /sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files
adb pull /sdcard/Android/data/com.quransafeguard.hifz.ibnkathirtest/files/. audit-runtime-screens/
test "$(find audit-runtime-screens -type f -name 'audit-*.png' | wc -l)" -ge 12
echo "PASS: instrumented Quiz and all Hifz/Map tests plus >=12 real emulator screenshots"
