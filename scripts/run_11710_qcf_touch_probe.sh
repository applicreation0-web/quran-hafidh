#!/usr/bin/env bash
set -euo pipefail
out=hifz-app/build/qcf-touch-probe
mkdir -p "$out"
collect(){ adb logcat -d > "$out/logcat.txt" || true; }
trap collect EXIT
adb shell wm size 1404x1872
adb shell wm density 226
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
 -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.AtlasDiscriminantInstrumentedTest#enduranceTafsirRotationsAndQcfDisposal \
 -Pandroid.testInstrumentationRunnerArguments.atlasEndurance=true \
 -Pandroid.testInstrumentationRunnerArguments.atlasTafsirCycles=1 \
 -Pandroid.testInstrumentationRunnerArguments.atlasTafsirStartIndex=41
