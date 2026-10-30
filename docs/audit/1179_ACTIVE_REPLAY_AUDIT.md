# 1.17.9 active-review corrective replay audit

Base inspected: `0b2fb5ba25afe462fce6a0251da308a1382155fa`.
Scope: automatic revealed-page ledger, corrective state machine, Activity rendering/assistance, and a focused full-mask entry point in MushafView. No acquisition, consolidation, initialisation, backup, or cursor engine is replaced. The normal active-review landmark/cue policy is preserved.

## Findings and changes

1. **Reveals could disappear across a date change before replay started.** Only a pending READ/RECALL checkpoint restored its date; a reveal ledger alone did not. A subsequent reveal could overwrite the older ledger because reads for another date returned an empty set. The Activity now restores the ledger's original date before plan rendering. Writes for another date/cursor refuse to replace unfinished reveals. Invalid or inconsistent recovery state blocks active-review rendering rather than silently validating without its pages. A persisted validation receipt permits cleanup after a crash between receipt and correction cleanup, even though the cursor already advanced.
2. **Stale callbacks could remove assistance or rewind a completed page.** Store transitions accepted any supplied State with the appropriate stage. Passing the pre-reveal RECALL state to `pageRecalled` succeeded after assistance had been persisted. Passing an old READ state after page completion rewound the replay. Every transition now verifies the complete supplied checkpoint against the persisted checkpoint before writing.
3. **Corrupt stage combinations could bypass remaining pages.** A restored DONE stage previously accepted any page index, and READ/DONE could carry an impossible assisted flag. Two additional regression tests first produced the expected assertion failures. State validation now requires DONE at the last page and assistance only in RECALL. Canonical verse validity is already enforced by VerseRef construction. Endpoint ordering is deliberately not restricted: acquired-corpus wrap and multiple laps are existing valid active-review behavior; a regression preserves an end before the start. Store transitions run on the Activity main thread; persisted-state equality rejects stale callbacks but is not advertised as a cross-process transaction/lock.
4. **Corrective recall retained normal active-review word holes.** Reusing the normal prepared active page preserved six landmarks and Ayah Themes starts. Corrective RECALL now uses all physical page lines, no selection clipping, no word landmarks, and no semantic holes. The raw SVG is hidden in initial HTML; its visibility is released only after the reader has produced a nonempty 100% mask with no protected-word holes. Existing page/generation bridge guards are preserved. Missing mask geometry does not count as a completed recall. Ordinary active review keeps its existing anchors. Verse rosettes remain governed by the existing renderer.
5. **Fail-open display and audio could escape assistance accounting.** An ordinary active page displayed fully visible because cue geometry is unavailable is now recorded before loading it. Audio consultation records the active page, or marks the current corrective recall assisted before audio opens. Audio is stopped when corrective phases/pages change. Explicit reveals still commit assistance before removing the mask. Assisted recall cannot advance; restart returns that page to visible READ before another recall attempt.

6. **Saved Lecture annotations could overlay recalled text.** The shared AnnotationOverlayView paints persisted strokes even when drawing is disabled. Hiding annotation controls alone therefore left handwritten mnemonics visible during normal active review and corrective RECALL, without setting the assisted flag. Active-review Activity construction now sets the complete overlay GONE in all phases, while other modes retain their current visibility and saved notes remain untouched. The Android regressions seed real AnnotationStore strokes and assert GONE during normal active display, corrective READ, corrective RECALL and recreated RECALL; exact persisted note JSON and points must survive. This UI regression is not executed locally because no Android runtime is available; its pre-fix failure is established by the default VISIBLE overlay and unconditional onDraw stroke loop, not claimed as a locally observed instrumented red run.

Selection remains once per unique physical page, in first-reveal order. Manual verse flags are not read. Replay never calls acquisition-credit APIs or advances the validated cursor. The original endpoint, original page, original active elapsed time, and original session date remain frozen until the existing active-review validation receipt commits.

## Verification actually performed

