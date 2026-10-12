# CP-12 — M4 Quran.com titles and exact QCF amorces

This public checkpoint contains no private English Quran.com title, private
chunk, or title-bearing screenshot. Private review evidence is archived
separately.

## Corpus and package verification

- Documentary boundaries remain exactly **1,903 groups / 114 surahs / 6,236 verses**.
- QCF geometry remains exactly **604 pages / 77,432 word boxes**.
- All eight private V2 chunks and their manifest were verified in isolation.
- Original mechanical V2 classification: **1,709 staged groups**, **154 groups
  without h2**, **40 malformed-only quarantines**.
- These extraction classes are not an editorial certification.

## Targeted 18-case decision matrix

The private package contains extracted h2 values, but not the original HTML
delimiter structure needed to prove where a heading ends and commentary starts.
No split is invented.

| Group | Range | Decision |
|---|---:|---|
| `IKEN007_038_039` | 7:38–39 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN018_017_017` | 18:17 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN018_019_020` | 18:19–20 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN018_077_078` | 18:77–78 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN018_083_084` | 18:83–84 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN021_083_084` | 21:83–84 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN023_042_044` | 23:42–44 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN023_117_118` | 23:117–118 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN026_038_048` | 26:38–48 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN031_029_030` | 31:29–30 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN032_015_017` | 32:15–17 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN034_028_030` | 34:28–30 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN040_004_006` | 40:4–6 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN041_006_008` | 41:6–8 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN041_052_054` | 41:52–54 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN047_029_031` | 47:29–31 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN054_018_022` | 54:18–22 | **NON_EXECUTE** — delimiter not independently provable; quarantine retained |
| `IKEN069_044_052` | 69:44–52 | **PASS** — false-positive heuristic; one complete verbatim h2 restored, without rewriting |

The isolated candidate consequently contains **1,710 staged groups / 2,891
title instances / 39 quarantines**. An independent delta check proved exactly
one group changed, exactly one source-verbatim item was added, and all 1,903
group boundaries and source archive bytes remained unchanged. The 17 uncertain
splits plus 22 confirmed unsafe entries remain quarantined.

## Four initial-order decisions

Private review established these source-array offsets. No source string is
edited, removed, translated or reordered; subsequent taps continue cyclically
through the complete original array.

| Group | Range | Initial index | Verdict |
|---|---:|---:|---|
| `IKEN011_001_004` | 11:1–4 | 2 | **PASS** |
| `IKEN018_001_005` | 18:1–5 | 2 | **PASS** |
| `IKEN020_001_008` | 20:1–8 | 1 | **PASS** |
| `IKEN079_001_014` | 79:1–14 | 1 | **PASS** |

The public implementation stores only these group IDs and integer offsets. A
bounds check fails closed to index zero if a private source array is shorter
than reviewed. Public tests use synthetic strings only.

## Amorces and isolation

- Each of the 1,903 documentary groups is still anchored at its exact first
  QCF word; the optional second word is included only when contiguous and still
  inside the same documentary group.
- Page and surah crossings are derived from the unchanged 77,432-box geometry;
  group boundaries are never recomputed from title data.
- Initial amorces state remains ON when exact data are available; the saved OFF
  preference remains authoritative.
- Missing, corrupt, unreviewed or unbundled private title content fails closed
  to no title. No technical reference is displayed.
- Title navigation has no `HifzPrefs`, progression, mask, counter, scheduler or
  network write path.

## Proof limits

- Public CI runs without the private title pack by design and proves fail-closed
  behaviour only.
- Actual private-title pixels, long/multiple-title clipping and BOOX placement
  are M5 visual evidence and remain **NON_EXECUTE** until captured privately.
- Rights for durable offline storage remain the independent M6 gate.
- No claim is made that 1,709 or 1,710 groups are editorially certified merely
  because an extraction/validation script accepted their structure.
