#!/usr/bin/env bash
set -euo pipefail

test -z "${AYAH_THEMES_PRIVATE_114:-}"
test -z "${HIFZ_KEYSTORE_PATH:-}"
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
target=com.quransafeguard.hifz.ibnkathirtest
package=com.quransafeguard.hifz.preview
evidence=haafidh-runtime-evidence
device_dir="/sdcard/Android/data/$target/files"
runner="$target.test/androidx.test.runner.AndroidJUnitRunner"
fragility="$package.FragilityReaderVisualInstrumentedTest#standaloneVisibleMaskedAndDeferredPreserveAllAcquisitionAndActiveEvidence"

reference="${1:?Expected the pinned 1.17.24 checkout}"
test "$(git -C "$reference" rev-parse HEAD)" = fe271ca6c3dff7b9e31ca5b0a2801c9fece3be0c
test ! -d "$reference/hifz-app/src/private114/assets"
gradle --no-daemon -p "$reference" :hifz-app:assembleDebug :hifz-app:assembleDebugAndroidTest
gradle --no-daemon :hifz-app:assembleDebug :hifz-app:assembleDebugAndroidTest
mapfile -t old_app < <(find "$reference/hifz-app/build/outputs/apk/debug" -name '*.apk' -type f)
mapfile -t old_test < <(find "$reference/hifz-app/build/outputs/apk/androidTest/debug" -name '*.apk' -type f)
mapfile -t app_apk < <(find hifz-app/build/outputs/apk/debug -name '*.apk' -type f)
mapfile -t test_apk < <(find hifz-app/build/outputs/apk/androidTest/debug -name '*.apk' -type f)
test "${#app_apk[@]}" -eq 1
test "${#test_apk[@]}" -eq 1
test "${#old_app[@]}" -eq 1
test "${#old_test[@]}" -eq 1

instrument() {
    local tests="$1" count="$2" output="$3"
    shift 3
    adb shell am instrument -w -r -e class "$tests" "$@" "$runner" | tr -d '\r' | tee "$output" || return 1
    grep -Eq "^OK \\($count tests?\\)$" "$output" || return 1
    grep -Fxq 'INSTRUMENTATION_CODE: -1' "$output"
}

run_update() {
    local destroy="$package.ProcessDeathPersistenceInstrumentedTest"
    local dest="$evidence/debug-update-62-to-63"
    mkdir -p "$dest"
    adb install -r "${old_app[0]}"
    adb install -r "${old_test[0]}"
    adb shell dumpsys package "$target" > "$dest/before.txt"
    grep -Eq '^ *versionCode=62( |$)' "$dest/before.txt"
    local first_install
    first_install="$(awk -F= '/firstInstallTime=/{print $2; exit}' "$dest/before.txt")"
    test -n "$first_install"
    instrument "$destroy#a_prepareFixtureForExternalForceStop" 1 "$dest/prepare.txt"
    adb shell am force-stop "$target"
    adb install -r "${app_apk[0]}"
    adb install -r "${test_apk[0]}"
    adb shell dumpsys package "$target" > "$dest/after.txt"
    grep -Eq '^ *versionCode=63( |$)' "$dest/after.txt"
    test "$first_install" = "$(awk -F= '/firstInstallTime=/{print $2; exit}' "$dest/after.txt")"
    instrument "$destroy#b_verifyFixtureAfterExternalForceStop" 1 "$dest/verify.txt" -e strictAndroidUpdate true
}

run_case() {
    local size="$1" density="$2" font="$3" tests="$4" count="$5"
    shift 5
    local dest="$evidence/${size}-font${font}"
    mkdir -p "$dest"
    adb shell am force-stop "$target"
    adb shell wm size "$size"
    adb shell wm density "$density"
    adb shell settings put system font_scale "$font"
    test "$(adb shell settings get system font_scale | tr -d '\r')" = "$font"
    for screenshot in "$@"; do
        adb shell rm -f "$device_dir/$screenshot.png"
    done
    local status=0
    instrument "$tests" "$count" "$dest/result.txt" || status=1
    for screenshot in "$@"; do
        adb pull "$device_dir/$screenshot.png" "$dest/" || status=1
        test -s "$dest/$screenshot.png" || status=1
    done
    if (( status != 0 )); then
        adb exec-out screencap -p > "$dest/failure-screen.png" || true
        adb shell dumpsys activity top > "$dest/failure-activity.txt" || true
    fi
    return "$status"
}

frames=(fragilite-visible fragilite-masquee fragilite-cible fragilite-reprise)
run_update
run_case 720x1600 360 1.0 \
    "$package.HaafidhLanguagePersistenceInstrumentedTest,$fragility,$package.FragilityReaderVisualInstrumentedTest#languageRecreationKeepsOutstandingAssistedRecall,$package.FragilityReaderVisualInstrumentedTest#languageRecreationKeepsPausedTimedRecall,$package.FragilityReaderVisualInstrumentedTest#programmedBudgetsExpireWithoutCreditOrLosingAnyOfTwelvePages" 6 \
    "${frames[@]}" fragility-fr fragility-en fragility-wo setup-summary-en
run_case 1080x2400 440 1.0 "$fragility" 1 "${frames[@]}"
run_case 900x1200 220 1.5 "$fragility" 1 "${frames[@]}"
run_case 1404x1872 240 1.5 "$fragility" 1 "${frames[@]}"
adb shell dumpsys activity exit-info "$target" > "$evidence/activity-exits.txt"
