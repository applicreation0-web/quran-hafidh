# Quran Safeguard 0.9.1 — Classical authenticity status

Date: 2026-09-01

## Principle

AUTHENTICITY BEFORE QUANTITY.

Quran Safeguard does not summarize, interpret, reconstruct or speak in the
authorial voice of Ibn ʿAṭāʾ Allāh, Ibn ʿAjība or al-Ghazālī.

For classical display eligibility the application requires:
- exact Arabic text present;
- sourceVerified=true;
- attributionVerified=true;
- translationAvailable=true;
- rightsStatus != UNRESOLVED;
- authenticityStatus=VERIFIED_SOURCE;
- complete documentary source metadata: author, work, edition/source,
  locator, source URL and explicit translation provenance.

`humanVerified` is separate, informative metadata. It is not a display
requirement. An internal Quran Safeguard French translation may be displayed
when the documentary requirements above are satisfied, and it is explicitly
identified as internal in the UI.

## Al-Hikam al-ʿAṭāʾiyya

Current display-eligible entries: Hikma 5, 10 and 12 from the retained
numbering source.

This release does **not** claim “264/264”, “all Hikam” or a complete corpus.
The repository may grow toward the retained source numbering only as entries
are individually sourced and translated.

For each current Hikma:
- Arabic source and attribution to Ibn ʿAṭāʾ Allāh are documented;
- a precise Hikma locator and source URL are retained;
- the French translation is internal Quran Safeguard;
- Ibn ʿAjība is identified only as COMMENTATOR;
- the available Īqāẓ al-Himam commentary excerpts use explicit […] cuts;
- commentary locators are p.39, p.50 and p.58 in the retained source;
- “Approfondir — commentaire classique” exposes the sourced commentator text,
  not an AI explanation.

A sourced Hikma remains displayable without “Approfondir” when no reliable
commentary is available.

## Al-Ghazālī

The short-reminder scope is limited to **Ayyuhā al-Walad** for 0.9.1.

Current display-eligible entry: one continuous passage beginning
“لا تكن من الأعمال مفلسا...” with the immediately following weapons/lion
context available in the detail layer.

The entry retains:
- the Arabic Wikisource revision URL and precise locator;
- immediate before/after context-control metadata;
- passage role = author’s own words;
- continuity and nuance-risk checks;
- internal French translation of the same continuous passage.

Bidāyat al-Hidāya and Iḥyāʾ ʿUlūm al-Dīn are not part of the 0.9.1
short-reminder corpus.

## Thought of the day / unlock boundary

Eligible classical texts may participate in the Thought-of-the-day rotation.
The card explicitly separates documentary authenticity from internal French
translation provenance.

Thought of the day is independent from Quran unlock. The unlock path remains:
protected app → Quran reading → validated reading summary → access.

## Release statement

Current classical scope is deliberately limited: 3 Hikam + 1 Ayyuhā al-Walad
entry. Authenticity gates are release-blocking; corpus completeness is not
claimed.
