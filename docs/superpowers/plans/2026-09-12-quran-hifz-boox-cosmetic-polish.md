# Quran Hifz 0.7 BOOX — Cosmetic Polish Plan

## Scope

Polish `integration/hifz-boox-polish-0.7` without changing Hifz semantics, Quran data, Mushaf geometry/rendering, repetition quotas, cursor rules, audio corpus, or Tafsir content.

Implementation tree was applied in commit `f41b2afb1adb15ce5959cd3a21ca94a65b898a63` after the cosmetic contract was first proven red.

## P0 — Audio surface

- Keep the existing asynchronous MediaPlayer lifecycle and verse-level Mushaf highlight.
- Replace the central modal audio dialog with an inline non-modal mini-player.
- Place the player below the compact header and above the Mushaf.
- Keep previous, play/pause, next, repeat and close controls.
- Remove permanent pack/source chrome from playback.
- Apply the same inline behavior in Study, structured Hifz sessions, and Free Memorisation.

## P1 — Compact reader grammar

- Preserve 44–48 dp hit targets while reducing visible controls to the 24 dp icon family.
- Compact Sabqi, Itqan and Murajaah headers into context + one metadata row.
- Keep their bottom actions semantically identical and visually quieter.
- Convert Hifz Settings to predominantly row-based controls with current values on the right.
- Keep all destructive/reposition confirmations unchanged.

## P2 — Liseuse finish

- Remove dashboard/card chrome from the home screen without removing destinations or the weekly information.
- Keep the BOOX Tafsir split at 58/42 and phone Tafsir at the existing bottom-panel behavior.
- Put Study header, audio host, Mushaf, actions and page slider in layout flow so they never overlay Quran lines.
- Separate reader actions from the page slider.
- Preserve reader geometry when controls auto-hide by using invisible rather than gone controls.

## Verification

1. Cosmetic contract gate must fail before implementation and pass after it.
2. Existing Hifz convergence and product-boundary gates must remain green.
3. Shared primitive and app-engine tests must remain green.
4. Candidate build must still verify package `com.quransafeguard.hifz`, non-debuggable state, offline corpus and no Safeguard blocking surface.
5. Physical BOOX ghosting/optical inspection remains a device-level acceptance check after the automated gate is green.
