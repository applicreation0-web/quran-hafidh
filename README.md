# Quran Safeguard

Quran Safeguard is a private Android digital-wellbeing application that creates a deliberate Quran reading pause before selected apps and eight mainstream browsers.

## Product principles

- Voluntary discipline, not device coercion.
- Android Settings and normal uninstall remain available to the device owner.
- Medina Mushaf, 604 pages, Hafs ‘an ‘Asim, available fully offline.
- The canonical 15-line Mushaf page is never reformatted.
- The reader initially shows approximately half of the rendered page and requires a genuine scroll to the bottom.
- Reading time is measured as real foreground reading time; there is no fixed minimum reading speed.
- After a completed page: reading recap -> daily spiritual reminder -> explicit Continue -> app unlock.
- Unlock allowance is per protected application, up to 20 minutes, consumed only while that application is actually in the foreground.
- Gentle usage reminders are shown at 10, 5 and 1 minute remaining.
- Three daily jokers, each capped to a maximum of five minutes.
- Eight explicit browsers only: Chrome, Firefox, Edge, Brave, Opera, Samsung Internet, DuckDuckGo and Vivaldi.
- Phone/dialer and emergency-call infrastructure are never intercepted.
- Accessibility window content retrieval is disabled.
- Private distribution during the testing phase.

## Daily spiritual reminder

- Exactly one main reminder is selected for each local calendar day.
- Arabic is displayed first, French immediately below, without transliteration by default.
- Hadiths must have verified provenance, reference and authenticity status.
- Scholar wisdom is explicitly separated from Prophetic hadiths.
- The release target is exactly 150 reviewed local reminders: 144 hadiths plus 6 scholar wisdom entries.
- Hadith data is frozen into the APK from the reviewed HadeethEnc dataset; the app does not call a daily reminder API.
- A low-importance, no-vibration reminder is scheduled for 20:00 local time when the user allows notifications.
- The 20:00 notification also includes the day's pages, total reading time and average time per page.

## Persistent-data compatibility

- Keep the same Android application ID: `com.applicreation0.quransafeguard`.
- Every release must use a strictly increasing `versionCode`.
- Official Google Play builds must remain on the same Play App Signing identity.
- Migrations are cumulative and non-destructive.
- Before a schema change, all persistent SharedPreferences files are backed up.
- Migration failure triggers restoration of the previous persistent state.
- Unknown legacy keys are preserved unless a known malformed value must be normalized.
- Direct migration from historical preference layouts to the current schema is covered by automated tests.

## Private distribution

Preferred route: Google Play Internal Testing, then Closed Testing if the invited group grows.

See:
- `docs/PRIVATE_DISTRIBUTION.md`
- `docs/GOOGLE_PLAY_PRIVATE_SETUP.md`
- `docs/DEVICE_TEST_MATRIX.md`
- `SECURITY.md`

Never present a debug APK, an APK received by message/email, or an unofficial mirror as an official update.

## Security

Official releases must be signed and distributed through the approved private Google Play channel. Do not distribute release signing keys, upload keys, passwords or other private credentials in the repository.
