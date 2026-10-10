#!/usr/bin/env python3
"""Fail-closed contract for the 13 primary and 10 secondary BOOX glyphs."""
import csv
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "docs/FROZEN_UI_ICONS_13_PLUS_10.tsv"
PRIMARY = ["home","book-open","network","key-round","book-text","scan","brain","flag",
           "pencil","check-circle","chart-no-axes-column","refresh-cw","arrow-left"]
SECONDARY = ["check","x","undo-2","trash-2","info","eye","mic","play","pause","ellipsis"]
PIN = "f93beca5957187620e30ae771d220d88501be621"

def fail(message):
    raise SystemExit("CONTRACTUAL ICONS FAIL: " + str(message))

raw = MANIFEST.read_text(encoding="utf-8").splitlines()
rows = list(csv.reader([line for line in raw if line and not line.startswith("#")], delimiter="\t"))
if len(rows) != 23: fail(f"Expected 23 unique rows, got {len(rows)}")
if len({row[2] for row in rows}) != 23: fail("Duplicate role or name")
if [r[2] for r in rows if r[0]=="principal"] != PRIMARY: fail("Primary icon list or order changed")
if [r[2] for r in rows if r[0]=="secondaire"] != SECONDARY: fail("Secondary icon list or order changed")
if sum(r[0]=="principal" for r in rows) != 13: fail("Primary count changed")
if sum(r[0]=="secondaire" for r in rows) != 10: fail("Secondary count changed")
immutable = {
    "ic_ui_reading.xml":"630eb9f4ded2387ab929f5945c6b634b5dee6b85",
    "ic_ui_semantic_map.xml":"bea50ef9f3c35c903fbe36400660053dbb075c54",
    "ic_ui_recall_key.xml":"bb6670756b22dfb43ab94d9c56e2c66ac87ce7d3",
    "ic_ui_tafsir.xml":"ca2b48f87f4f561a018361127d1d152243cd146e",
    "ic_ui_edit.xml":"9770d143f2854f6356d773375a5e42d2d52dc6e0",
}
for row in rows:
    if len(row)!=6: fail(f"Incorrect row format: {row}")
    kind, role, icon, filename, expected, source = row
    path=ROOT/"hifz-app/src/main/res/drawable"/filename
    if not path.is_file(): fail(f"Missing drawable: {filename}")
    if not re.fullmatch("[a-f0-9]{40}",expected): fail(f"Invalid fingerprint for {filename}")
    actual=subprocess.check_output(["git","hash-object",str(path)],cwd=ROOT,text=True).strip()
    if actual!=expected: fail(f"Changed frozen icon {icon} ({filename}): expected {expected}, got {actual}")
    if filename in immutable and immutable[filename]!=expected: fail(f"Original pictogram overwritten: {filename}")
    if source != "frozen-original" and not source.startswith("lucide:"):
        fail(f"Unverified pictogram provenance: {filename}")
    text=path.read_text(encoding="utf-8")
    for needle in ['android:width="24dp"','android:height="24dp"',
                   'android:viewportWidth="24"','android:viewportHeight="24"',
                   'android:strokeColor="#121211"']:
        if needle not in text: fail(f"BOOX visual code mismatch {filename}: {needle}")
    if "android:pathData=" not in text: fail(f"No vector geometry: {filename}")

ui=(ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java").read_text(encoding="utf-8")
for label, target in {
    '"accueil"':"ic_ui_home",
    '"parcours hifz"':"ic_ui_revision",
    '"focus"':"ic_ui_focus",
    '"révision"':"ic_ui_revision",
    '"difficulté"':"ic_ui_difficulty",
    '"carte"':"ic_ui_semantic_map",
    '"quiz"':"ic_ui_quiz",
    '"lecture"':"ic_ui_reading",
    '"reprendre"':"ic_ui_resume",
}.items():
    if label not in ui or f"R.drawable.{target}" not in ui: fail(f"Role mapping unrecognized: {label}/{target}")
for text in ['R.drawable.ic_ui_record','R.drawable.ic_ui_play','R.drawable.ic_ui_validate',
             'R.drawable.ic_ui_undo','R.drawable.ic_ui_annotation_erase']:
    if text not in ui and text not in (ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuizActivity.java").read_text(encoding="utf-8"):
        fail(f"Action icon is not wired: {text}")
reader=(ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java").read_text(encoding="utf-8")
if reader.count('readerActions.addView(')<4: fail("4 permanent reader controls missing")
if 'Ui.iconButton(this, "", "Focus"' in reader: fail("Focus improperly added as fifth permanent icon")
hifz=(ROOT/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java").read_text(encoding="utf-8")
if 'R.drawable.ic_hifz_anchor' not in hifz: fail("Itqan anchoring pictogram removed")
print(f"PASS: exact {len(PRIMARY)}+{len(SECONDARY)} contract icons, original 5 byte-immutable; Lucide commit {PIN}")
