# Quran Haafidh 1.17.10 — private offline release candidate

Status: **SOURCE CI GO / PHYSICAL DEVICE NO-GO**. No installable APK has been delivered.
Private use only. Public GitHub repository: **never attach a signed APK, full private Atlas pack, personal JSON backup, or keystore to GitHub releases, issues, artifacts, or commits**.

## Certified source and tests

- Reviewed application commit: `a50cacff8943a63aa42461f9a3553a5f831caffe` (PR #6, remains draft until delivery gates).
- Global independent validation: [run 41452396191](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41452396191), **6/6 PASS**. BOOX and phone profiles, Hifz preservation, JVM, Tafsir endurance.
- Exact QCF tap / 24:20 boundary reproduced red, corrected and verified green on both DPI profiles: [BOOX 41451183005](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41451183005), [phone 41452806850](https://github.com/applicreation0-web/quran-hafidh/actions/runs/41452806850).
- Preserved Android schema/final/recovery tests: **52/52 PASS**. Tafsir endurance per profile: 200 cycles, 100 rotations, 400 QCF selections, no app crash or ANR observed.
- This staging branch is not a substitute for the primary reviewed PR. Its only additions are private-delivery guards, synthetic tests, and CI workflow; release application binaries must remain identical to the reviewed code.

## Exact installed identity

- Android application ID: `com.quransafeguard.hifz`
- New version: `1.17.10`, versionCode `48`
- Historical APK signing certificate SHA-256: `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14`
- Supported signed historical APK references for offline validation: 1.17.4/code42, 1.17.8/code46, 1.17.9/code47. Actual installed package and certificate must be inspected on BOOX before update.
- Seven locally held, exact-hash private resources (Atlas/ayah themes), plus the 604 genuine QCF pages, must be present. Never substitute missing content.

## Build and validation path

1. Confirm the source-only RC CI passes and the reviewed application sources match the pinned candidate. **Unsigned code-only CI artifacts are NOT installable**.
2. On a trusted OFFLINE machine, with the seven private files at `hifz-app/src/private114/assets/`, complete 604 original QCF pages, the historical signer variables, Gradle offline dependencies and Android SDK build-tools 36:
   `python3 scripts/build_11710_private_offline.py --source <clean-checked-out-RC-root> --expected-sha <exact-RC-head-sha> --historical-apk <signed-historical-APK> --build-tools <android-sdk/build-tools/36.0.0> --output <external-private-path>/quran-haafidh-1.17.10-private-signed.apk`
3. **Do not** run this builder on GitHub Actions or upload the built APK. Builder checks: clean exact SHA, private SHA-256 hashes, historical APK package/certificate, signed version48/versionName1.17.10, APK v2 historical signing, seven package resources, 604 QCF pages.
4. Save a current BOOX Hifz JSON backup externally and calculate its SHA-256; do not edit the original, import artificial credits, or overwrite the backup.
5. Inspect the actual BOOX installation identity and signing certificate; if different or inaccessible, **NO-GO**. Install only as an in-place upgrade, never uninstall/clear data.
6. Inspect Sabqi, Itqān, finals, recent and old validated sessions, time tracking, masked reading, QCF/Atlas/Tafsir, and export a fresh backup to compare progression and credits. If any mismatch, stop and retain backup.
7. Only then authorize the user-specific private release/delivery. A public GitHub release asset would be publicly accessible and is prohibited.

## Explicit remaining gates

- Exact seven private assets and their local hashes: **not checked in this GitHub run**.
- Existing installation certificate and signed binary upgrade: **not checked on physical BOOX**.
- Physical BOOX E-Ink refresh, touch and layout: **not checked**.
- Personal progress post-install audit: **not checked**.
- Private signed APK build/delivery: **not performed**.

Do not mark these unchecked gates PASS based on emulator screenshots.
