# CLAUDE HANDOFF — Quran Hifz 0.5 tablet / multi-Tafsir TEST

## Candidate to audit

- Repository: `applicreation0-web/quran-unlock-android`
- Candidate branch used for build: `integration/hifz-test-app`
- **candidate_code_sha:** `ce7b1b6798f04720ff43ff58b99bf217f3aebfc1`
- Base phone-tested candidate: `cb5bcbb3d1799131289f114bf503ce8ff24f2494`
- GitHub Actions run: `34642663546`
- Artifact ID: `10280913042`
- APK SHA-256: `696d2a5d6f1b296a8af345ea9d2212ebb8630002d9389aaf5b424d84948cfff7`
- Android package: `com.quransafeguard.hifz.installtest1`
- versionName: `0.5-tablet-tafsir-test`

The build job is green. This is **not** a claim of phone or BOOX runtime validation. The user must still physically test this exact APK.

## Audit posture

Act as an independent code auditor. Do not redefine the Hifz product rules. Do not run paid CI without explicit permission. Inspect the exact SHA first; identify root causes and propose minimal patches. Preserve Quran text, geometry, Tafsir source text, typography and source honorifics exactly.

## Fixed Hifz rules

### Sabqi
- Monday / Wednesday / Friday morning.
- Exactly 5 consecutive physical Mushaf lines, no rebalancing.
- 37 repetitions: 15 visible, 5 at 25%, 5 at 50%, 5 at 75%, 7 at 100%.
- A block may end mid-verse; only fully acquired verses are promoted.
- Exact resume after interruption.
- Atomic completion required.
- Completing 37 reps early reinforces the same block; it must not automatically create another block.

### Itqan
- Tuesday / Thursday.
- One eligible unit repeated x30 with mandatory masking.
- Exact unit start/end/rep is frozen until 30/30.
- Corpus cycles `49:1 -> ... -> 114:6 -> 2:1 -> ... -> promoted frontier -> 49:1`.
- Assistance is tracked separately.
- Current 10/5/5/5/5 mask split is a working parameter, not a final product rule.

### Murajaah
- Weekend structured revision.
- Independent cursor; must never move the Itqan cursor.
- Includes recent Sabqi and older eligible Itqan corpus.
- Predicted endpoint is advisory; the user's actual endpoint is sovereign.
- No automatic double quota after absence.

### Free memorisation
- Independent of all structured cursors and promotions.
- Free passage, mask and manual repetition counter.
- No Tafsir in this mode.

## Personal weekly timetable to preserve

- Monday AM: Al-Baqarah Sabqi, 5 new lines, 90 min. PM: Sabqi review 15–20 min.
- Tuesday: Hujurat -> An-Nas, Itqan 1 page x30.
- Wednesday AM: Sabqi +5 lines, 90 min. PM: cumulative Sabqi review 15–20 min.
- Thursday: Itqan next page x30.
- Friday AM: Sabqi +5 lines, 90 min. PM: Sabqi review/bilan 15–20 min.
- Saturday AM: Murajaah Hujurat -> An-Nas 60 min. PM: Murajaah Al-Baqarah 60 min.
- Sunday AM: continue Murajaah Hujurat -> An-Nas 60 min. PM: continue Murajaah Al-Baqarah 60 min.

The 0.5 home screen now displays this plan and exposes direct `Sabqi / Itqan / Murajaah` access for testing. **Known gap:** the underlying scheduler still models one structured session type per day; evening review is not yet a separate engine/session type. Audit this explicitly; do not silently invent behaviour.

## Reader / tablet / BOOX rules

- Mushaf remains the exact local KFQC/Madinah 604-page SVG corpus. No reflow or text rewriting.
- Android status/navigation bars must not be hidden, moved or drawn under.
- Reader top/bottom controls float over the Mushaf, appear on tap and disappear; their visibility must not resize the Mushaf.
- On BOOX, no fade/alpha animation.
- Repetition counter changes should not redraw the Mushaf.
- Mask-stage changes redraw only what is necessary; page changes may full-clean.
- Cream/black neutral identity; selected verse uses a muted gray highlight, no orange enclosing outline.

## Mask change in this candidate

`reader.js` now creates a stable deterministic ordering of mask cells. 25/50/75/100 are nested thresholds, so 25% cannot randomly become visually empty when candidates exist. Audit geometry clipping and selected-verse boundaries, especially short selections and blocks crossing page boundaries.

