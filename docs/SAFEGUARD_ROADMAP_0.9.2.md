# Safeguard 0.9.2 — release contract

This branch follows the user-approved 39-point roadmap. Newer decisions in this
contract supersede older chat notes and legacy implementation details.

## Release-blocking invariants

- Retained Al-Hikam corpus: 264 edition-specific entries, each with Arabic,
  internal French translation and documentary metadata.
- Classical commentary remains optional and source-isolated:
  one commentator, one work and one locator per commentary unit; no AI synthesis.
- Al-Ghazali short-reminder scope is limited to Ayyuha al-Walad.
- Canonical Medina Mushaf page uses the full remaining reader viewport.
- Validation requires at least 60 seconds of active foreground reading and real
  page progress to the bottom when scrolling is required.
- Background, screen-off and interrupted time never count.
- Successful validation grants the target app credit directly and removes the
  Safeguard task, with no dashboard or completion screen.
- Credits are package-scoped and consumed only while that package is foreground.
- Sensitive categories, calls, alarms, keyboards and Play Store remain excluded.
- Thought of the day is stable for the local day and scheduled at 20:00 local.
- Morning adhkar is scheduled at the midpoint of Fajr to sunrise; evening adhkar
  at the midpoint of Asr to Maghrib.
- Spiritual notifications are silent, briefly vibrating and never force-open UI.
- Usage alerts at 10/5/1 minutes remain separate from spiritual notifications.
- Visual identity remains deep green, soft gold and cream, with an open Mushaf on
  a rehal inside a protective form, including adaptive and monochrome icons.

## Mandatory release sequence

Convergence audit -> tests -> adversarial audit -> direct corrections -> re-tests
-> second audit -> exact final APK -> APK device test -> release authorization.

No release is authorized from the first audit.
