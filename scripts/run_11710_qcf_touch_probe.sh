#!/usr/bin/env bash
set -euo pipefail
out=hifz-app/build/qcf-touch-probe
mkdir -p "$out"
collect(){ adb logcat -d > "$out/logcat.txt" || true; }
trap collect EXIT
adb shell wm size 1080x1920
adb shell wm density 420
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
 -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.AtlasDiscriminantInstrumentedTest#enduranceTafsirRotationsAndQcfDisposal \
 -Pandroid.testInstrumentationRunnerArguments.atlasEndurance=true \
 -Pandroid.testInstrumentationRunnerArguments.atlasTafsirCycles=1 \
 -Pandroid.testInstrumentationRunnerArguments.atlasTafsirStartIndex=41
