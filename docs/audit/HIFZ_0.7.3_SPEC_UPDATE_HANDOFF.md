# Quran Hifz 0.7.3 — Claude audit correction handoff

Date: 2026-09-13
Repository: `applicreation0-web/quran-unlock-android`
Working branch: `work/hifz-0.7.3-claude-all-fixes`
Claude-audited base: `ca83a714df4b7cac5d445e70e9cb512ee204e7b3`
Previous blocker-fix checkpoint: `67d5e8e68ab1d37105170895072b70b731012858`
Application correction SHA: `33fc02521193d2a129ab0540aef2057aa8662e4d`

## Explicit CDC decisions

- J10 priority group: maximum **5 physical lines** in production.
- Ancrage / `ITQAN`: **reusable J10 host capacity**, including its 60-minute envelope.
- A J10-hosted slot is accounted separately and must not falsely credit the normal Ancrage protocol.

## Claude findings closure map

- B1: long accessibility labels `Retirer une répétition` / `Ajouter une répétition` remain restored with explicit add/remove icons.
- B2: evidence-only blocker; closure requires the final official run to execute Android instrumentation, assemble the release APK, inspect it, and upload the complete evidence artifact.
- B3: an in-progress Anchoring unit is pinned during reconciliation and its protocol is resolved by persisted start/end range, not by queue head; Today and session use the same resolved entry.
- M1: `MAX_PRIORITY_LINES = 5`, with production-value regression coverage.
- M2: `SessionKind.ITQAN` is reusable J10 capacity and a reusable host; host budget persistence accepts ITQAN.
- M3: this file replaces the missing spec-update handoff and records the exact application correction SHA.
- M4: final CI workflow trigger is updated in the following CI-only commit so this working branch launches the official pipeline.
- M5: temporary J10/Astra/Claude TDD workflows are removed from the final candidate tree.
- M6: malformed Anchoring queue entries fail open, are removed and persisted, the queue is marked for reconciliation, and dashboard rendering has a runtime fallback instead of crashing the home screen.
- m1: behavioral coverage includes protocol continuity, a complete three-block fractionated Anchoring progression, and the production five-line J10 cap.
- m2: host-slot persistence is covered by instrumented JUnit, replacing the temporary source-string script.
- m3: returning from J10 suppresses exactly one immediate host resume instead of relying on a 500 ms timer.
- m4: `SpeedCalibrationPolicy` / test are renamed to `SpeedCalibration` / `SpeedCalibrationTest` with behavior unchanged.

## Pre-CI independent correction audit

The correction SHA was compared directly with checkpoint `67d5e8e68ab1d37105170895072b70b731012858`. The earlier validated B3 helper and test were explicitly preserved after detecting and rejecting an atelier-history regression during transplant.

Local source gates passed before the official run:

- `scripts/test_hifz_claude_audit_regressions.py`
- `scripts/verify_hifz_mask_hotfix.py`
- `scripts/test_hifz_mask_segments.js`
- `scripts/test_hifz_vertical_fit.js`

The final run HEAD is intentionally a later documentation/CI-only commit. No application source may change after the application correction SHA above without repeating the correction audit.

## Publication gate

Publication remains NO-GO until the official final run is fully green and its artifact contains at minimum:

- the unsigned release APK;
- `hifz-app-sha256.txt`;
- `hifz-app-apk-list.txt`;
- `hifz-app-manifest.xml`;
- Android instrumented test results;
- JVM test results;
- exact build-info / HEAD provenance.

After that evidence is independently checked, the validated APK may proceed to the established local signing flow. No keystore material or signing secret belongs in this repository.
