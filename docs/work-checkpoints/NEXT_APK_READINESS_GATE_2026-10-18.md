# Quran Haafidh / Atlas — Next private APK readiness gate

Date: 2026-10-18. Single authorized branch: `work/haafidh-1171-ibn-kathir-integration-independent`.

**Current result: NO-GO / NOT READY TO BUILD.** This document is a gate, never an authorization to release.
Official 1.17.1, Hifz engine, Ayah Themes source, and Jalalayn must remain intact.

## 1. Reader pictograms — code-only fixes
- [x] Distinguish **Ancres** (existing `ic_ui_semantic_anchor`) from **Amorces** (frozen Lucide `ic_ui_recall_key`).
- [x] Cover both enabled/disabled labels in `UiIconMappingTest`.
- [x] Update fail-closed `scripts/verify_j6_icon_roles.py` without weakening the 23-vector manifest.
- [x] Align app appellations: **Atlas**, **Unité Atlas**, **Passage Ayah Themes**, **Ancres** (anchor) and **Amorces** (key-round); keep labels available to accessibility, not below icons.
- [x] Freeze public UI inventory in `docs/ATLAS_APP_APPELLATIONS_ICONS_2026-10-18.md` (contains NO private editorial title data).
- [ ] Latest-head Android/J6 workflows BOTH green; link the run IDs and recheck after final commit.
- [ ] Visual smoke test on the target BOOX reader after an APK exists (postbuild); no screenshot can prove the new build today.

## 2. Atlas titles and passage display — hard editorial gate
- [x] Source inventory: 366 Atlas units / 1053 original Ayah Themes passages / 1143 intersections.
- [x] 89 original passages cross Atlas unit boundaries; never fork their IDs or rewrite their original ranges.
- [x] 120 previously approved titles and seven staged private updates preserved with pinned SHA-256.
- [x] 246 candidates and source references captured in a PRIVATE, **NON-INSTALLABLE** editorial QA roster.
- [x] 22 draft passage display aliases and 27/179 local intersection captions isolated from canonical source.
- [ ] All 246 new unit titles individually confirmed against precise verses, Ayah Themes, and Maududi/Jalalayn; zero generic fallback labels.
- [ ] All 1053 original passage titles individually verified for meaning (aliases only if necessary).
- [ ] All 179 shared-passage contexts checked; local labels only when supported.
- [ ] All editorial disputes resolved or excluded from the release with explicit sign-off; independent second review recorded.
- [ ] The final private approved-label asset contains EXACTLY the intended set; checksums and unit ID boundaries revalidated. Current runtime loader is pinned to 120 approvals: do not pretend it already supports 366.
- [ ] No private Ayah Themes source, tafsir PDF, personal signing data, or private JSON in this PUBLIC GitHub repository or its CI artifacts.

## 3. Before next APK generation
- [ ] Approval provenance for EVERY displayed title; candidate/documentation is not a semantic PASS.
- [ ] Android source tests, instrumented tests when environment permits, and Hifz regression including 180/365/548-day checks with retrievable reports.
- [ ] No regressions in map title hierarchy, portrait BOOX full QCF page display, page parity, swipe, Jalalayn with mushaf, saved validations.
- [x] APK generation is explicitly deferred until the user gives a separate go-ahead; this future permission is **NOT** a blocker for the READY-TO-BUILD readiness notification.

## 4. Only AFTER APK generation (never prematurely marked green)
- [ ] Confirm intended package/version, signing cert matching device's installed APK, in-place update without uninstall, SHA-256.
- [ ] BOOX manual acceptance, preserved Hifz achievements, no release if failures.

**Prebuild state: BLOCKED** until section 2 and all applicable parts of section 3 are proven. 
Tests on old commits DO NOT validate new editorial assets. NEVER mark a prospective fix PASS without proof.
Private review artifacts live in the user library, not in public GitHub.
