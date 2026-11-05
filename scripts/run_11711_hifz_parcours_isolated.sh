#!/usr/bin/env bash
# Disposable Android emulator audit only. Never sign/publish private APKs.
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <comma-separated-full-class-names> <surface>" >&2
  exit 2
fi
classes="$1"
surface="$2"
case "$surface" in phone|boox-portrait-emulation) ;; *) echo "Unrecognized surface" >&2; exit 2;; esac
out="hifz-app/build/parcours-android-logs/$surface"
mkdir -p "$out"

# Unlike connectedDebugAndroidTest, assemble leaves both APKs available for
# installation and leaves the instrumentation package installed for later classes.
gradle --no-daemon :hifz-app:assembleDebug :hifz-app:assembleDebugAndroidTest

mapfile -t app_candidates < <(find hifz-app/build/outputs/apk -type f -name '*.apk' ! -path '*/androidTest/*' | sort)
mapfile -t test_candidates < <(find hifz-app/build/outputs/apk -type f -name '*.apk' -path '*/androidTest/*' | sort)
if [[ ${#app_candidates[@]} -ne 1 || ${#test_candidates[@]} -ne 1 ]]; then
  printf 'Expected exactly one DEBUG app and one instrumentation APK; found app=%s test=%s\n' "${#app_candidates[@]}" "${#test_candidates[@]}" >&2
  find hifz-app/build/outputs/apk -type f -name '*.apk' -print >&2 || true
  exit 3
fi
adb install -r "${app_candidates[0]}"
adb install -r "${test_candidates[0]}"
adb wait-for-device

line="$(adb shell pm list instrumentation | tr -d '\r' | grep 'target=com.quransafeguard.hifz' | head -n 1 || true)"
if [[ -z "$line" ]]; then
  echo "Missing Android instrumentation after installing both debug APKs" >&2
  adb shell pm list instrumentation >&2
  exit 4
fi
instrumentation="${line#instrumentation:}"
instrumentation="${instrumentation%% *}"
target="${line##*target=}"
target="${target%%)*}"
test_package="${instrumentation%%/*}"
if [[ "$target" != com.quransafeguard.hifz.ibnkathirtest ||
      "$test_package" != com.quransafeguard.hifz* ]]; then
  echo "Unexpected test identity; refusing to target another package: $target / $test_package" >&2
  exit 5
fi
IFS=',' read -r -a class_names <<< "$classes"
failures=0
completed=0
attempt=0
for cls in "${class_names[@]}"; do
  if [[ ! "$cls" =~ ^com\.quransafeguard\.hifz\.preview\.[A-Za-z0-9_]+InstrumentedTest$ ]]; then
    echo "Invalid or out-of-scope test class: $cls" >&2
    exit 6
  fi
  simple="${cls##*.}"
  ((attempt+=1))
  simple="$(printf '%02d' "$attempt")-$simple"
  adb logcat -c
  adb shell cat /proc/sys/kernel/random/boot_id > "$out/$simple.boot-before.log"
  adb shell dumpsys activity activities > "$out/$simple.lifecycle-before.log"
  echo "ISOLATED CLASS: $cls"
  adb shell am force-stop "$target" || true
  adb shell pm clear "$target" > /dev/null
  # Also isolate preference state created by instrumentation helpers.
  adb shell pm clear "$test_package" > /dev/null
  # Persist logcat on either outcome, including Java/native crash buffers.
  adb logcat -b all -v threadtime > "$out/$simple.logcat.log" 2>&1 &
  logcat_pid=$!
  if ! adb shell am instrument -w -r -e class "$cls" "$instrumentation" > "$out/$simple.log" 2>&1; then
    kill "$logcat_pid" 2>/dev/null || true
    echo "FAILED: $cls (instrumentation exit)" >&2
    tail -n 90 "$out/$simple.log" >&2
    ((failures+=1))
    continue
  fi
  kill "$logcat_pid" 2>/dev/null || true
  wait "$logcat_pid" 2>/dev/null || true
  adb shell cat /proc/sys/kernel/random/boot_id > "$out/$simple.boot-after.log"
  adb shell dumpsys activity activities > "$out/$simple.lifecycle-after.log"
  adb shell dumpsys activity exit-info "$target" > "$out/$simple.exit-info.log"
  if ! grep -Eq 'OK \([1-9][0-9]* tests?\)' "$out/$simple.log"; then
    echo "FAILED: $cls (no nonzero passing-test assertion)" >&2
    tail -n 90 "$out/$simple.log" >&2
    ((failures+=1))
    continue
  fi
  ((completed+=1))
  echo "PASS: $cls"
done
printf 'RESULT: %s passing classes / %s requested; failures=%s\n' "$completed" "${#class_names[@]}" "$failures"
[[ "$failures" -eq 0 && "$completed" -eq "${#class_names[@]}" ]]
