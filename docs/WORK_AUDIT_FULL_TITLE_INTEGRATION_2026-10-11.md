# WORK HIGH — independent pre-debug audit after ALL Ibn Kathir title commits

**READ-ONLY. No GitHub writes. No APK. No release. Do not use an older build as proof.**

## Exact target
- Repo: `applicreation0-web/quran-hafidh`
- Branch: `work/haafidh-1171-ibn-kathir-integration-independent`
- 1.17.1 official baseline: `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21`
- Re-read and freeze ACTUAL HEAD at launch, then require every command and screenshot to match that SHA. Never guess a future SHA from this document.
- Main external standard: `STANDARD_COMPLET_HAAFIDH_FONCTIONNEL_VISUEL.md`; 133-case `MATRICE_TESTS.csv`, 23 exact icon SHA manifest and reference screenshots in the attached Work package.
- App sources: `docs/PREDEBUG_IMPLEMENTATION_HANDOFF_2026-10-11.md`, `IbnKathirGroupIndex.java`, `IbnKathirOfflineTitles.java`, `IbnKathirTitlePack.java`, eight `chunk-*.json` files and `StudyReaderActivity.java`.

## P0 source fidelity, copyright and integration
1. The original English source is spa5k/tafsir_api at exact commit `eb82bb6294efe30ad5c135c03b1864afaa70e855`, `tafsir/en-tafisr-ibn-kathir/{1..114}.json`. Retrieve every file, independently verify group boundaries of identical commentary and all 6,236 verses. Compare against `IbnKathirGroupIndex`: exactly 1,903 intervals over 114 surahs, no omissions, no overlap.
2. Execute `python3 scripts/audit_ibn_kathir_title_pack.py --out /tmp/WORK_IK_HEADINGS.csv`. If the pinned 114 source JSON files are locally available, run the same script with `--source-dir /path/to/114-jsons` and verify *every* extracted title line exactly appears verbatim in that exact group's source text. Expected **1,415 groups with extracted heading candidates**, **488 without**. Do not call them all verified/approved.
3. Review all 1,415 candidates **as actual editorial headings**, not merely English lines without punctuation. Red flags: alleged titles starting with a reporter's name, Hadith narration, verse translation, a commentary sentence, title of whole surah unrelated to the group, or first surah virtue when a better group title occurs later. Record source quote, group id, paragraph position, matched verse and correction/disposition. For 488 others record genuine absence vs regex false negative and whether a human-verified original short heading exists. Never fabricate a title, synthesize by LLM, translate automatically, or silently label prose as a headline.
4. **Independent rights gate:** repo `spa5k/tafsir_api` MIT licensing of software does NOT establish permissions for its modern English Darussalam abridgement. The public app repository now contains many short English extracts as an internal staging dataset; independently establish whether grouped heading reproduction and prospective APK redistribution are authorized. If not established, verdict is NO-GO for distribution. Distinguish source accessibility from permission to republish.
5. Verify `IbnKathirOfflineTitles.verifiedHeadings(group)` (17 source-checked groups (16 Qaf/Najm and Al-Baqarah 2:1)) is ALWAYS preferred over `IbnKathirTitlePack.sourceHeadings(...)`. No new canonical mnemonic cue, additional highlight coordinate, Quranic transcription, group segmentation or marker position is derived from the title text. Source titles are annotations only; group starts use existing exact pinned QCF boxes. Missing title => silent no-popup, NEVER "English heading not yet verified".
6. Offline performance/security: 8 chapter chunks (1..15,16..30,...106..114), read only when tapping a real heading, no remote browser, no network, no file picker, bounded (350 KB) read, no data loss on malformed/missing pack, UTF-8 apostrophes/punctuation, QCF untouched. Validate actual title at 2:1, 18:9, 50:1, 53:1, 78:1, 109:1 and no-title group 114:1. Confirm 109 favors a relevant thematic title over generic recitation introduction.
7. **Real pixel proof:** title English 11.5sp REGULAR, not bold, max width 480dp, left-to-right, centered. Real measured panel bottom + ≥4dp is ABOVE actual icon-bar top. Test 2:1, Qaf, long heading, multi-subheading, BOOX portrait+landscape/zoom, emulator and if accessible actual BOOX. No clipped title, no rectangular black backing, no giant pavés, NO title atop controls. The exact amorce word backgrounds return as light grey `var(--sel)` with E-Ink 0.16 opacity. A title/source asset must never affect touch actions or Qur'an reading masks.
8. Honor persisted Amorces OFF and default ON only when no saved preference; select key icon stays dark on light state. Validate the 23 immutable pictograms, brain/Parcours, `Std 3/4` vs honest `Manual day count`, compact home and all other 133 cases from the standard.

## P0 Hifz and persistence regression — prohibited changes
- Compare `hifz-core`, `HifzPrefs`, `HifzSessionActivity`, `ConsolidationCycleEngine`, `WeeklyDashboardPlanner`, all 604 Mushaf pages, QCF geometry and 77,432 Quranic word boxes against approved frozen baseline and exact authorized deltas.
- Validate Sabqi 5 physical lines/37 repetitions; Itqan pre-Nas two pages/four dates/40 repetitions, straddling page/surah boundaries, V6 atomic acquisition, current/next physical line, clock rollback, same date replay and process death; post-Nas maintenance and amorce masks; Renforcement/Consolidation new ×7 plus continuous older blocks; Sunday no generic active/passive revision; reappearance after validation; weekly Standard and Manual transitions; audio, settings, user ranges, no Quiz runtime, no learning regression.
- Run independent protected-source checks, JVM tests and real Android instrumentation. Source CI success is not BOOX approval.

## Evidence and verdict
- Deliver `WORK_AUDIT_VERDICT.md`: P0/P1/P2 table with PASS/FAIL/UNVERIFIED, source SHA, code line, reproducer, impact, mitigation, explicit GO/NO-GO. No unsupported "100%".
- Deliver `IK_TITLE_EDITORIAL_REVIEW.csv` (one row for EACH 1,903 groups), `IK_RIGHTS_FINDINGS.md`, `BOOX_PIXEL_SCREENSHOTS/` (if available), `HIFZ_REGRESSION_MATRIX.csv`, `MANUAL_8WEEK_SIMULATION.csv`, `ICON_SHA_CHECK.csv` and `NO_SIDE_EFFECT_EVIDENCE.md`.
- Any title/provenance/rights violation or loss/corruption of Hifz credit is **NO-GO**. If anything is unverified, state exactly what evidence is lacking and do NOT recommend unconditional debug delivery.
- There must be NO audit commits, PRs, app edits, builds intended for user distribution or APK releases. The audit is the final independent gate AFTER all implementation commits.


### Editorial override validated before Work — Al-Baqarah 2:1

The source `2.json` commentary on exactly 2:1 includes the genuine heading **“The Discussion of the Individual Letters”** immediately after the Arabic `الم`; the generic initial paragraph about surah virtues must NOT be displayed as the verse-specific cue title. `IbnKathirOfflineTitles.java` now explicitly overrides that one group, and Android instrumentation checks the exact wording and measured placement. All other extracted candidates remain provisional pending Work audit. Source SHA: `eb82bb6294efe30ad5c135c03b1864afaa70e855`.
