# Quran Hifz — Al-Husary Muʿallim embedded audio (personal build)

## Exact upstream source

- Reciter: Mahmoud Khalil Al-Husary — Muʿallim, Hafṣ ʿan ʿĀṣim.
- Upstream: EveryAyah / VerseByVerseQuran.
- Landing page: `https://everyayah.com/recitations_ayat.html`
- Per-ayah base path: `https://everyayah.com/data/Husary_Muallim_128kbps/`
- Per-surah ZIP directory: `https://everyayah.com/data/Husary_Muallim_128kbps/zips/`
- ZIP example: `https://everyayah.com/data/Husary_Muallim_128kbps/zips/002.zip`
- Ayah example: `https://everyayah.com/data/Husary_Muallim_128kbps/002075.mp3`
- Naming rule: `SSSAAA.mp3`, zero-padded surah and ayah; example `002075.mp3` = Al-Baqarah 2:75.
- Canonical expected file count: 6,236 ayah files.

The build-time fetcher is `scripts/prepare_husary_muallim_embedded.py`. It is the only project code allowed to contact the upstream source. The Android app itself remains without `INTERNET` permission and performs no runtime download.

## Personal embedded layout

The fetcher writes to the ignored local directory:

`private/hifz-audio/husary-muallim/`

That directory contains the 6,236 MP3 files plus `source.json` and `sha256.txt`. During a personal build, Gradle copies those files into the APK assets at:

`assets/audio/husary-muallim/`

Audio assets are stored uncompressed so Android `AssetFileDescriptor` can feed them directly to `MediaPlayer` without first duplicating the full corpus into app-private storage.

## Integrity contract

Before a personal embedded build is accepted:

1. every canonical Hafṣ ayah key from `001001.mp3` through the exact 114-surah ayah counts must exist;
2. no non-canonical `SSSAAA.mp3` may be present;
3. there must be exactly 6,236 unique files;
4. every MP3 must be non-empty;
5. a SHA-256 is written for every MP3;
6. `source.json` records the exact upstream path, file count and aggregate manifest SHA-256.

Any mismatch is a build-blocking failure; the app must never silently shift a recording to another ayah.

## Verse synchronization contract

Synchronization is verse-level only. No word-level highlighting is used.

- The current ayah outline changes only **after** `MediaPlayer.start()` has been called successfully for that ayah.
- While paused, the same ayah remains outlined.
- At the end of an ayah, the old outline remains until the next ayah actually starts, avoiding a false early transition and an unnecessary E-Ink clear/repaint pair.
- At the end of the queue or when the player closes, the outline is cleared.
- Audio playback has no path to repetition counters, Sabqi/Itqān promotion, Murājaʿah cursors or any other Hifz state.

## E-Ink / ghosting contract

The audio indicator is an outline, not a filled gray rectangle, minimizing the changed pixel area. BOOX partial refresh prefers `REGAL`, then `GU`, then `DU` when available through the Onyx SDK at runtime. A full `GC` cleanup occurs periodically after local changes and on page changes. This must still be validated on the physical BOOX Go 10.3 Gen II during a long continuous audio session.

## Rights / distribution note

This branch is for the owner's strictly personal application. The upstream audio recordings are not known to carry a public redistribution license. The project therefore records `redistributionApproved=false`. Do not publish or redistribute an APK containing these audio bytes without permission from the relevant reciter/producer/rightsholder. Quran.com likewise states it cannot grant redistribution permission for recitations it does not own.
