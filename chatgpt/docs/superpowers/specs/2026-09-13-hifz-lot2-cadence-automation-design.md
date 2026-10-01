# Quran Hifz 0.7.3 — Lot 2 Cadence Automation Design

Date: 2026-09-13
Base SHA: `c23428ccf50e5d5bd658edad47b79bd86e4c630c`
Branch: `feat/hifz-0.7.3-lot2-cadence`

## Goal

Automate workload sizing from one measured cadence while preserving every validated Hifz invariant: fixed session durations, Sabqi ×37, Itqān ×40, independent cursors, Cycle + Snowball order, BOOX reader geometry, masking, assets, and user-owned corpus bounds.

## Source of truth for cadence

There is exactly one runtime cadence used for workload sizing:

`murajaahSecondsPerLine`

It is measured only from old consolidated Itqān Murājaʿah, because that mode already has a real end marker and a reliable mapping from verse range to line count.

Calibration rules stay conservative:

- active foreground time only;
- ignore samples below 20 lines;
- ignore samples below 300 active seconds;
- measured cadence = active seconds / validated lines;
- new cadence = 70% old + 30% measured;
- clamp each accepted recalibration to ±10% of the previous cadence;
- invalid/non-finite/non-positive stored values fall back to the initial 9.0 seconds/line;
- one anomalous session must never radically resize future work.

No separate Sabqi-recent reading speed is measured. `recentSecPerLine` is legacy state only and must not drive runtime planning after Lot 2.

## Fixed-time invariant

Time is the invariant. Quantity is derived.

- Mon/Wed/Fri evening Sabqi du jour: 30 min fixed.
- Tue/Thu old-Itqān Murājaʿah: 60 min fixed.
- Sat/Sun recent Sabqi morning: 30 min fixed.
- Sat/Sun old-Itqān Murājaʿah evening: 30 min fixed.
- Sabqi ×37 stays exactly ×37.
- Itqān ×40 stays exactly ×40.
- No minutes move between modes.
- No faster/slower cadence may alter the repetition counts.

## Workload sizing

A single pure cadence helper owns workload calculations so formulas cannot diverge between screens.

For a positive duration and cadence:

`targetLines = floor(durationSeconds / secondsPerLine)`

Murājaʿah may use any positive line count.

Recent-Sabqi sizing is block-safe:

- five-line blocks are the atomic unit;
- capacity is rounded down to a whole five-line block;
- at least one five-line block is retained when recent material exists;
- sliding-window promotion still occurs only on safe whole-verse boundaries.

The same `murajaahSecondsPerLine` drives:

1. old-Itqān Murājaʿah planned quantity;
2. recent-Sabqi sliding-window capacity;
3. Wednesday/Friday advisory recent-Sabqi micro-review quantity.

## Wednesday / Friday micro-review

This is deliberately an advisory reminder, not a third scheduled completion gate. That avoids changing the canonical two-session daily schedule and avoids any cursor or completion-state regression.

On Wednesday and Friday, when recent Sabqi exists, the home screen may show one compact non-blocking line:

`Sabqi récent · 5–10 min · X–Y lignes`

Where X and Y are cadence-derived five-line-safe quantities for 5 and 10 minutes.

Rules:

- it never replaces or delays the scheduled morning/evening sessions;
- it creates no new cursor;
- it creates no new completion record;
- it does not promote, reorder, or consolidate material;
- it disappears when recent Sabqi is empty;
- it is informational only and must stay visually lightweight.

## Sabqi and Itqān parameter semantics

User-editable bounds remain user-owned.

### Sabqi

Editable:

- Début Sabqi
- Fin Sabqi

Automatic/read-only:

- live Sabqi cursor
- recent queue
- promoted coverage

Cadence never changes Sabqi bounds.

### Itqān

Editable:

- base Itqān ranges
- Itqān rotation start

Read-only:

- effective Itqān corpus = normalized union of base Itqān ranges + Snowball-promoted ranges;
- unconsolidated promoted ranges still requiring ×40;
- Itqān cursor;
- Murājaʿah cursor.

