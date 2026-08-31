#!/usr/bin/env python3
import argparse
import concurrent.futures
import hashlib
import json
import re
import time
import unicodedata
import urllib.parse
import urllib.request
from collections import defaultdict
from pathlib import Path

BASE = "https://hadeethenc.com/api/v1"
TRANSLATION_VERSION = "fr-v1.17.0"
SOURCE_PROVIDER = "HadeethEnc.com"
FETCHED_AT = "2026-08-31"

THEME_RULES = {
    "coran": ["coran", "qur", "récitation", "recitation", "sourate", "verset"],
    "famille": ["famille", "parent", "père", "pere", "mère", "mere", "époux", "epoux", "épouse", "epouse", "enfant"],
    "voisinage": ["voisin", "voisinage"],
    "propreté": ["propreté", "proprete", "purification", "ablution", "siwak", "hygiène", "hygiene"],
    "douceur": ["douceur", "doux", "bienveillance", "clémence", "clemence"],
    "patience": ["patience", "patient", "endurance"],
    "maîtrise de soi": ["colère", "colere", "maîtrise", "maitrise", "pardon", "humilité", "humilite"],
    "sincérité": ["sincérité", "sincerite", "intention", "ostentation"],
    "gratitude": ["gratitude", "remerci", "reconnaissan"],
    "générosité": ["aumône", "aumone", "généros", "generos", "dépense", "depense", "charité", "charite"],
    "entraide": ["entraide", "aide", "secours", "besoin", "frère", "frere"],
    "communauté": ["compagnon", "gens", "musulman", "communauté", "communaute", "réconcil", "reconcil"],
    "discipline": ["temps", "habitude", "assidu", "régular", "regular", "effort", "constan", "science", "savoir"],
    "bonnes mœurs": ["comportement", "caractère", "caractere", "vertu", "convenance", "politesse", "sourire", "parole"],
}

CATEGORY_HINTS = [
    "coran", "qur", "vertu", "mérite", "merite", "convenance", "caractère", "caractere",
    "parent", "famille", "voisin", "purification", "ablution", "propreté", "proprete",
    "rappel", "invocation", "aumône", "aumone", "patience", "douceur", "colère", "colere",
    "liens", "compagn", "science", "savoir", "fratern", "entraide", "comportement"
]

MIN_THEME_COUNTS = {
    "coran": 5,
    "famille": 5,
    "voisinage": 2,
    "propreté": 4,
    "douceur": 4,
    "patience": 3,
    "maîtrise de soi": 4,
    "sincérité": 4,
    "gratitude": 3,
    "générosité": 3,
    "entraide": 3,
    "communauté": 4,
    "discipline": 4,
    "bonnes mœurs": 5,
}

def get_json(path, params=None, retries=5):
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    last = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(
                url,
                headers={"User-Agent": "QuranSafeguardReminderBuilder/0.8"}
            )
            with urllib.request.urlopen(req, timeout=45) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception as exc:
            last = exc
            time.sleep(0.75 + attempt)
    raise RuntimeError(f"Failed API request {url}: {last}")

def norm(value):
    value = unicodedata.normalize("NFKD", value or "")
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    return value.lower()

def compact_spaces(value):
    return re.sub(r"\s+", " ", (value or "")).strip()

def theme_for(fr, category_titles):
    haystack = norm(" ".join([
        " ".join(category_titles),
        fr.get("title") or "",
        fr.get("hadeeth") or "",
        " ".join(fr.get("categories") or []),
    ]))
    for theme, words in THEME_RULES.items():
        if any(norm(word) in haystack for word in words):
            return theme
    return None

def grade_kind(fr, ar):
    grade_ar = compact_spaces(
        ar.get("grade") or ar.get("grade_ar") or fr.get("grade_ar")
    )
    grade_fr = compact_spaces(fr.get("grade"))
    combined = norm(grade_fr) + " " + grade_ar

    if "ضعيف" in grade_ar or "faible" in combined or "weak" in combined:
        return None
    if "صحيح" in grade_ar or "authentique" in combined or "sahih" in combined:
        return "Sahih"
    if "حسن" in grade_ar or re.search(r"\bbon\b", combined) or "hasan" in combined:
        return "Hasan"
    return None

