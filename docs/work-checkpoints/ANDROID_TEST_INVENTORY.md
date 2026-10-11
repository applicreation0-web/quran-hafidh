# Android instrumented-test inventory at 43a991e5

Source count: **25 Java files / 98 `@Test` methods**.

Current workflow `.github/workflows/diagnose-quiz-launch-android.yml` deletes every instrumented-test source except `ReaderSevenDayRuntimeInstrumentedTest.java` and `HifzAndMapRuntimeSmokeInstrumentedTest.java` in its ephemeral checkout. Those two files contain **7 tests**; **91 tests are not compiled or executed** by that run.

| Test class | Tests | M2 disposition |
|---|---:|---|
| AnchoringProtocolContinuityInstrumentedTest | 8 | Compile and run; protect anchoring continuity |
| AstraFixInstrumentedTest | 2 | Compile and run; retain regression coverage |
| C23SurahAwareAnchoringInstrumentedTest | 2 | Compile and run; cover surah boundaries |
| ClaudeNoGoRegressionInstrumentedTest | 5 | Compile and run; retain P0 regression coverage |
| ClaudeRound2ExtendedInstrumentedTest | 3 | Compile and run; reconcile fixtures if obsolete |
| ClaudeRound2MigrationInstrumentedTest | 1 | Compile and run; verify durable migration |
| ClaudeRound2RegressionInstrumentedTest | 3 | Compile and run; retain regression coverage |
| ConsolidationPersistenceInstrumentedTest | 4 | Compile and run; verify non-reappearance |
| HifzAndMapRuntimeSmokeInstrumentedTest | 3 | Already selected; retain in complete suite |
| HifzCarryoverDateInstrumentedTest | 1 | Compile and run; verify date carryover |
| HifzFractionatedAnchoringInstrumentedTest | 4 | Compile and run; protect fractionated anchoring |
| HifzPrefsV4MigrationInstrumentedTest | 17 | Compile and run; preserve legacy migration |
| HifzPrefsV6MigrationInstrumentedTest | 5 | Compile and run; cover T-130 migration safety |
| HifzSessionDateInstrumentedTest | 1 | Compile and run; verify session dates |
| HifzUiLayoutInstrumentedTest | 2 | Compile and run; retain layout evidence |
| HifzV6LegacyJ10MigrationInstrumentedTest | 4 | Compile and run; preserve J10 migration |
| HifzV6MigrationRecoveryInstrumentedTest | 2 | Compile and run; verify interrupted migration |
| HifzV6PersistentStateInstrumentedTest | 8 | Compile and run; verify durable V6 state |
| HifzV6StateTransitionsInstrumentedTest | 4 | Compile and run; protect transitions |
| J10ReviewStoreInstrumentedTest | 3 | Compile and run; preserve stored review state |
| PhysicalConsolidationRuntimeInstrumentedTest | 4 | Compile and run; cover physical units |
| PreviewGeometryInstrumentedTest | 5 | Compile and run; cover QCF/mask geometry |
| ProcessDeathPersistenceInstrumentedTest | 2 | Compile and run; cover T-131 force-stop safety |
| ReaderSevenDayRuntimeInstrumentedTest | 4 | Already selected; retain in complete suite |
| StabilizationHalfPageInstrumentedTest | 1 | Compile and run; protect half-page behavior |

## Required M2 reporting

M2 must publish four distinct numbers: 98 present, number compiling, number discovered by instrumentation, and number actually executed. Any ignored, filtered, crashed, or shard-lost test must be named. Deleting test sources, replacing assertions with stubs, or reporting source inspection as execution is prohibited.

## Initial P0 mapping to prove in M3

| Audit case | Required runtime evidence |
|---|---|
| T-009 | Real rendered QCF diacritics/ligatures; emulator evidence is not BOOX certification |
| T-010 | Masked page retains required readable landmarks and never becomes blank |
| T-040 | Mid-session Sabqi restart restores the same durable state |
| T-054 | Moving the clock backward cannot validate two Itqan dates |
| T-077 | Active review runs with the official 1.17.1 duration and does not corrupt state |
| T-130 | V5→V6 migration is fail-safe |
| T-131 | Force-stop during repetition preserves the last committed state |

Official 1.17.1 defines `HifzSchedule.ACTIVE_REVIEW_MINUTES = 15`; `hifz-core` is unchanged between the official reference and the audited HEAD. M1 therefore does not alter the engine to satisfy any conflicting narrative value.

