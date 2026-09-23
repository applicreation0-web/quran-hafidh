# Quran Safeguard 0.10.5 — Tafsīr typography and alignment contract

Status: blocking non-regression contract for the Plus Tafsīr reader.

## Governing rule

The source text remains authoritative. Typography may distinguish source roles and improve readability, but it must never rewrite, paraphrase, normalize away scholarly diacritics, invent honorifics, infer poetry, or change documentary meaning.

A source honorific/special glyph is preserved only when present in the source. No name-based insertion is allowed. Scholarly transliteration characters such as ṣ, ḍ, ḥ, ṭ, ẓ, ā, ī, ū, ʿ and ʾ must survive extraction and rendering exactly.

## Shared font family

All English/Latin Tafsīr reading text uses one Unicode-capable system sans-serif reading family for visual consistency across Jalālayn, Qurṭubī and Qushayrī. The renderer must allow platform glyph fallback for source honorific symbols and other Unicode characters rather than replacing them with ASCII letters.

No separate decorative font is permitted inside the commentary body before release. The Mushaf SVG remains visually independent from the Tafsīr text family.

## Semantic typography and alignment

| Role | Font/style convention | Size / line height | Alignment | Non-regression rule |
| --- | --- | --- | --- | --- |
| Commentary prose | shared reading family, regular; preserve source bold/italic emphasis | default 18sp, user range 16–26sp; line height 1.50× | **Justified** | prose may be reflowed, never rewritten |
| Source Qur’an translation inside Tafsīr | shared reading family, **bold italic** | same user-controlled base size; 1.50× | **Justified** | translation remains first for Qushayrī/Qurṭubī; never converted into a generated heading |
| Poetry / verse / strophe | shared reading family, italic unless the source explicitly carries another emphasis | base size; line height 1.55× | **Start/left, never justified** | preserve source line breaks and stanza boundaries; never infer poetry from italics alone |
| Scholarly transliteration | shared reading family; preserve source emphasis; italic only when the source/semantic tagging identifies a transliterated technical term | inline with surrounding text | **Inherits surrounding paragraph** | preserve every diacritic; no ASCII simplification |
| Technical term in commentary | shared reading family, medium/semi-bold only when explicitly tagged for glossary interaction | inline with surrounding text | **Inherits surrounding paragraph** | visual distinction must not alter source wording |
| Technical explanatory text / metadata | shared sans-serif utility treatment, regular/medium, slightly smaller than commentary | approximately 0.92× body size; line height about 1.40× | **Start/left** | never mixed visually with the author’s commentary |
| Lexicon headword | shared sans-serif utility treatment, bold | body size or +1sp | **Start/left** | source term and language identified explicitly |
| Lexicon definition | shared sans-serif utility treatment, regular | body size −1sp minimum 16sp; line height about 1.40× | **Justified when multi-line** | definition must be externally sourced; no AI-authored definition presented as source |
| Arabic lexicon headword | Unicode Arabic-capable fallback, normal/bold according to lexicon design | readable body size | **RTL / logical start** | preserve Arabic spelling and diacritics from the lexicon source |
| Qur’an cross-reference | inherits surrounding font, semi-bold + underline/link treatment | inline | **Inherits surrounding paragraph** | only explicit canonically valid source references are clickable |
| Note call | inherits reading family, superscript, reduced scale | 0.78em | **Inline** | call must resolve to an existing source note or remain non-interactive |
| Note body | shared reading family, regular; preserve source emphasis | body size −1sp, minimum 16sp; line height 1.45× | **Justified** | exact source note, no reconstruction |
| Honorific / special source glyph | exact Unicode/source-equivalent glyph with platform fallback; no forced weight/style | inline | **Inherits surrounding paragraph** | never add by person name; never leave extracted placeholder letters such as a font-mapped `g` when the source contains a special glyph |

## Paragraph and spacing rules

1. Commentary prose is justified. A final short line is not artificially stretched.
2. Poetry is never justified. Source line breaks and stanza spacing are retained. A poetry block is rendered as such only when the source/extractor explicitly tags that semantic role; italics alone never trigger poetry classification.
3. A real paragraph break is preserved; PDF block boundaries alone do not create a paragraph.
4. False PDF/OCR blank lines are removed only when source continuity is demonstrated.
5. Qur’an translation and commentary remain visually distinct without inserting generated labels.
6. Notes use a slightly tighter line height than commentary but never below the sustained-reading minimum size.
7. Lexicon and technical UI are visually separated from the author’s commentary so the reader can always distinguish source text from application/reference material.

## Release gate

The Tafsīr work cannot receive a final PASS unless the audit verifies these conventions together with: source glyph preservation, scholarly diacritics, poetry/stanza structure, Qushayrī notes, canonical Qur’an links, and Light/Plus isolation.
