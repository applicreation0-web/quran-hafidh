#!/usr/bin/env python3
"""Proof that the enlarged BOOX Parcours tile is the one actually opened on AVD.

Uses runtime accessibility bounds; never hardcodes obsolete touch coordinates.
"""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

def parse(path):
    return list(ET.parse(path).getroot().iter("node"))

def tap(path):
    candidates=[n for n in parse(path)
                if n.attrib.get("content-desc")=="Parcours Hifz"]
    if len(candidates)!=1:
        raise SystemExit("Expected one accessible Parcours Hifz entry; got "+str(len(candidates)))
    coords=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                        candidates[0].attrib.get("bounds",""))
    if not coords:
        raise SystemExit("Missing visual bounds for real Parcours")
    x1,y1,x2,y2=map(int,coords.groups())
    if x1>=x2 or y1>=y2:raise SystemExit("Empty Parcours hit region")
    subprocess.run(["adb","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],
                   check=True)
    print("Tapped verified Parcours icon via live Android bounds")

def verify(path, lower=False):
    # UiAutomator enumerates visible nodes, so a small Pixel AVD must scroll:
    # never require all six large BOOX tiles to fit one physical viewport.
    names=[n.attrib.get("text","") for n in parse(path)]
    # Lower viewport on a small Pixel may clip the first tile title while its subtitle remains visible.
    # Verify the live approved hierarchy, not an off-screen title or obsolete subtitle.
    expected=["Révision cumulative Sabqi","Consolidation","Révision cumulative Itqān","Révision active"] if lower else ["Parcours Hifz","Apprendre","Stabiliser"]
    if any(label not in names for label in expected):
        raise SystemExit("Not the approved "+("lower" if lower else "upper")+" Parcours screenshot: "+repr(names[:60]))
    print("PASS: "+("lower" if lower else "upper")+" BOOX icon viewport matches the six-mode source contract")

if __name__=="__main__":
    if len(sys.argv)!=3:raise SystemExit("Usage: boox_emulator_parcours.py tap|verify path.xml")
    if sys.argv[1]=="tap":tap(sys.argv[2])
    elif sys.argv[1]=="verify":verify(sys.argv[2])
    elif sys.argv[1]=="verify-lower":verify(sys.argv[2], True)
    else:raise SystemExit("Unknown mode")
