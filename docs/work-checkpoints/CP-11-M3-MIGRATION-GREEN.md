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

The 98-test run invokes the two process-death fixture methods in one instrumentation process. M3 still requires a shell-level gate that performs a real external `am force-stop` between preparation and verification, plus the final P0 mapping and eight-week planning evidence.
