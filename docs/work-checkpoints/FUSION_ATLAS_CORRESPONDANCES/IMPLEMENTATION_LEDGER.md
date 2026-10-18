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
