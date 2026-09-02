# Al-Hikam diacritics research

Date: 2026-09-02
Status: research only; no runtime promotion

## Source checked

The fully vocalized Arabic matn linked by Damas Cultural Society was downloaded
from:

- https://data.nur.nu/Kutub/Arabic/Ibn3AtaAllah_Hikam_themathesontrust.pdf

The source is explicitly presented as an Arabic Al-Hikam PDF “with tashkil”.

## Reproducible extraction result

Three text extraction modes were compared against the frozen 264-entry corpus
after removing diacritics, tatwil, punctuation and orthographic alef/ya
variants:

| Extractor | Exact/long-prefix skeleton matches |
| --- | ---: |
| pdftotext raw | 0/264 |
| pdftotext layout | 16/264 |
| mutool text | 0/264 |

The best exact alignment covered 16 entries: 14, 27, 37, 52, 75, 78, 91, 92,
96, 106, 108, 151, 176, 179, 199 and 233.

## Decision

This PDF is a valid vocalized control source, but its PDF text encoding and
edition differences make it unsafe as an automatic 264-entry replacement.
Quran Safeguard must not manufacture tashkil or silently transfer diacritics
across a non-matching base-letter sequence.

For each future promotion:

1. identify the exact numbered Hikma and page in the vocalized source;
2. compare the full base-letter skeleton with the retained production text;
3. resolve edition or numbering differences manually;
4. copy only source-visible diacritics;
5. retain a source locator and manual boundary-review record;
6. reject the change if the consonantal text cannot be reconciled.

The current partial diacritics remain untouched until this entry-by-entry
process succeeds. Automatic Arabic vocalization is forbidden.
