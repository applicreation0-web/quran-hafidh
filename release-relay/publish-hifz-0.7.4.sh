#!/usr/bin/env bash
set -euo pipefail
set +x
umask 077

TARGET_SHA='75d35aaf3f46877b4f6b65fe1c276cc7b3fba1a6'
SOURCE_RUN='34894587724'
SOURCE_ARTIFACT='quran-hifz-0.7.4-preboox-unsigned-75d35aaf3f46877b4f6b65fe1c276cc7b3fba1a6'
UNSIGNED_SHA256='7f499c75a313232ea2c400d9efb0139af1eca36da0d3b8e3326e1b4c4b3a6416'
CERT_SHA256='6843751077a6f26f1b99648d1b17b6eb1c577fd67ee55573f27b326c60e752b8'
RELAY_RUN='34925657637'
RELAY_ARTIFACT='quran-hifz-relay-private-34925657637'
RELAY_ARTIFACT_ID='10380042089'
TAG='v0.7.4-boox'
OUT_APK='Quran-Hifz-0.7.4-boox-release.apk'
SHA_FILE='Quran-Hifz-0.7.4-boox-release.sha256'
REPORT='Quran-Hifz-0.7.4-boox-FINAL-RELEASE-REPORT.txt'
REPO="${GITHUB_REPOSITORY:?}"

