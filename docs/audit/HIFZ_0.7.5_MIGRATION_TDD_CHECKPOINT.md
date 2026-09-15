# Quran Hifz 0.7.5 — Migration TDD checkpoint

Branch: `work/hifz-0.7.5-retention-j10`

Published 0.7.4 baseline: `75d35aaf3f46877b4f6b65fe1c276cc7b3fba1a6`

## RED evidence

Run `34961952831`, Android emulator job `104358106008` executed `HifzPrefsV6MigrationInstrumentedTest` on API 35.

All three migration tests failed for the same intended reason before the production change:

`java.lang.IllegalStateException: Unsupported Hifz preview schema: 5`

The emulator and instrumentation infrastructure were operational; this was a functional RED, not an infrastructure failure.

## Minimal GREEN candidate

Production commit: `73d2845e9a5d40eab75f23e36eeca8bc999ea45b`

The change is intentionally limited to:
- chain schema 4 -> 5 -> 6 instead of returning at schema 5;
- migrate installed schema 5 to schema 6 with one main-store `commit()`;
- preserve calibrated maintenance speed exactly;
- convert the uncalibrated historical maintenance default to 8.0 s/line;
- do not read/write or mutate `quran_hifz_j10_v1` in this minimal migration step;
- do not remove existing installed preference keys.

This checkpoint does **not** claim the complete v4.3 migration is implemented. J10 import/classification/quarantine/UNKNOWN_DUE and crash-phase coverage remain downstream gates.