def collection_from_reference(reference):
    raw = norm(reference)
    collections = []
    mapping = [
        (["bukhari", "boukhari", "البخاري"], "Sahih al-Bukhari"),
        (["muslim", "مسلم"], "Sahih Muslim"),
        (["tirmidhi", "الترمذي"], "Jami’ at-Tirmidhi"),
        (["nasa", "النسائي"], "Sunan an-Nasa’i"),
        (["abu dawud", "abou dawud", "أبو داود"], "Sunan Abi Dawud"),
        (["ibn majah", "ابن ماجه"], "Sunan Ibn Majah"),
        (["ahmad", "أحمد"], "Musnad Ahmad"),
        (["darimi", "الدارمي"], "Sunan ad-Darimi"),
        (["malik", "مالك"], "Al-Muwatta’"),
    ]
    for needles, label in mapping:
        if any(norm(needle) in raw for needle in needles):
            collections.append(label)
    return " / ".join(collections) if collections else "Référence HadeethEnc"

def fetch_pair(hid):
    fr = get_json("/hadeeths/one/", {"language": "fr", "id": hid})
    ar = get_json("/hadeeths/one/", {"language": "ar", "id": hid})
    return hid, fr, ar

def build_item(hid, fr, ar, category_titles):
    french = compact_spaces(fr.get("hadeeth"))
    arabic = compact_spaces(ar.get("hadeeth") or fr.get("hadeeth_ar"))
    reference = compact_spaces(ar.get("reference") or fr.get("reference"))
    grade = grade_kind(fr, ar)
    theme = theme_for(fr, category_titles)

    if not theme or not grade:
        return None
    if not french or not arabic or not reference:
        return None
    if len(french) > 620 or len(arabic) > 620:
        return None

    return {
        "id": "hadeethenc_" + hid,
        "type": "HADITH",
        "theme": theme,
        "arabicText": arabic,
        "frenchText": french,
        "author": "Prophète Muhammad ﷺ",
        "book": collection_from_reference(reference),
        "reference": reference,
        "authenticity": grade,
        "tags": sorted({theme, "hadith authentifié"}),
        "sourceProvider": SOURCE_PROVIDER,
        "sourceId": hid,
        "sourceVersion": TRANSLATION_VERSION,
        "sourceFetchedAt": FETCHED_AT,
        "reviewStatus": "VERIFIED_OFFICIAL_SOURCE",
        "translationStatus": "SOURCE_TRANSLATION_UNMODIFIED",
        "sourceUrl": "https://hadeethenc.com/fr/browse/hadith/" + hid,
    }

def collect_candidate_ids():
    categories = get_json("/categories/list/", {"language": "fr"})
    selected = [
        category for category in categories
        if any(norm(hint) in norm(category.get("title") or "") for hint in CATEGORY_HINTS)
    ]
    selected.sort(
        key=lambda c: (
            0 if any(
                norm(h) in norm(c.get("title") or "")
                for h in ["caractere", "vertu", "merite", "coran", "famille", "voisin"]
            ) else 1,
            -(int(c.get("hadeeths_count") or 0)),
            int(c.get("id") or 0),
        )
    )

    category_titles_by_id = defaultdict(set)
    ordered_ids = []

    for category in selected:
        page = 1
        while True:
            listing = get_json(
                "/hadeeths/list/",
                {
                    "language": "fr",
                    "category_id": str(category["id"]),
                    "page": page,
                    "per_page": 100,
                },
            )
            data = listing.get("data") or []
            if not data:
                break
            for brief in data:
                hid = str(brief.get("id") or "")
                if not hid:
                    continue
                if hid not in category_titles_by_id:
                    ordered_ids.append(hid)
                category_titles_by_id[hid].add(category.get("title") or "")

            meta = listing.get("meta") or {}
            if page >= int(meta.get("last_page") or page):
                break
            page += 1

    return ordered_ids, category_titles_by_id

