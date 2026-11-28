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

gradle --no-daemon :hifz-app:assembleDebug :hifz-app:assembleDebugAndroidTest
mapfile -t app_apk < <(find hifz-app/build/outputs/apk/debug -name '*.apk' -type f)
mapfile -t test_apk < <(find hifz-app/build/outputs/apk/androidTest/debug -name '*.apk' -type f)
test "${#app_apk[@]}" -eq 1
test "${#test_apk[@]}" -eq 1
# Keep both debug packages installed until their app-scoped screenshots have been pulled.
adb install -r "${app_apk[0]}"
adb install -r "${test_apk[0]}"

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
    adb shell am instrument -w -r -e class "$tests" "$runner" | tee "$dest/instrumentation.txt"
    tr -d '\r' < "$dest/instrumentation.txt" > "$dest/result.txt"
    grep -Eq "^OK \\($count tests?\\)$" "$dest/result.txt"
    grep -Fxq 'INSTRUMENTATION_CODE: -1' "$dest/result.txt"
    for screenshot in "$@"; do
        adb pull "$device_dir/$screenshot.png" "$dest/"
        test -s "$dest/$screenshot.png"
    done
}

frames=(fragilite-visible fragilite-masquee fragilite-cible fragilite-reprise)
run_case 720x1600 360 1.0 \
    "$package.HaafidhLanguagePersistenceInstrumentedTest,$fragility,$package.FragilityReaderVisualInstrumentedTest#languageRecreationKeepsOutstandingAssistedRecall" 4 \
    "${frames[@]}" fragility-fr fragility-en fragility-wo setup-summary-en
run_case 1080x2400 440 1.0 "$fragility" 1 "${frames[@]}"
run_case 900x1200 220 1.5 "$fragility" 1 "${frames[@]}"
run_case 1404x1872 240 1.5 "$fragility" 1 "${frames[@]}"
adb shell dumpsys activity exit-info "$target" > "$evidence/activity-exits.txt"
