# Classical texts: source integrity and "Approfondir"

Date: 2026-09-01

## Product rule

Quran Safeguard must never present an AI summary, paraphrase or reconstructed
passage as the words of a classical author or commentator.

Arabic source text, French translation and provenance stay attached to the same
record. Long passages may be split only for navigation; splitting must not
change wording or silently omit text.

## Al-Hikam

- Ibn 'Ata' Allah al-Iskandari is the author of the Hikam.
- The current preferred deep-reading source is Ibn Ajiba,
  *Iqaz al-Himam fi Sharh al-Hikam*.
- The UI label is: **Approfondir — commentaire classique**.
- The deep-reading page identifies Ibn Ajiba as the commentator.
- No "explication simple" or app-authored summary is permitted.
- If a passage is not complete, it must be explicitly labelled as an excerpt.
- A French translation is displayed only after source verification, translation
  rights review and human verification.

## Al-Ghazali

The current Ghazali reminders are direct excerpts from al-Ghazali's works, not
later commentary. The equivalent of "Approfondir" is therefore different:

- preferred UI label: **Approfondir — contexte dans l'œuvre**;
- show a contiguous surrounding passage by al-Ghazali from the same work;
- do not replace the author's context with an app summary;
- if a separate classical commentator is ever used, identify that commentator
  and work explicitly instead of presenting it as al-Ghazali's own words.

The current canonical sources are held in GhazaliRepository.

## Display gate

ClassicalDepthRepository is the only source for long-form classical material.
A record becomes visible only when all of the following are true:

1. Arabic source text is present.
2. Matching French translation is present.
3. Source URL and source reference are present.
4. Human verification is recorded.
5. Translation rights status is not unresolved.

This deliberately means that the UI can be implemented before the full corpus
is admitted, without exposing unverified content.

## Release rule

A build must not claim complete Hikam commentary, complete Ghazali context, or a
complete classical corpus until the corresponding source audit is complete.
