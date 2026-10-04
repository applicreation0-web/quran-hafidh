# Quran Safeguard 0.10.7 — Tafsîr reading contract

Status: blocking 0.10.7 presentation contract.

- The displayed Tafsîr remains source-backed English. No generated translation or paraphrase is inserted.
- Primary edition labels remain the author names: **Jalalayn**, **Qurtubi**, **Qushayri**.
- Commentary prose and notes use full justification for a calm editorial reading column. Poetry is the exception: it remains logical Start/left aligned. Source-provided Qur'an translation keeps its source emphasis, including bold italic where tagged.
- Established sizes remain: 18sp default commentary, 16–26sp user range; commentary 1.50×, notes 1.45×, poetry 1.55×.
- Poetry is source-semantic only, never inferred from italics. Source line breaks and stanza breaks are preserved. False PDF/OCR blank lines outside source poetry/stanzas are removed.
- Qushayri source-note calls are removed only when source verification establishes them as unexposed footnote calls. Arbitrary numbers must never be stripped.
- Explicit canonical Qur’an cross-references remain interactive with a tap target larger than the visible underlined reference. Back returns to the originating Tafsîr state.
- The Tafsîr panel has compact and expanded reading states; expansion must not duplicate controls or alter source text.
- Selected Mushaf verses use a muted neutral highlight with no enclosing outline. Selection must not imply neighboring verse fragments.
- Reading surfaces use warm cream/ivory colors and remain understandable in grayscale; color is not the only carrier of meaning.
