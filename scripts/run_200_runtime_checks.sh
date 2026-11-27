#!/usr/bin/env bash
set -euo pipefail

test -z "${AYAH_THEMES_PRIVATE_114:-}"
test -z "${HIFZ_KEYSTORE_PATH:-}"
target=com.quransafeguard.hifz.ibnkathirtest
package=com.quransafeguard.hifz.preview
mkdir -p haafidh-runtime-evidence
capture() {
    adb pull /sdcard/Pictures/HaafidhAudit haafidh-runtime-evidence/ || true
}
trap capture EXIT

# An isolated Android emulator only. No personal backup or release signing material.
adb shell wm size 720x1600
adb shell wm density 360
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
    "-Pandroid.testInstrumentationRunnerArguments.class=$package.HaafidhLanguagePersistenceInstrumentedTest,$package.FragilityReaderVisualInstrumentedTest#standaloneVisibleMaskedAndDeferredPreserveAllAcquisitionAndActiveEvidence"
adb shell dumpsys activity exit-info "$target" > haafidh-runtime-evidence/activity-exits.txt