The effective corpus may be discontinuous. The UI must therefore show real ranges, not pretend that one min/max pair is a continuous interval.

Promotion grows the effective Itqān corpus canonically but never moves `itqanCursor` or `murajaahCursor`.

## Settings UI

Under `Itqān · plages`, keep the existing editable base ranges.

Add a compact read-only `Corpus Itqān réel` section below them:

- each normalized effective range on its own compact row;
- count/summary of promoted coverage;
- `À consolider ×40` summary, with range details when non-empty;
- current Itqān cursor;
- current Murājaʿah cursor.

No promoted range gets an edit/delete control.

## Persistence and migration

Schema v3 remains valid.

No schema bump is required merely to stop using `recentSecPerLine`. Existing installations may retain the legacy key silently; runtime planning must no longer read or write it.

This avoids destructive migration risk.

## Baseline build blocker

Before Lot 2 application behavior is changed, repair the existing Kotlin DSL compile blocker in `hifz-app/build.gradle.kts` without changing application behavior.

The blocker is namespace shadowing around fully-qualified `java.util.Base64` and `kotlin.math.*` references inside the Gradle Kotlin DSL. The repair must use explicit imports/local symbols and be verified by the full official candidate workflow.

Lot 2 must not be declared complete on top of a red baseline.

## Non-regression invariants

The following are release blockers if changed unintentionally:

- package `com.quransafeguard.hifz`;
- versionCode 10 and versionName `0.7.3-boox` unless explicitly authorized otherwise;
- exactly 604 canonical Mushaf pages;
- no Mushaf reflow;
- no reader crop regression;
- WebView built-in zoom remains disabled;
- mask randomness/session stability/cumulative percentages stay intact;
- verse rosettes stay above masks;
- no MP3/audio bundled in APK;
- Light/Plus/Tafsir boundaries unchanged;
- no Safeguard blocking/accessibility surface;
- Sabqi ×37 unchanged;
- Itqān ×40 unchanged;
- fixed 30/60 minute schedule unchanged;
- no hidden time redistribution;
- no weekly cursor reset;
- promotion never jumps either cursor;
- Murājaʿah never changes Itqān cursor;
- Itqān never changes Murājaʿah cursor;
- user-edited base ranges remain distinguishable from Snowball growth.

## Tests required

1. Gradle script compiles and official candidate workflow reaches application tests/assembly.
2. Cadence helper returns deterministic line targets from minutes + seconds/line.
3. Invalid cadence falls back to 9.0 s/line.
4. Recalibration rejects <20-line and <300-second samples.
5. Recalibration uses 70/30 blend and ±10% clamp.
6. Murājaʿah planning uses the shared helper and the single stored cadence.
7. Recent-Sabqi capacity uses `murajaahSecondsPerLine`, never `recentSecPerLine`.
8. Recent capacity is five-line block-safe.
9. Wednesday/Friday advisory computes 5–10 minute line range from the same cadence and remains non-blocking.
10. No advisory appears when recent Sabqi is empty or on other weekdays.
11. Effective Itqān ranges equal normalized base + promoted coverage.
12. Unconsolidated promoted ranges remain read-only and visible as pending ×40.
13. Editing base ranges never mutates promoted ranges.
14. Snowball promotion never changes Itqān or Murājaʿah cursors.
15. ×37, ×40 and canonical schedule constants remain unchanged.
16. Existing Claude audit contracts, mask contracts, vertical-fit/no-crop contracts, product boundary checks, asset checks and official regression suite all remain green.

## Release gate

Lot 2 is GO only when:

- the baseline Gradle blocker is repaired;
- all targeted Lot 2 tests are green;
- the complete official CI is green on one exact SHA;
- APK assembly succeeds;
- package/version/debuggable/assets are re-verified;
- contradictory audit finds no cursor, schedule, Snowball, cadence, BOOX, mask or asset regression;
- physical BOOX crop verification remains a separate final device gate.

No merge, signing or publication is authorized by this design.