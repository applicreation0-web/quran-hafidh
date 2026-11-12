#!/usr/bin/env python3
"""Capture ACTUAL Android UI from an emulator at BOOX-like sizes; never a mockup."""
from __future__ import annotations
import os, re, subprocess, time
import xml.etree.ElementTree as ET
from pathlib import Path

OUTPUT = Path("boox-real-renders")
OUTPUT.mkdir(exist_ok=True)
PACKAGE = "com.quransafeguard.hifz.ibnkathirtest"
TITLES = ("Apprendre", "Renforcement", "Stabiliser",
          "Consolidation", "Révision active", "Révision passive")
def adb(*args: str, capture: bool=False):
    return subprocess.run(("adb", *args), check=True,
                          stdout=subprocess.PIPE if capture else None,
                          stderr=subprocess.PIPE if capture else None)
def hierarchy():
    last = None
    for _ in range(6):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/boox-ui.xml", capture=True)
            raw = adb("exec-out", "cat", "/sdcard/boox-ui.xml", capture=True).stdout
            root = ET.fromstring(raw)
            return root, raw
        except (subprocess.CalledProcessError, ET.ParseError) as exc:
            last = exc
            time.sleep(2)
    raise RuntimeError("Could not inspect live emulator hierarchy") from last
def visible(node):
    return node.attrib.get("text","") + " " + node.attrib.get("content-desc","")
def bounds(node):
    v = node.attrib.get("bounds", "")
    m = re.fullmatch(r"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]", v)
    return tuple(int(p) for p in m.groups()) if m else None
def tap_parcours():
    for _ in range(10):
        root, _ = hierarchy()
        for node in root.iter("node"):
            if "Parcours" == node.attrib.get("content-desc","") and bounds(node):
                x1,y1,x2,y2 = bounds(node)
                adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))
                return
        time.sleep(2)
    raise RuntimeError("Parcours tile not accessible on actual home")
def capture(tag, size, density, font):
    adb("shell","wm","size",size)
    adb("shell","wm","density",str(density))
    adb("shell","settings","put","system","font_scale",str(font))
    adb("shell","am","force-stop",PACKAGE)
    adb("shell","monkey","-p",PACKAGE,"1",capture=True)
    time.sleep(13)
    tap_parcours()
    time.sleep(13)
    root, xml = hierarchy()
    texts = [visible(n) for n in root.iter("node")]
    missing = [title for title in TITLES if not any(title in t for t in texts)]
    if missing:
        raise AssertionError("Missing BOOX tile(s): "+repr(missing))
    if any(n.attrib.get("class")=="android.widget.ScrollView"
           for n in root.iter("node")):
        raise AssertionError("BOOX Parcours still contains a ScrollView")
    width,height = (int(v) for v in size.split("x"))
    offscreen=[]
    for node in root.iter("node"):
        if any(t in visible(node) for t in TITLES):
            box=bounds(node)
            if box is not None and (box[0]<0 or box[1]<0 or box[2]>width or box[3]>height):
                offscreen.append((visible(node),box))
    if offscreen:
        raise AssertionError("Tiles out of physical bounds: "+repr(offscreen))
    (OUTPUT/(tag+".xml")).write_bytes(xml)
    (OUTPUT/(tag+".png")).write_bytes(adb("exec-out","screencap","-p",capture=True).stdout)
    print(tag, "ACTUAL emulator capture", size, "density", density,
          "font", font, "all six tiles visible, no scroll container", flush=True)

if __name__ == "__main__":
    apk = list(Path("hifz-app/build/outputs/apk/debug").glob("*.apk"))
    if not apk:
        raise RuntimeError("No Hifz debug APK built; no screenshot can be certified")
    adb("install","-r",str(apk[0]))
    capture("boox-1404x1872", "1404x1872", 240, 1.0)
    capture("boox-900x1200-font125", "900x1200", 240, 1.25)
