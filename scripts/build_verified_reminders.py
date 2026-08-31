#!/usr/bin/env python3
import argparse
import hashlib
import json
import re
import time
import unicodedata
import urllib.parse
import urllib.request
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

def get_json(path, params=None, retries=4):
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    last = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "QuranSafeguardReminderBuilder/0.8"})
            with urllib.request.urlopen(req, timeout=45) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception as exc:
            last = exc
            time.sleep(1.0 + attempt)
    raise RuntimeError(f"Failed API request {url}: {last}")

def norm(value):
    value = unicodedata.normalize("NFKD", value or "")
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    return value.lower()

def theme_for(details, category_title):
    haystack = norm(" ".join([
        category_title or "",
        details.get("title") or "",
        details.get("hadeeth") or "",
        " ".join(details.get("categories") or []),
    ]))
    for theme, words in THEME_RULES.items():
        if any(norm(word) in haystack for word in words):
            return theme
    return None

def accepted_grade(details):
    grade_ar = details.get("grade_ar") or ""
    grade_fr = norm(details.get("grade") or "")
    if "ضعيف" in grade_ar or "weak" in grade_fr or "faible" in grade_fr:
        return False
    return ("صحيح" in grade_ar or "حسن" in grade_ar or
            "authentique" in grade_fr or grade_fr.startswith("bon") or "hasan" in grade_fr)

def compact_spaces(value):
    return re.sub(r"\s+", " ", (value or "")).strip()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--target", type=int, default=144)
    args = parser.parse_args()

    categories = get_json("/categories/list/", {"language": "fr"})
    selected_categories = []
    for category in categories:
        title = category.get("title") or ""
        if any(norm(hint) in norm(title) for hint in CATEGORY_HINTS):
            selected_categories.append(category)

    # Deterministic priority: thematic categories first, then larger useful categories.
    selected_categories.sort(
        key=lambda c: (
            0 if any(norm(h) in norm(c.get("title") or "") for h in ["caractere", "vertu", "merite", "coran"]) else 1,
            -(int(c.get("hadeeths_count") or 0)),
            int(c.get("id") or 0),
        )
    )

    seen = set()
    items = []

    for category in selected_categories:
        if len(items) >= args.target:
            break
        category_id = str(category["id"])
        category_title = category.get("title") or ""
        page = 1
        while len(items) < args.target:
            listing = get_json(
                "/hadeeths/list/",
                {"language": "fr", "category_id": category_id, "page": page, "per_page": 100},
            )
            data = listing.get("data") or []
            if not data:
                break

            for brief in data:
                if len(items) >= args.target:
                    break
                hid = str(brief.get("id") or "")
                if not hid or hid in seen:
                    continue
                seen.add(hid)

                details = get_json("/hadeeths/one/", {"language": "fr", "id": hid})
                french = compact_spaces(details.get("hadeeth"))
                arabic = compact_spaces(details.get("hadeeth_ar"))
                reference = compact_spaces(details.get("reference"))
                grade = compact_spaces(details.get("grade"))
                attribution = compact_spaces(details.get("attribution"))
                theme = theme_for(details, category_title)

                if not theme or not accepted_grade(details):
                    continue
                if not french or not arabic or not reference or not grade:
                    continue
                # The module is intentionally for short daily reminders.
                if len(french) > 620 or len(arabic) > 620:
                    continue

                items.append({
                    "id": "hadeethenc_" + hid,
                    "type": "HADITH",
                    "theme": theme,
                    "arabicText": arabic,
                    "frenchText": french,
                    "author": "Prophète Muhammad ﷺ",
                    "book": attribution or "Tradition prophétique",
                    "reference": reference,
                    "authenticity": grade,
                    "tags": sorted({theme, "hadith authentifié"}),
                    "sourceProvider": SOURCE_PROVIDER,
                    "sourceId": hid,
                    "sourceVersion": TRANSLATION_VERSION,
                    "sourceFetchedAt": FETCHED_AT,
                    "reviewStatus": "VERIFIED_OFFICIAL_SOURCE",
                })
                time.sleep(0.03)

            meta = listing.get("meta") or {}
            last_page = int(meta.get("last_page") or page)
            if page >= last_page:
                break
            page += 1

    if len(items) != args.target:
        raise SystemExit(
            f"Expected exactly {args.target} verified hadiths, found {len(items)}. "
            "Broaden or adjust reviewed thematic filters instead of lowering quality."
        )

    payload = {
        "schemaVersion": 1,
        "sourceProvider": SOURCE_PROVIDER,
        "translationVersion": TRANSLATION_VERSION,
        "fetchedAt": FETCHED_AT,
        "count": len(items),
        "items": items,
    }
    canonical = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    payload_hash = hashlib.sha256(canonical.encode("utf-8")).hexdigest()

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(canonical, encoding="utf-8")
    out.with_suffix(out.suffix + ".sha256").write_text(payload_hash + "\n", encoding="utf-8")
    print(f"Wrote {len(items)} verified hadiths to {out}")
    print(f"sha256={payload_hash}")

if __name__ == "__main__":
    main()
