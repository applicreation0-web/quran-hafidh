# CP-09-M2-RED-HANDOFF — Reproducible migration defect handoff

## Evidence carried forward

- CP-08 commit: `509a19d84a86a75dddfe8c89b401bba21470224a`.
- Same-SHA run: `38207426156`.
- Result: **98 executed, 5 failed, 93 passed**.

Three failures came from comparing an already-started legacy tail plan against the current entry's fresh FULL protocol. The fixture now captures the protocol after the unit starts and verifies that exact protocol across reopen, which is the compatibility contract under test.

## M2 output and M3 RED input

The two remaining failures share one migration cause candidate and are intentionally retained:

- `ClaudeNoGoRegressionInstrumentedTest.legacyFiveLineProgressMigratesToStabilizedAndContinuesOnNewPhysicalUnit`;
- `HifzPrefsV4MigrationInstrumentedTest.schemaThreeMigrationIsAtomicAndPreservesUnrelatedProgress`.

Both say unfinished legacy Stabilisation material must be `Appris`; both observe that it is absent from the V6 learned set. The next same-SHA run is expected to contain only these two failures. If confirmed, M2 has restored complete discovery/execution and M3 may apply a minimal migration fix under RED/GREEN discipline.

## Protected surfaces

No production code is changed in this checkpoint. No test is removed, disabled, ignored, or converted to a stub.
