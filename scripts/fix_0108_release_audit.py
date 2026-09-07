#!/usr/bin/env python3
"""Keep the legacy release audit strict after removing reader helper prose in 0.10.8.

The old gate asserted the literal swipe instruction was visible. 0.10.8 deliberately
removes application-authored explanatory prose from Quran readers, so validate the
actual RTL gesture wiring instead. No unlock/budget condition is weakened.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "app" / "build.gradle.kts"
text = path.read_text(encoding="utf-8")
old = '        check(reader.contains("Balayez vers la droite pour avancer"))\n'
new = '''        check(
            reader.contains("onSwipeNext = {") &&
                reader.contains("ReaderSwipe.NEXT -> currentOnSwipeNext.value()") &&
                reader.contains("ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()")
        ) {
            "Quran reader must retain wired RTL page gestures after helper prose removal."
        }
'''
if old in text:
    text = text.replace(old, new, 1)
elif "Quran reader must retain wired RTL page gestures" not in text:
    raise SystemExit("0.10.8 audit adaptation: legacy swipe-prose assertion not found")
path.write_text(text, encoding="utf-8")
print("0.10.8 release audit now checks gesture behavior, not explanatory prose")
