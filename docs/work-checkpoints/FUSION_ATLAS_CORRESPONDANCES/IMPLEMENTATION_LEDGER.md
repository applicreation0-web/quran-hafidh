# Fusion preparation — implementation ledger

Spec: user-supplied master mandate of 18 October 2026, sections 3–7.
Execution: native implementation; independent editorial lots run separately.

Ruling: editorial review and engineering preparation may run concurrently, as the mandate permits unvalidated features to remain disabled. No new runtime data is activated before its provenance and review gates pass. The old loader stays until real 120-row parity is proven. This is preparation, not removal of the old path.

## Task 1 — presentation catalogue parser

- [x] Add behavioral tests for variable 120/366 unit and 1053 passage sets, exact canonical bounds, duplicate IDs, missing approvals, provenance, and local intersections (5:11, 51:1–23).
- [x] Observe RED before adding the parser.
- [x] Implement `AtlasDisplayCatalog.parse(String, Map<String,int[]>, Map<String,int[]>)` inside the allowlisted `MawdudiMapData.java` module. Separate unit, passage and contextual keys; no Hifz dependencies or writes.
- [x] Run the behavioral tests on the real Java implementation with real org.json and JUnit. Record the isolated JVM harness limits (Android adapters are stubs; no Android/BOOX claim).
- [x] Keep this parser unconnected until the migration and editorial gates have evidence.

## Task 2 — migration and activation

- [ ] Import legacy metadata without turning historical user approval into source or independent certification.
- [ ] Compare all 120 original IDs, bounds and titles before any switch.
- [ ] Only after review, connect the catalogue to the unit, passage and local-caption display paths; preserve neutral fallbacks and exact canonical source dialogs.
- [ ] Existing frozen-engine, 23-icon, source and latest-head JVM/Android checks must remain intact.

## Task 3 — Correspondences / common UI / navigation

- [ ] Consume only independently reviewed relations, preserving all 110 dossier IDs in audit data.
- [ ] Use the existing QCF page and polygon pipeline; missing geometry never fabricates focus.
- [ ] Verify return state and absence of Hifz writes before enabling contextual entry points.
- [ ] Record phone, simulated BOOX and physical BOOX results separately; no APK authorization is inferred.

Task 1 verification: isolated JVM tests observed RED for missing parser; then GREEN 9 tests. A strict-boolean approval test then failed against permissive org.json coercion; strict booleans were implemented. Final suite: 13 tests on both external org.json and the Android-derived org.json implementation. The harness compiles the real MawdudiMapData.java, using real org.json/JUnit and test-only Android/index adapters. This is not Android framework or BOOX verification. Frozen 23-icon integrity and J6 source role checks passed; no runtime catalogue has been installed.

Latest mandate: Work_Quran_Haafidh_Complet (1).zip, SHA-256 e6138652111df555c7059c5c0d34c5c75ce09349c3506055055c0c49506e3431. The eight supplied relation types and exact SVGs now supersede the old five-type design. Centre is the hatched square with four horizontal strokes. Existing source reviews remain evidence; only conflicting classification/UX work is revisited.

Independent code review CP03-PARSER-01 identified Android getString coercion of null/nontext provenance. Reproduced RED using Android-derived org.json, fixed with strict String checks on every textual schema field; dual-library suite GREEN 13/13. Numeric coercion was also reproduced RED and rejected by exact-integer checks. No runtime caller or real catalogue was installed.


## CP03 optional loader preparation — 19 October 2026

The ordinary optional asset `atlas/atlas_display_catalog_v1.json` is now recognized by `MawdudiMapData.load`. No real catalogue is installed. An absent asset follows the legacy loader exactly; a present invalid or unreadable asset fails closed without falling back. The four existing approval gates remain mandatory, and the final-private gate still requires 366 admitted unit titles. The old checksum-pinned 120 loader is retained for rollback.

Pure display methods are exposed for units, full passages and exact unit/passage intersections. A local 5:11 caption never replaces its canonical 5:7–11 passage heading. `MawdudiMapScreen` has not been wired to the new passage/context methods. Source IDs, bounds, order, original headings and Hifz remain untouched.

Verification: 40 isolated JVM tests pass with both external and Android-derived `org.json`, including 11 existing Atlas source/context checks. The real changed loader and real canonical passage parser execute; Android and the 366-unit source loader remain test adapters, so this does not attest Android/BOOX behavior. A malformed UTF-8 test was observed failing before strict decoding was added. A temporary negative-control loader with optional selection disabled triggered 6 failures; the production source was not changed for that control. Full Gradle verification is unavailable here: no wrapper or installed `gradle`.

Private staging of the 120 historical metadata rows preserves IDs, bounds, runtime-trimmed titles and order. Four private Python tests pass. Historical user approval is retained; source, independent and redistribution flags remain false. The staged file stays outside the public repository and is not an asset. This proves metadata parity and unchanged legacy behavior; it does not certify new gated display parity, publication rights, or resolution of the private-pack architecture. Evidence and remaining gates: private `checkpoints/CP03/MIGRATION_STATUS.json`.
