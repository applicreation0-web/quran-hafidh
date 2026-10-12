# CP-10-M3-MIGRATION-RED — Unfinished legacy tail disappears from Appris

## Reproduction

- M2 handoff commit: `5703112a14fc2984503aff6d6d88aa75a71bf423`.
- Same-SHA Android run: `38207906981`.
- Result: **98 executed, 96 passed, 2 failed**.
- No tests were ignored or filtered.

The two failures are:

1. `ClaudeNoGoRegressionInstrumentedTest.legacyFiveLineProgressMigratesToStabilizedAndContinuesOnNewPhysicalUnit` — the validated physical blocks become `Stabilisé`, but the unfinished tail is `NONE` instead of `LEARNED` (`Appris`).
2. `HifzPrefsV4MigrationInstrumentedTest.schemaThreeMigrationIsAtomicAndPreservesUnrelatedProgress` — a migrated pending tail does not populate the V6 learned set.

## Root cause

`migrateV5ToV6` currently builds `legacyStableLineIds` from `progressionMurajaahRanges()`, which includes every `promotedRanges` line. Historical pending material appears in both `promotedRanges` and `unconsolidatedPromotedRanges`; the adapter therefore supplies ordinary unfinished work as both pending and stable evidence. The pure classifier correctly quarantines contradictory evidence, but the adapter manufactured the contradiction.

## Minimal-fix boundary

- Keep configured `itqanRanges` and explicit `legacyMurajaahPromotedRanges` as stable evidence.
- Keep `promotedRanges` as stable evidence only outside the physical pending-line set.
- Preserve quarantine for genuinely explicit stable/pending overlap and preserve all existing dates.
- Change no session engine, cadence, UI, Quran asset, icon, or private title content.

## GREEN gate

The same 98-test Android run must pass all tests. The broader JVM/source audit workflow must pass on the identical SHA.
