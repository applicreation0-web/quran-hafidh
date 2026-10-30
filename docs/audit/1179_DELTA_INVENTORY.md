# 1.17.9 delta and evidence inventory

Date: 2026-10-30. Baseline: `ae68ee1a46e195e9da1068aed981f3363869a4e0` (1.17.8). Audited incoming candidate: `0b2fb5ba25afe462fce6a0251da308a1382155fa`. This is the committed incoming delta, not an assertion about later finalisation edits. Refresh the inventory against the final commit before release. JSON contains every changed path, status, line counts and both Git blob IDs; CSV contains the same file rows. No branch was merged by this audit.

## Complete incoming delta

`git diff --numstat --no-renames ae68ee1a46e195e9da1068aed981f3363869a4e0 0b2fb5ba25afe462fce6a0251da308a1382155fa` yields **44 files, 4075 added lines, 306 deleted lines**.

| Subsystem | Files | Risk requiring evidence |
|---|---:|---|
| Android UI/adapters/correction state | 15 | Session routing, temporary reveal/replay, correction state, comparison markers/strips and reader links can change user behavior despite unchanged engines |
| JVM/source contract tests | 12 | Some old source assertions were replaced/shortened; green historical counts cannot substitute for fresh execution |
| Android instrumentation tests | 4 | Activity routing, QCF excerpts, corrections and marker display require actual Android execution |
| CI/workflow | 3 | Added audit jobs must be run on final candidate SHA; workflow existence is not evidence of execution |
| Documentation/plan | 2 | Historical green claims and scope claims need reconciliation |
| Android manifest/resource | 2 | WeakVersesActivity registration removed; non-exported AtlasDiscriminantActivity added; icon added |
| Research inventory | 5 | 291/292 provisional research rows are not proof of a complete verified runtime corpus |
| Audit runner | 1 | Android runner invokes selected classes; successful invocation and outputs still required |

`HAAFIDH_1179_ATLAS_FINAL_INTEGRATION_20261030.md` says the manifest was not modified. The actual delta contradicts that sentence. `MushafView.java` adds an Atlas discriminant tap callback and JS interface; its shared surface is therefore changed, even though reader.js is unchanged. Review actual diff, not broad "UI only" language.

## Proven unchanged in incoming delta

Fresh `git diff --name-only BASE CANDIDATE -- PATH` returned no paths for: all `hifz-core`, `hifz-app/src/main/assets/hifzreader`, `hifz-app/build.gradle.kts`, and these production Java files: `HifzPrefs`, `HifzBackup`, `ConsolidationCycleEngine`, `ConsolidationPhysicalUnitPolicy`, `SundayFinalPagePolicy`, `WeeklyProgramPolicy`, `WeeklyDashboardPlanner`, `ItqanMaintenancePolicy`, `ItqanRegimeStore`, `PostFinalRecallStore`, `PostFinalCumulativePolicy`, `PostFinalFamilyCreditLedger`, `GeometryRepository`.

This is byte preservation against 1.17.8, not proof that modified HifzSessionActivity calls the preserved methods at the correct time. In particular, preserve finals/receipt exclusivity, no new Itqan finale after An-Nas, correct post-final family routing, frozen OPEN-session `sourceSnapshot`, schema-6 progression and v3 restore behavior through fresh tests.

## Branch decisions from available remote refs

Remote-tracking refs were inspected locally; this audit did not independently establish that the server has no newer refs. JSON records exact SHAs and every non-patch-equivalent commit from `git cherry CANDIDATE BRANCH`.

