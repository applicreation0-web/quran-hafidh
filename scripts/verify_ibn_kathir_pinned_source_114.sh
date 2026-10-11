#!/usr/bin/env bash
# Verify original English title candidates against the precise upstream 114
# files WITHOUT committing third-party commentary or shipping it in the APK.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SHA="eb82bb6294efe30ad5c135c03b1864afaa70e855"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

download() {
  local chapter="$1" tmp="$2" sha="$3"
  curl --fail --location --silent --show-error --retry 3 --retry-delay 2 \
    "https://raw.githubusercontent.com/spa5k/tafsir_api/$sha/tafsir/en-tafisr-ibn-kathir/$chapter.json" \
    --output "$tmp/$chapter.json"
  test -s "$tmp/$chapter.json"
}
export -f download
seq 1 114 | xargs -P 8 -I{} bash -c 'download "$1" "$2" "$3"' _ {} "$TMP" "$SHA"
python3 "$ROOT/scripts/audit_ibn_kathir_title_pack.py" \
  --source-dir "$TMP" --out "$TMP/source_headings_review.csv"
echo "PASS: 114 pinned original English source files; 1,903 groups; per-title verbatim provenance"
echo "NOTE: VERBATIM SOURCE MATCH IS NOT EDITORIAL APPROVAL OR RIGHTS CLEARANCE"
