# CP-14 — M6 private isolation and Quran.com offline restriction

Date checked: 2026-10-12. Public, source-safe checkpoint.

Operational verdict for the confirmed strictly personal configuration:
**COMPLETE WITH RIGHTS RESTRICTION**. Distribution of a build containing the
private English titles remains **NO-GO**.

This is a technical compliance record, not legal advice and not an
authorization to retain or redistribute Quran Foundation content. No
credential, private title, API response, title-bearing screenshot or private
corpus is present in this public repository.

## Private package and eight chunks

The baseline package was independently checked in its isolated workspace:
1,903 exact canonical groups, 6,236 verses, 2,951 Quran.com h2 records,
1,709 staged groups, 154 without h2, 40 malformed-only groups, and all eight
private chunks. The reviewed 69:44–52 candidate changes exactly one group:
1,710 staged, 154 without h2 and 39 quarantined. Its fixed-count baseline
validator therefore rejects it with the expected 1,710/154/39 delta; this is
not represented as a baseline PASS.

The isolated archive
`PRIVATE_M4_QURANCOM_REVIEWED_2026-10-12.zip` has SHA-256
`5019acd727f423515977d9759d4c25e8f7b20f4dde1c59e943f46a3b882949fa`.
It stays outside GitHub and is not an application artifact. The 17 possible
title/commentary splits remain quarantined/NON_EXECUTE as documented by M4.

## Demonstrated packaging risk and correction

The public repository intentionally retains eight historical spa5k extract
files and two obsolete Java providers as review/audit inputs. They were no
longer called by `StudyReaderActivity`, but Android packages every main asset
by default; the old files could therefore still enter an application package.

A new regression check first failed because no exclusion existed. The minimal
correction now:

- excludes `ibn_kathir_headings/**` from main assets;
- excludes `IbnKathirOfflineTitles.java` and `IbnKathirTitlePack.java` from
  main compilation;
- retains those files in Git solely for provenance/audit;
- asserts the reader uses only `IbnKathirQuranComPrivateTitles`;
- runs the exclusion check in public CI without reading any private text.

No Hifz engine, persistence, mask, counter, scheduler, Jalalayn source or QCF
geometry was changed.

## Official sources checked

- Quran Foundation Developer Terms, last updated 2026-10-11:
  <https://api-docs.quran.com/legal/developer-terms/>
- Official FAQ:
  <https://api-docs.quran.com/docs/tutorials/faq/>
- Content Sync — Getting Started:
  <https://api-docs.quran.com/docs/tutorials/content-sync/getting-started/>
- Content Sync — Offline Cache Patterns:
  <https://api-docs.quran.com/docs/tutorials/content-sync/offline-cache-patterns/>

The ordinary API path does not establish permission for indefinite embedded
storage. The Terms limit caching/storage of QF Content to one week unless the
resource is obtained and maintained through Content Sync, subject to stated
exceptions. A personal-only application does not itself erase those terms.

The generic Content Sync documentation lists the `tafsirs` resource group
and illustrates `tafsirs:151`; this does not prove production publication,
authorized access or exact required h2 fields for `tafsirs:169`. No
credentialed bootstrap was attempted.

## Scope decision applied

The user confirmed a strictly personal, non-public application and prohibited
new server, account, network or mandatory synchronization work. Accordingly:

- the private candidate remains a separate test-only sidecar;
- no private chunk is committed or attached to public CI;
- no network, backend, account, credential or sync architecture is added;
- no distributable debug or release is built;
- the functional offline code remains intact.

This completes the feasible M6 isolation work. It does **not** resolve durable
content rights. Written permission for this exact personal/offline use, or a
separately authorized and proven Content Sync 169 design, remains required
before any distribution or claim of permanent-storage authorization.

## Residual classification

- **PASS:** eight private chunks are isolated and mechanically matched to the
  1,903-group sidecar; candidate delta is explicitly classified.
- **PASS:** obsolete spa5k assets/classes cannot enter main packaging.
- **PASS:** Git tracking contains no private chunk/archive/title-bearing
  capture or credential at this checkpoint.
- **P0 NO-GO DISTRIBUTION:** no verified durable-storage authorization for the
  private titles.
- **P1 NON_EXECUTE:** Content Sync publication/access and exact fields for 169.
- **P1 NON_EXECUTE:** private title pixels and physical BOOX rendering.

## CI

Same-SHA source/JVM and Android 98-test plus external-force-stop results will
be recorded after this public checkpoint is committed. No APK or release is
published by either audit workflow.
