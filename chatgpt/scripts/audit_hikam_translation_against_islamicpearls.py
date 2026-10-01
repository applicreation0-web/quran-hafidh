#!/usr/bin/env python3
"""Cross-audit the private French Hikam translation against Islamic Pearls.

The English translation is a secondary control only: Arabic remains authoritative.
This script deliberately stores no copyrighted English corpus in the repository.
It downloads the reference during the audit, aligns all 264 rows by Arabic and emits
only metrics + numbered risk flags.
"""
import json
import re
import sys
import unicodedata
import urllib.request
from difflib import SequenceMatcher
from pathlib import Path

try:
    from bs4 import BeautifulSoup
except ImportError as exc:
    raise SystemExit("beautifulsoup4 is required for this audit") from exc

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
URL = "https://islamicpearls.net/ibnattaillah2.html"

ARABIC_DIACRITICS = re.compile(r"[\u0610-\u061a\u064b-\u065f\u0670\u06d6-\u06ed]")
ARABIC_PUNCT = re.compile(r"[^\u0621-\u064a0-9]+")


def norm_arabic(text: str) -> str:
    text = ARABIC_DIACRITICS.sub("", text)
    text = text.translate(str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ى": "ي", "ة": "ه", "ؤ": "و", "ئ": "ي"}))
    return ARABIC_PUNCT.sub("", text)


def compact(text: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFKC", text)).strip()


def fetch_reference():
    req = urllib.request.Request(URL, headers={"User-Agent": "Quran-Safeguard-translation-audit/0.10.3"})
    with urllib.request.urlopen(req, timeout=30) as response:
        html = response.read().decode("utf-8", errors="replace")
    soup = BeautifulSoup(html, "html.parser")
    rows = {}
    for tr in soup.find_all("tr"):
        cells = [compact(td.get_text(" ", strip=True)) for td in tr.find_all(["td", "th"], recursive=False)]
        if not cells:
            continue
        match = re.fullmatch(r"\s*(\d{1,3})\s*", cells[0])
        if not match:
            continue
        number = int(match.group(1))
        if not (1 <= number <= 264) or len(cells) < 3:
            continue
        # The published table starts: number | English Hikma | Arabic Hikma | ...
        english = cells[1]
        arabic = cells[2]
        if english and arabic:
            rows[number] = {"english": english, "arabic": arabic}
    return rows


# High-signal concepts only. Missing a synonym is a review flag, never an automatic failure.
CONCEPTS = {
    "hope": ("hope", {"espérance", "espoir"}),
    "deeds": ("deed", {"œuvre", "oeuvre", "acte", "action"}),
    "desire": ("desire", {"désir", "vouloir", "volonté"}),
    "destiny": ("destin", {"destin", "décret", "décrets", "prédestination"}),
    "knowledge": ("know", {"connaître", "connaissance", "savoir"}),
    "light": ("light", {"lumière"}),
    "heart": ("heart", {"cœur", "coeur"}),
    "soul": ("soul", {"âme"}),
    "world": ("world", {"monde"}),
    "truth": ("truth", {"vérité", "vrai"}),
    "mercy": ("mercy", {"miséricorde"}),
    "grace": ("grace", {"grâce", "faveur"}),
}


def has_french_negation(text: str) -> bool:
    low = text.lower()
    return any(token in low for token in [" ne ", " n’", " n'", "pas", "jamais", "aucun", "nulle", "manque", "absence", "sans "])


def has_english_negation(text: str) -> bool:
    low = " " + text.lower() + " "
    return any(token in low for token in [" not ", " no ", " never ", " lack ", " without ", " cannot ", " do not ", " does not "])


def main():
    entries = json.loads(CORPUS.read_text(encoding="utf-8"))
    by_number = {int(item["source_number"]): item for item in entries}
    assert set(by_number) == set(range(1, 265)), "Local Hikam must cover 1..264"

    reference = fetch_reference()
    if set(reference) != set(range(1, 265)):
        missing = sorted(set(range(1, 265)) - set(reference))
        raise SystemExit(f"Islamic Pearls parser did not recover 264 rows; got {len(reference)}, missing={missing[:20]}")

    arabic_scores = []
    risks = []
    for number in range(1, 265):
        item = by_number[number]
        ref = reference[number]
        local_ar = norm_arabic(item["arabic"])
        ref_ar = norm_arabic(ref["arabic"])
        score = SequenceMatcher(None, local_ar, ref_ar).ratio()
        arabic_scores.append(score)
        if score < 0.78:
            risks.append((number, "ARABIC_ALIGNMENT", round(score, 3)))

        english = ref["english"]
        french = compact(item["french"])
        en_words = max(1, len(re.findall(r"[A-Za-z]+", english)))
        fr_words = max(1, len(re.findall(r"[A-Za-zÀ-ÿŒœ]+", french)))
        ratio = fr_words / en_words
        if ratio < 0.45 or ratio > 2.35:
            risks.append((number, "LENGTH_RATIO", round(ratio, 2)))

        if has_english_negation(english) != has_french_negation(" " + french + " "):
            risks.append((number, "NEGATION_REVIEW", None))

        en_low = english.lower()
        fr_low = french.lower()
        for label, (needle, french_terms) in CONCEPTS.items():
            if needle in en_low and not any(term in fr_low for term in french_terms):
                risks.append((number, f"CONCEPT_{label.upper()}", None))

        if re.search(r"\b(the|and|your|you|lord|desire|deeds|hope)\b", french, re.I):
            risks.append((number, "POSSIBLE_UNTRANSLATED_ENGLISH", None))

    hard = [r for r in risks if r[1] == "ARABIC_ALIGNMENT"]
    print(json.dumps({
        "reference": URL,
        "local_count": len(entries),
        "english_reference_count": len(reference),
        "arabic_alignment_min": round(min(arabic_scores), 4),
        "arabic_alignment_mean": round(sum(arabic_scores) / len(arabic_scores), 4),
        "hard_alignment_failures": hard,
        "semantic_review_flag_count": len(risks) - len(hard),
        "semantic_review_flags": risks[:500]
    }, ensure_ascii=False, indent=2))

    if hard:
        raise SystemExit("Arabic numbering/alignment mismatch against Islamic Pearls")


if __name__ == "__main__":
    main()
