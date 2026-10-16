#!/usr/bin/env bash
# Audit ONLY. Never build or publish an APK.
# Exact source equality to the official Quran Haafidh 1.17.1 baseline.
set -euo pipefail
BASE=7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21
git cat-file -e "${BASE}^{commit}" || {
  echo "::error::Official 1.17.1 reference missing. Use fetch-depth: 0."
  exit 1
}

protected=(
  hifz-core
  app/src/main/assets/reader109/geometry.json
  app/src/main/assets/reader109/waqf.json
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/AnchoringQueue.java
)
changed="$(git diff --name-status "${BASE}" HEAD -- "${protected[@]}")"
if [[ -n "$changed" ]]; then
  echo "::error::Official v1.17.1 Hifz/Quiz protected file(s) differ:"
  printf '%s\n' "$changed"
  exit 1
fi
# Every QCF asset remains byte-identical except the exact approved Quiz-only JS
# mask cut-out. Prevent silently changing fonts, geometry or a different reader asset.
unexpected_assets="$(git diff --name-only --no-renames "${BASE}" HEAD -- hifz-app/src/main/assets/hifzreader | grep -v '^hifz-app/src/main/assets/hifzreader/reader[.]js$' || true)"
if [[ -n "$unexpected_assets" ]]; then
  echo "::error::Unexpected official Hifz reader asset changes:"
  printf '%s\n' "$unexpected_assets"
  exit 1
fi
# The retired Quiz must not remain anywhere in application Java sources.
for f in hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizActivity.java hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizCorpus.java hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizHistory.java hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizQuestion.java; do
  if [[ -e "$f" ]]; then echo "::error::Retired Quiz still compiled: $f"; exit 1; fi
done
python3 scripts/verify_public_amorce_keys.py
python3 scripts/verify_authorized_quiz_reader_delta.py
python3 scripts/verify_authorized_word_geometry_quiz_detach.py
python3 scripts/verify_itqan_four_session_plan_contract.py
python3 scripts/verify_itqan_rotation_atomicity.py
python3 scripts/verify_authorized_hifz_session_delta.py
python3 scripts/verify_authorized_program_preferences_delta.py
python3 scripts/verify_authorized_consolidation_delta.py
echo "PASS: Official Quiz logic/QCF assets frozen, except SHA-pinned exact-word Quiz mask fix."

