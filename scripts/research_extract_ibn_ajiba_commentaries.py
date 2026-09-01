#!/usr/bin/env python3
"""
Research-only extractor for Ibn ʿAjība's Iqāẓ al-Himam commentary.

It downloads the public text pages of the retained Arabic edition and aligns
them monotonically to Quran Safeguard's frozen Hikam numbering 1..264.

Nothing produced here is display-eligible. Promotion requires an independently
checked boundary, internal French translation of the same Arabic passage,
complete source metadata, and strict isolation from every other commentator.
"""

from __future__ import annotations

import concurrent.futures
import difflib
import html
import json
import re
import time
import unicodedata
import urllib.request
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
OUTDIR = ROOT / "build/research/hikam_commentary"
OUTDIR.mkdir(parents=True, exist_ok=True)
OUT = OUTDIR / "ibn_ajiba_candidates.json"
REPORT = OUTDIR / "ibn_ajiba_report.json"

BOOK_ID = 9684
BOOK_TITLE = "Īqāẓ al-Himam fī Sharḥ al-Ḥikam"
COMMENTATOR = "Ibn ʿAjība"
EDITION = (
    "Ibn ʿAjība, Īqāẓ al-Himam fī Sharḥ al-Ḥikam, "
    "taḥqīq/taṣḥīḥ Muḥammad ʿAbd al-Qādir Naṣṣār, "
    "Dār Jawāmiʿ al-Kalim, Cairo, 632 p."
)
BASE = f"https://ablibrary.net/book_content/b/{BOOK_ID}/{{page}}"
PAGES = range(20, 633)
BLOCK_TAGS = {
    "p", "div", "section", "article", "main", "h1", "h2", "h3", "h4",
    "li", "br", "tr", "td"
}


class MainTextParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.main_depth = 0
        self.parts: list[str] = []

    def handle_starttag(self, tag, attrs):
        if tag == "main":
            self.main_depth += 1
        if self.main_depth and tag in BLOCK_TAGS:
            self.parts.append("\n")

    def handle_endtag(self, tag):
        if self.main_depth and tag in BLOCK_TAGS:
            self.parts.append("\n")
        if tag == "main" and self.main_depth:
            self.main_depth -= 1

    def handle_data(self, data):
        if self.main_depth:
            self.parts.append(data)

    def text(self) -> str:
        return "".join(self.parts)


ARABIC_MARKS = re.compile(
    "[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]"
)
NON_ARABIC_WORD = re.compile(r"[^\u0621-\u063A\u0641-\u064A0-9]+")


def norm_ar(s: str) -> str:
    s = unicodedata.normalize("NFKC", s)
    s = ARABIC_MARKS.sub("", s)
    for a, b in (
        ("أ", "ا"), ("إ", "ا"), ("آ", "ا"), ("ى", "ي"),
        ("ؤ", "و"), ("ئ", "ي"), ("ة", "ه"),
    ):
        s = s.replace(a, b)
    s = NON_ARABIC_WORD.sub(" ", s)
    return " ".join(s.split())


def clean_page(raw: str, page: int) -> str:
    parser = MainTextParser()
    parser.feed(raw)
    text = html.unescape(parser.text())
    lines = []
    stop = False
    for line in text.splitlines():
        line = " ".join(line.split())
        if not line:
            continue
        if line == "هامش":
            stop = True
        if stop:
            continue
        if line in {
            "الرئيسية", "مطالعة الكتاب", "تفاصيل إضافية", "مساهمون",
            "الموضوعات", "الناشر", "اللغة", "رقم الكتاب", "صفحات الكتاب",
            "الصفحة السابقة", "الصفحة التالية", "تخطي إلى المحتوى الرئيسي",
        }:
            continue
        if line.startswith("إيقاظ الهمم في شرح حكم سيدي أحمد بن عطاء الله السكندري — صفحة"):
            continue
        if line == "إيقاظ الهمم في شرح حكم سيدي أحمد بن عطاء الله السكندري":
            continue
        if line == "أحمد بن محمد بن عجيبة الحسنى":
            continue
        if line.startswith("هوية الكتاب"):
            continue
        if line.startswith("تحقيق وتصحيح محمد عبد القادر نصار"):
            continue
        if line.startswith("مؤلف أحمد بن محمد بن عجيبة الحسنى"):
            continue
        if line.startswith("دار جوامع الكلم"):
            continue
        if line in {str(page), f"ص {page}", f"ص {page:02d}"}:
            continue
        lines.append(line)
    return "\n".join(lines)


