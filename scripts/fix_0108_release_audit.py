#!/usr/bin/env python3
"""Keep the legacy release audit strict after removing reader helper prose in 0.10.8.

0.10.8 deliberately removes application-authored explanatory prose from Quran
readers. Replace legacy assertions on that prose with assertions on the underlying
state/gesture wiring. No unlock, budget, validation or return-to-target condition is
weakened.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "app" / "build.gradle.kts"
text = path.read_text(encoding="utf-8")

replacements = [
    (
        '        check(reader.contains("Balayez vers la droite pour avancer"))\n',
        '''        check(
            reader.contains("onSwipeNext = {") &&
                reader.contains("ReaderSwipe.NEXT -> currentOnSwipeNext.value()") &&
                reader.contains("ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()")
        ) {
            "Quran reader must retain wired RTL page gestures after helper prose removal."
        }
''',
        "Quran reader must retain wired RTL page gestures",
    ),
    (
        '        check(reader.contains("Quota atteint • sortie libre • lecture facultative"))\n',
        '''        check(
            reader.contains("quotaReached") &&
                reader.contains("Quota atteint • sortie libre") &&
                reader.contains("continueFreely")
        ) {
            "Quota completion must still enter optional free-reading state."
        }
''',
        "Quota completion must still enter optional free-reading state",
    ),
]

for old, new, marker in replacements:
    if old in text:
        text = text.replace(old, new, 1)
    elif marker not in text:
        raise SystemExit(
            "0.10.8 audit adaptation: legacy prose assertion not found for " + marker
        )

path.write_text(text, encoding="utf-8")
print("0.10.8 release audit checks reader behavior/state instead of removed helper prose")
