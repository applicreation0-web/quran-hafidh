# Safeguard 0.9.4 — pre-release contract

This branch follows the user-approved 39-point roadmap. Newer decisions in this
contract supersede older chat notes and legacy implementation details.

## Release-blocking invariants

- Retained Al-Hikam corpus: 264 edition-specific entries, each with Arabic,
  internal French translation and documentary metadata.
- Classical commentary remains optional and source-isolated:
  one commentator, one work and one locator per commentary unit; no AI synthesis.
  Verified continuous Ibn ʿAjība excerpts cover Hikam 1–15 and 17–20; Hikma 16 remains withheld pending a reliable source boundary.
- Al-Ghazali is temporarily absent from the UI and daily reminders until a
  sufficient Ayyuha al-Walad corpus is verified.
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

CI gate: verifyReleaseAudit, unit tests, debug APK and unsigned release APK must all pass on the same commit.


## Reserved improvement — protected-only observation

Status: **pending; not yet implemented**.

The permanent Accessibility scope should contain only Quran Safeguard and the
applications explicitly selected for protection. Banking, payment, identity and
security applications must not be classified, monitored or retained.

Exact foreground accounting still needs one transition signal when a protected
application loses the foreground. The intended design is to consume only that
first exit signal, pause the protected package budget, discard the destination
identity immediately and return to the narrow protected-only scope.

Trusted authentication/payment Custom Tabs should be temporarily allowed from
their window class without creating a persistent association with the
originating sensitive application. This change requires its own adversarial
audit because it trades a small Custom Tab bypass surface for stricter
non-interaction with sensitive applications.
