#!/usr/bin/env bash
set -euo pipefail

reference="$1"
candidate="$PWD"
evidence="$candidate/haafidh-evidence"
mkdir -p "$evidence"/{after,before}

run_capture() {
    local checkout="$1" destination="$2" status=0
    (
        cd "$checkout"
        python3 scripts/capture_hifz_boox_ui.py
    ) >"$destination/result.txt" 2>&1 || status=$?
    cat "$destination/result.txt"
    if [[ -d "$checkout/boox-real-renders" ]]; then
        find "$checkout/boox-real-renders" -maxdepth 1 -type f \
            \( -name '*.png' -o -name 'build.txt' \) -exec cp {} "$destination" \;
    fi
    return "$status"
}

run_process_death_phase() {
    local method="$1" output="$evidence/after/$1-result.txt"
    local package="com.quransafeguard.hifz.ibnkathirtest"
    adb shell am instrument -w -r \
        -e class "com.quransafeguard.hifz.preview.ProcessDeathPersistenceInstrumentedTest#$method" \
        "$package.test/androidx.test.runner.AndroidJUnitRunner" >"$output" 2>&1 || return 1
    cat "$output"
    grep -Fxq 'OK (1 test)' "$output" && grep -Fxq 'INSTRUMENTATION_CODE: -1' "$output"
}

verify_process_death() {
    local package="com.quransafeguard.hifz.ibnkathirtest"
    local -a app_apk test_apk
    mapfile -t app_apk < <(find hifz-app/build/outputs/apk/debug -name '*.apk' -type f)
    mapfile -t test_apk < <(find hifz-app/build/outputs/apk/androidTest/debug -name '*.apk' -type f)
    [[ ${#app_apk[@]} -eq 1 && ${#test_apk[@]} -eq 1 ]] || return 1
    adb install -r "${app_apk[0]}" && adb install -r "${test_apk[0]}" || return 1
    run_process_death_phase a_prepareFixtureForExternalForceStop || return 1
    adb shell am force-stop "$package" || return 1
    adb shell am start -W -n "$package/com.quransafeguard.hifz.preview.MainActivity" \
        | tee "$evidence/after/restart-result.txt" | grep '^Status: ok' || return 1
    adb shell am force-stop "$package" || return 1
    run_process_death_phase b_verifyFixtureAfterExternalForceStop
}

status=0
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.ActiveReviewSlotsInstrumentedTest,com.quransafeguard.hifz.preview.HifzV6MigrationRecoveryInstrumentedTest,com.quransafeguard.hifz.preview.HifzCarryoverDateInstrumentedTest,com.quransafeguard.hifz.preview.ConsolidationPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.BackupSchemaRejectedInstrumentedTest,com.quransafeguard.hifz.preview.CapHifzReadOnlyInstrumentedTest,com.quransafeguard.hifz.preview.FragilityReaderVisualInstrumentedTest#threePageBatchAdvancesResumesDefersAndReturnsWithoutActiveCredit,com.quransafeguard.hifz.preview.FragilityReaderVisualInstrumentedTest#continuationCannotCrossTheMorningEveningBoundary \
    || status=1
verify_process_death || status=1
# The candidate now has a higher versionCode. Android refuses an ordinary
# install -r of the older reference. Remove ONLY the disposable emulator's
# synthetic debug package before reference capture; never touch a real device.
if [[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" != "1" ]]; then
    echo "Refusing reference reset: not an Android emulator" >&2
    exit 1
fi
adb uninstall com.quransafeguard.hifz.ibnkathirtest || exit 1
if ! run_capture "$reference" "$evidence/before"; then
    echo "Reference contract failure recorded in before/result.txt"
    for name in boox-1404x1872-font100 boox-900x1200-font100; do
        test -s "$evidence/before/$name-home.png" && test -s "$evidence/before/$name.png" || status=1
    done
    test -s "$evidence/before/phone-font100-home.png" || status=1
fi
# Both builds share the isolated debug package. Start each visual run with synthetic default state.
adb shell pm clear com.quransafeguard.hifz.ibnkathirtest || status=1
run_capture "$candidate" "$evidence/after" || status=1
# Reinstall instrumentation after removing the isolated debug target.
mapfile -t visual_test_apks < <(find hifz-app/build/outputs/apk/androidTest/debug -name '*.apk' -type f)
[[ ${#visual_test_apks[@]} -eq 1 ]] || exit 1
adb install -r "${visual_test_apks[0]}" || exit 1

# Dedicated reader captures, not merely the home/Parcours/Cap screens.
# Only isolated synthetic debug fixtures, never an installed private package.
for viewport in 900x1200:220 1404x1872:240 1080x2400:440; do
    size="${viewport%:*}"
    density="${viewport#*:}"
    for font in 1.0 1.5; do
        tag="fragilites-${size}-font${font}"
        dest="$evidence/after/$tag"
        mkdir -p "$dest"
        package=com.quransafeguard.hifz.ibnkathirtest
        adb shell am force-stop "$package" || { status=1; continue; }
        adb shell wm size "$size" && adb shell wm density "$density" && \
            adb shell settings put system font_scale "$font" || { status=1; continue; }
        adb shell rm -f "/sdcard/Android/data/$package/files/fragilite-visible.png" \
            "/sdcard/Android/data/$package/files/fragilite-masquee.png" \
            "/sdcard/Android/data/$package/files/fragilite-cible.png" \
            "/sdcard/Android/data/$package/files/fragilite-reprise.png" || { status=1; continue; }
        adb shell am instrument -w -r -e class \
            com.quransafeguard.hifz.preview.FragilityReaderVisualInstrumentedTest#standaloneVisibleMaskedAndDeferredPreserveAllAcquisitionAndActiveEvidence \
            "$package.test/androidx.test.runner.AndroidJUnitRunner" >"$dest/result.txt" 2>&1 || status=1
        cat "$dest/result.txt"
        grep -Fxq 'OK (1 test)' "$dest/result.txt" || status=1
        grep -Fxq 'INSTRUMENTATION_CODE: -1' "$dest/result.txt" || status=1
        for phase in visible masquee cible reprise; do
            adb pull "/sdcard/Android/data/$package/files/fragilite-$phase.png" "$dest/" || status=1
            test -s "$dest/fragilite-$phase.png" || status=1
        done
    done
done
adb shell wm size reset || status=1
adb shell wm density reset || status=1
adb shell settings put system font_scale 1.0 || status=1
exit "$status"
