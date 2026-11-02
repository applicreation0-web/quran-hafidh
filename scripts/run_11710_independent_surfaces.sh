#!/usr/bin/env bash
# Disposable CI emulator exclusively; never point this script at a user's device.
set -euo pipefail
out=hifz-app/build/independent-surfaces
mkdir -p "$out"
collect() {
  adb pull /sdcard/Pictures/HaafidhAudit "$out/" || true
  adb logcat -d -b events > "$out/events.txt" || true
}
trap collect EXIT
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
 -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.Independent11710ScreensInstrumentedTest,com.quransafeguard.hifz.preview.Independent11710RecoveryInstrumentedTest
# Cross a genuine Android reboot with the established five-store synthetic fixture.
package=com.quransafeguard.hifz.ibnkathirtest
runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
case=com.quransafeguard.hifz.preview.ProcessDeathPersistenceInstrumentedTest
adb shell am instrument -w -e class "$case#a_prepareFixtureForExternalForceStop" "$runner" > "$out/reboot-prepare.log"
grep -q 'OK (1 test)' "$out/reboot-prepare.log"
adb shell cat /proc/sys/kernel/random/boot_id > "$out/boot-id-before.txt"
adb reboot
timeout 180 adb wait-for-device
booted=false
for attempt in $(seq 1 90); do
  if [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; then booted=true;break;fi
  sleep 2
done
test "$booted" = true
adb shell input keyevent 82
adb shell cat /proc/sys/kernel/random/boot_id > "$out/boot-id-after.txt"
! cmp -s "$out/boot-id-before.txt" "$out/boot-id-after.txt"
adb shell am instrument -w -e class "$case#b_verifyFixtureAfterExternalForceStop" "$runner" > "$out/reboot-verify.log"
grep -q 'OK (1 test)' "$out/reboot-verify.log"
echo 'PASS: distinct Android boot IDs; synthetic five-store fixture restored without loss' > "$out/reboot-result.txt"
collect
