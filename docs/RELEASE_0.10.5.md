# Quran Safeguard 0.10.5 — integrated release candidate

This marker identifies the single integrated candidate containing:

- **Plus Tafsir as a release-blocking feature**: Jalalayn, Qushayri and Qurtubi, rebuilt/audited from the approved pinned sources;
- strict Qurtubi source/payload provenance checks separating the 2,959 raw embedded-font source glyphs from the 2,898 retained non-overlapping honorific marks;
- 604/604 Medina Mushaf completeness verification including page 552 / surah 61;
- automatic last-page resume **plus an explicit persistent user bookmark** in the free Quran reader;
- canonical 30 Juz / 60 Hizb planning corrections;
- 264/264 verified Arabic Hikam with French translation; commentary remains optional and separated from the matn/translation;
- privacy-first fixed Accessibility scope for banking/security/identity isolation;
- **Taddabur deferred from the published 0.10.5 runtime**: no Taddabur block, reminder alarm, dashboard card or declared activity; its internal core is preserved for later work;
- release-only version promotion to versionCode 24 / versionName 0.10.5.

Publication is authorized only through `.github/workflows/publish-0.10.5.yml` after every contradictory source/corpus audit, Light/Plus unit test, APK boundary check, Tafsir payload verification and signature verification succeeds.

The stability branch must remain unmerged until its final head is fully green.
