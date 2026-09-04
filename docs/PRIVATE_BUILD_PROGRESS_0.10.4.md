# Quran Safeguard Plus 0.10.4 — private build progress

Scope: strictly personal/private APK. No public distribution.

## Qushayri
- deterministic source parser implemented and audited against the exact Sands PDF SHA-256;
- 669/669 Sūras 1–4 verses remain structurally addressable;
- deeper typography-aware contradictory audit now retains **519 verse mappings / 644 commentary entries** under the fail-closed rule;
- **76 ambiguous multi-verse clusters** are deliberately hidden rather than guessed;
- Arabic source text is excluded;
- the printed English Quran translation is excluded from the tafsir commentary payload;
- explicit range 4:167–169 remains a single source range;
- 2:68 is retained as the verified true inline canonical anchor, using only commentary after that anchor;
- an earlier assumption about 4:120 was rejected: the apparent inline marker is not used as a boundary, so no independent 4:120 option is exposed rather than misattribute the source;
- local audited SQLite: 519 mapped verses, 644 entries, quick_check PASS, no Arabic-script commentary; SHA-256 recorded in the private audit document.

## Qurtubi
- architecture, source metadata and coverage audit remain complete through Qur'an 4:23;
- range comments are stored once and mapped to all source-declared verses;
- Sunniconnect contamination and source Warsh Arabic remain forbidden;
- full private payload still requires the exact four source PDFs/content files for the same fail-closed extraction review.

## Al-Hikam
- UI/model separation and release-integrity gates are implemented;
- the existing 264-entry corpus is not falsely promoted to the new matn-only provenance standard;
- unverified new sharh remains out of the private build until its source/locator checks pass.

## Build / CI
- Light and Plus unit tests are wired into Android CI;
- Plus APK verification remains fail-closed;
- the next substantive branch push is used to test the repaired GitHub runner with a fresh workflow execution;
- no merge to `main` and no release/version bump until the adversarial audit and a real end-to-end build pass.
