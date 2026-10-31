#!/usr/bin/env bash
set -euo pipefail
surface="${1:?surface}";size="${2:?size}";density="${3:?density}"
scope="${4:-full}";shard="${5:-0}"
audit_dir=hifz-app/build/atlas-comparison-audit
mkdir -p "$audit_dir"
collect_audit() {
    adb logcat -d -s HaafidhCatalogueAudit:V HaafidhAtlasQCF:V QcfAtlasStrips:V chromium:E > "$audit_dir/$surface-$scope-$shard-logcat.txt" || true
    adb pull /sdcard/Pictures/HaafidhAudit "$audit_dir/" || true
}
trap collect_audit EXIT
adb shell wm size "$size"
adb shell wm density "$density"
atlas=com.quransafeguard.hifz.preview.AtlasDiscriminantInstrumentedTest
if [ "$scope" = catalogue ]; then
    gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class="$atlas#everyPilotPairLoadsBothAuthenticQcfPagesAndStrips" \
      -Pandroid.testInstrumentationRunnerArguments.catalogueShard="$shard" \
      -Pandroid.testInstrumentationRunnerArguments.catalogueShards=4
    exit 0
fi
# Run the new UI, recall, backup and restoration gates before the expensive full
# catalogue. Every admitted pair is then checked in the separate catalogue jobs.
classes="com.quransafeguard.hifz.preview.ActiveReviewCorrectionInstrumentedTest,com.quransafeguard.hifz.preview.MutashPageMarkersInstrumentedTest,com.quransafeguard.hifz.preview.RevisionAtlasInstrumentedTest,com.quransafeguard.hifz.preview.BackupSchemaRejectedInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalMaskReadinessInstrumentedTest,$atlas"
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="$classes" \
    -Pandroid.testInstrumentationRunnerArguments.notClass="$atlas#everyPilotPairLoadsBothAuthenticQcfPagesAndStrips"
# Separate instrumentation processes around an OS force-stop. The fixture is
# synthetic and isolated; this proves disk persistence, not a signed release upgrade.
target=com.quransafeguard.hifz.ibnkathirtest
runner="$target.test/androidx.test.runner.AndroidJUnitRunner"
test_class=com.quransafeguard.hifz.preview.ActiveReviewCorrectionInstrumentedTest
for phase in Prepare Verify; do
    adb shell am instrument -w -r -e activeReplayForceStop true -e class "$test_class#forceStop$phase" "$runner" > "$audit_dir/$surface-force-stop-$phase.txt"
    cat "$audit_dir/$surface-force-stop-$phase.txt"
    if ! grep -Eq 'OK \(1 test\)' "$audit_dir/$surface-force-stop-$phase.txt"; then
        echo "Force-stop $phase failed" >&2; exit 1
    fi
    if [ "$phase" = Prepare ]; then adb shell am force-stop "$target"; fi
done
collect_audit
for orientation in portrait landscape; do
    test -s "$audit_dir/HaafidhAudit/haafidh-mutash-$surface-$orientation-contextual.png"
done
for capture in atlas-revision-passive atlas-revision-active-masked atlas-revision-active-validated; do
    test -s "$audit_dir/HaafidhAudit/$capture.png"
done