| Remote branch suffix | Decision at incoming candidate |
|---|---|
| work/haafidh-1179-cloud-sync | Included by ancestry; tip equals 1.17.8 baseline. No cloud implementation delta is present in this ref |
| work/haafidh-1179-compare-tafsir-layout-20261030 | Included by ancestry |
| work/haafidh-1179-reader-revision-comparison-ux-20261030 | Included by ancestry |
| work/haafidh-1179-reveal-replay-mutash-final-20261030 | Candidate tip |
| work/haafidh-atlas-discriminants-pilot-20261029 | Included by ancestry |
| work/haafidh-1179-reader-revision-ux-debug-20261030 | Divergent; 2 patch-equivalent and 2 unique commits; manual review, no automatic merge |
| work/haafidh-1179-atlas-private-debug-20261030 | Divergent; 4 unique commits; isolated debug/delivery branch, no automatic merge |
| debug/haafidh-1179-boox-test-20261030 | Divergent; 7 unique commits; no automatic merge |
| debug/haafidh-1179-encrypted-delivery-20261030 | Divergent; 9 unique commits; no automatic merge |
| work/haafidh-1179-boox-ui-20261026 | Divergent; 9 unique commits; manual review |
| work/haafidh-active-correction-mutashabihat-20261029 | Divergent; 18 unique commits; some behavior may be independently reimplemented, but not ancestry/cherry equivalent |
| research/alabbad-mutashabihat-stricts-20261030 | Divergent; 6 unique commits; research branch, not automatically production-ready |

A positive `git cherry` result means patch identity differs, not that the whole feature is missing. Ancestor inclusion proves history only, not desired behavior.

## Existing regression coverage and limits

No additional duplicate tests were added by this inventory task. Existing cases already address the identified boundaries; they still need fresh execution on final candidate.

| Area | Existing tests | What they actually establish when passing |
|---|---|---|
| 6/12/18-month dated loops | `M8TargetedLongitudinalRegressionTest` | 180/365/548 actual daily policy loops, late dates, 119–122/179–180/364–365/547 boundaries, 120-day display retention vs durable receipts, programme transitions, restart through real preference adapter and low-level V6 promotions. Starts at surah 49; five physical lines/week; forces a near-An-Nas rotation on days 170/171. Does not simulate every UI lesson or Quran-wide traversal |
| Large progression/snowball | `LongHorizonHifzPersistenceStressTest` | 1000 canonical QCF lines through three promotions with repeats; 52 weekly snowballs and duplicate close rejection; no full Android process or real disk SharedPreferences |
| Post-final horizons | `PostFinal1176HorizonStressTest` | 240 scenarios using horizon-offset dates, 20-page cap, restart/repeated advance. It does **not** execute 180/365/548 successive days per scenario |
| Week boundary/finals | `SevenDayCalendarMigrationTest`, `SundayFinalSessionProtocolTest`, `SundayFinalPagePolicyTest`, `LateSabqiFinalDateTest`, `SundayPageFinalCreditSafetyTest` | Policy/credit/date boundaries, not physical BOOX installation |
| Frozen OPEN-source recovery | `SnowballValidationIdempotenceTest`, `ConsolidationOpenSlotSafetyTest`, `ContradictoryAuditTwoPersistenceTest` | Frozen snapshot survives reopen; new accumulator work cannot be swallowed; legacy no-sourceSnapshot cases recover a prefix or distinct fallback |
| After An-Nas | `PostNasWeeklyConsolidationTest`, `ItqanPostNasMaintenanceTest`, `ItqanFullOnlyPostNasSourceContractTest`, `ItqanFourSessionStoreTest` | Post-Nas policy/receipts, no new ordinary/final Itqan window; source checks alone cannot prove runtime behavior |
| Schema/restore | `HifzBackupSchemaValidationTest`, `BackupSchemaRejectedInstrumentedTest`, `ProcessDeathPersistenceInstrumentedTest` | Format validation and actual Android restore/recovery only when those relevant cases run successfully; incoming backup source unchanged is insufficient |
| Sunday/Monday Android resumption | `SundayFinalPersistenceInstrumentedTest`, `SundayFinalProcessDeathInstrumentedTest`, `SundayMondayResumeInstrumentedTest` | Instrumented persistence/activity boundary coverage; evidence must identify selected cases and process-death mechanism |

The 6/12/18-month tests are complementary bounded harnesses, not exhaustive 18-month device usage. No claim is made that every surah boundary is traversed by the M8 harness. Run geometry/real-QCF tests plus explicit UI journeys where needed.

## Fresh commands run by this inventory audit

