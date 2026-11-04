#!/usr/bin/env bash
# Run only against the disposable CI emulator, never the user's physical BOOX.
set -euo pipefail
package=com.quransafeguard.hifz.ibnkathirtest
runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
out=hifz-app/build/boox-audit
mkdir -p "$out"
run_test() {
  local method="$1" log="$2"
  shift 2
  adb shell am instrument -w "$@" -e class "$method" "$runner" | tee "$out/$log.log"
  grep -q 'OK (1 test)' "$out/$log.log"
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

if [[ "${1:-}" == --active-replay ]]; then
  active=com.quransafeguard.hifz.preview.ActiveReviewCorrectionInstrumentedTest
  run_test "$active#forceStopPrepare" active-death-prepare -e activeReplayForceStop true
  kill_and_reopen
  run_test "$active#forceStopVerify" active-death-verify -e activeReplayForceStop true
fi
