import collections
import hashlib
import os
import re
import sqlite3

import fitz

from qushayri_source_semantics_0106_verified import (
    EXPECTED_UNIQUE_POETRY_LINES,
    build_qushayri_source_semantics,
)

PDF = os.environ.get("QUSHAYRI_PDF", "/mnt/data/tafsir-src/lataif.pdf")
OUT = os.environ.get("QUSHAYRI_OUT", "/mnt/data/qushayri_en.sqlite")
EXPECTED_SOURCE_SHA256 = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
EXPECTED_RAW_SEGMENTS = 806
EXPECTED_LOGICAL_ENTRIES = 720
EXPECTED_TRANSLATION_ONLY_ANCHORS = 86
EXPECTED_GROUPED_RANGES = 76
EXPECTED_VERIFIED_NOTE_CALLS = 928
EXPECTED_POETRY_INDEX_ENTRIES = 121
EXPECTED_POETRY_OCCURRENCES = 126
SOFT_HYPHEN_MARKER = "__QSH_SOFT_HYPHEN__"
SOURCE_PARAGRAPH_GAP = 15.0
SOURCE_PARAGRAPH_INDENT = 9.0

anchor_re = re.compile(r"^\[(\d+):(\d+)(?:[\u2013\u2014-](\d+))?\]\s*")
sura_heading_re = re.compile(r"^S(?:ūrat|urāt|ūra)\b", re.I)
header_re = re.compile(r"^(Subtle Allusions\s+\[|Laṭāʾif al-ishārāt\s+\[|\d+\s*\|\s*•|•\s*Laṭāʾif)")
pua_re = re.compile(r"[\ue000-\uf8ff]")
arabic_re = re.compile(r"[\u0600-\u06ff\u0750-\u077f\u0870-\u089f\u08a0-\u08ff\ufb50-\ufdff\ufe70-\ufeff]")

SOURCE_PUA_MAP = {
    "\uf063": "(swt)",
    "\uf067": "(ṣ)",
    "\uf068": "(r)",
    "\uf069": "(r)",
    "\uf06e": "(ʿa)",
    "\uf070": "(ʿa)",
    "\uf072": "(r)",
    "\uf082": "(ṣʿa)",
    "\uf094": "(t)",
    "\uf096": "(s)",
}
DECORATIVE_PUA = {"\uf023", "\uf081", "\uf085"}


def file_sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def restore_source_pua(text):
    out = []
    for ch in text:
        if ch in SOURCE_PUA_MAP:
            out.append(SOURCE_PUA_MAP[ch])
        elif ch in DECORATIVE_PUA:
            continue
        elif "\ue000" <= ch <= "\uf8ff":
            raise RuntimeError(f"Qushayri unknown private-use source glyph: U+{ord(ch):04X}")
        else:
            out.append(ch)
    return "".join(out)


def line_info(line):
    spans = line["spans"]
    text = "".join(s["text"] for s in spans).replace("\u00ad", SOFT_HYPHEN_MARKER)
    fonts = [s["font"] for s in spans if s["text"].strip()]
    sizes = [s["size"] for s in spans if s["text"].strip()]
    return text, fonts, sizes


def resolve_discretionary_hyphens(text):
    marker = re.escape(SOFT_HYPHEN_MARKER)
    text = re.sub(
        rf"([A-Za-zÀ-ÖØ-öø-ÿ]){marker}\s*([A-Za-zÀ-ÖØ-öø-ÿ])",
        r"\1\2",
        text,
    )
    return text.replace(SOFT_HYPHEN_MARKER, "")


def is_arabic_line(fonts, text):
    if any("KFGQPC" in f or "Arabic" in f for f in fonts):
        return not anchor_re.match(text.strip())
    chars = [c for c in text if not c.isspace()]
    if chars:
        ar = sum(bool(arabic_re.match(c)) for c in chars)
        if ar / len(chars) > 0.55:
            return True
    return False


