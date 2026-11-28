#!/usr/bin/env bash
set -euo pipefail

test -z "${AYAH_THEMES_PRIVATE_114:-}"
test -z "${HIFZ_KEYSTORE_PATH:-}"
target=com.quransafeguard.hifz.ibnkathirtest
package=com.quransafeguard.hifz.preview
mkdir -p haafidh-runtime-evidence

# An isolated Android emulator only. No personal backup or release signing material.
adb shell wm size 720x1600
adb shell wm density 360
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
    "-Pandroid.testInstrumentationRunnerArguments.class=$package.HaafidhLanguagePersistenceInstrumentedTest,$package.FragilityReaderVisualInstrumentedTest#standaloneVisibleMaskedAndDeferredPreserveAllAcquisitionAndActiveEvidence,$package.FragilityReaderVisualInstrumentedTest#languageRecreationKeepsOutstandingAssistedRecall"
for screenshot in fragilite-visible fragilite-masquee fragilite-cible fragilite-reprise fragility-fr fragility-en fragility-wo; do
    adb pull "/sdcard/Android/data/$target/files/$screenshot.png" haafidh-runtime-evidence/
    test -s "haafidh-runtime-evidence/$screenshot.png"
done
adb shell dumpsys activity exit-info "$target" > haafidh-runtime-evidence/activity-exits.txt
