# CP-03-M2-FIXTURE-1 — Align legacy tests to canonical APIs

## Reproduction

- Parent RED SHA: `53bb9f0b4977160af90673cc814d77d7bf32fd21`.
- GitHub Actions run: `38177121051`.
- Result: `FAIL` in `:hifz-app:compileDebugAndroidTestJavaWithJavac`.
- Exact scope: 29 compiler errors across five files.

The failing symbols were helpers removed by historical cleanup commit `e33956e4`: `completeRecentSabqiReview`, `failAndDeferAnchoring`, `progressStateV6`, `nextActionV6`, `isFractionatedUnit`, and `setProgramStartDate`, plus two evolved method signatures.

## Root cause

The cleanup commit described these helpers as unreferenced, but the Android instrumented source set still referenced them. The prior workflow's deletion of 23 test files hid this stale-fixture breakage. No production failure is demonstrated by these compiler errors.

## Minimal repair

- Read progression through `progressionSnapshotV6()` instead of removed single-line getters.
- Exercise the existing static hard-surah predicate instead of the removed instance wrapper.
- Seed the persisted program date through the test's real `SharedPreferences` fixture.
- Use the current date-based snowball API and current Stabilisation completion signature.
- Replace obsolete completion/defer wrapper checks with equivalent persistence-reader and pure-policy assertions.
- Seed snowball fixtures through `completeStabilizationBlockV6` so tests follow the current production write path.

## Protected surfaces

No file under `hifz-app/src/main`, `hifz-core`, Quran assets, icons, private title data, or persistence production code is modified. The source set remains 25 files / 98 tests.

## Verification state

- Original unfiltered compile RED: reproduced and linked above.
- Static obsolete-call scan: PASS.
- Source inventory: PASS, 25 files / 98 tests.
- Production diff: empty.
- Android compile and runtime after repair: pending same-SHA GitHub Actions run.

