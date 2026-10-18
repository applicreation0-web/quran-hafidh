# Atlas + Correspondances — fusion checkpoint, 18 October 2026

Authorized branch: `work/haafidh-1171-ibn-kathir-integration-independent`.
Verified source HEAD before this documentation checkpoint: `33d5daa58c05e59de27f11ab6c47b68ac35c1e86`.

## Mandate and precedence

The user-supplied fusion pack dated 18 October authorizes phased preparation of Atlas/Correspondances and a common BOOX interface. It supersedes the earlier no-fusion phase in `HANDOVER_FUTURE_RACCORD_CORRESPONDANCE_MERGE_2026-10-18.md`. Production integration remains conditional on editorial and technical gates. No APK, signing, installation or release is authorized by this checkpoint.

## Recovered evidence

- All 19 manifest entries of the supplied pack match SHA-256. Clean checkout on the authorized branch; no previous dirty checkout recovered in this workspace.
- Two existing runs on source HEAD are success: 39118665668 and 39118665657. Those historical results are not new editorial, migration or BOOX evidence.
- The 366 Atlas unit IDs/boundaries match the recovered pinned private index; 1053 passage IDs/boundaries match the canonical source CSV. The current workbook yields 1143 intersections, 89 shared passages and 179 shared contexts.
- The original 120-label asset was recovered separately. Its SHA-256 matches the existing runtime pin `421277c59519f2b904de23bbc1e0eee6faeb569e33ed6b425b4b9235cf82ff79`; all 120 IDs and boundaries match the 366-unit index. No migration has been executed.
- Correspondences input contains 110 unique IDs, exactly R001–R110. Its eight historical families are not the five target families. No semantic remapping or production approval was inferred.

## Exact Jalalayn edition identified

Jalal al-Din al-Mahalli and Jalal al-Din al-Suyuti, *Tafsir al-Jalalayn*, translated by Feras Hamza. Fons Vitae / Royal Aal al-Bayt Institute for Islamic Thought, 2008. ISBN 9781891785160.

The 675-page PDF at `https://rissc.jo/wp-content/uploads/2018/11/Tafsir-al-Jalalayn-ebook.pdf` has SHA-256 `bcd2abf8d86ff7ce9e10077a6a0268bf3aea8b6c6ef75153f577be542adc8b8a`, exactly matching the corpus source metadata. Bibliographic details come from PDF pages 3–4, not the contradictory translator heading on the landing page. The different 786-page Altafsir PDF must not supply page numbers for this edition. Locators in the private audit are PDF 1-based pages.

The database has 6236 verse-commentary records, passes SQLite integrity_check, and all 6236 stored body hashes match. These are integrity checks, not semantic approval. Copyright notice requires permission for reproduction: no PDF, translation, source-title corpus, secret or Hifz data is added by this commit.

## Editorial progress and limits

The 13 user-approved cases map to 14 canonical display rows and one local caption for 5:11. Full commentary for these 14 ranges and the local 5:11 verse has been read in this run. Existing source titles and ranges remain unchanged. Titles are user-approved display summaries, not attested Jalalayn section headings. All deployment flags remain false; independent review has not run.

CP01 remains PARTIAL: 1405 canonical rows have not received a full new reading in this run, including six historical full reviews retained without rerunning or recounting them. No global 1419-title semantic PASS. The 110 correspondence dossiers have not been individually audited in this run. CP03–CP07 are not complete. The existing 120-count/SHA loader remains unchanged.

## Next action

Resume the private CP01 register at entries with `current_reviewed_full=false`, prioritizing the remaining Al Imran triage and then An Nisa. Preserve the 13 decisions. Complete independent review, then CP02 R001–R110 and subsequent staged architecture/UI work under the supplied master mandate. Do not restart the historical CP00 inventories.

**Overall result: NO-GO. Application code unchanged; no APK.**
