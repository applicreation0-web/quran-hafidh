# Quran Safeguard — physical-device release matrix

This matrix is a release gate for broader private testing. Compilation and JVM tests are necessary but not sufficient for AccessibilityService behavior.

## Required device profiles

Test at minimum:
- Google Pixel / near-stock Android, current supported Android version.
- Samsung Galaxy / One UI.
- One additional OEM with aggressive background-management behavior when available.
- At least one Android 13+ device for notification permission behavior.

## Interception

For YouTube, WhatsApp, Chrome and one user-selected third-party app:
- cold launch triggers Quran Safeguard;
- launch from recents triggers it;
- launch from a notification triggers it;
- rapid close/reopen cannot bypass it;
- switching quickly between protected targets does not leave one accidentally unlocked;
- Play Store remains accessible;
- Android Settings remains accessible by product decision (voluntary Safeguard).

## Quran reading

- Canonical 15-line page is unchanged.
- Initial viewport is approximately half of the actual rendered SVG page.
- The completion button is unavailable before a real downward scroll reaches the page bottom.
- Pinch/zoom cannot shrink the whole page to bypass the scroll requirement.
- Screen off, Home, call interruption and leaving the reader pause the reading chrono.
- Different natural reading speeds are accepted; no fixed speed is imposed.
- One completed page is counted once even if the summary screen is left and reopened.

## Post-reading summary and unlock

- Completing the page records stats but does not yet unlock the target.
- Recap appears first, then the daily reminder.
- Back/leave from recap does not grant target access.
- Reopening the target returns to the already-completed recap without forcing another Quran page.
- Pressing Continue grants the target's configured allowance and reveals the previous target context when Android retains it.

## Per-app allowance

- Allowance decreases only while the protected target is actively foreground.
- Keyboard/IME use inside the target does not pause or reset the allowance.
- Notification shade/system UI pauses consumption while the target is not interactively foreground.
- Screen off pauses consumption.
- Calls pause consumption.
- Leaving for another app pauses consumption.
- Returning resumes from the remaining value.
- Gentle reminders occur once per allowance at 10, 5 and 1 minute remaining.
- At zero, the target is immediately gated again.

## Calls and emergency behavior

- Incoming call is never intercepted.
- Outgoing call/dialer is never intercepted.
- Emergency-call flow is never blocked.
- Returning from a call leaves timers in a coherent state.

## Daily reminder

- Home screen shows the same reminder all day.
- Repeated Quran unlocks on the same date show the same reminder.
- At local 20:00, when notifications are enabled and permission is granted, one low-importance/no-vibration notification appears.
- Notification contains daily pages, total reading time, average per page, Arabic reminder, French translation and source.
- Changing timezone or rebooting reschedules the next 20:00 reminder.
- No network connection is needed to display a reminder.

## Upgrade tests

Install historical official builds available in the testing channel and upgrade directly to the candidate build:
- verify application ID/signing-compatible in-place update;
- blacklist preserved;
- Juz/Hizb selections preserved;
- reading history/statistics preserved;
- unlock duration safely normalized;
- Accessibility consent preference preserved;
- no crash on first launch;
- migration diagnostic reports success;
- no user uninstall/reinstall step required.

## Release decision

Do not widen the beta if any P0/P1 row above fails on a supported device. Record OEM/model, Android version and diagnostic timestamp for any failure.
