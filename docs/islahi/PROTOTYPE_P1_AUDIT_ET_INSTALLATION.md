# Quran Haafidh — Iṣlāḥī prototype P1 (independent debug)

## Build provenance
- Baseline **source commit**: `7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21` (actual hifz-app 1.17.1, versionCode 39).
- Branch: `work/islahi-prototype-p1-from-1-17-1`.
- Artifact: `Quran-Haafidh-Islahi-Prototype-P1.apk`.
- Separate applicationId: `com.quransafeguard.hifz.islahitest`.
- VersionName: `1.17.1-islahi-p1`; display label `Quran Haafidh - Islahi TEST`.
- No merged changes to Claude branches, `main`, existing release build or memorization engines.
- CI: `.github/workflows/islahi-prototype-p1-debug.yml`, tests and private debug artifact upload only (no tag/release/signing secret).

## Scope
- Listing: 114 sourates, search by number or Arabic name.
- Documented Work03 structural pilot: S9, 26 blocks; S114, 1 whole-surah block. Original Analysis text **NOT INCLUDED**; source attribution remains subject to verification.
- **Demonstration data only** (NOT certified as Iṣlāḥī): S2 63–82/83–96; S50 1–5 / 6–11 / 12–14 / 15–18 / 19–35 / 36–37 / 38–45; S7 137–171 long-span test.
- All other sourates are displayed with explicit unavailable status; no fictional boundaries or authored analysis.
- Block detail: actual KFQC SVG pages from bundled 604-page Madinah Mushaf, word-coordinate gray overlay, local horizontal gallery with neighboring verses shown faintly, click thumbnail to open standard Lecture on exact page.
- Reader: optional pilot context flags only while entered from map; per-page verse-outline flag via existing MushafView.setHighlightVerses, normal Reader unchanged without flags; Back returns to map Activity.
- Commentary: real copyrighted commentary and `Analysis of the Discourse` not embedded, no invented text.
- No writes to HifzPrefs and no changes to Sabqi/Itqan/Consolidation/Renforcement/Ancrage/Quiz.

## Contradictory checks / acceptance gate
1. Install alongside release 1.17.1; both retain independent app data. Verify app label, package, versionName using aapt.
2. Confirm list 114 entries; search for 9, 50, 114 and Arabic names.
3. Confirm S9 has 26 ranges, contiguous 9:1–129; S114 has one 114:1–6.
4. Confirm S2, S50, S7 are unmistakably marked **DEMO**; all other sourates marked **À certifier**, with no invented data.
5. Open 50:15–18 -> check actual pages 518–519 and non-overlapping gray word overlays.
6. Open S7 7:137–171 -> check 8 actual page miniatures 166–173, gallery swipe and page jump.
7. Tap a thumbnail -> standard Reader opens at matching page and highlights only selected verses; swipe a page; press Back -> return to same block card.
8. On BOOX landscape: assess SVG load latency, refresh, ghosting, line readability, rotation, zoom. **Device testing pending.**
9. Open ordinary Lecture without map context -> check no Iṣlāḥī overlay. Check unrelated pages and established tafsir continue to work.
10. Test orientation/process recreation and Android Back on every screen. No device PASS should be claimed unless executed.
11. Regression CI: `:hifz-core:test`, `:hifz-app:testDebugUnitTest`, `:hifz-app:sourceContractTest`, `:hifz-app:assembleDebug`.
12. NO GO for full 114 corpus until actual independent facsimile proof, Analysis text, detailed tafsir mapping, and reproduction rights are checked.

## Known constraints
- Current reader `setHighlightVerses` uses weak-spot outlines rather than a purpose-built Iṣlāḥī gray-fill overlay. User validation required; can add a dedicated overlay after CI baseline passes.
- Interactive gallery renders multiple authentic inline SVGs in one WebView; user-device performance must be profiled.
- No English text or commentary from Work03 was delivered: absent intentionally.
- Our S7 eight-page sample is purely illustrative and not an attributed Iṣlāḥī block.
