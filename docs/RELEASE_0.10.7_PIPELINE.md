# Quran Safeguard 0.10.7 — private release pipeline

This branch is released through the same audited offline-signing path used for 0.10.6:

1. fail-closed 0.10.7 source preparation and contradictory audit;
2. inherited stability/privacy/runtime-clean gates;
3. full 604-page Medina Mushaf verification and Plus Tafsir rebuild/audit;
4. Light/Plus unit tests and unsigned release builds;
5. signed artifacts are produced offline with the historical Light/Plus signing lineages, never newly generated keys;
6. zipalign, apksigner v2/v3 verification, APK payload reinspection and SHA-256 precede private delivery.

This file records the explicit rerun of the final 0.10.7 release pipeline after the target-presence timer correction on `release/0.10.7-final`.
