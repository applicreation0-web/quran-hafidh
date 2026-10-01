# Quran Safeguard 0.9.0 — Firewall / out-of-scope audit

Date: 2026-09-01

## Objective

Applications used for calls/emergency, alarms, banking/finance, payment,
identity, authentication/2FA, password management and security must remain
outside Quran Safeguard. They must not receive a Quran gate, usage budget,
reading challenge or persisted Safeguard association.

Android Settings is intentionally NOT outside scope: it is a fixed system
protected target so that disabling the AccessibilityService is not a trivial
bypass. Google Play Store remains outside scope.

## Verified privacy boundary

- Android manifest does **not** request `INTERNET`.
- Android manifest does **not** request `QUERY_ALL_PACKAGES`.
- Accessibility window content retrieval is disabled:
  `canRetrieveWindowContent=false`.
- Accessibility events are limited to:
  `typeWindowStateChanged|typeWindowsChanged`.
- `typeViewFocused` is explicitly forbidden by a build-time check.
- The Quran gate is forbidden over the Android lock screen.
- No text fields, accessibility node tree, keystrokes or screen content are read.

The build fails if these invariants are relaxed.

## Fixed system policy

Always protected:

- Android Settings (`com.android.settings`).

Always outside scope:

- Google Play Store.
- Android calling / telecom infrastructure.
- The current default dialer, dynamically resolved.
- Google / AOSP / Samsung clock and alarm surfaces.
- Android / Google emergency and safety surfaces.
- The current system keyboard/input method (plus common Gboard/Samsung/AOSP/SwiftKey packages), because IME windows are transient overlays and must never be treated as a new foreground awareness target.
- Google Play Services and known credential/security frameworks.
- Known banking, finance, payment and fintech package families.
- Known authenticator, password-manager and identity-protection families.
- Apps whose local launcher label/package clearly indicates banking, finance,
  payment, identity, authentication, password management, VPN or security.

The classifier is deliberately conservative.

## Runtime behavior on an excluded app

On a window event for a permanently excluded package:

1. any pending gate retries are cancelled;
2. foreground usage for the previous protected app is stopped;
3. the external foreground target is cleared WITHOUT storing the excluded
   package name;
4. the interception state is reset when safe;
5. no gate is started;
6. no Quran challenge is created;
7. no usage timer is attached;
8. no package name is written to diagnostics or health state.

A stale gate intent also performs an independent foreground-target check before
rendering. This closes the race where a gate requested for app A could otherwise
appear after the user had already switched to a bank/security app B.

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
"Tout sélectionner". Persisted selections are self-healed at runtime.

Android Settings is also hidden from the catalogue because its protection is a
fixed system rule, not a user-selectable preference.

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

## URL-level limitation

Safeguard makes decisions at application/package level and deliberately does
not read browser URL bars or page content.

Consequences:

- if a banking link opens the installed banking app, the banking app is outside
  scope and is not gated;
- if the same banking URL remains inside Chrome/Firefox/etc., Safeguard sees the
  protected browser, not the semantic ownership of the URL.

Guaranteeing banking-domain exemptions inside a protected browser would require
URL/content inspection or a different network-level architecture. That is
intentionally NOT added because it would violate the current privacy boundary.

## Android framework limitation

The accessibility service cannot use a static package whitelist because the
protected-app list is user-configurable and Safeguard needs to detect when the
user leaves a protected app.

Android itself may therefore deliver package metadata for a window change
before Safeguard drops an excluded package. Safeguard does not retrieve its
window content, fields, accessibility tree or text, and does not persist the
excluded package identifier.

This is "no Safeguard blocking/content interaction/association", not a claim
that Android delivers literally zero metadata to an enabled AccessibilityService.

## Android profile limitation

Accessibility enablement is per Android user/profile. A separate work/secondary
profile is not automatically covered by an installation/service enabled only in
the primary profile.

## Classification limitation

Android exposes no universal trustworthy "banking app" or "identity app"
category covering every application worldwide. Quran Safeguard combines:

- explicit critical packages;
- package-family rules;
- package-name fragments;
- local launcher-label classification.

This substantially reduces false negatives but cannot mathematically guarantee
recognition of every opaque third-party package ever published. New families
can be added without changing interception logic.
