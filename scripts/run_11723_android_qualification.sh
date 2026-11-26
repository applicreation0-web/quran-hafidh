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

status=0
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.ActiveReviewSlotsInstrumentedTest,com.quransafeguard.hifz.preview.HifzV6MigrationRecoveryInstrumentedTest,com.quransafeguard.hifz.preview.HifzCarryoverDateInstrumentedTest,com.quransafeguard.hifz.preview.ConsolidationPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.BackupSchemaRejectedInstrumentedTest,com.quransafeguard.hifz.preview.CapHifzReadOnlyInstrumentedTest \
    || status=1
run_capture "$reference" "$evidence/before" || status=1
# Both builds share the isolated debug package. Start each visual run with synthetic default state.
adb shell pm clear com.quransafeguard.hifz.ibnkathirtest || status=1
run_capture "$candidate" "$evidence/after" || status=1
exit "$status"
