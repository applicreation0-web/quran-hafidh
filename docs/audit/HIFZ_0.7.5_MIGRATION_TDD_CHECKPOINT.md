# Quran Hifz 0.7.5 — Migration TDD checkpoint

Branch: `work/hifz-0.7.5-retention-j10`

Published 0.7.4 baseline: `75d35aaf3f46877b4f6b65fe1c276cc7b3fba1a6`

## Initial RED evidence

Run `34961952831`, Android emulator job `104358106008` executed `HifzPrefsV6MigrationInstrumentedTest` on API 35.

All three migration tests failed for the same intended reason before the production change:

`java.lang.IllegalStateException: Unsupported Hifz preview schema: 5`

The emulator and instrumentation infrastructure were operational; this was a functional RED, not an infrastructure failure.

## Initial schema 6 candidate

Production commit: `73d2845e9a5d40eab75f23e36eeca8bc999ea45b`

The change is intentionally limited to:
- chain schema 4 -> 5 -> 6 instead of returning at schema 5;
- migrate installed schema 5 to schema 6 with one main-store `commit()`;
- preserve calibrated maintenance speed exactly;
- convert the uncalibrated historical maintenance default to 8.0 s/line;
- do not remove existing installed preference keys.

## Complete v6 import RED and legacy-J10 race

Run `34969456763` on checkpoint `eeaa141aacab90028bb81c98b49dc35dfedf777f` kept JVM/source/core/compile/product/cosmetic/convergence gates green but produced 3/6 migration-instrumented failures. The old `quran_hifz_j10_v1` store was polluted with synthetic `historicalSeed` dates while schema 5 -> 6 migration was executing.

A first caller-level guard at commit `6a0a862b090d5a9e9bab506521be243e79fd2dd2` retired normal legacy J10 runtime after schema 6. Run `34976832520` proved that this was insufficient: an already in-flight v5 store mutation could still race the schema 6 commit, again causing 3/6 instrumented failures.

## Race-fix candidate

Production HEAD before this documentary checkpoint: `684ab407fbed361fe231a9196c9272859a690a55`.

The race fix is deliberately narrow:
- `J10ReviewStore` now uses one process-wide legacy-store lock across every snapshot and every legacy mutation;
- each legacy mutation re-checks the authoritative main-store schema while holding that lock and becomes a no-op once schema >= 6;
- the schema 5 -> 6 migration holds the same lock from legacy-J10 snapshot/classification through the single main-store schema6 commit;
- therefore an in-flight v5 legacy write must either finish before the migration snapshot or wait until schema6 is authoritative and then be rejected;
- the retained legacy J10 file remains the immutable migration backup after schema6.

This checkpoint still does **not** claim the complete v4.3 migration is GREEN. The exact same six emulator tests must pass before moving to process-death/recovery coverage.
