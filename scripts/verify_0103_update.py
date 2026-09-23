#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def require(condition, message):
    if not condition:
        raise SystemExit("0.10.3 AUDIT FAILURE: " + message)

build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
dashboard = (ROOT / "app/src/main/java/com/quranunlock/guard/DashboardActivity.kt").read_text(encoding="utf-8")
quran_hub = (ROOT / "app/src/main/java/com/quranunlock/guard/QuranHubActivity.kt").read_text(encoding="utf-8")
reader = (ROOT / "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt").read_text(encoding="utf-8")
gesture = (ROOT / "app/src/main/java/com/quranunlock/guard/ReaderGestureClassifier.kt").read_text(encoding="utf-8")
hikam_ui = (ROOT / "app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt").read_text(encoding="utf-8")
light_sharh = (ROOT / "app/src/light/java/com/quranunlock/guard/HikamSharhEdition.kt").read_text(encoding="utf-8")
plus_sharh = (ROOT / "app/src/plus/java/com/quranunlock/guard/HikamSharhEdition.kt").read_text(encoding="utf-8")
entries = json.loads((ROOT / "app/src/main/assets/hikam/al_hikam_verified.json").read_text(encoding="utf-8"))

# Historical 0.10.3 guarantees are retained inside newer releases; the current
# release audit owns the active version number.
require('versionCode = 22' in build and 'versionName = "0.10.3"' in build, "0.10.3 compatibility markers must remain auditable")
require(len(entries) == 264, "Hikam corpus must contain exactly 264 entries")
require({int(x["source_number"]) for x in entries} == set(range(1, 265)), "Hikam numbering must be exactly 1..264")
for item in entries:
    audit = item.get("translation_cross_audit", {})
    require(audit.get("status") == "cross_audited_against_english_reference", f'Hikma {item.get("source_number")} missing English cross-audit status')
    require(audit.get("reference") == "https://islamicpearls.net/ibnattaillah2.html", f'Hikma {item.get("source_number")} missing audit reference')
    require(audit.get("authority_rule") == "verified_arabic_remains_authoritative", f'Hikma {item.get("source_number")} changes authority rule')

by_number = {int(x["source_number"]): x for x in entries}
require(by_number[174]["french"] == "La survenue des privations est une fête pour les aspirants.", "Hikma 174 correction missing")
require(by_number[175]["french"] == "Il se peut que tu trouves dans les privations un surcroît que tu ne trouves ni dans le jeûne ni dans la prière.", "Hikma 175 correction missing")
require(by_number[176]["french"] == "Les privations sont les tapis des dons.", "Hikma 176 correction missing")

# 0.10.7 routes voluntary Qur'an access through the dedicated Qur'an hub.
# Plus must still reach FreeQuranReaderActivity when Tafsir is enabled, and
# this path must remain separate from challenge/unlock credit.
require('QuranHubActivity::class.java' in dashboard, "dashboard must expose direct voluntary Quran access")
require('if (TafsirEdition.isEnabled)' in quran_hub and 'FreeQuranReaderActivity::class.java' in quran_hub,
        "Plus Qur'an hub must expose voluntary Quran/Tafsir access")
require('challenge_key' not in reader.lower(), "free reader must not depend on a challenge key")
for forbidden in ["completeReadingAndUnlock", "consumeJokerAndUnlock", "markUnlocked"]:
    require(forbidden not in reader, f"free reader must never call {forbidden}")
require('return if (deltaX > 0f) ReaderSwipe.NEXT else ReaderSwipe.PREVIOUS' in gesture, "Arabic-book swipe direction must remain right=next, left=previous")
require('sharhAvailability.any { it.available }' in hikam_ui, "empty commentary panel must stay hidden")
require('isEnabled: Boolean = false' in light_sharh, "Light must not enable private Hikam sharh")
require('isEnabled: Boolean = true' in plus_sharh, "Plus must enable private Hikam sharh layer")
require('HikamCommentator.SHARNUBI' in hikam_ui and 'HikamCommentator.IBN_ABBAD' in hikam_ui, "Plus must keep both commentary selectors")

print("0.10.3 contradictory update audit: PASS")
print("- 264 Hikam cross-audit metadata present")
print("- 174/175/176 corrected")
print("- Arabic RTL navigation guarded")
print("- voluntary Quran/Tafsir isolated from unlock credit via Quran hub")
print("- Light/Plus sharh isolation guarded")
print("- dual-commentator selector guarded; empty data is not fabricated")