# Also freeze the rest of application logic: only explicitly whitelisted UI/documentary
# Java controllers may diverge. Any unexpected Java change is a regression risk.
prefix=hifz-app/src/main/java/com/quransafeguard/hifz/preview
other="$(git diff --name-only --no-renames "${BASE}" HEAD -- "$prefix" hifz-core)"
bad=0
while IFS= read -r file; do
  [[ -n "$file" ]] || continue
  case "$file" in
    "$prefix/HifzRecoveryApplication.java")
      if [[ "$(git hash-object "$file")" != "d198dc2ac398b68f48032f7b4f50ae4fee1b5d8f" ]]; then
        echo "::error file=$file::P0 recovery startup hook is not byte-pinned."
        bad=1
      else
        echo "EXACT P0 RECOVERY APPLICATION: $file"
      fi
      ;;
    "$prefix/HifzBackup.java")
      if [[ "$(git hash-object "$file")" == "4a8c2ab0b34d4de6b33e8b14809f297373ffdbf1" ]]; then
        echo "EXACT PRE-RELEASE P0 BACKUP: $file"
      else
        echo "::error file=$file::Backup V2 byte hash differs from approved adapter"
        bad=1
      fi
      ;;
    "$prefix/DashboardLedger.java")
      if [[ "$(git hash-object "$file")" == "45363eaa2b7e12211c92f3087cdbe939a31a5730" ]]; then
        echo "EXACT M9-P0: $file"
      else
        echo "::error file=$file::M9-P0 ledger hash differs from approved proof"
        bad=1
      fi
      ;;
    "$prefix/WordLookupAccessPolicy.java")
      if [[ "$(git hash-object "$file")" != "314cb9ae2c4f592b0ad7cf5822c0868939da3772" ]]; then
        echo "::error file=$file::M10 word lookup display-only source differs from the PINNED file 314cb9ae2c4f592b0ad7cf5822c0868939da3772"
        bad=1
      else
        echo "EXACT M10 WORD LOOKUP FOUNDATION: $file"
      fi
      ;;
    "$prefix/WordLookupHitTest.java")
      if [[ "$(git hash-object "$file")" != "4c1f77972ecd30ecd52563d1cdb5d981bda8908f" ]]; then
        echo "::error file=$file::M10 word lookup display-only source differs from the PINNED file 4c1f77972ecd30ecd52563d1cdb5d981bda8908f"
        bad=1
      else
        echo "EXACT M10 WORD LOOKUP FOUNDATION: $file"
      fi
      ;;
    "$prefix/HifzSessionActivity.java" | \
    "$prefix/HifzPrefs.java" | \
    "$prefix/ConsolidationCycleEngine.java" | \
    "$prefix/WeeklyProgramPolicy.java" | \
    "$prefix/WeeklyDashboardPlanner.java" | \
    "$prefix/ItqanRegimeStore.java" | \
    "$prefix/ItqanBalancedMaskSchedule.java" | \
    "$prefix/ItqanCompletionBoundaryPolicy.java" | \
    "$prefix/ItqanFourSessionPolicy.java" | \
    "$prefix/ItqanFourSessionSnapshot.java" | \
    "$prefix/ItqanFourSessionStore.java" | \
    "$prefix/MainActivity.java" | \
    "$prefix/FreeMemActivity.java" | \
    "$prefix/QuranSurahNames.java" | \
    "$prefix/SettingsActivity.java" | \
    "$prefix/StudyReaderActivity.java" | \
    "$prefix/WordGeometryRepository.java" | \
    "$prefix/QuizActivity.java" | \
    "$prefix/QuizCorpus.java" | \
    "$prefix/QuizHistory.java" | \
    "$prefix/QuizQuestion.java" | \
    "$prefix/Ui.java" | \
    "$prefix/IbnKathirGroupIndex.java" | \
    "$prefix/MawdudiPassageIndex.java" | \
    "$prefix/MawdudiMapModel.java" | \
    "$prefix/MawdudiMapData.java" | \
    "$prefix/MawdudiMapScreen.java" | \
    "$prefix/MapJalalaynDialog.java" | \
    "$prefix/MapUnitMarkers.java" | \
    "$prefix/AyahThemesCueIndex.java" | \
    "$prefix/AyahThemesReviewDisplay.java" | \
    "$prefix/PrivateDisplayIntegrity.java" | \
    "$prefix/BaqaraPilotSpec.java" | \
    "$prefix/BaqaraAyahThemeTitles.java" | \
    "$prefix/BaqaraPilotScreen.java" | \
    "$prefix/IbnKathirMapActivity.java" | \
    "$prefix/IbnAshurBaqaraThemes.java" | \
    "$prefix/IbnKathir"*.java | \
    "$prefix/QuranicCue"*.java | \
    "$prefix/QuranicAmorce"*.java | \
    "$prefix/SemanticPassageRepository.java" | \
    "$prefix/SemanticTitlePopup.java")
      echo "ALLOWED UI/DOCUMENTARY (not proof of behavior): $file"
      ;;
    *)
      echo "::error file=$file::Unexpected source change outside the allowed integration surface."
      bad=1 ;;
  esac
done <<< "$other"
[[ "$bad" -eq 0 ]] || exit 1

# The preservation guarantee extends to the 1.17.1 progression and Quiz tests:
# changes to their production logic are forbidden, whereas additive tests are encouraged.
echo "PASS: protected source freeze audited. Runtime and physical-device validation STILL REQUIRED."
