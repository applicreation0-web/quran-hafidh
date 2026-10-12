# CP-08-M2-FINAL-GATE — Complete-suite gate before M3

## Evidence carried forward

- CP-07 commit: `42b2c71038f3564478be0b44ee41324e1c3fedf7`.
- Same-SHA run: `38206973904`.
- Result: **98 executed, 6 failed, 92 passed**.

## Final M2 fixture correction

Four remaining `AnchoringProtocolContinuityInstrumentedTest` assertions still assumed that the fixture's requested protocol controlled the perpetual rotation entry. They now assert continuity of the protocol actually selected and persisted by the current engine. No test is removed or ignored.

## Candidate production defect retained

Two surviving assertions are expected to describe the same schema-5-to-6 migration defect:

1. a partially completed legacy unit's unvalidated tail appears `NONE` instead of `LEARNED` (`Appris`);
2. a schema-3 tail migrated through schemas 4 and 5 does not populate the V6 learned set.

The suspected adapter error is that every legacy `promotedRanges` line is supplied as stable evidence even when the same physical line is still in `unconsolidatedPromotedRanges`. M3 must confirm this with the complete Android RED result before any minimal production change.

## Protected surfaces

This checkpoint changes tests and public evidence only. Production remains untouched. The gate is the same unfiltered 98-test emulator run.
