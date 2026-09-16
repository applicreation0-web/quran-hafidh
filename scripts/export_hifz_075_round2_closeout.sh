#!/usr/bin/env bash
set -euo pipefail

FINAL=7e44ad133d46c0cca45bd5f60dffc74e130fdfe7
BASE7185=7185a9ca92dfa46c10f63b0e5c499dce610dd720
BASE5696=5696e7e8b91d213deaaa40c7b4893965cf46d4e4
FULL_RUN=35104620315

json="$(curl -fsSL -H "Authorization: Bearer $GH_TOKEN" -H 'Accept: application/vnd.github+json' \
  "https://api.github.com/repos/applicreation0-web/quran-unlock-android/actions/runs/${FULL_RUN}")"
status="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])' <<<"$json")"
conclusion="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("conclusion"))' <<<"$json")"
echo "full_gate=$status/$conclusion"
test "$status" = completed
test "$conclusion" = success

git cat-file -e "$FINAL^{commit}"
git worktree add --detach /tmp/hifz-final "$FINAL"
test "$(git -C /tmp/hifz-final rev-parse HEAD)" = "$FINAL"

mkdir -p /tmp/baseline074 /tmp/baseline074/extracted
curl -fL -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/octet-stream" \
  https://api.github.com/repos/applicreation0-web/quran-unlock-android/releases/assets/564818126 \
  -o /tmp/baseline074/hifz074.apk
echo 'c0007102e2e2f8ef69f303f141d148b416ae5f312a354f3f0a23fc2a70995ccc  /tmp/baseline074/hifz074.apk' | sha256sum -c -
unzip -q /tmp/baseline074/hifz074.apk 'assets/mushaf/*' 'assets/reader109/geometry.json' 'assets/tafsir/*' -d /tmp/baseline074/extracted
rm -rf /tmp/hifz-final/app/src/main/assets/mushaf /tmp/hifz-final/app/src/main/assets/reader109
mkdir -p /tmp/hifz-final/app/src/main/assets /tmp/hifz-final/app/src/main/assets/reader109
cp -a /tmp/baseline074/extracted/assets/mushaf /tmp/hifz-final/app/src/main/assets/
cp /tmp/baseline074/extracted/assets/reader109/geometry.json /tmp/hifz-final/app/src/main/assets/reader109/geometry.json
rm -rf /tmp/hifz-final/hifz-app/build/generated/hifzTafsir
mkdir -p /tmp/hifz-final/hifz-app/build/generated/hifzTafsir
cp -a /tmp/baseline074/extracted/assets/tafsir/. /tmp/hifz-final/hifz-app/build/generated/hifzTafsir/

cd /tmp/hifz-final
gradle --no-daemon :hifz-app:assembleRelease -x :hifz-app:prepareHifzTafsirRelease --stacktrace
APK="$(find hifz-app/build/outputs/apk/release -maxdepth 1 -name '*.apk' -print -quit)"
test -n "$APK" && test -s "$APK"
cp "$APK" /tmp/Quran-Hifz-0.7.5-boox-round2-release-unsigned.apk
"$ANDROID_HOME/build-tools/36.0.0/aapt" dump badging "$APK" > /tmp/APK_BADGING.txt
grep -F "package: name='com.quransafeguard.hifz' versionCode='12' versionName='0.7.5-boox'" /tmp/APK_BADGING.txt
if "$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose "$APK" > /tmp/APKSIGNER.txt 2>&1; then
  echo 'SIGNED=true' > /tmp/APK_SIGNATURE_STATUS.txt
else
  echo 'SIGNED=false' > /tmp/APK_SIGNATURE_STATUS.txt
fi
sha256sum /tmp/Quran-Hifz-0.7.5-boox-round2-release-unsigned.apk > /tmp/APK_SHA256.txt

ROOT=/tmp/CLAUDE_QURAN_HIFZ_0.7.5_ROUND2_AUTONOMOUS_7e44ad13
rm -rf "$ROOT"
mkdir -p "$ROOT/SOURCE_HEAD" "$ROOT/GEOMETRY" "$ROOT/DIFFS" "$ROOT/CI"
cp -a /tmp/hifz-final/hifz-app "$ROOT/SOURCE_HEAD/hifz-app"
rm -rf "$ROOT/SOURCE_HEAD/hifz-app/build"
cp -a /tmp/hifz-final/hifz-core "$ROOT/SOURCE_HEAD/hifz-core"
rm -rf "$ROOT/SOURCE_HEAD/hifz-core/build"
cp /tmp/hifz-final/settings.gradle.kts "$ROOT/SOURCE_HEAD/"
cp /tmp/hifz-final/gradle.properties "$ROOT/SOURCE_HEAD/"
cp /tmp/hifz-final/app/src/main/assets/reader109/geometry.json "$ROOT/GEOMETRY/geometry.json"
git -C /tmp/hifz-final diff "$BASE7185..$FINAL" > "$ROOT/DIFFS/ROUND2_7185_TO_FINAL.patch"
git -C /tmp/hifz-final diff --name-status "$BASE7185..$FINAL" > "$ROOT/DIFFS/ROUND2_FILES.txt"
git -C /tmp/hifz-final diff "$BASE5696..$FINAL" > "$ROOT/DIFFS/FULL_5696_TO_FINAL.patch"

