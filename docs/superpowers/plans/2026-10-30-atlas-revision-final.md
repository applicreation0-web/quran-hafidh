# Atlas revision integration

Goal: finish the existing four-pair QCF pilot on 7efa362c; preserve all Hifz engines,
initialization, receipts, backups and progression. User's 2026-10-30 mandate is the spec
and authorizes implementation and CI on the named branch without a new design round.

- [x] Add read-only session UI access: passive exact-page/corpus markers and toolbar;
  active access only after successful existing session validation. Endpoint taps and
  Reveal are not validation. No additional receipt or intermediate credit.
- [x] Retain the existing activity on the back stack; pause the existing clock before
  launching Atlas and rely on existing lifecycle to resume. Persist only transient
  view selection in Bundle. Test nested Atlas -> Lecture -> Back -> Revision.
- [x] Use the same paired-Mushaf icon in gutter and native UI. Crop genuine QCF SVG
  to marked words plus one context word, with exact source geometry.
- [x] Add behavioral and Android instrumented coverage for access, masks, clock,
  unchanged progress and backups; run existing Hifz/finals gates in CI.
- [x] Review complete diff, publish commits to the requested work branch, collect
  real emulator captures and report physical BOOX checks separately.

Review focus: failed validation must not unlock; unrelated/out-of-plan/stale-page
taps fail closed; a masked page must never acquire a relation marker; consultation
time never advances calibration; returning from nested Lecture cannot move endpoint.

Ruling: active comparison unlocks after the existing whole-session validation,
because no independent page-validation receipt exists. Cost: comparisons wait until
the session ends. No new validation protocol is introduced.

No release, installable deliverable APK, signing, or public publication. Android
instrumentation uses ephemeral test packages, as in the supplied validated audit.


Validation automatisée finale : 5ea88ab1, CI 41055535483 verte ; 14 tests Android, 285 contrats de source, quatre captures réelles. Le test actif vérifie le masque SVG effectivement peint avec une fixture de coordonnées canoniques, sans modifier les moteurs. Pack privé et BOOX physique restent à valider. Rapport : docs/audit/HAAFIDH_1179_ATLAS_FINAL_INTEGRATION_20261030.md.