## Tafsir — historical contract must be preserved

Tafsir is accessible **only in Lecture / Etude**.

This candidate restores the three historical audited local corpora from the validated 0.10.9 Plus APK:

- Jalalayn: 6236 verses + source notes/styles.
- Qurtubi: 432 source-backed entries. Metadata from the DB includes `al-Jami li-Ahkam al-Quran / The General Judgments of the Quran`, author `Abu Abdallah Muhammad ibn Ahmad al-Qurtubi`, translator `Aisha Abdurrahman Bewley`.
- Qushayri: 720 grouped entries from 806 raw source segments, Suras 1–4. Metadata includes `Lataif al-Isharat / Subtle Allusions`, author `Abu l-Qasim Abd al-Karim al-Qushayri`, translator `Kristin Zahra Sands`.

Build verification proves all three corpora are present and that verse 2:27 has an entry in all three.

Important historical non-regression files:
- `docs/TAFSIR_TYPOGRAPHY_ALIGNMENT_CONTRACT_0.10.5.md`
- `docs/TAFSIR_READING_CONTRACT_0.10.7.md`
- `docs/VISUAL_CONTRACT_0.10.7.md`
- `scripts/audit_0106_tafsir_honorifics.py`
- `scripts/audit_0106_tafsir_presentation.py`

Rules that must not regress:
- source text is authoritative; no generated paraphrase or fusion between commentators;
- source-provided scholarly diacritics survive unchanged;
- honorific/special glyphs are preserved only when source-backed; never infer an honorific from a person's name;
- Jalalayn presentation-only historical substitutions remain exact: `(ṣʿa)` and `(ṣ)` -> `ﷺ`; `(ʿa)` -> `عليه السلام`;
- commentary 18sp default, 16–26sp user range, 1.50x line height;
- notes 1.45x; poetry 1.55x, logical start/left, never justified;
- source Quran translation for Qurtubi/Qushayri is bold italic and precedes commentary;
- note calls remain superscript 0.78em;
- prose/notes are justified where supported;
- muted neutral Mushaf selection, no enclosing outline.

The panel now keeps the selected Mushaf verse above the floating lower panel before it opens.

**Known Tafsir gap to audit:** canonical interactive Quran cross-references and the compact/expanded reading-state behaviour from the 0.10.7 reading contract have not yet been fully ported into the Hifz Java panel. Do not mark the historical Tafsir contract fully restored until these are checked/ported.

## Prior phone evidence

The immediately preceding candidate `cb5bcbb3d1799131289f114bf503ce8ff24f2494` was installed by the user and showed the Mushaf successfully on the real phone. User-observed defects that motivated 0.5:
- only Friday Sabqi was exposed; Itqan/Murajaah could not be manually tested;
- 25% mask appeared unreliable;
- Tafsir exposed only unnamed Jalalayn;
- selected verse could be hidden under the Tafsir panel;
- UI was too phone/prototype-oriented for a 10.3-inch BOOX/tablet;
- settings exposed raw debug state.

0.5 contains targeted corrections for those issues but has not yet received physical phone proof.

## High-priority audit targets after install

1. No white/blank screen on open or reader transitions.
2. Main home direct access opens Sabqi, Itqan and Murajaah without moving cursors merely by opening.
3. 25/50/75/100 mask nesting is visually correct and constrained to the selected passage.
4. Itqan resumes the exact frozen unit and repetition.
5. Sabqi 37 completion remains atomic.
6. **Sabqi five-line block crossing a page boundary:** current structured page navigation is still a likely weak point; verify that all five physical lines are usable without allowing navigation outside the active block.
7. Completed-session UI clears stale mask/selection cleanly.
8. Multi-Tafsir selector only shows editions that actually contain the selected verse; source identity metadata is visible.
9. Tafsir panel never hides the selected verse.
10. Typography, scholarly diacritics, poetry semantics and source honorifics match the historical contracts.
11. Phone/tablet insets never overlap system bars.
12. BOOX physical ghosting/refresh remains unproven and must be tested on hardware.

## Out of scope for this audit pass

- No Safeguard blocking/accessibility functionality.
- No account/backend/server.
- Audio playback engine is not yet implemented; Al-Husary Muallim remains a later local pack.
- Do not use this audit to redesign the Hifz method or change Quran/Tafsir source data.
