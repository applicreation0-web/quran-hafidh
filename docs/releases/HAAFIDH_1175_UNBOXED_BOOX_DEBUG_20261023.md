# Quran Haafidh 1.17.5 — BOOX home redesign (no boxes) — private debug
Date: 2026-10-23

User feedback: two actual screenshots show bordered/tiny Hifz menu panels, omitted daily verse/page objectives and Hifz icons missing. Redesign was requested: no boxes, Atlas-inspired curves, readable actual objectives.

Changes (not core engines):
- `MainActivity.java`: unboxed home hero and three icon destinations; unboxed Hifz modes with the existing frozen Hifz icons and fitting labels; selectable 7-day timeline including calendar dates.
- `BooxFlowStroke.java`: static curved ink separator, no animation/alpha/box.
- `MainActivity.scheduleDetail`: full read-only `WeeklyDashboardPlanner` morning/evening objective preserved including QCF pages, lines and verse references; no fabricated future objectives, no cursor advancement.
- `WeeklyProgramPolicyTest.java`: preserve 1/3–3/3 and exact verse/page ranges; replace prior lossy-short-label assertions.
- Updated both protected-file allowlists only for the new UI-only stroke class. Hifz engines, QCF renderer, persistence, audio and Correspondances not modified.
- Changes compiled/tested at app source commit `b52f89a9b8ac162664263c69e0556ac1cc6a6ad1`.
- Full JVM/source CI PASS: https://github.com/applicreation0-web/quran-hafidh/actions/runs/40022948987
- Private staging build PASS: https://github.com/applicreation0-web/quran-hafidh/actions/runs/40022948977
- Android instrumentation PASS (26 tests) and first home screenshot: https://github.com/applicreation0-web/quran-hafidh/actions/runs/40022994664
- Separate Parcours screenshot audit: https://github.com/applicreation0-web/quran-hafidh/actions/runs/40023889561 (check job state; report not a pass until complete).
- Signed private debug created **offline**, never uploaded to GitHub. 7/7 prior private Atlas asset hashes checked, 0 Correspondances files. APK: `QURAN_HAAFIDH_BOOX_1.17.5_ACCUEIL_SANS_PAVES_DEBUG_PRIVE.apk`; size 91,891,791 bytes; SHA-256 `fc19a9acde7c3deb67d70875ac2d5f5fa6dca11dafd4c845c8f202db775bf38d`. Signature v2 and ZIP alignment PASS; private signer certificate `a41f5b8816edd8d63ec801c0cbb492006e1df5783030c3e552cf95a63a09329d`.
- APK ID `com.quransafeguard.hifz.boox1175debug`, version `1.17.5-BOOX-HOME-PRIVATE-DEBUG`, versionCode 42. **Different signer from the previous isolated 1.17.5 debug; cannot install over previous test copy.** If necessary uninstall *only* old BOOX Test package, never the official Quran Haafidh release. Data in old debug would not be preserved.
- BOOX physical E-Ink device validation remains untested; only Android emulator evidence. No public release, no official upgrade signing, no full CP2–CP6 claim.