def starts_italic(fonts):
    return bool(fonts) and "Italic" in fonts[0]


def normalize(parts):
    values = []
    for part in parts:
        text = restore_source_pua(part).replace("\u00a0", " ").strip()
        if text:
            values.append(text)
    text = " ".join(values)
    text = resolve_discretionary_hyphens(text)
    text = re.sub(r"([A-Za-z])\-\s+([a-z])", r"\1\2", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"[ \t]+([,.;:!?])", r"\1", text)
    return text.strip()


def clean_run_text(text):
    text = restore_source_pua(text).replace("\u00a0", " ")
    text = resolve_discretionary_hyphens(text)
    text = re.sub(r"([A-Za-z])\-\s+([a-z])", r"\1\2", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"[ \t]+([,.;:!?])", r"\1", text)
    text = re.sub(r" *\n *", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text


def is_true_prose_paragraph(previous, current):
    """Use source geometry; a PDF block change alone is never a paragraph."""
    if previous is None or previous["poetry"] is not None or current["poetry"] is not None:
        return False
    if previous["block"] == current["block"] and previous["page"] == current["page"]:
        return False
    indented = current["x0"] - previous["x0"] >= SOURCE_PARAGRAPH_INDENT
    if previous["page"] != current["page"]:
        return indented
    vertical_gap = current["y0"] - previous["y0"]
    return indented or vertical_gap > SOURCE_PARAGRAPH_GAP


def normalize_body(items):
    if not items:
        return "", []

    raw_runs = []

    def append_piece(style, value):
        if not value:
            return
        if raw_runs and raw_runs[-1][0] == style:
            raw_runs[-1] = (style, raw_runs[-1][1] + value)
        else:
            raw_runs.append((style, value))

    previous = None
    for item in items:
        poetry = item["poetry"]
        style = "POETRY" if poetry is not None else "REGULAR"
        if previous is not None:
            previous_poetry = previous["poetry"]
            if poetry is not None and previous_poetry is not None and poetry.poem_id == previous_poetry.poem_id:
                separator = "\n\n" if poetry.stanza_break_before else "\n"
            elif poetry is not None or previous_poetry is not None:
                separator = "\n\n"
            elif is_true_prose_paragraph(previous, item):
                separator = "\n\n"
            else:
                separator = " "
            if raw_runs:
                previous_style, previous_text = raw_runs[-1]
                raw_runs[-1] = (previous_style, previous_text + separator)
        append_piece(style, item["text"])
        previous = item

    runs = []
    for style, raw_text in raw_runs:
        text = clean_run_text(raw_text)
        if text:
            runs.append((style, text))
    commentary = "".join(text for _, text in runs).strip()
    if runs:
        first_style, first_text = runs[0]
        runs[0] = (first_style, first_text.lstrip("\n"))
        last_style, last_text = runs[-1]
        runs[-1] = (last_style, last_text.rstrip("\n"))
        runs = [(style, text) for style, text in runs if text]
        commentary = "".join(text for _, text in runs)
    return commentary.strip(), runs


def finish_current(current, segments):
    if not current:
        return None
    commentary, runs = normalize_body(current["body"])
    current["commentary"] = commentary
    current["commentary_runs"] = runs
    current["translation"] = normalize(current["translation_parts"])
    if current["body"]:
        current["page_end"] = max(item["page"] for item in current["body"])
    else:
        current["page_end"] = current["page"]
    del current["body"]
    del current["translation_parts"]
    segments.append(current)
    return None


def anchor_label(row):
    ayah = str(row["start"]) if row["start"] == row["end"] else f'{row["start"]}–{row["end"]}'
    return f'[{row["surah"]}:{ayah}]'


def group_shared_commentaries(raw_rows):
    logical = []
    pending = []
    blank_anchor_count = 0
    grouped_range_count = 0

    for row in raw_rows:
        if pending:
            previous = pending[-1]
            if row["surah"] != previous["surah"] or row["start"] > previous["end"] + 1:
                raise RuntimeError(
                    "Qushayri orphan translation-only anchor before "
                    f'{row["surah"]}:{row["start"]}; refusing to infer a non-contiguous commentary scope'
                )
        pending.append(row)
        if not row["commentary"].strip():
            blank_anchor_count += 1
            continue

        if len(pending) == 1:
            merged = dict(row)
        else:
            first = pending[0]
            last = pending[-1]
            grouped_range_count += 1
            merged = {
                "surah": first["surah"],
                "start": first["start"],
                "end": last["end"],
                "translation": "\n".join(
                    f"{anchor_label(part)} {part['translation']}" for part in pending
                ),
                "commentary": last["commentary"],
                "commentary_runs": last["commentary_runs"],
                "page": first["page"],
                "page_end": last.get("page_end", last["page"]),
            }
        logical.append(merged)
        pending = []

    if pending:
        first = pending[0]
        raise RuntimeError(
            "Qushayri source ends with translation-only anchor(s) without a following commentary: "
            f'{first["surah"]}:{first["start"]}'
        )
    if blank_anchor_count != EXPECTED_TRANSLATION_ONLY_ANCHORS:
        raise RuntimeError(
            f"Qushayri translation-only anchor count changed: {blank_anchor_count} "
            f"!= {EXPECTED_TRANSLATION_ONLY_ANCHORS}"
        )
    if grouped_range_count != EXPECTED_GROUPED_RANGES:
        raise RuntimeError(
            f"Qushayri grouped source range count changed: {grouped_range_count} "
            f"!= {EXPECTED_GROUPED_RANGES}"
        )
    return logical


def strip_verified_calls_without_count(semantics, page, block, line, text):
    value = text
    calls = semantics.note_calls_by_line.get((page, block, line), ())
    for call in sorted(calls, key=lambda item: item.start, reverse=True):
        if value[call.start:call.end] != str(call.number):
            raise RuntimeError(
                f"Qushayri special extraction note call drifted: {page}:{call.number}"
            )
        value = value[:call.start] + value[call.end:]
    return value


if not os.path.isfile(PDF):
    raise RuntimeError(f"Missing required Qushayri source PDF: {PDF}")
source_sha = file_sha256(PDF)
if source_sha != EXPECTED_SOURCE_SHA256:
    raise RuntimeError(f"Qushayri source SHA-256 mismatch: {source_sha}")

doc = fitz.open(PDF)
semantics = build_qushayri_source_semantics(doc)
if semantics.verified_note_relation_count != EXPECTED_VERIFIED_NOTE_CALLS:
    raise RuntimeError("Qushayri verified note-call inventory changed")
if semantics.poetry_entry_count != EXPECTED_POETRY_INDEX_ENTRIES:
    raise RuntimeError("Qushayri Poetry Index inventory changed")
if semantics.poetry_occurrence_count != EXPECTED_POETRY_OCCURRENCES:
    raise RuntimeError("Qushayri Poetry Index occurrence inventory changed")
if semantics.poetry_line_count != EXPECTED_UNIQUE_POETRY_LINES:
    raise RuntimeError("Qushayri unique poetry-line inventory changed")

segments = []
current = None
source_soft_hyphen_count = 0
for pi in range(37, 506):
    page = doc[pi]
    pd = page.get_text("dict")
    page_lines = []
    for bi, block in enumerate(pd["blocks"]):
        if "lines" not in block:
            continue
        for li, line in enumerate(block["lines"]):
            text, fonts, sizes = line_info(line)
            source_soft_hyphen_count += text.count(SOFT_HYPHEN_MARKER)
            text = semantics.strip_verified_note_calls(pi + 1, bi, li, text)
            x0, y0, _, y1 = [float(v) for v in line["bbox"]]
            st = text.strip()
            if not st:
                continue
            if re.fullmatch(r"\d+", st):
                continue
            if header_re.match(st):
                continue
            if sura_heading_re.match(st):
                current = finish_current(current, segments)
                continue
            if is_arabic_line(fonts, st):
                continue
            if sizes and max(sizes) <= 9.4:
                continue
            page_lines.append((bi, li, st, fonts, sizes, x0, y0, y1))

    for bi, li, st, fonts, sizes, x0, y0, y1 in page_lines:
        m = anchor_re.match(st)
        if m and any("Italic" in f for f in fonts):
            current = finish_current(current, segments)
            s = int(m.group(1))
            a1 = int(m.group(2))
            a2 = int(m.group(3) or a1)
            rest = st[m.end():].strip()
            current = {
                "surah": s,
                "start": a1,
                "end": a2,
                "translation_parts": [],
                "body": [],
                "page": pi + 1,
            }
            if rest:
                current["translation_parts"].append(rest)
            continue
        if not current:
            continue
        if not current["body"] and starts_italic(fonts):
            current["translation_parts"].append(st)
        else:
            current["body"].append(
                {
                    "text": st,
                    "page": pi + 1,
                    "block": bi,
                    "line": li,
                    "x0": x0,
                    "y0": y0,
                    "y1": y1,
                    "poetry": semantics.poetry_line(pi + 1, bi, li),
                }
            )
current = finish_current(current, segments)
semantics.assert_complete_note_stripping()

# 2:68 is typographically exceptional: its reference sits mid-sentence.
if not any(s["surah"] == 2 and s["start"] <= 68 <= s["end"] for s in segments):
    p68 = doc[118]
    lines = []
    for bi, block in enumerate(p68.get_text("dict")["blocks"]):
        if "lines" not in block:
            continue
        for li, line in enumerate(block["lines"]):
            text, fonts, sizes = line_info(line)
            text = strip_verified_calls_without_count(semantics, 119, bi, li, text)
            st = text.strip()
            if not st or is_arabic_line(fonts, st):
                continue
            if sizes and max(sizes) <= 9.4:
                continue
            lines.append((st, fonts))
    start = next(i for i, (t, _) in enumerate(lines) if 'When He said: “She is a cow neither old' in t)
    end = next(i for i, (t, _) in enumerate(lines[start:], start) if 'yet retains some of the vigor of his youth.' in t)
    raw = restore_source_pua(" ".join(t for t, _ in lines[start : end + 1])).replace("\u00a0", " ")
    raw = resolve_discretionary_hyphens(raw)
    raw = re.sub(r"([A-Za-z])-\s+([a-z])", r"\1\2", raw)
    raw = re.sub(r"\s+", " ", raw).strip()
    raw = re.sub(r"\s+([,.;:!?])", r"\1", raw)
    m = re.search(r'When He said: “(?P<tr>.+?)” \[2:68\], it meant (?P<com>.+)$', raw)
    if not m:
        raise RuntimeError("Could not extract Qushayri 2:68 source exception")
    commentary = "It meant " + m.group("com").strip()
    segments.append(
        {
            "surah": 2,
            "start": 68,
            "end": 68,
            "translation": m.group("tr").strip(),
            "commentary": commentary,
            "commentary_runs": [("REGULAR", commentary)],
            "page": 119,
            "page_end": 119,
        }
    )

raw_rows = [s for s in segments if 1 <= s["surah"] <= 4 and s["translation"]]
raw_rows.sort(key=lambda s: (s["surah"], s["start"], s["page"]))
if len(raw_rows) != EXPECTED_RAW_SEGMENTS:
    raise RuntimeError(
        f"Qushayri raw approved segment count changed: {len(raw_rows)} != {EXPECTED_RAW_SEGMENTS}"
    )
rows = group_shared_commentaries(raw_rows)
if len(rows) != EXPECTED_LOGICAL_ENTRIES:
    raise RuntimeError(
        f"Qushayri logical entry count changed: {len(rows)} != {EXPECTED_LOGICAL_ENTRIES}"
    )

counts = collections.Counter()
for row in rows:
    key = (row["surah"], row["start"], row["end"])
    counts[key] += 1
    row["segment_no"] = counts[key]

poetry_run_count = 0
for r in rows:
    joined = r["translation"] + " " + r["commentary"]
    if not r["commentary"].strip():
        raise RuntimeError(
            f'Qushayri empty commentary survived grouping: {r["surah"]}:{r["start"]}-{r["end"]}'
        )
    if "".join(text for _, text in r["commentary_runs"]) != r["commentary"]:
        raise RuntimeError(
            f'Qushayri semantic run/text mismatch: {r["surah"]}:{r["start"]}-{r["end"]}'
        )
    poetry_run_count += sum(1 for style, _ in r["commentary_runs"] if style == "POETRY")
    if SOFT_HYPHEN_MARKER in joined or "\u00ad" in joined:
        raise RuntimeError(
            f'Qushayri unresolved discretionary hyphen: {r["surah"]}:{r["start"]}-{r["end"]}'
        )
    if "\u00a0" in joined:
        raise RuntimeError(
            f'Qushayri non-breaking-space extraction debris: {r["surah"]}:{r["start"]}-{r["end"]}'
        )
    if pua_re.search(joined):
        raise RuntimeError("Qushayri private-use PDF glyph leaked into corpus")
    if arabic_re.search(joined):
        raise RuntimeError(
            f'Qushayri Arabic source text leaked into row: {r["surah"]}:{r["start"]}-{r["end"]}'
        )
    if "| • Laṭāʾif al-ishārāt" in joined or sura_heading_re.search(r["commentary"]):
        raise RuntimeError("Qushayri page/sura heading leaked into verse commentary")

if os.path.exists(OUT):
    os.remove(OUT)
con = sqlite3.connect(OUT)
con.executescript(
    """
PRAGMA journal_mode=OFF;
PRAGMA synchronous=OFF;
CREATE TABLE source_metadata(key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE tafsir_entry(
 id INTEGER PRIMARY KEY,
 surah INTEGER NOT NULL,
 verse_start INTEGER NOT NULL,
 verse_end INTEGER NOT NULL,
 segment_no INTEGER NOT NULL,
 verse_translation TEXT NOT NULL,
 commentary TEXT NOT NULL,
 source_page INTEGER NOT NULL,
 source_page_end INTEGER NOT NULL,
 UNIQUE(surah,verse_start,verse_end,segment_no)
);
CREATE INDEX idx_tafsir_lookup ON tafsir_entry(surah,verse_start,verse_end,segment_no);
CREATE TABLE tafsir_run(
 entry_id INTEGER NOT NULL,
 run_no INTEGER NOT NULL,
 style TEXT NOT NULL CHECK(style IN ('REGULAR','POETRY')),
 text TEXT NOT NULL,
 PRIMARY KEY(entry_id, run_no),
 FOREIGN KEY(entry_id) REFERENCES tafsir_entry(id)
);
CREATE INDEX idx_tafsir_run_entry ON tafsir_run(entry_id, run_no);
"""
)
meta = {
    "schema_version": "2",
    "edition_id": "qushayri",
    "display_name": "Qushayri",
    "author": "Abu l-Qasim Abd al-Karim al-Qushayri",
    "work": "Lataif al-Isharat / Subtle Allusions",
    "translator": "Kristin Zahra Sands",
    "language": "English",
    "coverage": "Suras 1-4",
    "arabic_included": "false",
    "english_verse_translation_included": "true",
    "source_pdf_sha256": source_sha,
    "raw_segment_count": str(EXPECTED_RAW_SEGMENTS),
    "translation_only_anchor_count": str(EXPECTED_TRANSLATION_ONLY_ANCHORS),
    "grouped_source_range_count": str(EXPECTED_GROUPED_RANGES),
    "source_soft_hyphen_count": str(source_soft_hyphen_count),
    "soft_hyphen_policy": "preserve-marker-then-source-driven-join",
    "source_honorific_glyph_policy": "restore-edition-pua-to-source-abbreviations-no-name-inference",
    "entry_count": str(len(rows)),
    "verified_note_call_count": str(EXPECTED_VERIFIED_NOTE_CALLS),
    "note_call_display_policy": "remove-only-source-verified-unexposed-footnote-calls",
    "poetry_index_entry_count": str(EXPECTED_POETRY_INDEX_ENTRIES),
    "poetry_index_occurrence_count": str(EXPECTED_POETRY_OCCURRENCES),
    "poetry_unique_line_count": str(EXPECTED_UNIQUE_POETRY_LINES),
    "poetry_semantics": "source-poetry-index-v1",
    "paragraph_policy": "source-geometry-no-pdf-block-breaks",
    "semantic_run_table": "tafsir_run",
}
con.executemany("INSERT INTO source_metadata(key,value) VALUES (?,?)", meta.items())
for r in rows:
    cursor = con.execute(
        "INSERT INTO tafsir_entry(surah,verse_start,verse_end,segment_no,verse_translation,commentary,source_page,source_page_end) "
        "VALUES (?,?,?,?,?,?,?,?)",
        (
            r["surah"], r["start"], r["end"], r["segment_no"],
            r["translation"], r["commentary"], r["page"], r.get("page_end", r["page"]),
        ),
    )
    entry_id = cursor.lastrowid
    con.executemany(
        "INSERT INTO tafsir_run(entry_id,run_no,style,text) VALUES (?,?,?,?)",
        [(entry_id, index, style, text) for index, (style, text) in enumerate(r["commentary_runs"])],
    )
con.commit()
con.execute("VACUUM")
con.close()

cov = collections.defaultdict(set)
for r in rows:
    for a in range(r["start"], r["end"] + 1):
        cov[r["surah"]].add(a)
expected = {1: 7, 2: 286, 3: 200, 4: 176}
print("raw segments", len(raw_rows))
print("logical entries", len(rows))
print("translation-only anchors merged", EXPECTED_TRANSLATION_ONLY_ANCHORS)
print("grouped source ranges", EXPECTED_GROUPED_RANGES)
print("source discretionary hyphens observed", source_soft_hyphen_count)
print("verified source note calls removed", semantics.stripped_note_calls)
print("Poetry Index entries", semantics.poetry_entry_count)
print("Poetry Index occurrences", semantics.poetry_occurrence_count)
print("unique poetry source lines", semantics.poetry_line_count)
print("semantic poetry runs", poetry_run_count)
for s, n in expected.items():
    missing = [a for a in range(1, n + 1) if a not in cov[s]]
    if missing:
        raise RuntimeError(f"Qushayri coverage gap in sura {s}: {missing[:25]}")
    print("sura", s, "covered", len(cov[s]), "missing", len(missing), missing[:25])
print("repeated exact logical ranges", sum(1 for v in counts.values() if v > 1), "max segments", max(counts.values()))
print("db bytes", os.path.getsize(OUT), "sha256", hashlib.sha256(open(OUT, "rb").read()).hexdigest())
con = sqlite3.connect(OUT)
for s, a in [(2, 10), (2, 11), (2, 36), (2, 68), (3, 55), (4, 167), (4, 176)]:
    rs = con.execute(
        "SELECT id,verse_start,verse_end,segment_no,substr(verse_translation,1,240),"
        "substr(commentary,1,420),source_page,source_page_end "
        "FROM tafsir_entry WHERE surah=? AND verse_start<=? AND verse_end>=? ORDER BY id",
        (s, a, a),
    ).fetchall()
    print("\n", s, a, "rows", len(rs))
    for x in rs[:3]:
        print(x)
        print("runs", con.execute("SELECT run_no,style,substr(text,1,260) FROM tafsir_run WHERE entry_id=? ORDER BY run_no", (x[0],)).fetchall())
con.close()
