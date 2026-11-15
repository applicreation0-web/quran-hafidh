# Quran Haafidh 1.17.18 — private upgrade, release gate

**Status: CODE-ONLY CANDIDATE; NO APK SIGNED; NO RELEASE PUBLISHED.**

Source: this dedicated release-candidate branch, based on source-qualified commit
`1c70f1786aac50810342bd989749430dce2059c5` (PR #27). Only Android
identity metadata and an offline private builder/check were staged afterward.

## Recovering the real historical signing route

The last unambiguous publicly published, signed GitHub release recovered was
`1.17.1`, workflow
https://github.com/applicreation0-web/quran-hafidh/actions/runs/37824956514
(success, 8 October 2026). It used repository secrets to decode the original
release keystore, JDK 17, Gradle 9.5, tests, `assembleRelease`, `apksigner`
and `gh release create`. The logged signer SHA-256 was
`66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14`.

**This old workflow is NOT suitable for the current private app.**
It does not stage/verify the seven historical private Atlas/Ayah Themes assets.
The current Gradle release packaging gate therefore has not been weakened:
- `AYAH_THEMES_PRIVATE_114=1` and `HIFZ_PRIVATE_RELEASE_UPGRADE=1`
- the **same** release certificate and package `com.quransafeguard.hifz`
- exactly seven rights-scoped files in `hifz-app/src/private114/assets/`,
  checked by SHA-256 (never commit/share these or the signing key)
- 604 original QCF pages and versionCode monotonicity
- no private material uploaded to a public CI/workflow/artifact/release

## What is staged

Version `1.17.18`, `versionCode 56`; no change of package/keystore identity.
`scripts/build_11718_private_offline.py` is a fail-closed LOCAL-only
assembler + verification tool (updated from audited 1.17.17 gate).
`scripts/test_build_11718_private_offline.py` tests wrong versions, signers,
public checkout output paths, and acceptance of a genuine 1.17.17 prior build.
CI for this branch checks code/tests and compiles source **without a private APK**.

## Authorized last-mile procedure

Use a **trusted, offline** checkout at the exact green candidate SHA, with
all private assets in their git-ignored local directory, SDK build-tools 36.0.0,
the ORIGINAL keystore in an external location, and a previously installed
historical APK carrying exactly the same package and certificate.
Check the device's current versionCode before staging; it must be lower than 56.

```bash
# On the trusted/offline signing computer only; do not upload private inputs.
export HIFZ_KEYSTORE_PATH=/private/path/original-release-keystore
export HIFZ_KEYSTORE_PASSWORD='(set locally)'
export HIFZ_KEY_PASSWORD='(set locally)'
export HIFZ_KEY_ALIAS='(set locally)'
python3 scripts/build_11718_private_offline.py \
  --source /private/checkout/quran-hafidh \
  --expected-sha THE_EXACT_40_CHARACTER_GREEN_COMMIT_SHA \
  --historical-apk /private/reference/previous-signed.apk \
  --build-tools /path/to/android-sdk/build-tools/36.0.0 \
  --output /private/release/Quran-Haafidh-1.17.18-private.apk
```

This script verifies the old APK and new candidate via `apksigner` and `aapt`,
checks the seven assets inside the NEW signed APK and writes a private JSON
attestation. It never installs, publishes, uploads or overwrites files.

**Final release requires separate real-device in-place upgrade test, backup
comparison, and signed binary inspection.** Never uninstall or erase current
Hifz progress as a substitute for signature continuity.
