# Quran Hifz 0.7.5 PHONE Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the 0.7.5 PHONE UI and scheduling integration with schema 6 while preserving migration state, J10, consolidation, and local Al-Husary import.

**Architecture:** Keep historical persistence keys and internal mode constants unchanged. Canonicalize user-facing actions/states at presentation boundaries, expose two validated schema-6 range editors that atomically reconcile legacy runtime stores, and route weekly planning through the canonical cadence rather than the obsolete dual-session plan.

**Tech Stack:** Android Java, Kotlin core schedule, SharedPreferences schema 6, JUnit/source contracts, Android instrumented tests, GitHub Actions.

**Spec:** `docs/handoff/CLAUDE_QURAN_HIFZ_TEST_SPEC.md` plus frozen schema-6 decisions represented by `HifzPrefs.ProgressState/ProgressAction`.

## Global Constraints
- Canonical UI chain: Apprentissage → Appris → Stabilisation → Stabilisé → Consolidation → Acquis → Révision.
- M/W/F Apprentissage; Tue/Thu Stabilisation; Sat/Sun Révision.
- Consolidation is progression-driven, not a fixed weekday.
- Preserve local Al-Husary pack import unchanged.
- Preserve schema-6 migration, J10 dates, legacy read-only guarantees, counters and cursors.
- Do not change half-page pedagogical policy in this patch.
- PHONE before BOOX; no release/publish/merge.

---

### Task 1: Freeze PHONE feedback as RED contracts
**Files:** Test `hifz-app/src/test/java/com/quransafeguard/hifz/preview/CanonicalProgressUiSourceContractTest.java`
- [x] Write cadence, lexicon, dual-range-list, session-label and audio-preservation contracts.
- [x] Run CI and confirm only new contracts fail while historical JVM/core stay green.

### Task 2: Canonical cadence and user-facing terminology
**Files:** Modify `SettingsActivity.java`, `WeeklyDashboardPlanner.java`, `MainActivity.java`, `HifzSessionActivity.java`; tests above.
- [ ] Replace obsolete user-facing action labels with canonical names while retaining internal mode constants.
- [ ] Render M/W/F Apprentissage, Tue/Thu Stabilisation, Sat/Sun Révision.
- [ ] Remove fixed Sunday Consolidation from user-facing cadence.
- [ ] Rewrite Repères around four actions, three states, J10 and soft carryover.
- [ ] Run source contracts; expect terminology/cadence tests GREEN.

### Task 3: Independent À stabiliser / Acquis multi-lists
**Files:** Modify `HifzPrefs.java`, `SettingsActivity.java`; add focused unit/instrumented tests if needed.
- [ ] Expose two independent ordered disjoint range collections.
- [ ] Validate ranges fail-closed: canonical order, no self-overlap, no overlap between collections.
- [ ] Persist an edit atomically across schema-6 and legacy compatibility keys without moving cursors silently.
- [ ] Reconcile J10 membership/dates without inventing dates.
- [ ] Expose add/edit/delete for each list.
- [ ] Test persistence, overlap rejection, cursor preservation and transfer semantics.

### Task 4: Regression gates
**Files:** Existing migration/J10/consolidation/stabilization tests and workflow.
- [ ] Run 150 behavior JVM + source contracts + 20 core.
- [ ] Run schema 5→6, legacy J10, migration recovery, persistent state, consolidation persistence, half-page, state transitions and carryover emulator tests.
- [ ] Confirm local Al-Husary import contract unchanged.
- [ ] Record exact HEAD/run/counts; do not call GREEN while emulator is pending.

### Task 5: PHONE-only status-bar diagnosis
**Files:** Inspect `Ui.java` and `SettingsActivity.java`; no BOOX-specific change without evidence.
- [ ] Confirm whether inset padding is attached to scroll content and therefore scrolls away on PHONE.
- [ ] If fixing, use a generic system-bar-safe container change with a regression check; otherwise leave as documented PHONE observation for device comparison.