cat > "$ROOT/00_READ_ME_FIRST.txt" <<EOF
QURAN HIFZ 0.7.5 — ROUND2 AUTONOMOUS RE-AUDIT KIT
HEAD exact: $FINAL
Snapshot branch: audit/final-hifz-0.7.5-round2-7e44ad13
Parent audit candidate: $BASE7185
Original audited candidate: $BASE5696
Baseline release: f344fc7d0313e4e327a53c0fe6da8f287d12bdac
Full gate run: $FULL_RUN
This kit is self-contained. No GitHub/network is required.
EOF

cat > "$ROOT/01_PROMPT_CLAUDE.txt" <<'EOF'
MISSION CLAUDE — FINAL ROUND2 RE-AUDIT QURAN HIFZ 0.7.5
Audit only exact HEAD 7e44ad133d46c0cca45bd5f60dffc74e130fdfe7 from this kit. No network.

Re-check:
B1' ownerless verse/page portions: no Home crash, no queue poison, no Consolidation crash; invalid manual ownerless Stabilisation rejected.
M4 daily Consolidation gate: after today's completed Consolidation, Home yields to due cadence unless an OPEN session must resume.
M5 ranges: merge same-surah adjacency; do not merge adjacency across surah; do not explode a stored cross-surah range; union coverage can span fragments.
BLOCKING MIGRATION ZERO-LOSS: compatible schema5 open Stabilisation that remains one physical unit and has completedBlocks=0 preserves itqanRep/itqanAssisted/itqanFinalReveals/itqanElapsedMs. Incompatible remap (e.g. legacy 5/5/5 to physical 7+8) keeps safe reset/re-index and never invents ACQUIRED.

Re-check invariants: physical lineIds, no PAGE/SURAH crossing per unit, page48 2:282 = 15 lines => 7+8, U1 never acquires U2, exact process-death Consolidation persistence, J10 only ACQUIRED, no out-of-scope Round2 audio/Tafsir/BOOX/vector/J10-engine changes.

CI/VERIFIED_RESULTS.txt is context only; independently inspect sources/tests/geometry.
Return concise:
HEAD audité: <sha>
BLOQUANT: <none/findings>
MAJEUR: <none/findings>
MINEUR: <only useful>
Migration zéro-perte: OK/NO
B1': OK/NO
M4: OK/NO
M5: OK/NO
Page48 7+8: OK/NO
Process death: OK/NO
Hors scope: OK/NO
Verdict: AUDIT CLAUDE GREEN or AUDIT CLAUDE NO-GO
No merge/release/signature. No GO_PHONE.
EOF

cat > "$ROOT/CI/VERIFIED_RESULTS.txt" <<EOF
Exact HEAD: $FINAL
Full run: $FULL_RUN
Behavior JVM: 166/166, failures=0 errors=0 skipped=0
Source contracts: 71/71, failures=0 errors=0 skipped=0
Core JVM: 20/20, failures=0 errors=0 skipped=0
Compile Android test sources: GREEN
Product boundary: GREEN
Cosmetic verifier: GREEN
Convergence verifier: GREEN
Android instrumented API35 x86_64 Pixel6: 45/45, failures=0 skipped=0
Migration RED before fix: run 35103834954 expected itqanRep=9, actual=0 for compatible 86:1-86:17.
Migration GREEN after fix: included in 45/45 final matrix.
User-supplied Claude patch SHA256: 3ec9cbdc970d4ffcf22419c2b0b419f671fa58738c377bb005aaca737eab829b
EOF
cp /tmp/APK_BADGING.txt "$ROOT/CI/APK_BADGING.txt"
cp /tmp/APK_SIGNATURE_STATUS.txt "$ROOT/CI/APK_SIGNATURE_STATUS.txt"
cp /tmp/APK_SHA256.txt "$ROOT/CI/APK_SHA256.txt"

cd "$ROOT"
find . -type f ! -name SHA256SUMS.txt -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS.txt
cd /tmp
zip -qr CLAUDE_QURAN_HIFZ_0.7.5_ROUND2_AUTONOMOUS_7e44ad13.zip "$(basename "$ROOT")"
sha256sum CLAUDE_QURAN_HIFZ_0.7.5_ROUND2_AUTONOMOUS_7e44ad13.zip > CLAUDE_ROUND2_ZIP_SHA256.txt

echo CLOSEOUT_OK
