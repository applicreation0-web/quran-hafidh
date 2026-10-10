#!/usr/bin/env python3
"""J6: fail-closed icon-role source audit, not a simulated BOOX visual test.

Do not alter engines or per-screen mechanics. Detect new unmapped compact action
roles, changed vector shapes, accidental Unicode fallbacks, or a missing UI role.
"""
from __future__ import annotations
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
PREVIEW = ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview"
UI = (PREVIEW / "Ui.java").read_text(encoding="utf-8")
MAPPING = (ROOT / "hifz-app/src/test/java/com/quransafeguard/hifz/preview"
           / "UiIconMappingTest.java").read_text(encoding="utf-8")
CONTRACT = ROOT / "docs/FROZEN_UI_ICONS_13_PLUS_10.tsv"
ANDROID_DRAWABLES = ROOT / "hifz-app/src/main/res/drawable"

def check(test: bool, what: str) -> None:
    if not test:
        raise RuntimeError("J6 frozen pictogram contract FAIL: " + what)

icons = []
for line in CONTRACT.read_text(encoding="utf-8").splitlines():
    if not line or line.startswith("#"):
        continue
    pieces = line.split("\t")
    check(len(pieces) == 6, "invalid 23-icon manifest row")
    icons.append((pieces[2], pieces[3]))
check(len(icons) == 23 and len(set(icons)) == 23, "23-vector count")
check(len(set(x[1] for x in icons)) == 23, "duplicate vector filename")
check(all((ANDROID_DRAWABLES / fn).is_file() for _,fn in icons), "missing vector")
check("button.setText(symbol)" not in UI, "Unicode fallback must never render")
check("throw new IllegalArgumentException(\"Unmapped compact icon role: \"" in UI,
      "unmapped compact role must fail explicitly, not fall back")
check("box.addView(iconButton(context, symbol, label, listener))" in UI,
      "icon-only layout must remain a single standard 48dp target")
check("int size = dp(context, 48)" in UI, "compact touch size must remain 48dp")
check("setButtonIcon(button, iconRes)" in UI, "compact button must use a vector")
check("button.setContentDescription(value)" in UI, "accessible icon label absent")

# R.drawable entries are explicit Android build IDs tested on the JVM.
roles = dict(re.findall(r'\{"([^"]+)",\s*R\.drawable\.(ic_[a-z0-9_]+)\}', MAPPING))
check(len(roles) >= 65, "C22 mapping coverage unexpectedly reduced")
compiled = re.compile(
    r'\bUi\.(?:iconButton|roundAction)\s*\(\s*this\s*,\s*'
    r'"(?:[^"\\]|\\.)*"\s*,\s*"([^"]+)"')
all_actions = 0
literal_actions = 0
unmapped = []
dynamic = []
screens = []
for path in sorted(PREVIEW.glob("*.java")):
    if path.name == "Ui.java":
        continue
    source = path.read_text(encoding="utf-8")
    found = list(re.finditer(r'\bUi\.(?:iconButton|roundAction)\s*\(', source))
    if not found:
        continue
    static = compiled.findall(source)
    count_dynamic = len(found)-len(static)
    check(count_dynamic >= 0, path.name+" invalid parser accounting")
    all_actions += len(found)
    literal_actions += len(static)
    unmapped.extend(path.name+": "+role for role in static if role not in roles)
    if count_dynamic:
        dynamic.append((path.name,count_dynamic))
    screens.append(path.name)
check(not unmapped, "unmapped literal icon labels: "+repr(unmapped))
# Only the Hifz engine's existing ternary mark label and the addRoundAction
# wrapper carry variable labels; both target existing, C22-pinned roles.
check(dynamic == [("HifzSessionActivity.java",2)],
      "new variable compact icon action needs explicit review: "+repr(dynamic))
hifz = (PREVIEW/"HifzSessionActivity.java").read_text(encoding="utf-8")
check('weakMarkMode ? "Touchez le verset…" : "Marquer"' in hifz
      and 'Ui.roundAction(this,symbol,label,listener)' in hifz,
      "the two dynamic label exceptions have changed")
check('"Touchez le verset…"' in MAPPING and '"Marquer"' in MAPPING,
      "dynamic mark roles unmapped")
# Hifz engine invokes this wrapper only with literal labels: verify all of
# those labels are covered by UiIconMappingTest as well.
indirect_roles=re.findall(r'\baddRoundAction\s*\(\s*"(?:[^"\\]|\\.)*"\s*,\s*"([^"]+)"',
                         hifz)
check(len(indirect_roles)>=8, "Hifz round-action call-site inventory reduced")
check(all(role in roles for role in indirect_roles), "unmapped indirect Hifz action")
check(len(screens)>=10 and all_actions>=60, "screen inventory unexpectedly shrank")
# All contracted action types are explicit in the central mapper, even where
# their screen is hidden until Hifz progresses.
expected = {
    "Accueil":"ic_ui_home","Lecture":"ic_ui_reading","Carte":"ic_ui_semantic_map",
    "Afficher les amorces":"ic_ui_recall_key","Tafsir":"ic_ui_tafsir",
    "Focus":"ic_ui_focus","Révision":"ic_ui_revision",
    "Difficulté":"ic_ui_difficulty","Annoter":"ic_ui_edit",
    "Quiz":"ic_ui_quiz","Progression":"ic_ui_progress_map",
    "Reprendre":"ic_ui_resume","Retour":"ic_ui_back",
    "Valider":"ic_ui_validate","Fermer":"ic_ui_close",
    "Annuler":"ic_ui_undo","Effacer":"ic_ui_annotation_erase",
    "Référence":"ic_ui_info","Révéler":"ic_ui_reveal",
    "Enregistrer":"ic_ui_record","Écouter":"ic_ui_play",
    "Pause":"ic_ui_pause","Plus d’options":"ic_ui_more",
    "Supprimer la plage":"ic_ui_annotation_erase",
    "Retirer le repère":"ic_ui_annotation_erase",
}
check(all(roles.get(role)==image for role,image in expected.items()),
      "contract icon role drift in C22 fixture")
for line in ('if (s.contains("supprimer")) return R.drawable.ic_ui_annotation_erase;',
             'if (s.contains("annuler")) return R.drawable.ic_ui_undo;',
             'if (s.contains("effacer")) return R.drawable.ic_ui_annotation_erase;'):
    check(line in UI, "implementation mismatch for "+line)
print("PASS J6 source: %d contracted vectors, %d compact actions, %d literal"
      " call sites, %d explicitly verified dynamic exceptions, %d screens."
      % (len(icons), all_actions, literal_actions, len(dynamic), len(screens)))
print("NOTE: Separate non-contract legacy navigation/category icons remain; "
      "not a physical BOOX image/gesture certificate. No APK.")
