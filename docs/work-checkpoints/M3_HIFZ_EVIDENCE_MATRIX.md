# M3 Hifz evidence matrix

Scope: public, source-safe evidence for Work 5.6 M3. `PASS` means the named
contract was independently exercised at the cited SHA. It does not extend to
unobserved pixels, physical BOOX behaviour, or every application feature.

Evidence SHA: `84bee60f1fb364a97ac8622c53f92ba484d121c1`.

| Case | Verdict | Independent evidence | Limit |
|---|---|---|---|
| T-009 — QCF diacritics/ligatures | **NON_EXECUTE** | Android opens a real `MushafView`, waits for `pageShown`, and saves emulator captures; source contracts cover 604 pages and 77,432 QCF word boxes. | No automated glyph comparison and no physical BOOX capture. An emulator screenshot is not the required BOOX certification. |
| T-010 — masked page landmarks/non-blank | **NON_EXECUTE** | JVM source contracts cover active-review landmark cells, rosette redraw, mask scope, clean rectangles and full-erase residue. | No independent pixel assertion proving every required landmark and a non-blank masked page on BOOX. |
| T-040 — mid-session Sabqi restart | **PASS** | `ProcessDeathPersistenceInstrumentedTest` writes Sabqi repetition `11`, assistance `3`, launches the real app, receives external `am force-stop`, then verifies the same values in a fresh instrumentation process. Android run `38236706405`: both one-test phases GREEN. | Emulator process-death proof, not physical BOOX. |
| T-054 — clock rollback | **PASS** | `ItqanFourSessionStoreTest.frozenNextDateCannotBeValidatedTwiceOnSameDayOrBackdated` executes in source/JVM run `38236706373`. | Deterministic store-level proof; no wall-clock manipulation on BOOX. |
| T-077 — active review duration/state | **PASS** | The external gate checks official `HifzSchedule.ACTIVE_REVIEW_MINUTES == 15` and preserves elapsed `420321` ms across real force-stop. The first run `38235307061` exposed the asynchronous-write loss; the minimal checked `commit()` fix passes run `38236706405`. | Does not wait fifteen real minutes or certify BOOX timing accuracy. |
| T-130 — V5→V6 migration | **PASS** | Android migration/recovery classes execute in the unfiltered 98-test suite. RED `38207906981` (96/98) demonstrated the unfinished-tail defect; GREEN `38208907462` (98/98) verifies ordinary pending tails, completed physical blocks, explicit historical conflicts and interrupted migration. | Synthetic fixtures derived from documented schemas; no destructive test on a user's only database. |
| T-131 — force-stop during repetition | **PASS** | Separate prepare and verify instrumentation processes surround an external `adb shell am force-stop`. Itqan block, repetition `3`, assistance, reveal count, exact unit, FULL protocol/40 total repetitions, Sabqi and active-review elapsed time survive. Run `38236706405`. | Android emulator, not physical BOOX or power-loss-at-storage-layer. |

## Additional engine coverage

| Contract | Verdict | Executed evidence |
|---|---|---|
| Itqan before An-Nas: physical window, four dates, forty repetitions and balanced masks | **PASS** | `ItqanFourSessionStoreTest`, `ItqanBalancedMaskScheduleTest`, `ItqanCompletionBoundaryPolicyTest`, and the live four-date Android tap/recreation test. |
| Distinct post-An-Nas maintenance | **PASS** | Nineteen `ItqanPostNasMaintenanceTest` cases cover the one-time transition, persisted regime, Hujurat restart, frontier, FULL-only four-pass maintenance and non-credit of Sabqi. |
| Consolidation: new acquired material ×7 and old continuous pass | **PASS** | `ConsolidationCycleEngineTest`, consolidation persistence instrumentation and Sunday-final regression contracts. |
| Single validation/no double credit/no unjustified next-day reappearance | **PASS** | Atomic Itqan rotation, consolidation persistence, contradictory persistence and 52-week idempotence tests. |
| Sunday policy | **PASS** | Weekly policy, seven-day migration and runtime tests allow Itqan/finals while excluding active and general revision scheduling. |
| Seven-day-only planning, delays, programme changes, eight-week snowball | **PASS** | Weekly planning/policy tests, overdue-date preservation, rolling-eight-week contracts and a real 52-week `HifzPrefs` stress test. |
| Progression/history and V6 preferences | **PASS** | 1,000-line/1,000-duplicate-event stress, V6 persistent-state/transitions, legacy migration and external force-stop gate. |

## Exact run accounting

- Source/JVM/source-contract run `38236706373`: **PASS**.
- Android run `38236706405`: **PASS**.
- Main Android inventory/presence/compile/discovery/execution: **98 / 98 / 98 / 98**.
- Main Android result: **98 passed, 0 failed, 0 ignored, 0 filtered**.
- External process-death phases: **1 prepare passed + 1 fresh-process verify passed**.
- These two one-test phases are additional invocations of methods already in the 98-test source inventory; they are not reported as 100 distinct tests.
