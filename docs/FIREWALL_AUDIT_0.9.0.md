# Quran Safeguard 0.9.0 — Firewall / out-of-scope audit

Date: 2026-09-01

## Objective

Applications used for calls/emergency, banking/finance, payment, identity,
authentication/2FA, password management and security must remain outside
Quran Safeguard. They must not receive a Quran gate, usage budget, reminder,
reading challenge or persisted Safeguard association.

## Verified privacy boundary

- Android manifest does **not** request `INTERNET`.
- Android manifest does **not** request `QUERY_ALL_PACKAGES`.
- Accessibility window content retrieval is disabled:
  `canRetrieveWindowContent=false`.
- Accessibility events are limited to:
  `typeWindowStateChanged|typeWindowsChanged`.
- `typeViewFocused` is explicitly forbidden by a build-time check.
- No text fields, accessibility node tree, keystrokes or screen content are read.

The build fails if these invariants are relaxed.

## Permanent exclusions

Always outside scope:

- Google Play Store and Android Settings.
- Android calling/emergency infrastructure.
- The current default dialer, dynamically resolved.
- Google Play Services and known credential/security frameworks.
- Known banking, finance, payment and fintech package families.
- Known authenticator, password-manager and identity-protection families.
- Apps whose local launcher label/package clearly indicates banking, finance,
  payment, identity, authentication, password management, VPN or security.

The classifier is deliberately conservative.

## Runtime behavior on an excluded app

On the first window event for a permanently excluded package:

1. pending gate retries are cancelled;
2. any foreground usage session for the previous protected app is stopped;
3. the current interception state is reset when safe;
4. no gate is started;
5. no Quran challenge is created;
6. no usage timer is attached;
7. no package name is written to diagnostics or health state;
8. the excluded decision is cached locally to avoid repeated classification.

There is no network communication because the application has no INTERNET
permission.

## Persistence defense-in-depth

Even if future code accidentally attempts to persist an excluded package:

- `GuardPrefs` rejects unlock, timer, reminder and challenge state;
- reading/joker history rejects excluded package names;
- `GuardHealth` rejects excluded package names;
- `GuardDiagnostics` strips excluded package names and details.

Schema 6 migration removes legacy excluded-package state from:

- protected package selections;
- unlock/session keys;
- challenge/reading keys;
- reading history;
- last protected package health state;
- diagnostics.

## Application catalogue

Out-of-scope applications are removed before the selectable app catalogue is
shown. Therefore they cannot be selected individually or through
"Tout sélectionner". Persisted selections are also self-healed at runtime.

## Browser scope

The web rule is intentionally restricted to exactly eight browsers:

1. Chrome
2. Firefox
3. Microsoft Edge
4. Brave
5. Opera
6. Samsung Internet
7. DuckDuckGo
8. Vivaldi

Regression tests verify this exact set.

## Android platform limitation

The accessibility service is not configured with a static package whitelist
because the protected-app list is user-configurable and the app must reliably
stop foreground usage when the user leaves a protected app.

Therefore Android itself may deliver the package identifier associated with a
window change before Safeguard can decide that the package is excluded.
Safeguard immediately drops excluded packages at this boundary. It does not
retrieve their window content, fields, accessibility tree or text, and it does
not persist their package identifier.

This is "no Safeguard interaction / no Safeguard association", not a claim that
the Android framework delivers literally zero package metadata to the enabled
AccessibilityService.

## Classification limitation

Android exposes no universal trustworthy "banking app" or "identity app"
category covering every application worldwide. Quran Safeguard combines:

- explicit critical packages;
- package-family rules;
- package-name fragments;
- local launcher-label classification.

This substantially reduces false negatives but cannot mathematically guarantee
recognition of every opaque third-party package ever published. The policy is
structured so new families can be added without changing interception logic.