def fetch_page(page: int) -> tuple[int, str, str | None]:
    req = urllib.request.Request(
        BASE.format(page=page),
        headers={"User-Agent": "QuranSafeguardResearch/0.9.1 (+source audit)"},
    )
    last = None
    for attempt in range(2):
        try:
            with urllib.request.urlopen(req, timeout=8) as response:
                raw = response.read().decode("utf-8", errors="replace")
            return page, clean_page(raw, page), None
        except Exception as exc:
            last = f"{type(exc).__name__}: {exc}"
            time.sleep(0.15 * (attempt + 1))
    return page, "", last


def explicit_candidates(page_texts, matn_by_num):
    by_number = {n: [] for n in matn_by_num}
    pattern = re.compile(r"^\s*(\d{1,3})\s*[-–—]\s*(.+)$")
    for page in sorted(page_texts):
        for line_index, line in enumerate(page_texts[page].splitlines()):
            m = pattern.match(line)
            if not m:
                continue
            n = int(m.group(1))
            if n not in matn_by_num:
                continue
            expected = norm_ar(matn_by_num[n])
            observed = norm_ar(m.group(2))
            if not expected or not observed:
                continue
            window = observed[: len(expected) + 60]
            score = difflib.SequenceMatcher(None, expected, window[: len(expected) + 30]).ratio()
            prefix_len = min(38, len(expected))
            prefix_hit = prefix_len >= 12 and expected[:prefix_len] in observed
            if score >= 0.43 or prefix_hit:
                by_number[n].append({
                    "number": n,
                    "page": page,
                    "line_index": line_index,
                    "line": line,
                    "score": round(score, 4),
                    "kind": "numbered_heading",
                })
    return by_number


def fallback_candidate(n, expected_raw, page_texts, min_page, max_page):
    expected = norm_ar(expected_raw)
    if not expected:
        return None
    prefix = expected[: min(55, len(expected))]
    best = None
    for page in range(min_page, min(max_page, 632) + 1):
        body = page_texts.get(page, "")
        for line_index, line in enumerate(body.splitlines()):
            observed = norm_ar(line)
            if len(observed) < 10:
                continue
            direct = prefix and prefix in observed
            if not direct:
                # Only spend fuzzy matching on lines sharing a meaningful start token.
                first = expected.split()[0] if expected.split() else ""
                if first and first not in observed:
                    continue
            window = observed[: len(expected) + 80]
            score = difflib.SequenceMatcher(
                None, expected, window[: len(expected) + 35]
            ).ratio()
            if direct:
                score = max(score, 0.90)
            if score < 0.56:
                continue
            candidate = {
                "number": n,
                "page": page,
                "line_index": line_index,
                "line": line,
                "score": round(score, 4),
                "kind": "matn_fallback",
            }
            if best is None or (candidate["score"], -page) > (best["score"], -best["page"]):
                best = candidate
        # A strong direct hit near the current position is preferable to a later citation.
        if best and best["score"] >= 0.90:
            return best
    return best


def choose_monotone(matn_by_num, page_texts, by_number):
    chosen = {}
    previous_page = 20
    for n in range(1, 265):
        options = [
            c for c in by_number.get(n, [])
            if c["page"] >= previous_page
        ]
        # Avoid later appendix/citation duplicates: choose the earliest plausible
        # occurrence after the preceding Hikma, then best score on that page.
        if options:
            earliest_page = min(c["page"] for c in options)
            same_page = [c for c in options if c["page"] == earliest_page]
            candidate = max(same_page, key=lambda c: c["score"])
        else:
            candidate = None

        # If the explicit heading looks weak or jumps implausibly far, directly
        # search the frozen matn in the next 35 pages.
        jump = candidate["page"] - previous_page if candidate else 999
        if candidate is None or candidate["score"] < 0.60 or jump > 35:
            fallback = fallback_candidate(
                n,
                matn_by_num[n],
                page_texts,
                previous_page,
                min(previous_page + 35, 632),
            )
            if fallback is not None:
                if candidate is None or fallback["page"] <= candidate["page"] or fallback["score"] > candidate["score"]:
                    candidate = fallback

        if candidate is not None:
            chosen[n] = candidate
            previous_page = candidate["page"]
    return chosen


def absolute_offsets(page_texts):
    book_parts = []
    page_offsets = {}
    offset = 0
    for page in sorted(page_texts):
        marker = f"\n[[PAGE {page}]]\n"
        book_parts.append(marker)
        offset += len(marker)
        page_offsets[page] = offset
        body = page_texts[page]
        book_parts.append(body)
        offset += len(body)
    return "".join(book_parts), page_offsets


