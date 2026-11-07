#!/usr/bin/env bash
set -euo pipefail
surface="boox-portrait-emulation"
out="hifz-app/build/freeze-boox-proof"
mkdir -p "$out/home-parcours" "$out/frises"
# Disposable Android-only regression; the Hifz package under test uses its separate DEBUG identity.
bash scripts/run_11713_hifz_parcours_isolated.sh \
  'com.quransafeguard.hifz.preview.Independent11710ScreensInstrumentedTest,com.quransafeguard.hifz.preview.CorrespondenceNavigationInstrumentedTest,com.quransafeguard.hifz.preview.MandatCaptureInstrumentedTest,com.quransafeguard.hifz.preview.FreezeVisualInstrumentedTest' "$surface"
# Actual screenshots taken by Android instrumentation, never synthetic mockups.
adb pull "/sdcard/Pictures/HaafidhAudit/." "$out/home-parcours/" >/dev/null
for suffix in frise-R047 frise-R047-fin frise-R048 frise-R048-fin frise-R059 frise-R059-fin \
              lecture-deux-familles comparaison-p235; do
  if adb pull "/data/local/tmp/haafidh-$suffix.png" "$out/frises/" >/dev/null 2>&1; then
    echo "REAL_BOOX_SCREENSHOT: $suffix"
  fi
done
for expected in independent-home.png independent-parcours-top.png \
                independent-parcours-calendar-bottom.png independent-reader.png \
                independent-progress-map.png; do
  test -s "$out/home-parcours/$expected" || {
    echo "NO_REAL_BOOX_EVIDENCE: $expected" >&2; exit 3;
  }
done
for zoom in independent-freeze2-juz-overview.png independent-freeze2-juz-filled-fixture.png independent-freeze2-progress-juz01.png independent-freeze2-progress-juz30.png; do
  test -s "$out/home-parcours/$zoom" || { echo "NO_REAL_F2_ZOOM: $zoom" >&2; exit 3; }
done
test -s "$out/frises/haafidh-frise-R048.png" || {
  echo "NO_REAL_BOOX_EVIDENCE: frise Ar-Rahman" >&2; exit 3;
}
echo "BOOX_FREEZE_VISUAL_PROOFS_OK"
