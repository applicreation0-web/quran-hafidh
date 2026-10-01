# Quran Safeguard 0.9.1 — Scope / firewall audit

Date: 2026-09-01

## Product scope

Selectable targets are deliberately restricted to:
- social/communication targets declared in ProtectedApps.socialTargets;
- exactly eight browsers declared in ProtectedApps.browserTargets.

Android Settings is a fixed anti-bypass protected target and is not user-selectable.

Banking, finance, payment, identity, authentication/2FA, password-manager,
security/VPN, calls, emergency and alarms are not selectable Safeguard targets.

## Runtime accessibility model

Idle / normal state:
- AccessibilityService packageNames is narrowed to the selected social/browser targets,
  Android Settings and Quran Safeguard itself.
- no global package-label classification is performed;
- no app catalogue scan is used for protection decisions.

Active protected session:
- packageNames is temporarily broadened only while a protected target is active,
  so the first transition away can pause the true foreground timer and cancel stale
  gate retries;
- the first package outside the fixed event scope is treated generically as
  "outside scope": no label lookup, no banking/security classification, no
  diagnostic package entry and no Safeguard state association;
- the service immediately narrows its packageNames again.

This is the minimal compromise required to keep actual-foreground time accurate
without a Usage Access permission or URL/content inspection.

## Privacy boundary

- canRetrieveWindowContent=false
- no typeViewFocused
- no INTERNET permission
- no QUERY_ALL_PACKAGES
- no URL-bar or page-content inspection
- no package install/change receiver in 0.9.1
- build-time guard requires a static packageNames scope and forbids known sensitive
  packages from appearing there.

## Migration

Schema 7 reuses the defensive purge with the new fixed target policy. Legacy
arbitrary application selections, session keys, challenge keys, reading history
and diagnostics that do not belong to the fixed Safeguard scope are removed.

## Expected lag improvement

The 0.9.0 service received every foreground package and classified exclusions.
0.9.1 no longer does that while idle. When leaving a protected target, at most
the first outside-scope window transition is used to stop the timer and is then
discarded without semantic classification.

## Browser URL limitation

A banking URL that remains inside a protected browser is still the browser from
Android's point of view. Safeguard intentionally does not inspect the URL.
