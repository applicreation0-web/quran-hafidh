# Quran Hifz 0.7.3 — Final Work / Audit Handoff

Date: 2026-09-14  
Repository: `applicreation0-web/quran-unlock-android`  
Branch: `work/hifz-0.7.3-claude-all-fixes`

## Status

This document supersedes the former 2026-09-12 handoff and its obsolete release branch, candidate SHA, CI run, job, artifact ID, APK hash and verdict.

The release candidate is the exact repository HEAD checked out by the next official `Quran Hifz 0.7.3 final candidate` workflow run after this handoff update. The immutable candidate SHA and produced artifact facts MUST be taken from that run's `hifz-app-build-info.txt`, `hifz-app-sha256.txt`, `process-death-proof.txt`, workflow metadata and artifact metadata. No older run or artifact is release evidence.

## Final corrective delta already applied

- `acaf537f095e1f720d527e0e3a5dea28275a0d7b` — UI GREEN: Study reader hidden chrome uses `GONE` so the Mushaf recovers layout height; structured Hifz session title allows two lines.
- `3d4ad14c599bcb5fc861a7111756c99a8bad1f10` — official CI now proves real process death with an external `adb shell am force-stop`, relaunch, PID change and post-relaunch persistence verification.
- `9cc9bfd507f62b87e36583423e918f3d5884a3b4` — process-death persistence fixture covering in-progress anchoring range, FULL/LIGHT protocol state, block index, repetition counter and reveal/assistance state.
- `f3acb445aef6cb92a7ae97d8e32eaab8d31b3c7c` — deterministic J10 instrumented tests isolated from live wall-clock date.

Temporary correction workflows have been removed. The official full workflow is the sole CI release gate.

## Mandatory final CI evidence

The final official run on the exact post-handoff HEAD must complete all of the following successfully:

- canonical 604-page Mushaf restore/injection and geometry checks;
- Tafsir source and packaged-source integrity checks;
- core and application JVM suites;
- product-boundary, cosmetic and convergence gates;
- all Android instrumentation, including the UI regressions and J10 regressions;
- real process-death proof: fixture prepare → application PID captured → `adb shell am force-stop com.quransafeguard.hifz` → process absent → relaunch → different PID → fixture verification;
- unsigned release APK assembly;
- manifest/package/version/debuggable/security-boundary inspection;
- official evidence artifact upload.

Expected release identity remains:

- package: `com.quransafeguard.hifz`
- versionCode: `10`
- versionName: `0.7.3-boox`
- label: `Quran Hifz`
- audio delivery: separate local pack; no Husary MP3 embedded in the APK.

## M3 provenance rule

M3 is closed only by provenance tied to the exact final candidate. After the official run, the independent audit must record and verify:

- final candidate SHA from workflow checkout / `hifz-app-build-info.txt`;
- workflow run ID and job ID;
- artifact ID, exact artifact name, artifact ZIP digest and size;
- exact unsigned APK filename and independently recalculated SHA-256 matching `hifz-app-sha256.txt`;
- successful `process-death-proof.txt` with distinct before/after PIDs.

Those values are intentionally not hard-coded here before the run exists: committing post-run IDs back into this file would create a different, unaudited HEAD and break SHA provenance.

## Independent audit / release gates

Previous audit verdicts do not transfer to the new SHA. The exact final SHA must receive a fresh independent Work audit. B1/B2/B3, M1–M6, the real kill/restart blocker, M3 provenance and the two UI corrections must all be explicitly CLOSED before signing.

Signing remains local with the durable Quran Hifz JKS and is forbidden before independent GO. Publication remains NO-GO until the signed APK passes the mandatory physical BOOX test. No merge, signing or publication is performed by this handoff update.
