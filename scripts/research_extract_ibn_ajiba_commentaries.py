#!/usr/bin/env python3
"""
Research-only extractor for Ibn ʿAjība's Iqāẓ al-Himam commentary.

The extractor downloads the public reading pages of the retained Arabic edition,
matches the numbered Hikam against the frozen 1..264 Safeguard matn, and produces
candidate commentary spans. It does NOT make candidates display-eligible and it
never rewrites or synthesizes the commentator's words.

Output is a research artifact only. Promotion into the app requires:
- exact Hikma match;
- identified commentator/work/edition/locator;
- clean Arabic excerpt or full passage from the commentator;
- internal French translation of the same passage;
- source-isolation review;
- no cross-commentator synthesis.
"""

from __future__ import annotations

import concurrent.futures
import difflib
import html
import json
import re
import sys
import time
import unicodedata
import urllib.error
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
PAGES = range(1, 633)

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
    s = (
        s.replace("أ", "ا")
        .replace("إ", "ا")
        .replace("آ", "ا")
        .replace("ى", "ي")
        .replace("ؤ", "و")
        .replace("ئ", "ي")
        .replace("ة", "ه")
    )
    s = NON_ARABIC_WORD.sub(" ", s)
    return " ".join(s.split())


def compact_lines(raw: str, page: int) -> str:
    parser = MainTextParser()
    parser.feed(raw)
    text = html.unescape(parser.text())
    lines = []
    for line in text.splitlines():
        line = " ".join(line.split())
        if not line:
            continue
        # Page chrome / metadata from both old and new ABLibrary templates.
        if line in {
            "الرئيسية", "مطالعة الكتاب", "تفاصيل إضافية", "مساهمون", "الموضوعات",
            "الناشر", "اللغة", "رقم الكتاب", "صفحات الكتاب", "الصفحة السابقة",
            "الصفحة التالية", "تخطي إلى المحتوى الرئيسي"
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
        if line == "هامش":
            # Everything after this marker on a page is editorial footnote /
            # navigation material, not Ibn ʿAjība's running commentary.
            break
        lines.append(line)
    return "\n".join(lines)


def fetch_page(page: int) -> tuple[int, str, str | None]:
    url = BASE.format(page=page)
    headers = {
        "User-Agent": "QuranSafeguardResearch/0.9.1 (+noncommercial source audit)"
    }
    last = None
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=25) as response:
                raw = response.read().decode("utf-8", errors="replace")
            return page, compact_lines(raw, page), None
        except Exception as exc:
            last = f"{type(exc).__name__}: {exc}"
            time.sleep(0.4 * (attempt + 1))
    return page, "", last


def heading_candidates(page_text: str, page: int, matn_by_num: dict[int, str]):
    results = []
    # Headings are printed as "N - <matn>" and normally followed by "قلت".
    pattern = re.compile(r"(?m)(?:^|\n)\s*(\d{1,3})\s*[-–—]\s*(.+?)(?=\n|$)")
    for m in pattern.finditer(page_text):
        number = int(m.group(1))
        if number not in matn_by_num:
            continue
        line = m.group(2).strip()
        expected = norm_ar(matn_by_num[number])
        observed = norm_ar(line)
        if not expected or not observed:
            continue
        # Heading may include the first commentary words, so compare only a
        # prefix roughly the size of the frozen matn.
        observed_prefix = observed[: max(len(expected) + 60, len(expected))]
        score = difflib.SequenceMatcher(
            None, expected, observed_prefix[: len(expected) + 25]
        ).ratio()
        contains = expected in observed or observed.startswith(expected[: min(35, len(expected))])
        if score >= 0.62 or contains:
            results.append(
                {
                    "number": number,
                    "page": page,
                    "start": m.start(),
                    "end": m.end(),
                    "heading": line,
                    "score": round(score, 4),
                }
            )
    return results


def page_from_offset(markers, offset: int) -> int:
    page = 1
    for pos, candidate in markers:
        if pos > offset:
            break
        page = candidate
    return page


