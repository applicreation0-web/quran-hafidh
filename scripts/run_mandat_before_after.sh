#!/usr/bin/env bash
# Disposable emulator, public debug corpora only. Pinned source BEFORE vs current AFTER.
set -euo pipefail
root="$(pwd)"
baseline="$(mktemp -d)"
rmdir "$baseline"
git worktree add --detach "$baseline" a266b279e3f0c1a9684861aa703ca5e2125ef3d9
cleanup() { git -C "$root" worktree remove --force "$baseline"; }
trap cleanup EXIT
class_path=hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/MandatCaptureInstrumentedTest.java
cp "$root/$class_path" "$baseline/$class_path"
mkdir -p hifz-app/build/android-proof
before_status=0
(
 cd "$baseline"
 gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
   -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.MandatCaptureInstrumentedTest \
   -Pandroid.testInstrumentationRunnerArguments.mandatPhase=avant
) || before_status=$?
for image in avant-lecture-p11 avant-lecture-p14 avant-lecture-p532 avant-lecture-p235 avant-lecture-p529 avant-lecture-p293 avant-comparaison-p235 avant-sources; do
 adb pull "/data/local/tmp/haafidh-$image.png" "$root/hifz-app/build/android-proof/" >/dev/null || before_status=1
done
cp -r "$baseline/hifz-app/build/outputs/androidTest-results" "$root/hifz-app/build/android-proof/before-test-results" || before_status=1
if (( before_status != 0 )); then exit "$before_status"; fi
