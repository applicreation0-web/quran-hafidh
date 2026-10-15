#!/usr/bin/env python3
"""Verify approved Lucide geometry independently from whole-file fingerprints.

The digests below were produced from the pinned SVG geometry after review, not
from Git blob bytes. They cover pathData plus stroke/fill/cap/join attributes.
The four historical originals without an external approved SVG remain outside
this automatic silhouette verdict and must be reported NON_CERTIFIABLE.
"""
import csv
import hashlib
import pathlib
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "hifz-app/src/main/res/drawable"
MANIFEST = ROOT / "docs/FROZEN_UI_ICONS_13_PLUS_10.tsv"
A = "{http://schemas.android.com/apk/res/android}"
PIN = "f93beca5957187620e30ae771d220d88501be621"
APPROVED = {
    "ic_ui_home.xml": "075fef82ef28951d424364a03c780f69eed7317636dce17eed8df1be17d21e79",
    "ic_ui_recall_key.xml": "2b54255d7f1e763e565f4572020f80ac372dca5a534ed2e72322babc627f0354",
    "ic_ui_semantic_map.xml": "1324709d9daf48750193221e9785d73ba25b02917a345966b202f0e7395a5a24",
    "ic_ui_focus.xml": "986142e0e21c65c88c9329a650e5a7175fde99daaa6cede32330506b676577a7",
    "ic_ui_revision.xml": "270e67099393a0a6b3ebdf119d1c5ca07a12ec3111eb9d0a48b531652a97ff05",
    "ic_ui_difficulty.xml": "b1d45d0d1cf89f441984c1ec818ec5d9d52e2b9f61a05551ae521c21974f726c",
    "ic_ui_quiz.xml": "a88d987cfaec9189e22b436ba9835f12dff2e8248b15cc1de30490f7e1547f4c",
    "ic_ui_progress_map.xml": "c0a07f03aaf784ad7e6ab023b038f767493be21e1c33b2c05a4f1e807c56994b",
    "ic_ui_resume.xml": "79455fb0b03ebfee4ae342a0f256aba4855a52eddce88acd83bdeea2b6034f26",
    "ic_ui_back.xml": "579895b438caf48a115f02da49ab770d29237d7d55d73c45d8a067268408aebb",
    "ic_ui_validate.xml": "2f590f750fbcab6b1417ec7ed2c0d5617b2f583993f7be719a88713bca5445ef",
    "ic_ui_close.xml": "13d179963f2fe90cac85a5faa978e043dfb19ebf90ea0284838d48fa88674dea",
    "ic_ui_undo.xml": "c46dfca59d735617d0e13c64d7eed73ed9b163d2cfc6cd9ac2a2f5bd20adfd3d",
    "ic_ui_annotation_erase.xml": "c4ab05e891d7f63426f52ce475afcafc381809002f132036bdffc878af515ec5",
    "ic_ui_info.xml": "8c38ed8efd1251e867ac1d8f259fa9b9ec759792989d8ea7b28e2d7997665f50",
    "ic_ui_reveal.xml": "5d58577de9ee9ef4cfd7525b013b9e8e564ca0e99221183329a5b5179cb16364",
    "ic_ui_record.xml": "7fd038206cdf5fca05fc4fcb520584934bd3f4f8239d421a3493352a2cb9a7ab",
    "ic_ui_play.xml": "99ff7497af95799f9ff78b32ab493bf87d2e8d530512b321f256ad075968c638",
    "ic_ui_pause.xml": "9f2e7f3585fd23c2ab0ee130a67964fd86a04d1df16d2d4f01301b13fd5313dc",
    "ic_ui_more.xml": "55123d791d90cd03af73b204e7b353a986b9382662407cc835b198e4e1598982",
}


def geometry_digest(path):
    root = ET.parse(path).getroot()
    pieces = []
    for item in root.findall("path"):
        pieces.append("|".join(item.get(A + key, "") for key in (
            "pathData", "strokeWidth", "strokeLineCap", "strokeLineJoin",
            "fillColor", "strokeColor")))
    return hashlib.sha256("\n".join(pieces).encode()).hexdigest()


rows = list(csv.reader((line for line in MANIFEST.read_text(encoding="utf-8").splitlines()
                        if line and not line.startswith("#")), delimiter="\t"))
lucide = {row[3] for row in rows if row[5].startswith("lucide:")}
if lucide != set(APPROVED):
    raise SystemExit("SILHOUETTE FAIL: approved Lucide inventory changed")
for filename, expected in APPROVED.items():
    actual = geometry_digest(DRAWABLE / filename)
    if actual != expected:
        raise SystemExit(f"SILHOUETTE FAIL: {filename}: {actual} != {expected}")

key = ET.parse(DRAWABLE / "ic_ui_recall_key.xml").getroot().findall("path")
if len(key) != 2 or not key[0].get(A + "pathData", "").startswith("M2.586 17.414"):
    raise SystemExit("SILHOUETTE FAIL: approved key-round body/keyhole missing")
if key[0].get(A + "strokeWidth") != "2":
    raise SystemExit("SILHOUETTE FAIL: approved key-round 2-unit stroke missing")
if key[1].get(A + "fillColor") != "#121211" or key[1].get(A + "strokeWidth") != "2":
    raise SystemExit("SILHOUETTE FAIL: approved stroked keyhole missing")

manual = [row[3] for row in rows if row[5] == "frozen-original"]
if set(manual) != {"ic_ui_reading.xml", "ic_ui_tafsir.xml", "ic_ui_edit.xml"}:
    raise SystemExit("SILHOUETTE FAIL: manual-review inventory changed")
print(f"PASS: 20 approved Lucide silhouettes match pinned geometry {PIN}; "
      "3 historical originals remain NON_CERTIFIABLE without approved references")
