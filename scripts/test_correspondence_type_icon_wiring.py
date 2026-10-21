#!/usr/bin/env python3
"""J4 public source test: eight relation types, eight intact icons and safe runtime.
No private dossiers and no production catalogue are introduced.
Run: python3 scripts/test_correspondence_type_icon_wiring.py
"""
import hashlib
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview"
SVG = ROOT / "docs/correspondences/icons_contractual_8"
DRAWABLE = ROOT / "hifz-app/src/main/res/drawable"
CONTRACT = {
    "REPETITION": ("repetition", "01_repetition.svg"),
    "RESSEMBLANCE": ("ressemblance", "02_ressemblance.svg"),
    "OPPOSITION": ("opposition", "03_opposition.svg"),
    "PROGRESSION": ("progression", "04_progression.svg"),
    "ALTERNANCE": ("alternance", "05_alternance.svg"),
    "INVERSION": ("inversion", "06_inversion.svg"),
    "CENTRE": ("centre", "07_centre.svg"),
    "DEBUT_FIN_INCLUSIO": ("debut_fin_inclusio", "08_debut_fin_inclusio.svg"),
}

class CorrespondenceTypeIconWiringTest(unittest.TestCase):
    def test_eight_types_and_exact_drawable_wiring(self):
        model = (JAVA / "CorrespondenceModel.java").read_text(encoding="utf-8")
        enum = re.search(r"public enum Type\s*\{([^}]+)\}", model)
        self.assertIsNotNone(enum)
        self.assertEqual(set(CONTRACT), set(re.findall(r"[A-Z][A-Z_]+", enum.group(1))))
        ui = (JAVA / "CorrespondenceScreen.java").read_text(encoding="utf-8")
        method = re.search(r"static int icon\(CorrespondenceModel\.Type type\)\s*\{(.*?)\n \}", ui, re.S)
        self.assertIsNotNone(method)
        pairs = re.findall(r"case\s+([A-Z_]+)\s*:\s*return\s+R\.drawable\.ic_corr_relation_([a-z_]+)\s*;", method.group(1))
        self.assertEqual(len(CONTRACT), len(pairs))
        self.assertEqual({key: value[0] for key, value in CONTRACT.items()}, dict(pairs))
        for suffix, _ in CONTRACT.values():
            self.assertTrue((DRAWABLE / ("ic_corr_relation_" + suffix + ".xml")).is_file())

    def test_unapproved_historical_rows_never_borrow_certified_icons(self):
        directory = (JAVA / "CorrespondenceDirectoryActivity.java").read_text(encoding="utf-8")
        # A proposed classification is not a certified relation or icon.
        self.assertIn("CorrespondenceModel.Type iconType=approved==null?null:approved.type;", directory)
        self.assertIn("icon.setImageResource(type==null?R.drawable.ic_ui_info:CorrespondenceScreen.icon(type))", directory)
        # A dossier with no valid original references must not offer a dead QCF action.
        self.assertIn("if(refs!=null&&refs.length()>0&&refs.length()<=40)", directory)

    def test_historical_qcf_carousel_keeps_per_reference_viewport(self):
        activity = (JAVA / "CorrespondenceReferenceActivity.java").read_text(encoding="utf-8")
        self.assertIn("rememberVisibleFolios();", activity)
        self.assertIn("position=next;expanded=-1;showReferences();", activity)
        self.assertIn("surface.show(focus,remembered.get(position+i));", activity)
        self.assertIn("if(state!=null)restoreFolioStates(state);", activity)
        self.assertIn('state.putBundle("folios",folios);', activity)
        self.assertIn('state.putInt("expanded",expanded);', activity)
        self.assertIn("Float.isFinite(zoom)&&zoom>0f&&zoom<=10f&&x>=0&&y>=0", activity)

    def test_original_eight_svgs_have_unchanged_manifest_hash(self):
        hashes = dict(re.findall(r"(\S+\.svg)\s+SHA256\s+([a-f0-9]{64})",
                                 (SVG / "MANIFEST_SHA256.txt").read_text(encoding="utf-8")))
        self.assertEqual({value[1] for value in CONTRACT.values()}, set(hashes))
        for _, filename in CONTRACT.values():
            self.assertEqual(hashes[filename], hashlib.sha256((SVG / filename).read_bytes()).hexdigest())

    def test_ui_preserves_internal_icon_ink_and_runtime_fail_closed(self):
        entry = (JAVA / "CorrespondenceEntryPoints.java").read_text(encoding="utf-8")
        screen = (JAVA / "CorrespondenceScreen.java").read_text(encoding="utf-8")
        runtime = (JAVA / "CorrespondenceRuntime.java").read_text(encoding="utf-8")
        self.assertIn("CorrespondenceScreen.icon(r.type)", entry)
        self.assertIn("button.setImageTintList(null)", entry)
        self.assertIn("symbol.setImageTintList(null)", screen)
        pin = re.search(r'APPROVED_SHA256\s*=\s*"([^"]*)"', runtime)
        self.assertIsNotNone(pin)
        if pin.group(1):
            self.assertRegex(pin.group(1), r"^[a-f0-9]{64}$")
            # The approved catalogue is a PRIVATE OFFLINE overlay, not a public
            # source asset. This source test must not force its publication.
            public_catalogue = ROOT / "hifz-app/src/main/assets/correspondences/approved_v1.json"
            self.assertFalse(public_catalogue.exists(), "Private correspondence rows leaked to GitHub")
            self.assertIn('static final String ASSET="correspondences/approved_v1.json"', runtime)
            self.assertIn("CorrespondenceCatalog.parse(bytes,APPROVED_SHA256,geometry)", runtime)
        else:
            self.assertIn("APPROVED_SHA256.isEmpty()", runtime)

if __name__ == "__main__":
    unittest.main()
