# Haafidh 1.17.5 — six genuine BOOX photos (differential source audit, 25 Oct 2026)

**Provenance:** six original BOOX photographs supplied as ZIP; six SHA-256 hashes independently checked against their manifest. The pictured installed build is not proven to be the current branch HEAD. Do not mistake the BOOX floating circle for an app button.

**Reference head:** `0c9f40a2c7b09fbbd3420197eca868ac90cbda69`.  
**UI-only correction:** [17305f34](https://github.com/applicreation0-web/quran-hafidh/commit/17305f34ddbbdc212ae60cef8b0c34a932f68e7e).

| Real photo | Defect visible in pictured debug | Current code verification / remaining physical test |
| --- | --- | --- |
| 01 Settings root | No helpful sublines, very small category labels | `SettingsActivity.menuSummary` adds six short summaries; `Ui.settingRow` uses 21 sp bold and 18 sp serif on BOOX (>=600dp), 56dp minimum row. Verify actual E-Ink. |
| 02 Ranges and corpus | Tiny Qur'an range captions, narrow right controls | `SettingsActivity.renderRanges`: 19 sp labels, 18 sp serif verses without ellipses; `Ui.iconButton` 48dp touch target. Verify Arabic visual direction and wrapping. |
| 03 Atlas + QCF Mushaf | Small unit and passage captions; large side margins | Tablet text enlargement from `f900dd3d` was deliberately reverted by `5fba3b05` to preserve approved QCF/Atlas snapshot. **No change made**; physical A/B verification needed before touching these captions. Preserve page aspect, exact QCF boxes and highlighting, validated editorial titles. |
| 04 Progress | Unexplained 604-cell grid and hatching | `ProgressMapActivity.legend()` already sits above grid (4 statuses, 18 sp BOOX); exact file protected by two frozen-source guards. Verify hatching legibility physically, not progression-state mutation. |
| 05 Hifz paths | Six good large pictograms, small section headers, weekly plan far below | Existing jump `PLANNING DE LA SEMAINE ↓` and enlarged labels/subtitles; commit 17305f34 makes SABQI / ITQĀN / RÉVISION headings 22 sp BOOX, 16 sp phone. Test scrolling and spacing. |
| 06 Home | Today and CTA text tiny relative to oversized icons | `MainActivity` now uses 22/30/21/24sp for Today label/action details and larger menu titles on BOOX; original vector rehal icon enforced. Validate in real device. |

## Control boundary

Only `MainActivity.java` changed in commit 17305f34: presentational group headings. No Hifz engine, student progress, saves, Atlas catalogue, Mushaf QCF geometry, masking, icons or signing configuration changed in this correction.

- Source+JVM audit: [run 40174811428](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40174811428)
- Offline non-installable staging: [run 40174811404](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40174811404)
- Android instrumentation and phone-emulator screenshots: [run 40174811470](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40174811470)

**Packaging:** versionName 1.17.5; versionCode 43; main applicationId com.quransafeguard.hifz. Debug uses suffix (BOOX .boox1175debug). Staged artifact **UNFINALIZED** excludes private Atlas corpus and official signing; it is not an installable in-place upgrade. An official release requires a matching official signing certificate and data-preserving installation proof.

**Release: NO-GO** pending physical BOOX screenshots/tests, Atlas caption review, exact finalized artifact, matching release signature, and express user approval. CI PASS alone is not a BOOX acceptance test.

## Completed CI checks at UI commit `17305f34`

- **Source contracts + JVM:** PASS, [workflow 40174811428](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40174811428).
- **Unsigned non-private staging:** PASS, [workflow 40174811404](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40174811404), artifact ID `12591558125`. This is NOT an installable APK.
- **Android API 35 Pixel 2 emulator instrumentation:** PASS, **29 tests, 0 failures, 0 skipped**, [workflow 40174811470](https://github.com/applicreation0-web/quran-hafidh/actions/runs/40174811470), evidence artifact ID `12591029522`. Three PNG captures named `home-portrait-android-emulator.png`, `parcours-portrait-android-emulator.png`, `parcours-lower-icons-android-emulator.png`. These are emulator screenshots, **NOT physical BOOX photographs**.

The on-screen Hifz typography and six approved large icons are visible in the emulator evidence, with the weekly plan reachable by scrolling. This does not validate the 10-inch BOOX E-Ink surface, the private Atlas corpus, or an official in-place update. Official release remains NO-GO.
