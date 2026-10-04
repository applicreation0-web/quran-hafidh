# Quran Hifz 0.7.3 — J10 Review Guarantee Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Guarantee that every acquired physical Mushaf line is effectively recited at least once every 10 calendar days, while preserving the existing weekly programme and fractionated Ancrage behavior.

**Architecture:** Add a separate J10 persistence layer keyed by canonical physical line IDs, avoiding any Hifz schema bump. Existing structured sessions credit only lines actually completed. A pure policy computes J10/J9 urgency, J8/J7 smoothing under pressure, and a 10-day sustainability forecast. A small priority review activity gates entry to normal weekly sessions only when the policy requires it; the normal session remains unchanged and resumes after priority review.

**Tech Stack:** Java/Android SharedPreferences, existing `GeometryRepository`, existing `HifzSchedule`, JUnit4, Android instrumented tests, GitHub Actions.

**Spec:** Conversation decisions fixed on 2026-09-13: acquired Leçon, completed Ancrage and completed fractionated Ancrage must be re-recited within 10 days by any weekly session; J10 work takes precedence when needed; at minimum warn when the 10-day load is no longer sustainable.

## Global Constraints

- Baseline application SHA: `31fcfe6ec6c1728f8f6cfdea1f281e274ea41de5`.
- Work branch: `work/hifz-0.7.3-j10` created exactly from the baseline SHA.
- Preserve current weekly calendar and session protocols.
- Do not reorder persisted Ancrage queues or alter fractionated Ancrage block progression.
- Do not touch Mushaf rendering, audio artifacts, signing, release, merge or publication.
- Keep `SCHEMA_VERSION = 4`; J10 state uses a separate versioned SharedPreferences namespace.
- Calendar-day semantics: a review on day D is next due on D+10.
- Bootstrap existing acquired material at the feature activation date; do not fabricate historical per-line review dates.
- New configured acquired groups begin their J10 clock when first synchronized into the J10 store.
- Only completed/effective recitation events credit J10; mere display/opening never credits.
- Final candidate remains NO-GO until fresh full CI, exact APK inspection and contradictory audit.

---

### Task 1: Pure J10 policy — RED then GREEN

**Files:**
- Create: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/J10ReviewPolicyTest.java`
- Create after RED: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPolicy.java`

**Interfaces:**
- `deadline(LocalDate lastReviewed)` -> `lastReviewed.plusDays(10)`.
- `ageDays(LocalDate lastReviewed, LocalDate today)` -> non-negative calendar age.
- `forecast(Collection<LocalDate>, LocalDate, double secondsPerLine, int availableMinutes)` -> required lines/minutes, available minutes, deficit and status.
- `shouldPreempt(int ageDays, Sustainability status)` -> J10/J9 always, J8 under tension/non-tenable, J7 only non-tenable.

- [ ] Write failing tests for J9/J10 boundary, 10-day forecast window, exact tension/non-tenable thresholds and J7/J8 smoothing.
- [ ] Run `:hifz-app:testDebugUnitTest` and prove failure because `J10ReviewPolicy` is absent.
- [ ] Implement minimal pure policy.
- [ ] Re-run unit tests and prove green.

### Task 2: Persistent line-level J10 store

**Files:**
- Create: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewStore.java`
- Create: `hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/J10ReviewStoreInstrumentedTest.java`

**Interfaces:**
- Separate prefs namespace `quran_hifz_j10_v1`.
- `syncAcquired(Collection<String> lineIds, LocalDate seedDate)` atomically adds missing lines at seedDate and removes stale lines.
- `markReviewed(Collection<String> lineIds, LocalDate date)` atomically updates only already-acquired lines.
- `snapshot()` returns immutable line ID -> review date state.

- [ ] Write instrumented tests for bootstrap, persistence, selective update and stale-line removal.
- [ ] Implement store using one JSON object and synchronous `commit()` for progression-critical writes.

### Task 3: Acquired corpus synchronization, priority grouping and capacity forecast

**Files:**
- Create: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPlanner.java`
- Create: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/J10CapacityTest.java`

**Interfaces:**
- Acquired set = `HifzPrefs.effectiveItqanRanges()` union current `recentSabqi` physical lines.
- `syncAcquired(...)` maps those ranges to canonical physical line IDs.
- `forecast(...)` uses calibrated maintenance seconds/line and remaining target minutes from current day through the next nine calendar days.
- `nextPriorityGroup(...)` selects the oldest required contiguous lines, maximum 5 physical lines, without changing any Hifz cursor.
- Capacity may use Reprise, Ancrage, Consolidation and Entretien minutes because J10 may take precedence over those sessions; `SABQI_NEW` has target 0 and contributes no assumed capacity.

- [ ] Test exact capacity from representative weekly windows and completion subtraction for today.
- [ ] Implement planner and group selection.

### Task 4: Credit existing structured sessions

**Files:**
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java`

**Interfaces:**
- Leçon neuve validation credits its exact 5 line IDs.
- Reprise du soir credits that day’s exact 5-line block when its timed session completes.
- Consolidation `Revu` credits that exact recent block; `À renforcer` does not automatically credit.
- Ancrage success credits `currentLineIds`; fractionated intermediate/final success credits only the active sub-block; failed/deferred Ancrage does not credit.
- Entretien validation credits only the physical lines actually traversed through the user-selected real endpoint.

- [ ] Add store initialization.
- [ ] Add one small helper for line-index intervals and one for actual Entretien traversal.
- [ ] Add credit calls only after the existing progression write succeeds.
- [ ] Preserve all existing queue/cursor writes exactly.

### Task 5: J10 priority gate and sustainability alert

**Files:**
- Create: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java`
- Modify: `hifz-app/src/main/AndroidManifest.xml`

**Interfaces:**
- Every structured `openMode(mode)` first synchronizes acquired J10 state.
- If `nextPriorityGroup` exists, open `J10ReviewActivity` with the intended mode as continuation; otherwise open the normal mode directly.
- Priority UI displays the canonical Mushaf lines with no mask; `Revu` is the only action that credits and advances.
- When no priority group remains, automatically continue to the intended structured session.
- Home shows hidden/normal state when sustainable, a concise tension warning near the threshold, and a mandatory non-tenable warning with exact required/available/deficit minutes.

- [ ] Implement activity without changing Mushaf renderer behavior.
- [ ] Add home gate and alert.
- [ ] Register activity without new permissions or services.

### Task 6: Source/contract regression and exact final verification

**Files:**
- Create temporary TDD workflow on work branch for RED/GREEN evidence, remove before final candidate.
- Use a separate helper CI branch to verify the exact final candidate SHA, following the previous exact-candidate pattern.

- [ ] Prove RED on test-only commit.
- [ ] Prove GREEN after implementation.
- [ ] Remove temporary workflow from final candidate.
- [ ] Compare final candidate against `31fcfe6...` and confirm only J10 logic/tests/docs/manifest/session/home integration changed.
- [ ] Run full unit/contract suite.
- [ ] Run Android instrumented tests on emulator.
- [ ] Run `assembleRelease` and inspect package/version/604 pages/no embedded Husary/no debuggable/no Accessibility/QUERY_ALL_PACKAGES.
- [ ] Recalculate APK SHA-256 and upload unsigned audit artifact.
- [ ] Do not merge, sign, release or publish.
