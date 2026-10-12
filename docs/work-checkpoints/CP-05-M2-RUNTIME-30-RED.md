# CP-05-M2-RUNTIME-30-RED — First complete runtime result

## Same-SHA evidence

- Commit: `051c7bd5e9dd6a8c9da7d7c93ae00578d4b83515`.
- GitHub Actions run: `38178017596`.
- AndroidJUnitRunner result: **98 tests executed, 30 failures, 68 successes**.
- This is the first branch run that executes every discovered instrumented test instead of the former seven-test allow-list.

## Failure inventory

| Area | Failures | First diagnosis |
|---|---:|---|
| Legacy anchoring/session fixtures | 6 | Fixtures predate the frozen four-session Itqan planner and durable V6 corpus state. |
| Schema/migration fixtures | 15 | Seven assertions still require schema 5; several schema-4 fixtures omit data now required by the 5-to-6 migration; two tests call a removed helper instead of the retained private fault field. |
| Date-sensitive home routing | 2 | Tests use the wall-clock day and therefore exercised Sunday cadence instead of their intended weekday. |
| Physical consolidation proof | 1 | Fixture omits the now-required earned evening repetition receipt. |
| Process-death fixture | 2 | Fixture writes legacy queue keys but not the authoritative V6 stabilization corpus. |
| Remaining semantic assertions | 4 | Require contract-by-contract comparison with current production and JVM tests before correction. |

## Safety ruling

The 30 failures are diagnostic evidence, not proof of 30 product regressions. No production Hifz file is changed at this checkpoint. Corrections must remain test-only unless a current contract is reproduced RED independently and then fixed GREEN.

## Next gate

Repair only obsolete or incomplete test fixtures, rerun all 98 tests, and retain every still-failing current-contract case for focused diagnosis.
