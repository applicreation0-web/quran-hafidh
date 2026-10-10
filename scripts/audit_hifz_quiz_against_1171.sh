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
  hifz-app/src/main/assets/hifzreader
  app/src/main/assets/reader109/geometry.json
  app/src/main/assets/reader109/waqf.json
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/AnchoringQueue.java
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizActivity.java
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizCorpus.java
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizQuestion.java
  hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizHistory.java
)
changed="$(git diff --name-status "${BASE}" HEAD -- "${protected[@]}")"
if [[ -n "$changed" ]]; then
  echo "::error::Official v1.17.1 Hifz/Quiz protected file(s) differ:"
  printf '%s\n' "$changed"
  exit 1
fi
echo "PASS: all core, progress, session, anchoring, renderer, Quran geometry and Quiz source identical to 1.17.1."

# Also freeze the rest of application logic: only explicitly whitelisted UI/documentary
# Java controllers may diverge. Any unexpected Java change is a regression risk.
prefix=hifz-app/src/main/java/com/quransafeguard/hifz/preview
other="$(git diff --name-only --no-renames "${BASE}" HEAD -- "$prefix" hifz-core)"
bad=0
while IFS= read -r file; do
  [[ -n "$file" ]] || continue
  case "$file" in
    "$prefix/MainActivity.java" | \
    "$prefix/StudyReaderActivity.java" | \
    "$prefix/Ui.java" | \
    "$prefix/IbnKathirGroupIndex.java" | \
    "$prefix/IbnKathirMapActivity.java" | \
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
