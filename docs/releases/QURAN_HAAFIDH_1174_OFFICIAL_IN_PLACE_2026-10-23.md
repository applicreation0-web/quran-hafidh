# Quran Haafidh 1.17.4 — verified private in-place release — 23 October 2026

**Private delivery via ChatGPT only. This public GitHub repository must not carry the private Atlas files or official signing key.**

- Final APK filename: `QURAN_HAAFIDH_BOOX_1.17.4_ATLAS_RELEASE_MAJ_1171_SIGNEE.apk`
- APK SHA-256: `0c84025313cd8eec404b4ea2b3cac70550b292c5a356f878df1cc7657a8d06dd`
- APK bytes: **91,778,218**.
- Android package: `com.quransafeguard.hifz`; label: `Quran Haafidh`.
- Version: `1.17.4`, code `42`; official GitHub baseline `1.17.1`, code `39`.
- Identical verified RSA-4096 signer SHA-256 for BOTH APKs: `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14`.
- Android APK Signature Scheme v2: **PASS** on baseline and final. v3: absent on baseline and final (v2 valid for minSdk 26).
- ZIP alignment: **PASS**.
- Seven approved private Atlas resources: **7/7 SHA-256 checked locally before and after insertion**; deferred Correspondances not included.
- Real source commit: `8e7b132d7e075bc3e6f8f1eb6216b48e7178c95e`.
- Android instrumented tests: **26/26 PASS**, GitHub Actions: https://github.com/applicreation0-web/quran-hafidh/actions/runs/39949327953
- Local offline private-Atlas resource staging build: **PASS**, GitHub Actions (no private files): https://github.com/applicreation0-web/quran-hafidh/actions/runs/39965893762
- Remote signing: GitHub Actions **only signed the v2 signedData hash + public certificate**, not the private APK: https://github.com/applicreation0-web/quran-hafidh/actions/runs/39966526633
- The temporary digest-signing workflow and challenge file were removed after successful signing.
- The actual in-place installation and BOOX E-Ink ghosting must be validated on the user's physical device.

The user's former `Quran Haafidh Privé 114` is a **different appId** and must not be used for the in-place upgrade. Do not uninstall `Quran Haafidh` before verifying the backup and successful update. If the installed 1.17.1 has a different signer from the official GitHub build, do not uninstall; Android will reject the update.
