# Quran Safeguard 0.10.5 — integrated release candidate

The integrated 0.10.5 candidate has passed the complete functional release
audit, Light and Plus unit tests, release builds, Medina Mushaf 604/604 checks,
canonical 30 Juz / 60 Hizb checks, strict sensitive-app Accessibility scope,
Tafsir source/database audits, and unsigned APK payload inspections.

## Remaining release blocker

Publication is blocked only because the GitHub Actions signing secrets are not
available to the publish workflow. The run receives these four variables empty:

- LIGHT_KEYSTORE_B64
- LIGHT_KEYSTORE_PASSWORD
- PLUS_KEYSTORE_B64
- PLUS_KEYSTORE_PASSWORD

The original signing material must be restored in GitHub Actions from its secure
backup. It must never be committed to the repository or pasted into issue/PR
comments. Once restored, rerun the same fail-closed publish pipeline; it will
verify the retained certificate fingerprints before creating v0.10.5.
