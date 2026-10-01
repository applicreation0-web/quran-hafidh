#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
icon = (ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml").read_text(encoding="utf-8")
colors = (ROOT / "app/src/main/res/values/colors.xml").read_text(encoding="utf-8")
design = (ROOT / "app/src/main/java/com/quranunlock/guard/SafeguardDesign.kt").read_text(encoding="utf-8")

required = {
    "icon ink": (icon, "#171715"),
    "launcher cream": (colors, "#FBF7EF"),
    "app warm cream": (design, "Color(0xFFFBF7EF)"),
    "reading cream": (design, "Color(0xFFF7F2E8)"),
    "black ink": (design, "Color(0xFF171715)"),
}
for label, (text, token) in required.items():
    if token not in text:
        raise SystemExit(f"FAIL current cream/black design boundary: missing {label} {token}")

legacy_icon_colors = ("#2C5D49", "#D8BA73", "#7A5337", "#FFFDF5")
for token in legacy_icon_colors:
    if token in icon:
        raise SystemExit(f"FAIL current cream/black design boundary: legacy launcher color still rendered: {token}")

for forbidden in ("sahelianButtonOrnament", "drawDiamond"):
    if forbidden in design:
        raise SystemExit(f"FAIL current cream/black design boundary: obsolete ornament present: {forbidden}")

print("PASS current cream/black design boundary")
print("- launcher: cream field + #171715 abstract Quran/rehal strokes")
print("- UI: warm cream / black / neutral grayscale")
print("- legacy green/gold/brown launcher palette rejected")
