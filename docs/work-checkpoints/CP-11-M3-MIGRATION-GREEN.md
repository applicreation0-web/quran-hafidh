# CP-11-M3-MIGRATION-GREEN — Exact V5→V6 evidence classification

## Corrected adapter

`migrateV5ToV6` now distinguishes three independent kinds of historical evidence:

1. ordinary unfinished promoted material remains `Appris`;
2. completed physical sub-blocks become `Stabilisé`;
3. explicit Murajaah/Itqan overlap, or a dated J10 review inside the still-active unit, remains quarantined for manual resolution.

The pure classifier, state-transition engine, cadence, UI, Quran assets, and private-title boundary are unchanged. The exact-file source guard was repinned to the reviewed adapter hash.

## Same-SHA GREEN evidence

- Remote commit: `e61a27fa87e309729fdaf160f804e129be874c56`.
- Source/JVM audit run `38208907474`: **PASS**.
- Android run `38208907462`: **PASS**.
- Android evidence: build successful, `OK (98 tests)`, `INSTRUMENTATION_CODE: -1`.
- Present / compiled / discovered / executed: **98 / 98 / 98 / 98**.
- Failed / ignored / filtered: **0 / 0 / 0**.

## Remaining M3 work

The first shell-level force-stop run `38235307061` proved the distinction matters:

- the unfiltered suite still reported `OK (98 tests)`;
- the independent preparation process reported `OK (1 test)`;
- after `am force-stop`, Itqan and Sabqi state survived but active-review elapsed time was `0` instead of `420321` ms.

Root cause: `setElapsedFor` used asynchronous `SharedPreferences.apply()`, so an immediate process kill could race the disk write. The follow-up patch uses a checked synchronous commit for this durable checkpoint only. Its same-SHA GREEN rerun is required before M3 closure, followed by the final P0 mapping and planning evidence.
