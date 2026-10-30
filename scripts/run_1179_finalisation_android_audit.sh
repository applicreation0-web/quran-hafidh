#!/usr/bin/env bash
set -euo pipefail
surface="${1:?surface}";size="${2:?size}";density="${3:?density}"
audit_dir=hifz-app/build/atlas-comparison-audit
mkdir -p "$audit_dir"
collect_audit() {
    adb logcat -d -s HaafidhAtlasQCF:V QcfAtlasStrips:V chromium:E > "$audit_dir/$surface-qcf-logcat.txt" || true
    adb pull /sdcard/Pictures/HaafidhAudit "$audit_dir/" || true
}
trap collect_audit EXIT
adb shell wm size "$size"
adb shell wm density "$density"
bash scripts/run_atlas_revision_android_audit.sh
# Separate instrumentation processes around a real OS force-stop. The fixture is
# synthetic and isolated; this proves disk persistence, not a signed release upgrade.
target=com.quransafeguard.hifz.ibnkathirtest
runner="$target.test/androidx.test.runner.AndroidJUnitRunner"
test_class=com.quransafeguard.hifz.preview.ActiveReviewCorrectionInstrumentedTest
for phase in Prepare Verify; do
    adb shell am instrument -w -r -e activeReplayForceStop true -e class "$test_class#forceStop$phase" "$runner" > "$audit_dir/$surface-force-stop-$phase.txt"
    cat "$audit_dir/$surface-force-stop-$phase.txt"
    if ! rg -q 'OK \(1 test\)' "$audit_dir/$surface-force-stop-$phase.txt"; then
        echo "Force-stop $phase failed" >&2; exit 1
    fi
    if [ "$phase" = Prepare ]; then adb shell am force-stop "$target"; fi
done
collect_audit
for orientation in portrait landscape; do
    test -s "$audit_dir/HaafidhAudit/haafidh-mutash-$surface-$orientation-contextual.png"
done