| Command | Actual outcome |
|---|---|
| `python3 scripts/verify_1178_family_invariants.py` | **FAIL**, exit 1: frozen MushafView differs from older pin |
| `python3 scripts/verify_1176_invariants.py` | **FAIL**, exit 1: frozen MushafView differs from older pin |
| `bash scripts/guard_frozen_hifz_engines.sh` | **FAIL**, exit 1: added adapters outside allowlist and older exact pins including DashboardLedger/vocabulary/HifzPrefs/MushafView; no exemptions changed by this task |
| `python3 scripts/test_1172_private_upgrade.py` | **PASS**, 7 synthetic/mocked old-version identity tests; not a 1.17.9 signed-upgrade test |
| Full Gradle/Android candidate tests | Not run by this task; coordinator records actual fresh results separately |

The failed guards contain historical baselines and do not themselves demonstrate a new scheduler defect; equally, they cannot be counted as green. A deliberate reviewed pin/contract update plus rerun is required. Old green audit documents are historical evidence only.

## Identity, certificate and private-resource boundary

Incoming candidate still has official base application ID `com.quransafeguard.hifz`, **versionName 1.17.8 / versionCode 46**. This is not yet 1.17.9 release metadata. Debug uses isolated suffixes (`.ibnkathirtest`, `.ayahthemes114private`, `.boox1175debug`, or pilot suffix) and cannot prove an official in-place upgrade.

The preserved build guard requires official certificate SHA-256 `66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14` and seven exact private asset SHA-256 checks. These fingerprints are public identity/integrity data; no signing key, password or private corpus is reproduced here. `verify_haafidh_1174_update_compatibility.py` still hardcodes target 1.17.4; `test_1172_private_upgrade.py` targets 1.17.2. Neither is a ready-made genuine 1.17.9 acceptance result.

Remaining real release proof requires actual baseline and candidate signed APK metadata, same official package and certificate, strictly increased code over installed version, expected 1.17.9 name, APK v2 signature verification, seven packaged asset hashes, and a backed-up physical BOOX `adb install -r` upgrade with state comparison. Never infer that from a debug APK or mocked verifier. This inventory authorizes no GO claim.

## Finalisation supplement: current baseline guard

The JSON/CSV above remain the reproducible **incoming committed** inventory. Finalisation adds separate review/test/CI changes and release metadata; do not silently reinterpret the 44-file count as the final worktree count.

`python3 scripts/verify_1179_invariants.py` now provides the current-baseline production guard. It compares actual worktree bytes, including new files, to official 1.17.8 `ae68ee1a46e195e9da1068aed981f3363869a4e0`. All core source, app production source/assets and private assets must remain exact outside 15 individually named adapters and the reviewed manifest/icon. The build file must reconstruct exactly from baseline with only versionCode 46→47 and versionName 1.17.8→1.17.9 replacements. This retains official package, certificate fingerprint, private asset hashes, signing configuration, and every other build byte. It does not permit blanket scheduler/persistence exceptions.

Fresh initial finalisation run: **PASS**. Mutation probes, performed in memory without changing shared files, confirmed **rejection** of forbidden changes to HifzPrefs, HifzBackup, HifzV6Migration, HifzRecoveryApplication and build configuration. Historical 1171/1176/1178 guards remain unchanged and their recorded failures remain expected against the new reviewed adapters; this new result is not a retroactive green result for those guards. Fresh final-candidate rerun is still required after subsequent edits.

`AtlasDiscriminantSourceContractTest` was updated to the revised user contract while preserving offline/source-authenticity, exact QCF/word geometry, non-exported activity and immutable Hifz-state checks. It now requires indexed page access, no second bottom chain action, one grouped 44px marker after selecting its anchor, a reserved outside gutter, valid SVG attribute escaping, and the contextual full Jalalayn panel rather than buried columns. Standalone JDK 17/JUnit 4.13.2 run after approved catalogue expansion: **10 tests passed**. Isolated source-mutation probes confirmed the test rejects the original malformed SVG attribute escapes, an extra marker created per verse, and an invented explanation in generated catalogue data. These are source contracts; actual visible marker shape and device layout require instrumented evidence.
