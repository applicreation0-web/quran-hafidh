#!/usr/bin/env bash
# Quran Haafidh 1.17.1 — Ibn Kathir: fail-closed engine freeze.
# Reference = last green BOOX branch BEFORE documentary migration.
set -euo pipefail
BASELINE=0b70b2b34aca9334680ad8f282273706cf868313
APP_ROOT=hifz-app/src/main/java/com/quransafeguard/hifz/preview

git cat-file -e "${BASELINE}^{commit}" || {
  echo "::error::Baseline commit unavailable; check out full history (fetch-depth: 0)."
  exit 1
}
changed="$(git diff --name-only --no-renames "${BASELINE}" HEAD -- hifz-core "$APP_ROOT" hifz-app/src/main/assets/hifzreader)"
if [ -z "$changed" ]; then
  echo "PASS: Frozen Hifz core, session engines and renderer unchanged."
  exit 0
fi

bad=0
while IFS= read -r file; do
  [ -n "$file" ] || continue
  case "$file" in
    # ONLY screen/UI and documentary adapters may be modified.
    hifz-app/src/main/assets/hifzreader/reader.js)
      echo "REVIEW ALLOWED (immutable renderer delta): $file"
      ;;
    "$APP_ROOT/HifzRecoveryApplication.java")
      if [ "$(git hash-object "$file")" != "d198dc2ac398b68f48032f7b4f50ae4fee1b5d8f" ]; then
        echo "::error file=$file::P0 recovery startup hook differs from approved byte-pinned source."
        bad=1
      else
        echo "EXACT P0 RECOVERY APPLICATION: $file"
      fi
      ;;
    "$APP_ROOT/HifzBackup.java")
      if [ "$(git hash-object "$file")" != "4a8c2ab0b34d4de6b33e8b14809f297373ffdbf1" ]; then
        echo "::error file=$file::Pre-release backup V2 is not the audited byte-pinned adapter."
        bad=1
      else
        echo "EXACT PRE-RELEASE P0 BACKUP: $file"
      fi
      ;;
    "$APP_ROOT/DashboardLedger.java")
      if [ "$(git hash-object "$file")" != "45363eaa2b7e12211c92f3087cdbe939a31a5730" ]; then
        echo "::error file=$file::M9-P0 completion ledger differs from the exact tested source."
        bad=1
      else
        echo "EXACT M9-P0 ONLY: $file"
      fi
      ;;
    "$APP_ROOT/WordLookupAccessPolicy.java")
      if [ "$(git hash-object "$file")" != "314cb9ae2c4f592b0ad7cf5822c0868939da3772" ]; then
        echo "::error file=$file::M10 word lookup display-only source differs from the PINNED file 314cb9ae2c4f592b0ad7cf5822c0868939da3772"
        bad=1
      else
        echo "EXACT M10 WORD LOOKUP FOUNDATION: $file"
      fi
      ;;
    "$APP_ROOT/WordLookupHitTest.java")
      if [ "$(git hash-object "$file")" != "4c1f77972ecd30ecd52563d1cdb5d981bda8908f" ]; then
        echo "::error file=$file::M10 word lookup display-only source differs from the PINNED file 4c1f77972ecd30ecd52563d1cdb5d981bda8908f"
        bad=1
      else
        echo "EXACT M10 WORD LOOKUP FOUNDATION: $file"
      fi
      ;;
    "$APP_ROOT/AtlasPrivateV2.java")
      if [ "$(git hash-object "$file")" != "955304414fe110092fad800263f6a4f44fb870af" ]; then
        echo "::error file=$file::Atlas 366 source index adapter differs from exact reviewed source."
        bad=1
      else
        echo "EXACT CP09 ATLAS DOCUMENTARY INDEX: $file"
      fi
      ;;
    "$APP_ROOT/AtlasSourcePdfActivity.java")
      if [ "$(git hash-object "$file")" != "e5635309af326d1e073c47bc50e0e5fc498e3d05" ]; then
        echo "::error file=$file::Atlas offline source reader differs from exact reviewed source."
        bad=1
      else
        echo "EXACT CP09 ATLAS SOURCE READER: $file"
      fi
      ;;
    "$APP_ROOT/HifzSessionActivity.java" | \
    "$APP_ROOT/HifzPrefs.java" | \
    "$APP_ROOT/ConsolidationCycleEngine.java" | \
    "$APP_ROOT/WeeklyProgramPolicy.java" | \
    "$APP_ROOT/WeeklyDashboardPlanner.java" | \
    "$APP_ROOT/ItqanRegimeStore.java" | \
    "$APP_ROOT/ItqanBalancedMaskSchedule.java" | \
    "$APP_ROOT/ItqanCompletionBoundaryPolicy.java" | \
    "$APP_ROOT/ItqanFourSessionPolicy.java" | \
    "$APP_ROOT/ItqanFourSessionSnapshot.java" | \
    "$APP_ROOT/ItqanFourSessionStore.java" | \
    "$APP_ROOT/MainActivity.java" | \
    "$APP_ROOT/FreeMemActivity.java" | \
    "$APP_ROOT/QuranSurahNames.java" | \
    "$APP_ROOT/SettingsActivity.java" | \
    "$APP_ROOT/Ui.java" | \
    "$APP_ROOT/StudyReaderActivity.java" | \
    "$APP_ROOT/WordGeometryRepository.java" | \
    "$APP_ROOT/QuizActivity.java" | \
    "$APP_ROOT/QuizCorpus.java" | \
    "$APP_ROOT/QuizHistory.java" | \
    "$APP_ROOT/QuizQuestion.java" | \
    "$APP_ROOT/SemanticPassageRepository.java" | \
    "$APP_ROOT/SemanticTitlePopup.java" | \
    "$APP_ROOT/MawdudiPassageIndex.java" | \
    "$APP_ROOT/MawdudiMapModel.java" | \
    "$APP_ROOT/MawdudiMapData.java" | \
    "$APP_ROOT/MawdudiMapScreen.java" | \
    "$APP_ROOT/MapJalalaynDialog.java" | \
    "$APP_ROOT/MapUnitMarkers.java" | \
    "$APP_ROOT/MapQcfThumbnails.java" | \
    "$APP_ROOT/CorrespondenceSvgFocus.java" | \
    "$APP_ROOT/CorrespondenceModel.java" | \
    "$APP_ROOT/CorrespondenceGeometry.java" | \
    "$APP_ROOT/CorrespondenceDocument.java" | \
    "$APP_ROOT/CorrespondenceQcfSurface.java" | \
    "$APP_ROOT/CorrespondenceScreen.java" | \
    "$APP_ROOT/CorrespondenceSwipe.java" | \
    "$APP_ROOT/CorrespondenceAccess.java" | \
    "$APP_ROOT/CorrespondenceState.java" | \
    "$APP_ROOT/CorrespondenceStateViewport.java" | \
    "$APP_ROOT/CorrespondenceCatalog.java" | \
    "$APP_ROOT/CorrespondenceRuntime.java" | \
    "$APP_ROOT/CorrespondenceEntryPoints.java" | \
    "$APP_ROOT/CorrespondenceActivity.java" | \
    "$APP_ROOT/AyahThemesCueIndex.java" | \
    "$APP_ROOT/AyahThemesReviewDisplay.java" | \
    "$APP_ROOT/PrivateDisplayIntegrity.java" | \
    "$APP_ROOT/BaqaraPilotSpec.java" | \
    "$APP_ROOT/BaqaraAyahThemeTitles.java" | \
    "$APP_ROOT/BaqaraPilotScreen.java" | \
    "$APP_ROOT/IbnKathirMapActivity.java" | \
    "$APP_ROOT/IbnAshurBaqaraThemes.java" | \
    "$APP_ROOT/IbnKathirGroupIndex.java" | \
    "$APP_ROOT/IbnKathir"*.java | \
    "$APP_ROOT/QuranicCue"*.java | \
    "$APP_ROOT/QuranicAmorce"*.java)
      echo "REVIEW ALLOWED (UI/documentary only): $file"
      ;;
    *)
      echo "::error file=$file::Protected Hifz file changed versus immutable 1.17.1 BOOX baseline."
      bad=1
      ;;
  esac
done <<< "$changed"

if [ "$bad" -ne 0 ]; then
  echo "::error::Freeze violated. Do not modify Hifz engines, persistence, scheduler, audio, quiz logic, masks or reader renderer."
  exit 1
fi

python3 scripts/verify_public_amorce_keys.py
python3 scripts/verify_authorized_quiz_reader_delta.py
python3 scripts/verify_authorized_word_geometry_quiz_detach.py
python3 scripts/verify_itqan_four_session_plan_contract.py
python3 scripts/verify_itqan_rotation_atomicity.py
python3 scripts/verify_authorized_hifz_session_delta.py
python3 scripts/verify_authorized_program_preferences_delta.py
python3 scripts/verify_authorized_consolidation_delta.py
echo "PASS: core/renderer frozen; HifzSessionActivity contains only the byte-verified fail-open delta."
echo "IMPORTANT: passing file freeze does NOT prove UI/adapters cannot alter behavior; run full tests."
