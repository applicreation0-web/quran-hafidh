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
    -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.ActiveReviewSlotsInstrumentedTest,com.quransafeguard.hifz.preview.HifzV6MigrationRecoveryInstrumentedTest,com.quransafeguard.hifz.preview.HifzCarryoverDateInstrumentedTest,com.quransafeguard.hifz.preview.ConsolidationPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.BackupSchemaRejectedInstrumentedTest,com.quransafeguard.hifz.preview.CapHifzReadOnlyInstrumentedTest \
    || status=1
verify_process_death || status=1
run_capture "$reference" "$evidence/before" || status=1
# Both builds share the isolated debug package. Start each visual run with synthetic default state.
adb shell pm clear com.quransafeguard.hifz.ibnkathirtest || status=1
run_capture "$candidate" "$evidence/after" || status=1
exit "$status"
