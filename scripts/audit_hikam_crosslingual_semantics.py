#!/usr/bin/env python3
"""Rank English/French Hikam pairs by multilingual semantic similarity.

This is a contradiction aid, not an authority: low scores are manually reviewed
against Arabic before any French text is changed.
"""
import json
import re
import unicodedata
import urllib.request
from pathlib import Path

from bs4 import BeautifulSoup
from sentence_transformers import SentenceTransformer

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
URL = "https://islamicpearls.net/ibnattaillah2.html"
MODEL = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"


def compact(text: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFKC", text)).strip()


def fetch_english():
    request = urllib.request.Request(URL, headers={"User-Agent": "Quran-Safeguard-semantic-audit/0.10.3"})
    with urllib.request.urlopen(request, timeout=30) as response:
        soup = BeautifulSoup(response.read(), "html.parser")
    rows = {}
    for tr in soup.find_all("tr"):
        cells = [compact(td.get_text(" ", strip=True)) for td in tr.find_all(["td", "th"], recursive=False)]
        if len(cells) < 3 or not re.fullmatch(r"\d{1,3}", cells[0]):
            continue
        number = int(cells[0])
        if 1 <= number <= 264:
            rows[number] = cells[1]
    if set(rows) != set(range(1, 265)):
        raise SystemExit(f"Expected 264 English rows, found {len(rows)}")
    return rows


def main():
    entries = json.loads(CORPUS.read_text(encoding="utf-8"))
    local = {int(item["source_number"]): compact(item["french"]) for item in entries}
    english = fetch_english()

    model = SentenceTransformer(MODEL)
    english_texts = [english[n] for n in range(1, 265)]
    french_texts = [local[n] for n in range(1, 265)]
    en = model.encode(english_texts, normalize_embeddings=True, show_progress_bar=False)
    fr = model.encode(french_texts, normalize_embeddings=True, show_progress_bar=False)
    scores = (en * fr).sum(axis=1)
    ranked = sorted(((i + 1, float(score)) for i, score in enumerate(scores)), key=lambda pair: pair[1])

    print(json.dumps({
        "model": MODEL,
        "count": len(ranked),
        "mean_similarity": round(sum(score for _, score in ranked) / len(ranked), 4),
        "minimum_similarity": round(ranked[0][1], 4),
        "below_0_65": [[n, round(s, 4)] for n, s in ranked if s < 0.65],
        "lowest_40": [[n, round(s, 4)] for n, s in ranked[:40]]
    }, indent=2))


if __name__ == "__main__":
    main()
