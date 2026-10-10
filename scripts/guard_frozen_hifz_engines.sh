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
    "$APP_ROOT/MainActivity.java" | \
    "$APP_ROOT/Ui.java" | \
    "$APP_ROOT/StudyReaderActivity.java" | \
    "$APP_ROOT/SemanticPassageRepository.java" | \
    "$APP_ROOT/SemanticTitlePopup.java" | \
    "$APP_ROOT/IbnKathirMapActivity.java" | \
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

echo "PASS: no engine/core/renderer file was changed."
echo "IMPORTANT: passing file freeze does NOT prove UI/adapters cannot alter behavior; run full tests."
