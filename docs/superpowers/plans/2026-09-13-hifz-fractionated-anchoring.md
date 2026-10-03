# Quran Hifz 0.7.3 — Ancrage fractionné implementation plan

**Baseline application SHA:** `eeb64d305bf2232b2994f9184ffa6725f0c35c09`
**Branch:** `work/hifz-0.7.3-spec-update`
**Scope:** audit-only candidate; no merge, signature, release, publication, or audio changes.

## Reconciliation with the supplied design specification

- P0-1 is already fixed at this baseline: `HifzSchedule.targetMinutesFor()` returns a duration by mode and does not throw for an off-schedule quick-access session.
- P1-1 is already contained at this baseline: display-order deferral may reorder the recent queue, but promotion always uses `HifzPrefs.canonicalRecentOrder(...)` before applying age/attendance policy. Preserve the existing user-facing “À renforcer” behavior and add regression proof rather than rewriting it.
- P2-7 arbitration: `reviewStreak` is a review-quality/display metric only. It MUST NOT be a promotion criterion. Promotion remains age + completed Sunday attendance (+ >60 guardrail), as implemented by `RecentPromotionPolicy`.
- The permanent candidate workflow already executes `connectedDebugAndroidTest`; keep it and extend instrumented coverage rather than adding a second emulator job.
- Product choices for v1: strict D5 (one validated sub-block per day), schema 4 with optional-key repair (no schema bump), filtered surah selector, LIGHT ×35 for every fractionated sub-block.

## TDD sequence

1. Add RED pure tests for balanced fraction sizes, count, start/length offsets, index clamping, and LIGHT ×35 mask profile.
2. Implement the pure `PreviewConfig` fraction helpers.
3. Add persisted schema-4 optional keys `hardAnchoringSurahs` and `itqanBlockIndex`, robust JSON parsing, D7 “any marked surah”, and atomic block advance/reset APIs.
4. Add `GeometryRepository.versesOnLines(...)` without touching `eligiblePageUnit`.
5. Branch `renderItqan` so the whole canonical page stays displayed while only the active sub-block lines/verses are selected and masked; force LIGHT ×35.
6. Branch `validateItqan`: intermediate blocks advance atomically and end the day; final block follows the existing whole-page consolidation path; no reveal gate/defer/escalation for fractionated units.
7. Add filtered “Sourates difficiles à ancrer” Settings selector; do not change home/dashboard.
8. Add instrumented coverage proving persistence, three-block progression, final queue removal / Murajaah eligibility, and no Murajaah cursor teleport.
9. Add source/Gradle contracts for “Ancrage fractionné” and pure fraction helper presence while preserving all existing fragile literal contracts.
10. Run unit, contract, source-regression, instrumented emulator, assembleRelease, and APK inspection on the exact resulting SHA. Produce an unsigned audit APK only.

## Release gate

This implementation does not waive the physical BOOX gate. After external contradictory audit and BOOX testing, signature/release can be considered separately.
