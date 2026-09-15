# Quran Hifz 0.7.5 — Schema 6 migration GREEN checkpoint

- Branch: `work/hifz-0.7.5-retention-j10`
- Application parent SHA: `e91b132b661799391e2a7539e5b340b5bae9afe3`
- RED runtime run: `34966589969`
- RED result: 3 legacy migration tests PASS; 3 schema-6 persistence tests FAIL only on missing v6 durable state keys.
- Production patch: full schema-5 → schema-6 classification is built in memory and persisted to the main `quran_hifz_preview_v1` store with one `SharedPreferences.Editor.commit()`.
- Legacy `quran_hifz_j10_v1` is read via `snapshot()` during migration only; this checkpoint does not authorize any merge, signing, publication, or release.
- Verification required: JVM + source/product/cosmetic gates + Android emulator migration suite.
