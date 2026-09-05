#!/usr/bin/env python3
"""Review aid for technical Sufi vocabulary in the 264 verified Hikam.

Arabic is authoritative. This script never rewrites French. It identifies rows
where a high-signal Arabic technical term is present but none of the approved
French renderings appears, so those rows can be manually checked against the
English secondary control before release.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"

# Arabic patterns are intentionally narrow. Accepted French forms are broad
# enough to allow context-sensitive translation while protecting the core
# technical sense from accidental flattening.
TERMS = {
    "tajrid": (r"التجريد|تجريد", ("dépouillement", "détachement", "dénuement")),
    "asbab": (r"الأسباب|اسباب", ("cause", "causes", "moyen", "moyens")),
    "tadbir": (r"التدبير|تدبير", ("régenter", "gestion", "gérer", "planifier", "disposition", "administrer")),
    "himma": (r"الهم[مة]|همة|همم", ("aspiration", "aspirations", "élan", "élans", "résolution")),
    "marifa": (r"المعرفة|معرفة", ("connaissance", "gnose", "connaître")),
    "tawhid": (r"التوحيد|توحيد", ("unicité", "unité", "tawḥīd", "tawhid")),
    "fana": (r"الفناء|فناء", ("anéantissement", "annihilation", "extinction", "effacement")),
    "baqa": (r"البقاء|بقاء", ("subsistance", "permanence", "demeure", "maintien")),
    "nafs": (r"النفس|نفس", ("âme", "ego", "moi", "nafs")),
    "raja": (r"الرجاء|رجاء", ("espérance", "espoir")),
    "irada": (r"الإرادة|ارادة|إرادة", ("volonté", "désir", "aspiration")),
    "khawatir": (r"الخواطر|خواطر", ("pensée", "pensées", "suggestion", "suggestions", "mouvement intérieur", "mouvements intérieurs")),
    "warid": (r"الواردات|واردات|الوارد|وارد", ("inspiration", "inspirations", "venue", "venues", "irruption", "irruptions", "wārid", "warid")),
    "qabd": (r"القبض|قبض", ("resserrement", "contraction", "saisie", "qabd", "qabḍ")),
    "bast": (r"البسط|بسط", ("dilatation", "expansion", "déploiement", "bast", "basṭ")),
    "anwar": (r"الأنوار|انوار|أنوار", ("lumière", "lumières")),
    "agh yar": (r"الأغيار|اغيار|أغيار", ("autres", "altérités", "autre que", "créatures")),
    "asrar": (r"الأسرار|اسرار|أسرار", ("secret", "secrets")),
}


def compact(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip().lower()


entries = json.loads(CORPUS.read_text(encoding="utf-8"))
flags = []
counts = {}
for label, (pattern, accepted) in TERMS.items():
    seen = 0
    for item in entries:
        arabic = item.get("arabic", "")
        if not re.search(pattern, arabic):
            continue
        seen += 1
        french = compact(item.get("french", ""))
        if not any(term.lower() in french for term in accepted):
            flags.append({
                "number": int(item["source_number"]),
                "term": label,
                "arabic": arabic,
                "french": item.get("french", ""),
            })
    counts[label] = seen

print(json.dumps({
    "status": "manual_review_aid",
    "authority": "verified Arabic matn",
    "count": len(entries),
    "technical_term_occurrence_counts": counts,
    "review_flag_count": len(flags),
    "review_flags": flags,
}, ensure_ascii=False, indent=2))
