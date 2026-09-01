# Quran Safeguard

Quran Safeguard is a private Android digital-wellbeing application that creates a deliberate Quran reading pause before selected apps and eight mainstream browsers.

## Product principles

- Voluntary discipline: Android Settings and normal uninstall remain accessible to the device owner.
- Medina Mushaf, 604 canonical pages, Hafs ‘an ‘Asim, available offline.
- The original 15-line page format is preserved.
- A reading session shows roughly half a rendered page at once and requires a natural scroll to the bottom.
- Reading time is measured for personal progress without a fixed minimum reading speed.
- Each normal unlock grants up to 20 minutes to the relevant app, counted only while that app is actually in the foreground.
- Gentle usage reminders appear at 10, 5 and 1 minute remaining.
- Three daily jokers remain available, each capped to a maximum of five minutes.
- Eight explicit browsers only: Chrome, Firefox, Edge, Brave, Opera, Samsung Internet, DuckDuckGo and Vivaldi.
- Phone and emergency calling infrastructure is never intercepted.
- Accessibility window content retrieval is disabled.

## Spiritual reminders

- One main reminder is selected locally for each day and remains stable throughout that day.
- The bundled library has no numerical quota: only texts that pass authenticity, provenance and suitability checks are enabled.
- Arabic is shown first, followed by French and an identifiable source.
- Automatically curated hadiths are restricted to Sahih al-Bukhari and/or Sahih Muslim; individually reviewed texts from other recognized collections retain their explicit grading.
- Hadith, al-Ghazali quotations and al-Hikam are explicitly distinguished.
- Every curated reminder and every adhkar has a local provenance record; frozen HadeethEnc entries are verified at build time.
- Adhkar can optionally show a discreet transliteration layer (off by default).
- Daily/adhkar reminders use a small heads-up banner with one short vibration and no sound.
- Banking, payment, identity, authentication, password-manager and security apps are permanently outside Safeguard scope and are rejected from persistence/logging.
- No transliteration is shown by default.
- A gentle daily notification targets 20:00 local time.
- Authenticated morning adhkar are offered between Fajr and sunrise.
- Authenticated evening adhkar are offered between ‘Asr and Maghrib.
- Prayer windows are calculated locally from an approximate location; coordinates are not uploaded by Quran Safeguard.

## Updates and data

- The Android application ID remains `com.applicreation0.quransafeguard`.
- Persistent migrations are cumulative and non-destructive.
- Existing user preferences are backed up before a schema migration.
- Recognized legacy formats are normalized; malformed obsolete values are ignored safely instead of crashing.
- A failed migration restores the pre-migration preference backup.
- Official updates must continue through the same Google Play app/signing identity.

## Private distribution

Preferred route: Google Play Internal Testing, then Closed Testing if the invited group grows.

See:
- `docs/PRIVATE_DISTRIBUTION.md`
- `docs/GOOGLE_PLAY_PRIVATE_SETUP.md`
- `SECURITY.md`

## Security

Official releases must be signed and distributed through the approved private Google Play channel. Do not treat APK files received by message, email or third-party download sites as official builds.
