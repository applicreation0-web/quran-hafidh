#!/usr/bin/env python3
"""Fail-closed review of high-signal Sufi vocabulary in the 264 verified Hikam.

Arabic remains authoritative. Matching is diacritic-insensitive but token-exact so
ordinary inflected verbs and homographs are not silently turned into technical
terms. The script never rewrites French; it only blocks when a high-confidence
technical noun is present and the French translation has no acceptable contextual
rendering at all.
"""
from __future__ import annotations
import json, re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
DIACRITICS = re.compile(r"[\u064B-\u065F\u0670\u06D6-\u06ED]")
ARABIC_WORD = re.compile(r"[\u0621-\u063A\u0641-\u064A\u066E-\u06D3]+")


def words(text: str) -> set[str]:
    out: set[str] = set()
    for token in ARABIC_WORD.findall(DIACRITICS.sub("", text)):
        out.add(token)
        # Match the Android runtime's deliberately conservative clitic handling.
        if len(token) > 3 and token[0] in {"و", "ف"}:
            out.add(token[1:])
        if len(token) > 4 and (token.startswith("بال") or token.startswith("كال")):
            out.add("ال" + token[3:])
        if len(token) > 3 and token.startswith("لل"):
            out.add("ال" + token[2:])
    return out


def compact(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip().lower()

# Token lists deliberately prefer unambiguous noun forms. Bare بسط is excluded
# because Hikma 176 uses it in the carpet metaphor بسط المواهب, not the basṭ state.
# Bare نفس is excluded because it can mean breath/self rather than technical nafs.
TERMS = {
    "tajrid": ({"التجريد", "تجريد"}, ("dépouillement", "détachement", "dénuement")),
    "asbab": ({"الأسباب", "اسباب", "أسباب"}, ("cause", "causes", "moyen", "moyens")),
    "tadbir": ({"التدبير", "تدبير"}, ("régenter", "gestion", "gérer", "planifier", "disposition", "administrer", "souci")),
    "himma": ({"الهمة", "همة", "همم"}, ("aspiration", "aspirations", "élan", "élans", "résolution")),
    "marifa": ({"المعرفة", "معرفة"}, ("connaissance", "gnose", "connaître")),
    "tawhid": ({"التوحيد", "توحيد"}, ("unicité", "unité", "tawḥīd", "tawhid")),
    "fana": ({"الفناء", "فناء"}, ("anéantissement", "annihilation", "extinction", "effacement", "disparition")),
    "baqa": ({"البقاء", "بقاء"}, ("subsistance", "permanence", "demeure", "maintien", "rester", "reste")),
    "nafs": ({"النفس"}, ("âme", "ego", "moi", "soi", "part")),
    "warid": ({"الوارد", "الواردات", "واردات"}, ("influx", "inspiration", "survient", "surviennent", "arrive", "arrivent", "venue", "irruption", "afflux")),
    "qabd": ({"القبض"}, ("resserrement", "contraction", "contract")),
    "bast": ({"البسط"}, ("dilatation", "expansion", "élargissement", "déploiement", "élargit")),
    "anwar": ({"الأنوار", "انوار", "أنوار"}, ("lumière", "lumières")),
    "aghyar": ({"الأغيار", "اغيار", "أغيار"}, ("autres", "altérit", "autre que", "créatures", "étrang")),
    "asrar": ({"الأسرار", "اسرار", "أسرار"}, ("secret", "secrets")),
}

entries = json.loads(CORPUS.read_text(encoding="utf-8"))
flags = []
counts = {}
for label, (tokens, accepted) in TERMS.items():
    normalized_tokens = {next(iter(words(token)), token) for token in tokens}
    seen = 0
    for item in entries:
        if not (words(item.get("arabic", "")) & normalized_tokens):
            continue
        seen += 1
        french = compact(item.get("french", ""))
        if not any(term in french for term in accepted):
            flags.append({
                "number": int(item["source_number"]),
                "term": label,
                "arabic": item.get("arabic", ""),
                "french": item.get("french", ""),
            })
    counts[label] = seen

# Explicit regression checks for previously observed false positives.
by_number = {int(item["source_number"]): item for item in entries}
if "bast" in {label for label, (tokens, _) in TERMS.items() if words(by_number[176]["arabic"]) & {next(iter(words(t)), t) for t in tokens}}:
    raise SystemExit("Hikam lexicon audit failure: Hikma 176 carpet metaphor was misclassified as technical basṭ")
if "qabd" in {label for label, (tokens, _) in TERMS.items() if words(by_number[249]["arabic"]) & {next(iter(words(t)), t) for t in tokens}}:
    raise SystemExit("Hikam lexicon audit failure: Hikma 249 verb يقبض was misclassified as technical qabḍ")

payload = {
    "status": "PASS" if not flags else "REVIEW_REQUIRED",
    "authority": "verified Arabic matn",
    "count": len(entries),
    "matching": "diacritic-insensitive exact Arabic word tokens",
    "technical_term_occurrence_counts": counts,
    "review_flag_count": len(flags),
    "review_flags": flags,
}
print(json.dumps(payload, ensure_ascii=False, indent=2))
if flags:
    raise SystemExit(f"0.10.5 HIKAM LEXICON AUDIT FAILURE: {len(flags)} high-confidence occurrences need review")