def locate_candidate(candidate, page_texts, page_offsets, after_absolute=-1):
    page = candidate["page"]
    body = page_texts[page]
    lines = body.splitlines()
    line = candidate["line"]
    line_index = candidate["line_index"]

    # Use line-index reconstruction rather than a global text search so repeated
    # matn quotations on the same page cannot move the boundary.
    relative = 0
    for idx, value in enumerate(lines):
        if idx == line_index:
            break
        relative += len(value) + 1

    absolute = page_offsets[page] + relative
    if absolute <= after_absolute:
        # Same-page fallback: search the literal line after the previous boundary.
        pos = body.find(line, max(0, after_absolute - page_offsets[page] + 1))
        if pos >= 0:
            absolute = page_offsets[page] + pos
    return absolute, absolute + len(line)


def clean_segment(text: str) -> str:
    text = re.sub(r"\[\[PAGE \d+\]\]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def excerpt(text: str, max_chars: int = 850) -> str:
    text = clean_segment(text)
    text = re.sub(r"^قلت\s*[:：]?\s*", "", text)
    if len(text) <= max_chars:
        return text
    cut = text[:max_chars]
    boundary = max(cut.rfind(" ."), cut.rfind(" ؛"), cut.rfind(" ؟"))
    if boundary > int(max_chars * 0.55):
        cut = cut[: boundary + 2]
    else:
        cut = cut.rsplit(" ", 1)[0]
    return cut.strip() + " […]"


def main():
    corpus = json.loads(CORPUS.read_text(encoding="utf-8"))
    matn_by_num = {int(x["source_number"]): x["arabic"] for x in corpus}

    page_texts = {}
    failures = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=24) as pool:
        futures = [pool.submit(fetch_page, page) for page in PAGES]
        for future in concurrent.futures.as_completed(futures):
            page, text, error = future.result()
            if error:
                failures[page] = error
            else:
                page_texts[page] = text

    if len(page_texts) < 500:
        raise SystemExit(f"Too many source-page failures: {len(failures)}")

    raw = explicit_candidates(page_texts, matn_by_num)
    chosen = choose_monotone(matn_by_num, page_texts, raw)
    book, page_offsets = absolute_offsets(page_texts)

    located = []
    previous_offset = -1
    for n in range(1, 265):
        c = chosen.get(n)
        if c is None:
            continue
        start, heading_end = locate_candidate(c, page_texts, page_offsets, previous_offset)
        if start <= previous_offset:
            continue
        located.append((n, start, heading_end, c))
        previous_offset = start

    candidates = []
    for idx, (n, start, heading_end, c) in enumerate(located):
        next_start = located[idx + 1][1] if idx + 1 < len(located) else len(book)
        segment = book[heading_end:next_start].strip()

        # Prefer Ibn ʿAjība's standard "قلت" opening when it appears near the
        # boundary, but do not discard a valid commentary merely because this
        # edition omits/reflows the marker.
        q = re.search(r"قلت\s*[:：]?", segment[:500])
        if q:
            commentary = segment[q.start():]
            start_mode = "qilt_marker"
        else:
            commentary = segment
            start_mode = "after_matn_heading"

        full = clean_segment(commentary)
        short = excerpt(commentary)
        end_page = located[idx + 1][3]["page"] if idx + 1 < len(located) else 632
        candidates.append({
            "hikma_number": n,
            "commentator": COMMENTATOR,
            "work_title": BOOK_TITLE,
            "edition": EDITION,
            "source_pages": [c["page"], end_page],
            "source_url": BASE.format(page=c["page"]),
            "heading_match_score": c["score"],
            "heading_match_kind": c["kind"],
            "commentary_start_mode": start_mode,
            "arabic_full_candidate": full,
            "arabic_excerpt_candidate": short,
            "status": "candidate_needs_boundary_and_internal_translation_review",
            "source_isolation": True,
            "ai_synthesis": False,
        })

    found = {x["hikma_number"] for x in candidates if x["arabic_excerpt_candidate"]}
    missing = [n for n in range(1, 265) if n not in found]

    payload = {
        "collection": "al_hikam_al_ataiyya_commentary_candidates",
        "commentator": COMMENTATOR,
        "work_title": BOOK_TITLE,
        "edition": EDITION,
        "source_book_id": BOOK_ID,
        "source_page_count": 632,
        "entries": candidates,
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    explicit_count = sum(len(v) for v in raw.values())
    report = {
        "pages_fetched": len(page_texts),
        "page_failures": failures,
        "explicit_heading_candidates": explicit_count,
        "monotone_matches": len(chosen),
        "commentary_candidates_with_arabic": len(found),
        "missing_numbers": missing,
        "fallback_matches": sum(1 for c in chosen.values() if c["kind"] == "matn_fallback"),
        "promotion_policy": {
            "requires_internal_french_translation": True,
            "requires_boundary_review": True,
            "requires_source_metadata": True,
            "cross_commentator_synthesis_forbidden": True,
        },
    }
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))

    if len(found) < 250:
        raise SystemExit(f"Extractor coverage too low: {len(found)}/264")


if __name__ == "__main__":
    main()
