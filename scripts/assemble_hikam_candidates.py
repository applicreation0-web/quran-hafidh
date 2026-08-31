#!/usr/bin/env python3
import glob
import json
import math
import re
from pathlib import Path

PRIMARY=Path("app/src/main/assets/hikam/primary_extraction.json")
CANDIDATES=Path("app/src/main/assets/hikam/al_hikam_candidates.json")
ACTIVE=Path("app/src/main/assets/hikam/al_hikam_verified.json")
PENDING=Path("app/src/main/assets/hikam/pending_verification.json")

primary=json.loads(PRIMARY.read_text(encoding="utf-8"))
translations={}
for filename in sorted(glob.glob("app/src/main/assets/hikam/translations_fr_*.json")):
    for row in json.loads(Path(filename).read_text(encoding="utf-8")):
        n=int(row["source_number"])
        if n in translations:
            raise SystemExit(f"Duplicate French translation for Hikma {n}")
        french=row["french"].strip()
        if not french:
            raise SystemExit(f"Empty French translation for Hikma {n}")
        translations[n]=french

expected=set(range(1,265))
missing=sorted(expected-set(translations))
extra=sorted(set(translations)-expected)
if missing or extra:
    raise SystemExit(f"French coverage mismatch. missing={missing} extra={extra}")

THEMES = {
    "tawakkul": ("تدبير","الأقدار","قدر","مشيئة","توكل"),
    "ikhlas": ("الإخلاص","الرياء","صدق","الصّدق"),
    "dhikr": ("الذكر","أذكار","ذاكر"),
    "dunya": ("الدنيا","الدار","الأكوان"),
    "nafs": ("النفس","شهوة","الهوى"),
    "repentance": ("توبة","يتب","الذنب","المعصية","الزلات"),
    "hope": ("الرجاء","اليأس","حسن الظن"),
    "fear": ("الخوف","الخشية"),
    "patience": ("الصبر","تصبر","البلاء"),
    "gratitude": ("الشكر","يشكر","النعم"),
    "divine_decree": ("الأقدار","قدر","الأزل","المشيئة"),
    "worship": ("العبودية","الطاعة","الصلاة","العبادة"),
    "knowledge": ("العلم","المعارف","الفهم"),
    "spiritual_presence": ("الحضور","الشهود","البصيرة","الأنوار","النور"),
    "attachment": ("الأغيار","الطمع","الخلق"),
    "humility": ("التواضع","الذلة","الافتقار","الفاقة")
}

def themes_for(arabic):
    found=[]
    for theme,keys in THEMES.items():
        if any(k in arabic for k in keys):
            found.append(theme)
    return found or ["spiritual_presence"]

def reading_seconds(french):
    words=len(re.findall(r"\b\w+[’'-]?\w*\b",french,flags=re.UNICODE))
    return max(5, math.ceil(words/3.2))

PRIMARY_EDITION=(
    "Matn al-Hikam al-‘Ata’iyya reproduced in al-Lata’if al-Ilahiyya "
    "fi Sharh Mukhtarat min al-Hikam al-‘Ata’iyya, Dar al-Kutub "
    "al-‘Ilmiyya, Beirut, 1426/2005, ed. ‘Asim Ibrahim al-Kayyali"
)
PRIMARY_BASE="https://ablibrary.net/book_content/8787/"
SECONDARY_PDF="https://data.nur.nu/Kutub/Arabic/Ibn3AtaAllah_Hikam_000802_al-mostafa.pdf"
ACADEMIC="https://dspace.univ-ghardaia.edu.dz/jspui/bitstream/123456789/4884/1/408.04.106.pdf"

candidates=[]
for row in primary["entries"]:
    n=int(row["source_number"])
    arabic=row["arabic"].strip()
    french=translations[n]
    sources=[SECONDARY_PDF]
    if row.get("source_page"):
        sources.insert(0, PRIMARY_BASE+str(row["source_page"]))
    else:
        sources.extend(row.get("recovery_sources", []))
    if n==233 and ACADEMIC not in sources:
        sources.append(ACADEMIC)

    candidates.append({
        "id": f"hikam_{n:03d}",
        "collection": "al_hikam_al_ataiyya",
        "author": "Ibn Ata Allah al-Iskandari",
        "arabic": arabic,
        "french": french,
        "explanation": "",
        "source_title": "Al-Hikam al-Ata'iyya",
        "source_edition": PRIMARY_EDITION,
        "source_number": str(n),
        "source_page": "" if row.get("source_page") is None else str(row["source_page"]),
        "verification_status": "pending_verification",
        "verification_sources": sources,
        "text_type": "author_wisdom",
        "themes": themes_for(arabic),
        "estimated_reading_seconds": reading_seconds(french),
        "alternate_numbering": {},
        "textual_variant": "",
        "translator_note": (
            "Traduction française éditoriale originale réalisée à partir du "
            "matn arabe vérifié; elle ne reproduit pas une édition française moderne."
        ),
        "translation_status": "pending_verification",
        "translation_sources": [
            "Arabic source text listed in verification_sources",
            "Paul Nwyia, edition critique et traduction des Hikam, Dar el-Machreq — https://www.darelmachreq.com/book/485-ikam-ibn-aa-allah-arabefrancais-a2",
            "Hassan Boutaleb, Sagesses et confidences, Albouraq — https://catalogue.bnf.fr/ark:/12148/cb47245528k"
        ],
        "translation_method": "editorial_translation_from_verified_arabic",
        "verification_notes": (
            "Not active until Arabic cross-check and French translation review are complete."
        )
    })

assert len(candidates)==264
assert [int(x["source_number"]) for x in candidates]==list(range(1,265))
assert all(x["arabic"] and x["french"] for x in candidates)
assert len({x["id"] for x in candidates})==264

CANDIDATES.write_text(json.dumps(candidates,ensure_ascii=False,indent=2),encoding="utf-8")
PENDING.write_text(json.dumps(candidates,ensure_ascii=False,indent=2),encoding="utf-8")

# Never activate pending content. The active file is generated only by a later
# verification pass and is intentionally empty at this stage.
ACTIVE.write_text("[]\n",encoding="utf-8")

print(json.dumps({
    "candidate_count":len(candidates),
    "french_coverage":len(translations),
    "active_count":0,
    "pending_count":len(candidates)
},ensure_ascii=False))
