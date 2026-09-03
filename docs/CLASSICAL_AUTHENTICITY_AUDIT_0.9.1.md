# Quran Safeguard 0.9.1 — Classical authenticity status

Date: 2026-09-01

## Principle

AUTHENTICITY BEFORE QUANTITY — AND NO FALSE COMPLETENESS CLAIM.

Quran Safeguard does not summarize, interpret, reconstruct or speak in the
authorial voice of Ibn ʿAṭāʾ Allāh, Ibn ʿAjība or al-Ghazālī.

For classical display eligibility the application requires:
- exact Arabic text present;
- sourceVerified=true;
- attributionVerified=true;
- translationAvailable=true;
- rightsStatus != UNRESOLVED;
- authenticityStatus=VERIFIED_SOURCE;
- complete documentary source metadata;
- explicit internal-French-translation provenance.

`humanVerified` remains separate, informative metadata and is not required for
display when the Arabic/source/attribution and translation provenance are valid.

## Al-Hikam al-ʿAṭāʾiyya

### Release corpus

0.9.1 bundles **264/264 entries according to the retained 1–264 numbering**.

Release-blocking corpus checks require:
- exactly 264 entries;
- source_number exactly 1 through 264 with no gap or duplicate;
- non-empty Arabic and French for every entry;
- verified Arabic/source status for every entry;
- verified translation status and provenance for every entry;
- text_type=author_wisdom;
- no explanation, simpleExplanation, aiSummary or meaning field.

The frozen verification report records:
- reference_count: 264;
- verified_and_translated: 264;
- pending_verification: 0;
- missing_french_translation: 0.

### Arabic source controls

Primary matn:
- Al-Latāʾif al-Ilāhiyya fī Sharḥ Mukhtārāt min al-Ḥikam al-ʿAṭāʾiyya;
- Dār al-Kutub al-ʿIlmiyya, Beirut, 1426/2005;
- ed. ʿĀṣim Ibrāhīm al-Kayyālī;
- digital matn pages 95–114.

Secondary controls retained in the frozen report:
- independently numbered Arabic Al-Hikam PDF;
- University of Ghardaïa academic appendix containing Matn al-Hikam.

The corpus keeps the retained edition numbering rather than attempting to
normalize every historical edition to a universal number.

### French

Every entry uses an internal Quran Safeguard French translation produced from
the verified Arabic matn. Modern published French editions are bibliographic
and terminological controls and are not copied wholesale into the application.

The UI identifies the translation as internal Quran Safeguard translation.

### Classical commentaries

The 264 Hikam themselves do not depend on commentary availability.

Commentaries follow a strict source-isolation rule:
- one commentary unit = one identified commentator + one identified work/source;
- different commentators are never merged into a synthetic explanation;
- no AI synthesis, reconciliation or paraphrase is produced from several commentaries;
- when several verified commentaries exist for one Hikma, the UI shows them as
  separate cards, each with its own author, work, locator, source and translation provenance.

Separately verified Ibn ʿAjība excerpts are attached to Hikam 1–15 and 17–20.
Every production passage is continuous and free of internal cuts; each entry
retains its exact Hikma number, edition page, source URL and separate internal
French translation. Hikma 16 remains withheld because the collected source
boundary is not yet reliable enough for production.

Future verified commentaries may come from other classical commentators without
changing the Hikma text itself. Absence of a verified commentary means absence
of “Approfondir” content, not exclusion of the Hikma.

## Al-Ghazālī

Al-Ghazālī is temporarily absent from the application UI and from daily
reminders because the single verified Ayyuhā al-Walad passage is not a
sufficient user-facing corpus. The dormant source record remains restricted to
Ayyuhā al-Walad for future editorial work.

Bidāyat al-Hidāya and Iḥyāʾ ʿUlūm al-Dīn remain outside this corpus.

## Thought of the day / unlock boundary

The 264 eligible Hikam may participate in the Thought-of-the-day rotation.
Thought of the day remains independent from Quran unlock.

Unlock path remains:
protected app → Quran reading → validation → immediate return to the protected app.

## Release statement

For the retained edition numbering used by Quran Safeguard, the 0.9.1 Hikam
corpus is complete at 264/264 and all 264 bundled entries have Arabic, French
translation and documentary provenance.

This statement is edition-specific: it does not assert that every historical
edition divides or numbers Al-Hikam identically.
