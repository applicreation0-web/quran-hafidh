#!/usr/bin/env bash
# Run only against the disposable CI emulator, never the user's physical BOOX.
set -euo pipefail
package=com.quransafeguard.hifz.ibnkathirtest
runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
out=hifz-app/build/boox-audit
mkdir -p "$out"
run_test() {
  adb shell am instrument -w -e class "$1" "$runner" | tee "$out/$2.log"
  grep -q 'OK (1 test)' "$out/$2.log"
}
kill_and_reopen() {
  adb shell am start -n "$package/com.quransafeguard.hifz.preview.MainActivity"
  for i in $(seq 1 15); do
    before_pid="$(adb shell pidof "$package" 2>/dev/null | tr -d '\r' || true)"
    [ -n "$before_pid" ] && break
    sleep 1
  done
  test -n "$before_pid"
  adb shell am force-stop "$package"
  test -z "$(adb shell pidof "$package" 2>/dev/null | tr -d '\r' || true)"
  adb shell am start -n "$package/com.quransafeguard.hifz.preview.MainActivity"
  for i in $(seq 1 15); do
    after_pid="$(adb shell pidof "$package" 2>/dev/null | tr -d '\r' || true)"
    [ -n "$after_pid" ] && break
    sleep 1
  done
  test -n "$after_pid"
  test "$before_pid" != "$after_pid"
}
gate=com.quransafeguard.hifz.preview.ProcessDeathPersistenceInstrumentedTest
run_test "$gate#a_prepareFixtureForExternalForceStop" process-death-prepare
kill_and_reopen
run_test "$gate#b_verifyFixtureAfterExternalForceStop" process-death-verify
sunday=com.quransafeguard.hifz.preview.SundayFinalProcessDeathInstrumentedTest
run_test "$sunday#prepare" sunday-death-prepare
kill_and_reopen
run_test "$sunday#verify" sunday-death-verify
