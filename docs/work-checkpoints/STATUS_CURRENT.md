# Current Work 5.6 status

- Movement: M2 — full 98-test runtime gate.
- Starting HEAD: `43a991e5a6b75f939e50da313078a1c2b1419240`.
- Branch: `work/haafidh-1171-ibn-kathir-integration-independent`.
- Official reference: `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`.
- Audit verdict carried forward: **NO-GO**.
- Code changes in M1: none.
- M1 same-SHA Android run: **PASS**, run `38176731635`, seven selected tests only.
- M2 local Android tests: **NOT RUN** — this workspace has Java 17 but no Gradle executable or Android SDK.
- M2 RED run: `38177121051`, **FAIL as expected** with 29 compile errors in five legacy fixture files.
- M2 compile-repair run: `38177602466`, **PASS**, but only seven tests executed because the runtime script retained a class filter.
- M2 complete runtime run: `38178017596`, **FAIL**, with all 98 tests executed: 68 passed and 30 failed.
- Initial classification: stale schema-5 assertions, incomplete V6 fixtures, wall-clock-dependent cadence fixtures, and missing earned-evening proof dominate the failures; four semantic cases remain under focused comparison.
- Known P0 blockers: Quran Foundation durable-storage decision; complete Android runtime suite not yet green.
- CP-06 repairs only obsolete/incomplete runtime fixtures; production Hifz code remains untouched.
- CP-06 same-SHA run `38206291992`: **98 executed, 81 passed, 17 failed**.
- CP-07 keeps one migration assertion deliberately RED: an unvalidated schema-5 tail appears `NONE` instead of `Appris`; 16 obsolete fixture contracts are refined without production changes.
- CP-07 same-SHA run `38206973904`: **98 executed, 92 passed, 6 failed**; four failures are final legacy anchoring expectations and two expose the same migration-tail candidate.
- CP-08 aligns those four continuity assertions with the actual persisted rotation protocol, still without production changes.
- Next gate: rerun all 98 tests on CP-08; if only the two migration-tail assertions remain, close M2 honestly and enter M3 with reproduced RED evidence.