def source_priority(item):
    book = item.get("book") or ""
    if "Sahih al-Bukhari" in book or "Sahih Muslim" in book:
        return 0
    if item.get("authenticity") == "Sahih":
        return 1
    return 2

def select_with_theme_minimums(valid_items, target):
    valid_items = sorted(
        valid_items,
        key=lambda item: (source_priority(item), item["sourceId"])
    )
    by_theme = defaultdict(list)
    for item in valid_items:
        by_theme[item["theme"]].append(item)

    selected = []
    selected_ids = set()

    for theme, minimum in MIN_THEME_COUNTS.items():
        candidates = by_theme.get(theme, [])
        if len(candidates) < minimum:
            raise RuntimeError(
                f"Authenticity gate: theme '{theme}' has only {len(candidates)} verified items; "
                f"{minimum} required."
            )
        for item in candidates[:minimum]:
            selected.append(item)
            selected_ids.add(item["id"])

    for item in valid_items:
        if len(selected) >= target:
            break
        if item["id"] not in selected_ids:
            selected.append(item)
            selected_ids.add(item["id"])

    if len(selected) != target:
        raise RuntimeError(
            f"Expected exactly {target} verified hadiths, found {len(selected)} after thematic gates."
        )
    return selected

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--target", type=int, default=144)
    args = parser.parse_args()

    ordered_ids, category_titles_by_id = collect_candidate_ids()
    if len(ordered_ids) < args.target:
        raise SystemExit(
            f"Only {len(ordered_ids)} thematic French HadeethEnc candidates found."
        )

    valid_items = []
    batch_size = 60
    for start in range(0, len(ordered_ids), batch_size):
        batch = ordered_ids[start:start + batch_size]
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
            futures = [executor.submit(fetch_pair, hid) for hid in batch]
            for future in concurrent.futures.as_completed(futures):
                hid, fr, ar = future.result()
                item = build_item(hid, fr, ar, category_titles_by_id[hid])
                if item:
                    valid_items.append(item)

        # Preserve deterministic category/list ordering after concurrent fetch.
        order = {hid: index for index, hid in enumerate(ordered_ids)}
        valid_items.sort(key=lambda item: order[item["sourceId"]])

        # Do not stop until every theme minimum is already satisfiable.
        counts = defaultdict(int)
        for item in valid_items:
            counts[item["theme"]] += 1
        if len(valid_items) >= args.target and all(
            counts[theme] >= minimum for theme, minimum in MIN_THEME_COUNTS.items()
        ):
            break

    try:
        items = select_with_theme_minimums(valid_items, args.target)
    except RuntimeError as exc:
        raise SystemExit(str(exc))

    payload = {
        "schemaVersion": 1,
        "sourceProvider": SOURCE_PROVIDER,
        "translationVersion": TRANSLATION_VERSION,
        "fetchedAt": FETCHED_AT,
        "count": len(items),
        "themeCounts": dict(sorted(
            ((theme, sum(1 for item in items if item["theme"] == theme))
             for theme in THEME_RULES),
            key=lambda pair: pair[0]
        )),
        "items": items,
    }

    canonical = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    payload_hash = hashlib.sha256(canonical.encode("utf-8")).hexdigest()

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(canonical, encoding="utf-8")
    out.with_suffix(out.suffix + ".sha256").write_text(payload_hash + "\n", encoding="utf-8")

    print(f"Wrote {len(items)} verified hadiths to {out}")
    print("themeCounts=" + json.dumps(payload["themeCounts"], ensure_ascii=False, sort_keys=True))
    print(f"sha256={payload_hash}")

if __name__ == "__main__":
    main()
