#!/usr/bin/env bash
set -euo pipefail

# emulator-runner invokes each YAML script line in its own shell. Keep the
# test, EXIT trap and evidence collection in ONE bash process instead.
audit_dir=hifz-app/build/atlas-comparison-audit
mkdir -p "$audit_dir"
collect_audit() {
    adb logcat -d -s HaafidhAtlasQCF:V QcfAtlasStrips:V chromium:E > "$audit_dir/emulator-qcf-logcat.txt" || true
    adb pull /sdcard/Pictures/HaafidhAudit/haafidh-atlas-discriminant-portrait.png "$audit_dir/atlas-pilot-emulator-portrait.png" || true
    for capture in passive active-masked active-validated; do
        adb pull "/sdcard/Pictures/HaafidhAudit/atlas-revision-$capture.png" "$audit_dir/" || true
    done
}
trap collect_audit EXIT

gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.ActiveReviewCorrectionInstrumentedTest,com.quransafeguard.hifz.preview.MutashPageMarkersInstrumentedTest,com.quransafeguard.hifz.preview.AtlasDiscriminantInstrumentedTest,com.quransafeguard.hifz.preview.RevisionAtlasInstrumentedTest,com.quransafeguard.hifz.preview.BackupSchemaRejectedInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalMaskReadinessInstrumentedTest

collect_audit
for capture in atlas-pilot-emulator-portrait atlas-revision-passive atlas-revision-active-masked atlas-revision-active-validated; do
    test -s "$audit_dir/$capture.png"
done
