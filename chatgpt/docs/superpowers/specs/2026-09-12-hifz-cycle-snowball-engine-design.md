# Quran Hifz — Cycle + Snowball Engine Design

Date: 2026-09-12
Branch: `release/hifz-0.7.3-final`

## Goal

Align the structured Hifz engine with the user's definitive operating model while preserving all validated 0.7.3 reader, BOOX, masking, Tafsir, audio-boundary, and persistence guarantees.

The engine is governed by two complementary philosophies:

1. **Cycle** — work advances in canonical Quran order from a user-selected Itqan rotation start, continues through the eligible corpus, then wraps to the same rotation start. No promotion may jump or reset the cycle cursor.
2. **Snowball** — the eligible consolidated corpus grows automatically as Sabqi moves from new → recent → old → Itqan ×40 → consolidated → global Murajaah.

The user never manually manages cursors.

## Weekly schedule

### Monday / Wednesday / Friday

- Morning: new Sabqi, exactly 5 lines, existing 37-repetition protocol.
- Evening: a real tracked session of exactly 30 minutes using only the same 5 lines learned that morning.
- The evening block loops the same material for the full 30 minutes.
- It does not use old Itqan and does not move `itqanCursor` or `murajaahCursor`.

### Tuesday / Thursday

- Morning: Itqan ×40.
- Evening: exactly 60 minutes of old consolidated Itqan Murajaah only.
- No recent Sabqi in the evening session.
- The session resumes the single global `murajaahCursor` from its exact prior position.

### Saturday / Sunday

- Morning: exactly 30 minutes of recent Sabqi only.
- Recent blocks loop cyclically for the full 30 minutes. One block means that block repeats for the full duration.
- No transfer of unused time to old Itqan.
- Evening: exactly 30 minutes of old consolidated Itqan Murajaah only, continuing the same global `murajaahCursor`.

## Fixed-time rule

Session duration is the invariant. Quran quantity is derived from actual measured speed.

There is no redistribution of minutes between session types. In particular, code equivalent to `unusedA -> availableB` is forbidden.

The runtime must stop or mark completion at the configured fixed duration for the relevant session. Background time must not count because `SessionClock` remains lifecycle-paused.

## Itqan cycle

`itqanRotationStart` is the canonical anchor chosen by the user.

`itqanCursor` is a single persistent cyclic cursor for the main Itqan cycle.

Rules:

- Continue in canonical Quran order through the eligible Itqan work corpus.
- On reaching the corpus end, wrap to `itqanRotationStart`.
- Never reset weekly.
- Never jump directly to newly promoted Sabqi.
- Never lose the current cycle position when the corpus grows.
- If an Itqan ×40 unit is already active, resume and complete it before selecting a new unit.

## Snowball promotion lifecycle

New Sabqi follows this automatic lifecycle:

`Sabqi new → recentSabqi → old Sabqi → Itqan work corpus → ×40 → consolidated Itqan → global Murajaah`

The existing sliding recent-Sabqi window remains the mechanism that decides when the oldest safe material leaves `recentSabqi`.

Promotion rules:

- Respect safe verse boundaries.
- Only fully covered verses may leave recent Sabqi.
- Promotion is idempotent.
- A promoted passage enters the Itqan work corpus at its canonical Quran position.
- Promotion does **not** move `itqanCursor`.
- Promotion does **not** move `murajaahCursor`.
- A promoted passage is not considered newly consolidated for Murajaah until its required ×40 has been completed.

## No priority promotion queue

The original mission text proposed `pendingPromotedItqan` as a priority FIFO that interrupts the principal cycle. That is intentionally superseded by the user-approved cycle philosophy.

The implementation still needs durable consolidation state for promoted passages, but **not** a priority work queue.

Recommended model:

- `promotedRanges`: canonical ranges that have joined Itqan work scope.
- `unconsolidatedPromotedRanges` (or equivalent durable status): promoted ranges still requiring ×40.
- `consolidatedPromotedRanges` need not be duplicated if consolidation can be represented by removing ranges from the unconsolidated set while keeping them in `promotedRanges`.

When the normal `itqanCursor` reaches an unconsolidated promoted range, the selected unit is worked with the normal ×40 protocol. On successful validation, that covered portion is marked consolidated. The cycle then continues canonically.

## Migration from current schema

The current branch uses schema v2 and already persists `recentSabqi`, `promotedRanges`, `itqanCursor`, `murajaahCursor`, active Itqan state, Murajaah A/B runtime state, speeds, and completion labels.

Migration must be explicit and non-destructive, producing schema v3.

Rules:

- Preserve `programStartDate`, Sabqi bounds/cursor/progress, `recentSabqi`, speeds, `itqanRotationStart`, `itqanCursor`, `murajaahCursor`, and active Itqan unit/progress.
- Preserve all existing `promotedRanges`.
- Existing historical promoted ranges are treated as normal Itqan corpus material, not priority work.
- Because old state does not prove that a historical promoted range already received a real ×40, mark those historical promoted ranges as requiring one future ×40 when naturally encountered by the cycle.
- Do not move `itqanCursor` to them.
- Do not remove them from legacy Murajaah visibility during migration; this avoids destructive loss of historical review coverage.
- New promotions after schema v3 enter Murajaah eligibility only after their ×40 consolidation is validated.
- Migration must be idempotent across repeated app starts/crash recovery.

## Independent cursors and state

Persist and preserve independently:

