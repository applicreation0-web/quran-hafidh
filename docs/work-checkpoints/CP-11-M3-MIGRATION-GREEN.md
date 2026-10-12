# CP-11-M3-MIGRATION-GREEN — Exact V5→V6 evidence classification

## Corrected adapter

`migrateV5ToV6` now distinguishes three independent kinds of historical evidence:

1. ordinary unfinished promoted material remains `Appris`;
2. completed physical sub-blocks become `Stabilisé`;
3. explicit Murajaah/Itqan overlap, or a dated J10 review inside the still-active unit, remains quarantined for manual resolution.

The pure classifier, state-transition engine, cadence, UI, Quran assets, and private-title boundary are unchanged. The exact-file source guard was repinned to the reviewed adapter hash.

## Migration GREEN evidence

- Remote commit: `e61a27fa87e309729fdaf160f804e129be874c56`.
- Source/JVM audit run `38208907474`: **PASS**.
- Android run `38208907462`: **PASS**.
- Android evidence: build successful, `OK (98 tests)`, `INSTRUMENTATION_CODE: -1`.
- Present / compiled / discovered / executed: **98 / 98 / 98 / 98**.
- Failed / ignored / filtered: **0 / 0 / 0**.

## Real process-death RED then GREEN

The first shell-level force-stop run `38235307061` proved the distinction matters:

- the unfiltered suite still reported `OK (98 tests)`;
- the independent preparation process reported `OK (1 test)`;
- after `am force-stop`, Itqan and Sabqi state survived but active-review elapsed time was `0` instead of `420321` ms.

Root cause: `setElapsedFor` used asynchronous `SharedPreferences.apply()`, so an immediate process kill could race the disk write. The follow-up patch uses a checked synchronous commit for this durable checkpoint only.

The minimal fix and its public trace are present at
`84bee60f1fb364a97ac8622c53f92ba484d121c1`:

- source/JVM/source-contract run `38236706373`: **PASS**;
- Android run `38236706405`: **PASS**;
- main unfiltered suite: `OK (98 tests)`, `INSTRUMENTATION_CODE: -1`;
- prepare process: `OK (1 test)`;
- external `adb shell am force-stop` of the real package;
- fresh verification process: `OK (1 test)`;
- final gate: `PASS: 98 live Android tests + external force-stop persistence + 14 screenshots, PUBLIC no-title fail-closed only`.

The two one-test phases re-run methods already counted in the 98-test inventory;
they are not 100 distinct tests.

## M3 verdict and residual limits

M3 engine and migration scope is **GREEN**. See
[`M3_HIFZ_EVIDENCE_MATRIX.md`](M3_HIFZ_EVIDENCE_MATRIX.md) for the complete
mapping. T-009 and T-010 remain **NON_EXECUTE** at the strict audit level:
emulator/source evidence cannot certify exact BOOX glyph pixels or masked-page
visibility on physical hardware. No result here certifies every feature, a
physical BOOX, private Quran.com title rendering, a distributable debug or a
release.
