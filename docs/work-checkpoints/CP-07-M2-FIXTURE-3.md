# CP-07-M2-FIXTURE-3 — Separate obsolete contracts from one P0 candidate

## Evidence carried forward

- CP-06 commit: `e3c64497b810b8041e9d5a5a12b50ac4c8643e1c`.
- Same-SHA run: `38206291992`.
- Result: **98 executed, 17 failed, 81 passed**; compilation and discovery remained complete.

## Test-only refinement

- Legacy Itqan queue tests now verify persisted compatibility state; the current frozen four-session UI remains covered by `ReaderSevenDayRuntimeInstrumentedTest` with a real repetition tap and Activity recreation.
- Fresh and migrated V6 tests no longer infer progression from the legacy Murajaah range accessor.
- The process-death fixture starts the engine's actual current perpetual-rotation unit instead of injecting an unreachable historical unit.
- Home routing tests inspect the current multi-session “Aujourd’hui” window instead of expecting the former direct launch behavior.
- Migration crash recovery compares idempotence after the constructor has completed its separate seven-day-program bootstrap.

## Deliberately retained RED

`ClaudeNoGoRegressionInstrumentedTest.legacyFiveLineProgressMigratesToStabilizedAndContinuesOnNewPhysicalUnit` remains unchanged at its core assertion: the unvalidated tail of a partially completed schema-5 unit must remain `LEARNED` (`Appris`). The complete runtime run returned `NONE`. This is retained as a candidate migration defect for M3; it is not converted to PASS by weakening the expectation.

## Protected surfaces

No production code is changed. The next run must execute all 98 tests and is expected to isolate the retained candidate if every fixture diagnosis is correct.