- `sabqiLineCursor`
- active Sabqi morning block and completion state
- same-day Sabqi evening session state and elapsed time
- `recentSabqi` and its order
- weekend recent-Sabqi session state and elapsed time
- active Itqan ×40 unit + repetition/reveal state
- `itqanCursor`
- `itqanRotationStart`
- unconsolidated promoted-range state
- `murajaahCursor`
- old-Itqan Murajaah actual end + elapsed time
- morning/evening completion records separately

Invariants:

- Murajaah never changes `itqanCursor`.
- Itqan never changes `murajaahCursor`.
- Sabqi never overwrites either cursor.
- Promotion never moves either cursor.

## Murajaah engine

Old-Itqan Murajaah uses one global `murajaahCursor` shared chronologically by:

Tuesday evening → Thursday evening → Saturday evening → Sunday evening → Tuesday evening → …

No weekly reset and no day-specific Murajaah cursor.

Tuesday/Thursday target duration: 60 minutes.
Saturday/Sunday target duration: 30 minutes.

The plan size is calculated from `murajaahSecondsPerLine`, not from a fixed page/line quota. Real validated end controls cursor advancement and speed calibration.

Only consolidated Itqan is eligible for new Murajaah work. Historical migrated promoted ranges remain visible under the migration compatibility rule above.

## Recent-Sabqi weekend engine

Saturday/Sunday morning is a dedicated recent-Sabqi session, not Murajaah block A.

- Duration is 30 minutes fixed.
- Iterate blocks in stable order.
- After the last block, wrap to the first and continue.
- Do not remove/promote/reorder material merely because it was reviewed successfully.
- Difficulty may be recorded as a signal only.
- If `recentSabqi` is empty, show a truthful empty-state session; never fall through to old Itqan.

## Same-day Sabqi evening engine

Monday/Wednesday/Friday evening is a dedicated persistent session using only the exact five-line block validated that morning.

- Duration: 30 minutes.
- Loop that same block for the entire duration.
- Persist elapsed time and completion independently of morning Sabqi.
- Survive Activity pause, app close, and reboot.
- Completion of the morning session must never automatically complete the evening session.

## Dashboard and Today launcher

Dashboard and runtime must use the same schedule engine and session-status model.

Required rows:

- Mon/Wed/Fri: morning Sabqi; evening same-day Sabqi 30 min.
- Tue/Thu: morning Itqan ×40; evening old-Itqan Murajaah 60 min.
- Sat/Sun: morning recent Sabqi 30 min; evening old-Itqan Murajaah 30 min.

Each displayed session includes type, range where known, duration, state, and resume/progress information where relevant.

The Today launcher must open the next incomplete scheduled session for the current day without conflating morning and evening completion.

## Reader and release protections

This engine change must not regress:

- non-deterministic random source-ink masking with session-stable draw and cumulative 25/50/75/100 stages;
- verse-number rosettes visible above masks;
- no linguistic word-mapping dependency;
- BOOX vertical-fit correction with no >100% Mushaf scaling and vertical safety margin;
- canonical 604 Mushaf assets byte-identical;
- Tafsir assets and Tafsir/Memorization boundary unchanged;
- no MP3/audio bundled into APK;
- package `com.quransafeguard.hifz`;
- no Safeguard/blocking/accessibility functionality.

## Tests required before release

Add explicit tests covering:

1. exact weekly morning/evening schedule;
2. Monday/Wednesday/Friday evening uses morning five-line block for 30 minutes;
3. Tuesday/Thursday evening uses old Itqan only for 60 minutes;
4. weekend recent-Sabqi loop repeats one or many blocks until 30 minutes;
5. no time redistribution and no old-Itqan fall-through from weekend morning;
6. one global Murajaah cursor sequence across Tue/Thu/Sat/Sun;
7. no weekly cursor reset;
8. Murajaah does not mutate Itqan cursor;
9. safe whole-verse promotion from recent Sabqi;
10. promotion adds canonical work scope without cursor jump;
11. promoted material is marked unconsolidated exactly once;
12. active Itqan unit resumes before selecting a new unit;
13. canonical cycle reaches promoted material naturally and applies ×40;
14. cycle resumes in canonical order afterwards and wraps at rotation start;
15. v2→v3 migration preserves cursors and state, is idempotent, and marks historical promoted material conservatively;
16. dashboard/runtime agreement;
17. morning/evening completion independence;
18. lifecycle pause prevents background time consumption;
19. speed calibration changes quantity, never fixed duration;
20. random-mask stability and BOOX vertical-fit regression contracts remain green.

## Contradictory audit / release gate

After implementation and normal CI, perform a deliberate adversarial audit for:

- false-positive tests;
- hidden time transfers;
- cursor corruption or resets;
- promoted ranges skipped or processed twice;
- migration duplication;
- active unit lost after restart;
- dashboard/runtime divergence;
- morning completion incorrectly completing evening;
- empty recent-Sabqi behavior;
- very low/high measured reading speed;
- mask randomness/stability regression;
- BOOX top/bottom crop regression;
- any leftover true-word mapping code or dependencies;
- any unexpected Mushaf/Tafsir/audio asset change.

Publication is NO-GO until this audit is green and the physical BOOX crop check is confirmed.

Additionally, before publication the user requires an independent **ChatGPT Work** audit of the final exact candidate SHA and CI/artifact evidence. No merge or publication may occur before that Work audit is reviewed and accepted.
