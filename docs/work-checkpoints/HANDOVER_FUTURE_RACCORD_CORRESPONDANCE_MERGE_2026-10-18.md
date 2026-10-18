# Quran Haafidh — handover: Atlas preparation and future Raccord Correspondance merge
Date: 2026-10-18. Public repository documentation only — do NOT include any private tafsir content, Ayah Themes asset, signing material or user Hifz data.

## User instructions (binding)
1. Complete and commit all warranted fixes to the existing Quran Haafidh application before the next APK is requested. Do NOT generate, sign, install, publish or release the APK until the user explicitly asks.
2. Keep the separately developed **Raccord Correspondance** feature independent. Its intended purpose is to show similarities across different verses and links/correspondences across surahs. It is **not yet part of this branch's integration mandate**.
3. The user will initiate a **dedicated future chat for fusion**. Only then inventory the separate feature's actual source/head/assets/contracts and plan integration. Never invent its implementation, APIs, branch, state or completion.
4. Preserve official 1.17.1, package/signature continuity, all Hifz acquired progress, frozen Hifz engines and persistent state, Mushaf QCF V2, and Jalalayn untouched.
5. No new repository or branch for the existing Atlas work; use only `work/haafidh-1171-ibn-kathir-integration-independent`. At fusion time discover the independently implemented feature's actual ref; do not modify it preemptively.

## Current user-facing naming
- **Atlas** is the graphical semantic map; **Unité Atlas** is the larger/bold thematic summary; **Passage Ayah Themes** is the smaller/italic source passage label.
- **Ancres** reader cues use `ic_ui_semantic_anchor`; **Amorces** use the preserved Lucide `ic_ui_recall_key` (key-round).
- BOOX controls remain icon-only with accessible spoken descriptions. Existing Hifz vocabulary adapter is display-only; no persisted mode or engine migration implied.
- The icon manifest is `docs/FROZEN_UI_ICONS_13_PLUS_10.tsv`; keep icon contracts fail-closed.

## Private editorial hard gate (NO FALSE GREEN)
- Corpus: 366 Atlas units, 1053 canonical Ayah Themes passages, 1143 intersections. 89 canonical passages cross Atlas unit boundaries: never fork or move their identities or limits.
- 120 previously approved unit labels, seven approved private corrections SHA-pinned. 246 new title candidates are not all individually certified.
- Original Ayah Themes source immutable. Context-specific aliases only when justified and approved. Preserve exact source/provenance and reliable verset/surah boundaries.
- Finish individual verse-based justification for all titles, document unresolved disputes, run independent editorial second-pass, and stage approved private JSON **only in local/private artifacts** (NEVER in public GitHub/CI). Update only the verified SHA-256 pin/code permitted by the contract.
- Current private loader requires exactly 120 approved titles; do not pretend that a 366-title release is ready without a validated private asset and compatible loader.
- Follow `docs/work-checkpoints/NEXT_APK_READINESS_GATE_2026-10-18.md`, which remains **NO-GO** until all applicable prebuild criteria are supported by evidence.

## Required final prebuild deliverable, before the future merge
- Exact authorized branch HEAD and ordered commit ledger (implementation, tests, docs), real CI outcome **on latest HEAD**, failures vs PASS vs NOT RUN.
- Source correctness checklist (Atlas unit/passages titles and provenance), icon/naming screenshots or appropriate evidence, preserved Jalalayn/QCF, no Hifz state mutation, no public private data.
- BOOX-specific layout and usability checks where actually executable; distinguish source-level CI from tests requiring APK/device.
- Explicit list of unresolved issues and resulting GO/NO-GO; no APK until separate authorization.
- Save checkpoint and handover. Future fusion chat must reuse them, not redo completed tasks.

## Future dedicated merge chat (NOT authorized yet)
Once the independently completed Raccord Correspondance implementation is provided or its repository reference is supplied:
1. Verify both concrete HEADs, scope, local changes, APIs, storage namespaces and any license/source requirements.
2. Plan navigation and UI: Atlas (themes / visual placement) is different from Raccord Correspondance (cross-verse similarity / cross-surah relations); avoid overloading the Atlas map or duplicating source passages.
3. Keep references/boundaries immutable, make links non-destructive and opt-in, ensure BOOX low-refresh navigation, and test return-to-QCF and Hifz persistence.
4. Integrate only with user's explicit instruction; run full source, regression, APK and device validation. No independent code is merged automatically.
