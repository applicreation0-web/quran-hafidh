# CP-13 — M5 BOOX visual contract and icon silhouette audit

Date: 2026-10-12. Public, source-safe checkpoint. No private Quran.com title,
title-bearing capture, APK or release is included.

## Method correction and demonstrated defect

A Git blob SHA proves byte identity, not graphic conformity. The former
`ic_ui_recall_key.xml` (`bb6670756b22dfb43ab94d9c56e2c66ac87ce7d3`)
was therefore rechecked as geometry. Its custom large circular bow, diagonal
shaft, `1.8` stroke and teeth did not match the approved `key-round` reference.
The drawable now uses the pinned Lucide `key-round` silhouette at commit
`f93beca5957187620e30ae771d220d88501be621`, with a 24×24 viewport, 2-unit
stroke, round caps/joins and the source keyhole. The new blob SHA is
`f4161d77563b2a572934df143ff1ae5af83cfbc2`.

`verify_contractual_ui_icons.py` is now expressly a byte-integrity check.
`verify_approved_icon_silhouettes.py` independently checks reviewed path
geometry and styling. Updating a manifest hash alone can no longer accept a
different approved-reference silhouette.

## 23-icon decision table

`PASS` below means the Android vector geometry was compared with the named,
pinned Lucide reference. It does not mean a physical BOOX rendered it.

