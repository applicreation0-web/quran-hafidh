#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# 1) Hikam translation corrections confirmed by Arabic + Islamic Pearls English control.
corpus_path = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
entries = json.loads(corpus_path.read_text(encoding="utf-8"))
corrections = {
    174: "La survenue des privations est une fête pour les aspirants.",
    175: "Il se peut que tu trouves dans les privations un surcroît que tu ne trouves ni dans le jeûne ni dans la prière.",
    176: "Les privations sont les tapis des dons.",
}
for item in entries:
    number = int(item["source_number"])
    if number in corrections:
        item["french"] = corrections[number]
        item["estimated_reading_seconds"] = max(5, round(len(corrections[number].split()) / 2.6))
    item["translation_cross_audit"] = {
        "status": "cross_audited_against_english_reference",
        "reference": "https://islamicpearls.net/ibnattaillah2.html",
        "date": "2026-09-04",
        "authority_rule": "verified_arabic_remains_authoritative"
    }
    source = "Islamic Pearls English cross-audit — https://islamicpearls.net/ibnattaillah2.html"
    if source not in item.get("translation_sources", []):
        item.setdefault("translation_sources", []).append(source)

corpus_path.write_text(json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# 2) Do not show an empty dual-sharh panel while no verified commentary is bundled.
hikam_ui = ROOT / "app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt"
text = hikam_ui.read_text(encoding="utf-8")
old = "if (HikamSharhEdition.isEnabled && sharhAvailability.isNotEmpty()) {\n                DualSharhBlock(sharhAvailability)\n            }"
new = "if (HikamSharhEdition.isEnabled && sharhAvailability.any { it.available }) {\n                DualSharhBlock(sharhAvailability)\n            }"
if text.count(old) != 1:
    raise SystemExit("Unexpected HikamDetail sharh block shape")
hikam_ui.write_text(text.replace(old, new), encoding="utf-8")

# 3) Plus gets direct Qur'an & Tafsir access from bottom navigation; Light keeps Reading settings.
dashboard = ROOT / "app/src/main/java/com/quranunlock/guard/DashboardActivity.kt"
text = dashboard.read_text(encoding="utf-8")
old = '''onClick = {
                            startActivity(
                                Intent(
                                    this@DashboardActivity,
                                    ReadingSelectionActivity::class.java
                                )
                            )
                        },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_nav_quran),
                                contentDescription = null
                            )
                        },
                        label = { Text("Lecture") },'''
new = '''onClick = {
                            startActivity(
                                Intent(
                                    this@DashboardActivity,
                                    if (TafsirEdition.isEnabled) {
                                        FreeQuranReaderActivity::class.java
                                    } else {
                                        ReadingSelectionActivity::class.java
                                    }
                                )
                            )
                        },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_nav_quran),
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(if (TafsirEdition.isEnabled) "Qur’an" else "Lecture")
                        },'''
if text.count(old) != 1:
    raise SystemExit("Unexpected dashboard Quran navigation shape")
dashboard.write_text(text.replace(old, new), encoding="utf-8")

# 4) Version bump: 0.10.3 / versionCode 22.
build = ROOT / "app/build.gradle.kts"
text = build.read_text(encoding="utf-8")
replacements = {
    'check(buildFile.contains("versionCode = 21")) {\n            "0.10.2 must use versionCode 21 for an in-place update over 0.10.1."\n        }':
    'check(buildFile.contains("versionCode = 22")) {\n            "0.10.3 must use versionCode 22 for an in-place update over 0.10.2."\n        }',
    'check(buildFile.contains("versionName = \\"0.10.2\\"")) {\n            "Expected isolated tafsir editions update 0.10.2."\n        }':
    'check(buildFile.contains("versionName = \\"0.10.3\\"")) {\n            "Expected audited personal Plus update 0.10.3."\n        }',
    'versionCode = 21\n        versionName = "0.10.2"':
    'versionCode = 22\n        versionName = "0.10.3"',
}
for old, new in replacements.items():
    if text.count(old) != 1:
        raise SystemExit(f"Expected one build replacement, got {text.count(old)}: {old[:70]}")
    text = text.replace(old, new)
build.write_text(text, encoding="utf-8")

print("Applied 0.10.3 audited release fixes")