def first_clean_excerpt(text: str, max_chars: int = 950) -> str:
    text = re.sub(r"\[\[PAGE \d+\]\]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    text = re.sub(r"^قلت\s*[:：]?\s*", "", text)
    if len(text) <= max_chars:
        return text
    cut = text[:max_chars]
    # Prefer a sentence boundary, otherwise a word boundary.
    boundary = max(cut.rfind(" ."), cut.rfind(" ؛"), cut.rfind(" ؟"))
    if boundary > max_chars * 0.55:
        cut = cut[: boundary + 2]
    else:
        cut = cut.rsplit(" ", 1)[0]
    return cut.strip() + " […]"


def main():
    corpus = json.loads(CORPUS.read_text(encoding="utf-8"))
    matn_by_num = {int(x["source_number"]): x["arabic"] for x in corpus}

    page_texts: dict[int, str] = {}
    failures = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        futures = [pool.submit(fetch_page, page) for page in PAGES]
        for future in concurrent.futures.as_completed(futures):
            page, text, error = future.result()
            if error:
                failures[page] = error
            else:
                page_texts[page] = text

    if len(page_texts) < 600:
        raise SystemExit(f"Too many source-page failures: {len(failures)}")

    headings = []
    for page in sorted(page_texts):
        headings.extend(heading_candidates(page_texts[page], page, matn_by_num))

    # Keep best match per source number, favoring score then earliest page.
    best = {}
    for h in headings:
        n = h["number"]
        current = best.get(n)
        if current is None or (h["score"], -h["page"]) > (current["score"], -current["page"]):
            best[n] = h

    # Concatenate page text with explicit page markers so complete spans can be
    # cut between validated numbered headings.
    chunks = []
    markers = []
    offset = 0
    for page in sorted(page_texts):
        marker = f"\n[[PAGE {page}]]\n"
        chunks.append(marker)
        offset += len(marker)
        markers.append((offset, page))
        body = page_texts[page]
        chunks.append(body)
        offset += len(body)
    book = "".join(chunks)

    # Re-find accepted headings in the concatenated book using their literal
    # heading line to obtain stable offsets.
    accepted = []
    search_from = 0
    for number in sorted(best):
        h = best[number]
        literal = f"{number} - {h['heading']}"
        pos = book.find(literal, search_from)
        if pos < 0:
            # Handle alternate dash or whitespace by searching the heading body.
            pos = book.find(h["heading"], search_from)
        if pos < 0:
            continue
        accepted.append((number, pos, h))
        search_from = pos + 1

    candidates = []
    for idx, (number, pos, h) in enumerate(accepted):
        end = accepted[idx + 1][1] if idx + 1 < len(accepted) else len(book)
        segment = book[pos:end]
        # Commentary normally starts with Ibn ʿAjība's "قلت".
        q = re.search(r"قلت\s*[:：]?", segment)
        if not q:
            arabic = ""
            reason = "missing_qilt_commentary_marker"
        else:
            arabic = segment[q.start():].strip()
            reason = None
        start_page = page_from_offset(markers, pos)
        end_page = page_from_offset(markers, max(pos, end - 1))
        candidates.append(
            {
                "hikma_number": number,
                "commentator": COMMENTATOR,
                "work_title": BOOK_TITLE,
                "edition": EDITION,
                "source_pages": [start_page, end_page],
                "source_url": BASE.format(page=start_page),
                "heading_match_score": h["score"],
                "arabic_full_candidate": arabic,
                "arabic_excerpt_candidate": first_clean_excerpt(arabic) if arabic else "",
                "status": "candidate_needs_human_boundary_and_translation_review",
                "failure_reason": reason,
                "source_isolation": True,
                "ai_synthesis": False,
            }
        )

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

    report = {
        "pages_fetched": len(page_texts),
        "page_failures": failures,
        "raw_heading_candidates": len(headings),
        "best_number_matches": len(best),
        "commentary_candidates_with_arabic": len(found),
        "missing_numbers": missing,
        "duplicate_heading_numbers": sorted(
            n for n in set(x["number"] for x in headings)
            if sum(1 for x in headings if x["number"] == n) > 1
        ),
        "promotion_policy": {
            "requires_internal_french_translation": True,
            "requires_boundary_review": True,
            "requires_source_metadata": True,
            "cross_commentator_synthesis_forbidden": True,
        },
    }
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False))

    # Research run is successful even with some misses; the report drives
    # targeted secondary-source searches. Fail only if extraction is unusable.
    if len(found) < 220:
        raise SystemExit(f"Extractor coverage too low: {len(found)}/264")


if __name__ == "__main__":
    main()