| Kind | Role / contract | Android drawable | Reference | Verdict |
|---|---|---|---|---|
| Primary | Accueil / house | `ic_ui_home.xml` | Lucide pinned SVG | **PASS** |
| Primary | Lecture / book-open | `ic_ui_reading.xml` | Historical approved reference unavailable in the supplied evidence | **NON_CERTIFIABLE** |
| Primary | Carte / network | `ic_ui_semantic_map.xml` | Historical documentary pictogram; screen is retired | **NON_CERTIFIABLE** |
| Primary | Amorces / key-round | `ic_ui_recall_key.xml` | Lucide pinned SVG | **PASS — CORRECTED** |
| Primary | Tafsir / book-text | `ic_ui_tafsir.xml` | Historical approved reference unavailable in the supplied evidence | **NON_CERTIFIABLE** |
| Primary | Focus / scan | `ic_ui_focus.xml` | Lucide pinned SVG | **PASS** |
| Primary | Révision / brain | `ic_ui_revision.xml` | Lucide pinned SVG | **PASS** |
| Primary | Difficulté / flag | `ic_ui_difficulty.xml` | Lucide pinned SVG | **PASS** |
| Primary | Crayon / pencil | `ic_ui_edit.xml` | Historical approved reference unavailable in the supplied evidence | **NON_CERTIFIABLE** |
| Primary | Quiz / circle-check | `ic_ui_quiz.xml` | Lucide pinned SVG; historical manifest only | **PASS (DORMANT)** |
| Primary | Progression / chart | `ic_ui_progress_map.xml` | Lucide pinned SVG | **PASS** |
| Primary | Reprendre / refresh-cw | `ic_ui_resume.xml` | Lucide pinned SVG | **PASS** |
| Primary | Retour / arrow-left | `ic_ui_back.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Valider / check | `ic_ui_validate.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Fermer / x | `ic_ui_close.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Annuler / undo-2 | `ic_ui_undo.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Effacer / trash-2 | `ic_ui_annotation_erase.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Information / info | `ic_ui_info.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Révéler / eye | `ic_ui_reveal.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Enregistrer / mic | `ic_ui_record.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Lire / play | `ic_ui_play.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Pause / pause | `ic_ui_pause.xml` | Lucide pinned SVG | **PASS** |
| Secondary | Autres options / ellipsis | `ic_ui_more.xml` | Lucide pinned SVG | **PASS** |

The dormant Quiz drawable remains only because the historical 23-icon manifest
requires it. No Quiz activity, destination, home row or semantic-list Carte
screen was restored. The separate Itqan anchor remains `ic_hifz_anchor`.

## Screen-by-screen matrix

Public Android captures contain no private English titles. Consequently they
can prove that the real activities render and that the Mushaf appears, but not
the final title pixels. `PASS-SOURCE` is narrower than visual `PASS`.

| Existing interface | Expected reference | Actual evidence / difference | Action or limit | Verdict |
|---|---|---|---|---|
| Home | Compact uniform rows, no Quiz/Carte, icon-only actions | Runtime capture plus assertions for equal rows and absence of Quiz/Carte | No change | **PASS-SOURCE / emulator capture** |
| Reading | QCF-first Mushaf, four icon controls | Real page waits for `pageShown`; public capture has no private title | Final private-title pixels absent | **NON_EXECUTE (final visual)** |
| Sabqi / learning | Native masks, counters, 48dp actions | Real activity/capture in 98-test suite | No physical BOOX pixel review | **PASS-SOURCE / BOOX NON_EXECUTE** |
| Itqan / stabilization | Anchor distinct from key, exact masks | Real activity, four-date capture, source role check | No physical BOOX pixel review | **PASS-SOURCE / BOOX NON_EXECUTE** |
| Consolidation | Compact icon actions; no engine change | Real activity/capture and source inventory | No private or BOOX capture | **PASS-SOURCE / BOOX NON_EXECUTE** |
| Reinforcement | Compact icon actions; no engine change | Covered by remaining Hifz-mode rendering | No private or BOOX capture | **PASS-SOURCE / BOOX NON_EXECUTE** |
| Active/general revision | Effective masks, landmarks, monochrome controls | Real activities/captures and mask contracts | T-009/T-010 physical pixels unresolved | **NON_EXECUTE (strict visual)** |
| Free memorization | Icon-only controls, QCF unchanged | Source-wide icon inventory and runtime screen | No dedicated signed BOOX comparison | **NON_EXECUTE (strict visual)** |
| Progress | Chart icon and compact content | Source-wide icon inventory and JVM screen contracts | No dedicated signed BOOX comparison | **NON_EXECUTE (strict visual)** |
| Planning/program choice | Seven-day selector, compact dialogue | Two real Android captures | Emulator only | **PASS-SOURCE / BOOX NON_EXECUTE** |
| Settings | Seven-day calendar and compact controls | Real Android capture | Emulator only | **PASS-SOURCE / BOOX NON_EXECUTE** |
| Tafsir | Historical book icon, unchanged Jalalayn | Source inventory; supplied approved silhouette reference missing | No invented replacement | **NON_CERTIFIABLE** |
| Difficulty marks | Flag icon, QCF geometry unchanged | Pinned Lucide geometry and source contracts | No physical BOOX pixel review | **PASS-SOURCE / BOOX NON_EXECUTE** |
| Dialogs/audio | 48dp icon buttons, record/play/pause roles | 23-icon path and all-screen wiring checks | Five dynamic call sites still require device review | **NON_EXECUTE (strict visual)** |

## Title and amorce visual contract

Source/runtime contracts establish: amorces initially ON when exact data exists;
saved OFF choice; exact QCF boxes; light grey fill behind the words; no underline;
regular centered LTR `11.5sp`; unframed popup; `3200ms`; maximum `480dp`;
measured placement at least `4dp` above the icon row; one original title at a
time; cycling in source order; no fallback title. The implemented positioning
uses a 10dp requested gap.

These facts do not replace the required final private screenshots. AND-01 to
AND-10 are **NON_EXECUTE** because no isolated Android run with the reviewed
private V2 candidate and no signed human pixel comparison was available in
this Work environment. BOOX-01 to BOOX-03 are **NON_EXECUTE** because no
physical BOOX was accessible. Old no-title captures are not reused as proof.

## Non-regression and residuals

- Hifz engines, masks, counters, persistence, Jalalayn and QCF geometry were not
  modified by the M5 correction.
- Portrait/landscape private title clipping and physical E-Ink ghosting remain
  **NON_EXECUTE**.
- P0: final private-title Android capture set and physical BOOX certification
  are absent; therefore there is no visual GO for a debug.
- P1: four historical icons lack a supplied independent approved silhouette.
- No APK, debug artifact or release was built or published.

## CI

Checkpoint SHA: `92f93e5ec73408c0456d8ebaa504f816cc9863b8`.

- source/JVM run `38273528132`: **PASS**. The 23-file byte check and the
  independent 19-silhouette geometry check both passed; all Gradle source/JVM
  tasks completed successfully.
- Android run `38273528162`: **PASS**. Exact main result `OK (98 tests)`,
  then preparation `OK (1 test)`, external `adb shell am force-stop`, and
  fresh-process verification `OK (1 test)`; 14 public no-title screenshots.

The two one-test phases re-run members of the 98-test inventory and are not
reported as 100 distinct tests. A passing hash/geometry script and emulator
run are not represented as physical BOOX certification or private-title pixel
proof.