- Added `ActiveReviewCorrectionStoreTest` with real production Store/Plan code and the existing in-memory/fault-injecting preference implementations.
- Before the store fix, the isolated JUnit run produced **three expected assertion failures**: later-date ledger overwrite, stale unassisted validation after help, and stale READ rewinding an already completed page.
- After the fix, the isolated JUnit run passed **12 tests**: nine store tests and the existing three plan tests. Cases include repeated reveals, insertion order, partial replay reopen, frozen date/time/page/endpoint, original-credit/cursor preservation, receipt crash-gap cleanup date, rejected mismatched cursor, and failed persistence not advancing a phase.
- Isolated command used `java com.sun.tools.javac.Main` with Android/JUnit/JSON jars followed by `org.junit.runner.JUnitCore` for `ActiveReviewCorrectionStoreTest` and `ActiveReviewCorrectionPlanTest`. Only the scratch dependency substitutes for `VerseRef` and `GeometryRepository.parseVerse` were used; production Android Activity/WebView code was **not** exercised by that isolated run. This is state-machine evidence, not full-build or device evidence.
- `git diff --check` passed at handoff.

## Android regression coverage added, execution pending

`ActiveReviewCorrectionInstrumentedTest` now also exercises:

- Resuming the original reveal-ledger date before any corrective replay checkpoint exists, despite a later requested launch date.
- Real Activity visible READ followed by a real QCF WebView RECALL. DOM assertions require mask 100, visible prepared SVG, a nonempty mask layer, zero protected-word boxes, no hole mask, and no Atlas markers.
- A real press/release of Révéler persists help, disables successful recall, and remains assisted after Activity recreation with the original date, endpoint and cursor.

Existing Android tests also cover ordered duplicate reveal deduplication, READ/RECALL/DONE persistence, assisted restart, original-page retention, completion without extra receipt mutation, and preservation of a corrupt checkpoint.

These Android tests have **not been run in this environment** at this handoff. No emulator/KVM is available. Recreation and reopening preferences simulate lifecycle restart; they are not evidence of an actual operating-system force-stop, sudden power loss, or BOOX E-Ink paint timing. The DOM probe verifies the mask layer and absence of holes; it is not a pixel-by-pixel audit of every glyph on all 604 pages. Audio and fail-open paths were traced in source but do not yet have dedicated instrumented execution. No screenshot or device success is claimed.

Required full project checks, coordinated with the main task owner:

```sh
gradle :hifz-app:testDebugUnitTest :hifz-app:compileDebugAndroidTestJavaWithJavac
```

Required Android execution on an available emulator/device:

```sh
gradle :hifz-app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.ActiveReviewCorrectionInstrumentedTest
```

The main task's consolidated validation report must record full Gradle results and any unrelated failures by name. No secrets, user backups, signed APK, screenshot, or private headings corpus is published by this audit.

## Explicit external force-stop harness

Two additional opt-in methods in `ActiveReviewCorrectionInstrumentedTest` are skipped in normal bulk runs unless instrumentation argument `activeReplayForceStop=true` is supplied:

- `forceStopPrepare`: uses only the separate synthetic `haafidh_active_replay_force_stop_fixture` preference file. It commits duplicate/ordered reveals, completes the first page, and leaves the second page in assisted RECALL with an old fixed date and frozen endpoint/time/page. It commits exact expected ledger/state strings and the preparation process PID.
- `forceStopVerify`: must run in a new instrumentation process after the CI harness externally force-stops the target package. It checks exact persisted checkpoint strings and frozen fields, rejects advancing assisted recall and overwriting with a later date, completes restart/READ/RECALL, checks unchanged synthetic receipt/cursor/endpoint, and clears the fixture in `finally`. No global date/clock is changed and no production preference file is touched by these methods.

Parent CI must run these method filters separately, with `am force-stop <target applicationId>` between them, and pass `-e activeReplayForceStop true` on both instrumentation launches. Full method filters:

```text
com.quransafeguard.hifz.preview.ActiveReviewCorrectionInstrumentedTest#forceStopPrepare
com.quransafeguard.hifz.preview.ActiveReviewCorrectionInstrumentedTest#forceStopVerify
```

This harness is added but has not been executed locally. A distinct process PID alone proves a process boundary; actual force-stop evidence must come from the external CI commands/logs. Its fixed old date and rejection of a new date exercise ledger continuity, not a device clock change. Activity/WebView restoration is tested separately by the recreation tests above.
