#!/usr/bin/env bash
# Compare identical disposable Android BOOX emulator states on the EXACT 1.17.14
# production source and the 1.17.15 candidate. No synthetic PNG, no release APK.
set -euo pipefail
root="$(pwd)"
out="$root/hifz-app/build/11715-boox-comparison"
mkdir -p "$out"
base_sha=64943fe77480869bd654fe7f7ba555a46fdba31f
test_path=hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/Haafidh11715CumulCaptureInstrumentedTest.java
baseline="$(mktemp -d)"
rmdir "$baseline"
git worktree add --detach "$baseline" "$base_sha"
cleanup(){ git -C "$root" worktree remove --force "$baseline"; }
trap cleanup EXIT
# The same screenshot instrument belongs to both trees; no 1.17.14
# production code or saved-state schema is changed.
cp "$root/$test_path" "$baseline/$test_path"

capture_phase(){
    local phase="$1" source="$2"
    echo "CAPTURE: $phase source=$(git -C "$source" rev-parse HEAD)"
    adb shell rm -rf /sdcard/Pictures/HaafidhAudit || true
    (
      cd "$source"
      bash scripts/fetch_mushaf_pages.sh
      bash scripts/run_freeze_boox_visual.sh
      bash scripts/run_11711_hifz_parcours_isolated.sh \
        com.quransafeguard.hifz.preview.Haafidh11715CumulCaptureInstrumentedTest \
        boox-portrait-emulation
    )
    mkdir -p "$out/$phase/screens" "$out/$phase/legacy-proof"
    adb pull /sdcard/Pictures/HaafidhAudit/. "$out/$phase/screens/" >/dev/null
    cp -a "$source/hifz-app/build/freeze-boox-proof/." "$out/$phase/legacy-proof/"
    for png in independent-home.png independent-parcours-top.png \
        independent-parcours-calendar-bottom.png independent-reader.png \
        independent-freeze2-juz-overview.png independent-freeze2-juz-filled-fixture.png \
        independent-freeze2-progress-juz01.png independent-freeze2-progress-juz30.png \
        independent-11715-cumul-itqan-x5.png independent-11715-cumul-sabqi-x10.png; do
        test -s "$out/$phase/screens/$png" || {
            echo "MISSING_AUTHENTIC_ANDROID_SCREENSHOT: $phase/$png" >&2; exit 3;
        }
    done
    for id in R047 R048 R059; do
        test -s "$out/$phase/legacy-proof/frises/haafidh-frise-$id.png" || {
            echo "MISSING_AUTHENTIC_REFRAIN: $phase/$id" >&2;exit 4;
        }
    done
    sha256sum "$out/$phase/screens/"*.png > "$out/$phase/screens-sha256.txt"
    (cd "$source" && git rev-parse HEAD) > "$out/$phase/source-sha.txt"
}
capture_phase avant "$baseline"
capture_phase apres "$root"
echo "Android emulator: 1404 x 1872 / 226 dpi" > "$out/metadata.txt"
echo "BEFORE_EXACT_11714: $base_sha" >> "$out/metadata.txt"
echo "AFTER_11715: $(git rev-parse HEAD)" >> "$out/metadata.txt"
echo "REAL_ANDROID_BOOX_EMULATED_BEFORE_AFTER_PASS"
