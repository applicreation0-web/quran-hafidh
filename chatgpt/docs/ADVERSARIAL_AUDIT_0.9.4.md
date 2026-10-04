# Adversarial audit — Quran Safeguard 0.9.4

Audit date: 2026-09-02  
Audited implementation commit: `d98e4b389cd455186eefd8c73b35655206ea598d`  
Release state: **blocked — no merge, signing or publication authorized**

## First adversarial pass: defects found and corrected

1. **Sensitive handoff lease could slide.** Browser or Android Settings events inside a bank-origin authentication flow renewed the 120-second lease. The service now anchors expiry only to a real sensitive-app event; a handoff event cannot silently prolong the exemption.
2. **A general Android Settings shortcut remained in the app.** The “Infos et autorisations Android” action and its `ACTION_APPLICATION_DETAILS_SETTINGS` intent were removed. The guided Accessibility activation is the only Safeguard action that opens Android Settings.
3. **Reading time was not strict enough in Android multi-window.** On Android 10+, an activity can remain resumed without being the top-resumed activity. The Mushaf reader now starts/stops accumulation with `onTopResumedActivityChanged`, while preserving the legacy lifecycle path on older Android versions.

Each correction has a source-level release-audit guard.

## Independent source audit

A second, independently implemented audit checked 54 release invariants across privacy, Accessibility scope, sensitive applications, Android Settings boundaries, Mushaf reading, per-app budgets, jokers, adhkar, the classical library, Juz/Hizb selection, reading history and visual consistency.

Result after evidence-path reconciliation: **54/54 passed**.

The reconciliation did not waive product requirements:

- the 264 Hikam count is enforced by `scripts/verify_hikam_264.py` and `:app:verifyReleaseAudit`;
- 7-day and 30-day reading averages are invoked with numeric arguments `7` and `30`;
- the green/cream/gold/brown palette is declared in `QuranSafeguardTheme`;
- all application buttons delegate to the shared Sahelian ornament implementation.

## Automated verification

Android CI run **#467** completed successfully on the audited implementation commit:

- total release audit;
- unit tests;
- debug APK assembly;
- unsigned release APK assembly;
- signing-tool packaging.

The commit containing this report must pass the same complete workflow again. Artifact upload failures caused only by repository storage quota are configured as non-product failures; compilation, tests and assembly remain mandatory.

## Classical-source boundary

- The base Al-Hikam corpus remains exactly 264 Arabic/French/source entries.
- Production commentaries remain limited to the 19 continuous, source-isolated Ibn ʿAjība units reviewed for Hikam 1–15 and 17–20.
- Hikma 16 and every unresolved boundary remain withheld.
- No fuzzy match, reconstructed diacritic or cross-commentator synthesis is eligible for automatic production promotion.

## Signing continuity blocker

Recovered binaries expose two distinct certificate lineages:

- retained release lineage: `6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC`;
- recovered 0.9.0 first-install APK: `9C:66:DE:17:3F:EB:0E:10:24:5F:8D:84:9C:2A:7C:B6:4B:BF:75:F5:0B:63:C5:F8:30:A2:28:AB:D1:00:9F:E7`.

The installed device certificate must be measured before any signed candidate is produced. Chat history cannot override certificate evidence.

## Remaining device gate

No release is authorized until a physical-device pass verifies:

1. installed certificate lineage;
2. bank/payment/identity/security fail-open behavior, including biometric and browser authentication handoffs;
3. call and VoIP budget freeze;
4. direct return to the protected app after a valid 60-second active reading;
5. multi-window, screen-off, keyboard and app-switch budget accounting;
6. upgrade installation without data loss when and only when the certificate lineage matches.

Until those checks are performed, the repository may produce test APKs only; it must not publish a new version.
