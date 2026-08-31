#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/mushaf/hafs/kfqc/svg"
MUSHAF_REPO="https://github.com/quranpedia/quran-svg.git"
MUSHAF_COMMIT="1b427fab77aae1403fe7e1f0b8c794a5384d5605"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

git -C "$WORK" init -q
git -C "$WORK" remote add origin "$MUSHAF_REPO"
git -C "$WORK" sparse-checkout init --cone
git -C "$WORK" sparse-checkout set mushafs/hafs/kfqc/svg
git -C "$WORK" fetch -q --depth 1 --filter=blob:none origin "$MUSHAF_COMMIT"
git -C "$WORK" checkout -q --detach FETCH_HEAD

rm -rf "$DEST"
mkdir -p "$DEST"

for page in $(seq 1 604); do
    name="$(printf '%03d' "$page")"
    src="$WORK/mushafs/hafs/kfqc/svg/$name.svg"
    test -f "$src"
    cp "$src" "$DEST/$name.svg"
done

count="$(find "$DEST" -maxdepth 1 -type f -name '[0-9][0-9][0-9].svg' | wc -l | tr -d ' ')"
test "$count" = "604"

verify_blob() {
    local page="$1"
    local expected="$2"
    local actual
    actual="$(git hash-object "$DEST/$page.svg")"
    if [ "$actual" != "$expected" ]; then
        echo "Mushaf integrity check failed for page $page" >&2
        echo "expected: $expected" >&2
        echo "actual:   $actual" >&2
        exit 1
    fi
}

verify_blob "001" "d250c1c97d3f9669f9559ec8a4106328b20ce784"
verify_blob "302" "2763720f80c003a64d4fd78d72b3ddb07ba58ec4"
verify_blob "604" "2616984354c805b909ebcb7d3dfd012575a527fe"

echo "Verified Medina Mushaf Hafs/KFQC: 604 SVG pages at $MUSHAF_COMMIT"
