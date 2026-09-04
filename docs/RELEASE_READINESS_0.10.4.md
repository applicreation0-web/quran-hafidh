# Quran Safeguard Plus 0.10.4 — release readiness / contradictory audit

Status: **NO-GO FOR MERGE OR PUBLICATION**

This document is the pre-release gate requested before an effective 0.10.4 update. It is deliberately fail-closed: a technically compilable scaffold is not a release if the requested religious corpora have not passed their source, content and redistribution gates.

## Executive verdict

The 0.10.4 branch substantially improves the multi-tafsir and Al-Hikam architecture without altering the frozen Jalalayn corpus. However, the requested release is **not yet publishable** because the new Qurtubi/Qushayri full-text payloads are not bundled or rights-cleared, the 264/264 Hikam boundary audit is unfinished, the post-freeze French audit is unfinished, and the latest GitHub Actions jobs are failing before any runner step starts.

No version bump, merge, tag or published release is permitted while any release-blocking row below is not PASS.

## Detailed gate table

| Gate | Result | Evidence / action |
|---|---|---|
| Verified 0.10.3 baseline retained | PASS | Feature branch is based on the audited 0.10.3 line. |
| Jalalayn database unchanged | PASS | Existing v1 DB remains frozen; runtime and APK verifier retain exact SHA and 6236 commentary / 427 note invariants. |
| Jalalayn UX preserved | PASS (static) | Existing bottom panel, green surface, A−/A+, notes and scrolling remain the golden UX. |
| Jalalayn default | PASS | Unknown/null saved edition resolves to Jalalayn. |
| Partial tafsir options hidden when unavailable | PASS (code) | UI now asks the Plus repository for actual distributable editions for the tapped verse. |
| Exact verse mapping required for Qurtubi/Qushayri option | PASS (code) | Static source coverage is insufficient; `entry_verse_map` must contain the exact verse. |
| Stale async tafsir response protection | PASS | Request identity includes verse + edition. |
| Multi-segment support | PASS (architecture) | Multiple source blocks can render sequentially in the same Jalalayn-style panel. |
| Range commentary support | PASS (architecture) | Source ranges are retained rather than split or invented. |
| Qushayri source-address audit | PASS — structural only | Sands Sūras 1–4: 669/669 canonical verses addressable; known inline/range cases preserved. |
| Qushayri exact reviewed English payload | **PENDING / BLOCKING** | Full source-specific body/note extraction has not passed final content audit. |
| Qushayri Arabic-source exclusion | PASS (gate) | Manifest + APK verifier require English content and reject Arabic-script payload text. |
| Qushayri redistribution rights | **FAIL / BLOCKING** | Current Sands edition states reproduction requires publisher permission; no documented redistribution permission is present. |
| Qurtubi coverage audit | PASS — metadata/index only | Four-volume Bewley set verified through Qur'an 4:23; 4:24 is outside this set. |
| Qurtubi exact reviewed English payload | **PENDING / BLOCKING** | Final source-verified extraction is not bundled. Archive OCR is not accepted as wording authority. |
| Qurtubi Arabic/Warsh exclusion | PASS (gate) | New payload contract is English-only; Quran Safeguard's Mushaf remains authoritative for Arabic display. |
| Qurtubi Sunniconnect contamination rejection | PASS (gate) | Builder/APK verifier reject known third-party contamination markers. |
| Qurtubi redistribution rights | **FAIL / BLOCKING** | Bewley/Diwan Press English edition is not cleared for redistribution in this project. |
| Light contains no private tafsir corpus | PASS (architecture) | Light remains no-op and its APK verifier rejects tafsir/Qurtubi/Qushayri material. |
| Plus unit tests wired into CI | PASS (workflow) | Workflow requests both Light and Plus unit-test tasks. |
| Latest CI execution | **FAIL / BLOCKING (infrastructure)** | Latest attempts terminate with zero steps and no assigned runner; therefore no current full build/test PASS exists. |
| Hikam three-layer UI | PASS (code) | ḤIKMA / TRADUCTION FRANÇAISE / COMMENTAIRE CLASSIQUE are explicit. |
| Hikam matn physically separate from sharh | PASS (architecture) | Main corpus and Plus-only sharh remain separate. |
| Sharh author/work cross-attribution gate | PASS (code) | Sharnubi and Ibn Abbad work IDs are bound and duplicate pairs fail closed. |
| Full 264/264 matn-only boundary audit | **PENDING / BLOCKING** | Current corpus cannot inherit `verified` boundary status from the earlier mixed sharh source. |
| >=2 independent witnesses per Hikma | **PENDING / BLOCKING** | Registry is defined; all 264 entries still need documentary completion. |
| Hikma 16 long matn preservation | PASS as regression rule | No shortening heuristic; final release must match the approved exact matn fingerprint. |
| French 264/264 audit after matn freeze | **PENDING / BLOCKING** | Required ordering is matn freeze -> hash -> French re-audit. |
| French 174–176 corrections | PASS as regression gate | Existing corrected wording is locked for final comparison. |
| Dual Sharnubi/Ibn Abbad verified sharh corpus | **PENDING / BLOCKING** | Architecture exists; verified final commentary payload is not bundled. |
| Version bump to 0.10.4 | NOT DONE — CORRECT | A release number must not be assigned to an incomplete payload. |
| Merge / publication | NOT DONE — CORRECT | Publishing now would misrepresent the requested 0.10.4 as complete. |

## Contradictory findings fixed during this audit

### 1. Coverage is not availability

A previous implementation could offer Qurtubi/Qushayri merely because a verse fell within the edition's broad coverage range. That is insufficient: a source can contain a lacuna, extraction omission or rejected block.

**Correction:** the menu now requires a real, audited, distributable DB plus an actual `entry_verse_map` row for the tapped verse. Jalalayn remains the only option otherwise.

### 2. English-only must be an executable invariant

A prose rule saying “do not show Arabic from Qushayri/Qurtubi” was too weak.

**Correction:** both manifest entries explicitly declare `content_language=en` and `arabic_source_text_included=false`; runtime metadata checks repeat the rule; the APK verifier scans new v2 `plain_text` fields and fails if Arabic-script text is present.

### 3. A release-ready boolean is insufficient

A single `ready_for_distribution=true` field could be toggled accidentally.

**Correction:** a new edition requires all of these simultaneously: approved rights status, verified source audit, verified content audit, English-only contract, Arabic-source exclusion, valid SHA-256, nonempty approved parts, valid DB metadata and actual verse mapping.

### 4. Structural Qushayri success is not content success

669/669 addressability proves mapping structure, not that every commentary boundary/note has been extracted exactly.

**Correction:** the structural audit remains explicitly labelled structural-only and cannot activate the manifest.

### 5. Archive Qurtubi OCR is not source authority

OCR is useful for locating sections but contains known contamination and transcription errors.

**Correction:** final payload activation requires source-verified wording and the contamination gates remain mandatory.

### 6. Build success cannot be inferred from an unstarted Actions job

The current GitHub Actions attempts end without an assigned runner or any executed step.

**Correction:** no CI PASS is claimed. A real end-to-end runner execution is mandatory before merge.

## Release rule

0.10.4 may be version-bumped, merged and published only after all blocking rows become PASS. In particular, it must not be published as “Qurtubi + Qushayri + audited 264 Hikam” while those payload/content/source gates are incomplete.