cleanup() {
  set +e
  rm -rf relay relay-private ci-artifact unsigned.apk "$OUT_APK" "$SHA_FILE" "$REPORT" RELEASE_NOTES.md apksigner-verify.txt apk-badging.txt published release.json
  gh api --method DELETE "repos/${REPO}/actions/artifacts/${RELAY_ARTIFACT_ID}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# Never overwrite an existing final tag/release.
if gh api "repos/${REPO}/git/ref/tags/${TAG}" >/dev/null 2>&1; then
  echo "Tag already exists: ${TAG}" >&2
  exit 1
fi
if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  echo "Release already exists: ${TAG}" >&2
  exit 1
fi

mkdir -p relay-private ci-artifact relay/plain

gh run download "$RELAY_RUN" --repo "$REPO" --name "$RELAY_ARTIFACT" --dir relay-private
gh run download "$SOURCE_RUN" --repo "$REPO" --name "$SOURCE_ARTIFACT" --dir ci-artifact

mapfile -t apks < <(find ci-artifact -type f -name '*.apk' -print)
test "${#apks[@]}" = 1
printf '%s  %s\n' "$UNSIGNED_SHA256" "${apks[0]}" | sha256sum -c -
cp "${apks[0]}" unsigned.apk

sed -n 's/^BUNDLE_B64=//p' release-relay/hifz-signing-payload.env | base64 -d > relay/bundle.enc
sed -n 's/^HMAC_B64=//p' release-relay/hifz-signing-payload.env | base64 -d > relay/bundle.hmac
sed -n 's/^KEYMAT_B64=//p' release-relay/hifz-signing-payload.env | base64 -d > relay/keymat.enc
IV="$(sed -n 's/^IV=//p' release-relay/hifz-signing-payload.env)"
test -n "$IV"

test -s relay-private/relay-private.pem
openssl pkeyutl -decrypt -inkey relay-private/relay-private.pem \
  -pkeyopt rsa_padding_mode:oaep \
  -pkeyopt rsa_oaep_md:sha256 \
  -pkeyopt rsa_mgf1_md:sha256 \
  -in relay/keymat.enc -out relay/keymat.bin
test "$(stat -c '%s' relay/keymat.bin)" = 64

KEYHEX="$(od -An -tx1 -v relay/keymat.bin | tr -d ' \n')"
AES_KEY="${KEYHEX:0:64}"
MAC_KEY="${KEYHEX:64:64}"
openssl dgst -sha256 -mac HMAC -macopt "hexkey:${MAC_KEY}" -binary relay/bundle.enc > relay/bundle.actual.hmac
cmp relay/bundle.hmac relay/bundle.actual.hmac

openssl enc -d -aes-256-cbc -K "$AES_KEY" -iv "$IV" -in relay/bundle.enc -out relay/bundle.tar.gz -nosalt
tar -xzf relay/bundle.tar.gz -C relay/plain
test -s relay/plain/Quran-Hifz-release.jks
test -s relay/plain/password.txt
KS_PASS="$(tr -d '\r\n' < relay/plain/password.txt)"
test -n "$KS_PASS"

keytool -list -keystore relay/plain/Quran-Hifz-release.jks -storepass "$KS_PASS" -alias quran-hifz >/dev/null
KEY_CERT="$(keytool -list -v -keystore relay/plain/Quran-Hifz-release.jks -storepass "$KS_PASS" -alias quran-hifz | sed -n 's/.*SHA256: //p' | head -1 | tr '[:upper:]' '[:lower:]' | tr -d ':')"
test "$KEY_CERT" = "$CERT_SHA256"

TOOLS="$ANDROID_HOME/build-tools/36.0.0"
test -x "$TOOLS/apksigner"
test -x "$TOOLS/zipalign"
test -x "$TOOLS/aapt2"

"$TOOLS/zipalign" -c -p 4 unsigned.apk
"$TOOLS/apksigner" sign \
  --ks relay/plain/Quran-Hifz-release.jks \
  --ks-key-alias quran-hifz \
  --ks-pass "pass:$KS_PASS" \
  --key-pass "pass:$KS_PASS" \
  --v1-signing-enabled false \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --v4-signing-enabled false \
  --out "$OUT_APK" unsigned.apk

"$TOOLS/zipalign" -c -p 4 "$OUT_APK"
"$TOOLS/apksigner" verify --verbose --print-certs "$OUT_APK" | tee apksigner-verify.txt
grep -Fq 'Verified using v1 scheme (JAR signing): false' apksigner-verify.txt
grep -Fq 'Verified using v2 scheme (APK Signature Scheme v2): true' apksigner-verify.txt
grep -Fq 'Verified using v3 scheme (APK Signature Scheme v3): true' apksigner-verify.txt
grep -Fq 'Number of signers: 1' apksigner-verify.txt
ACTUAL_CERT="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' apksigner-verify.txt | head -1 | tr '[:upper:]' '[:lower:]' | tr -d ':')"
test "$ACTUAL_CERT" = "$CERT_SHA256"

"$TOOLS/aapt2" dump badging "$OUT_APK" | tee apk-badging.txt
grep -Fq "package: name='com.quransafeguard.hifz' versionCode='11' versionName='0.7.4-boox'" apk-badging.txt

python3 - <<'PY'
import hashlib, zipfile
u = zipfile.ZipFile('unsigned.apk', 'r')
s = zipfile.ZipFile('Quran-Hifz-0.7.4-boox-release.apk', 'r')
un = u.namelist(); sn = s.namelist()
assert len(un) == 668, f'expected 668 unsigned entries, got {len(un)}'
assert un == sn, 'ZIP entry name/order set changed during signing'
for name in un:
    ui = u.getinfo(name); si = s.getinfo(name)
    assert (ui.CRC, ui.file_size, ui.compress_size, ui.compress_type) == (si.CRC, si.file_size, si.compress_size, si.compress_type), f'ZIP metadata changed: {name}'
    assert hashlib.sha256(u.read(name)).digest() == hashlib.sha256(s.read(name)).digest(), f'payload bytes changed: {name}'
print('PAYLOAD_IDENTITY=668/668 PASS')
PY

SIGNED_SHA="$(sha256sum "$OUT_APK" | awk '{print $1}')"
test -n "$SIGNED_SHA"
echo "SIGNED_APK_SHA256=$SIGNED_SHA"
printf '%s  %s\n' "$SIGNED_SHA" "$OUT_APK" > "$SHA_FILE"

cat > "$REPORT" <<EOF
Quran Hifz 0.7.4 BOOX — FINAL GITHUB RELEASE REPORT
Repository: ${REPO}
Target SHA: ${TARGET_SHA}
Source CI run: ${SOURCE_RUN}
Source artifact: ${SOURCE_ARTIFACT}
Unsigned SHA-256: ${UNSIGNED_SHA256}
Package: com.quransafeguard.hifz
versionCode: 11
versionName: 0.7.4-boox
Signing certificate SHA-256: ${CERT_SHA256}
APK Signature Scheme v1: false
APK Signature Scheme v2: verified
APK Signature Scheme v3: verified
ZIP payload identity: 668/668 entries byte-identical unsigned -> signed
Final APK: ${OUT_APK}
Final SHA-256: ${SIGNED_SHA}
CI: 104/104 JVM, 49/49 source contracts, 61/61 instrumentation, process-death PASS
Release tag targets ${TARGET_SHA}.
Signing material was transported only through an ephemeral encrypted relay; no plaintext key or password was committed.
EOF

cat > RELEASE_NOTES.md <<EOF
## Quran Hifz 0.7.4 — BOOX

Candidat final validé après audit indépendant et CI complet.

- candidat final : \`${TARGET_SHA}\`
- CI officiel final : \`${SOURCE_RUN}\`
- tests JVM : 104/104
- contrats source : 49/49
- instrumentation Android : 61/61
- force-stop / relance : PASS
- package : \`com.quransafeguard.hifz\`
- versionName : \`0.7.4-boox\`
- versionCode : \`11\`
- certificat historique Hifz : \`${CERT_SHA256}\`
- signatures APK v2/v3 : vérifiées
- payload applicatif : 668/668 entrées identiques au candidat CI unsigned
- SHA-256 APK publié : \`${SIGNED_SHA}\`

Version destinée au test physique BOOX final. Le pack audio reste séparé et local.
EOF

gh release create "$TAG" "$OUT_APK" "$SHA_FILE" "$REPORT" \
  --repo "$REPO" \
  --target "$TARGET_SHA" \
  --title 'Quran Hifz 0.7.4 — BOOX' \
  --notes-file RELEASE_NOTES.md

mkdir published
gh release download "$TAG" --repo "$REPO" --pattern "$OUT_APK" --dir published
printf '%s  %s\n' "$SIGNED_SHA" "published/$OUT_APK" | sha256sum -c -
gh release view "$TAG" --repo "$REPO" --json tagName,targetCommitish,isDraft,isPrerelease,url > release.json
jq -e --arg tag "$TAG" --arg target "$TARGET_SHA" '.tagName==$tag and .targetCommitish==$target and .isDraft==false and .isPrerelease==false' release.json >/dev/null
cat release.json

echo "PUBLICATION_OK tag=$TAG sha=$SIGNED_SHA target=$TARGET_SHA"
