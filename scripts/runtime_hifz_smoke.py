#!/usr/bin/env python3
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image, ImageStat

PACKAGE = "com.quransafeguard.hifz.installtest1"
ACTIVITY = "com.quransafeguard.hifz.preview.MainActivity"
EVIDENCE = Path("runtime-evidence")
EVIDENCE.mkdir(parents=True, exist_ok=True)


def run(*args, check=True, capture=True):
    result = subprocess.run(list(args), text=True, stdout=subprocess.PIPE if capture else None, stderr=subprocess.STDOUT if capture else None)
    if check and result.returncode != 0:
        raise RuntimeError(f"Command failed ({result.returncode}): {' '.join(args)}\n{result.stdout or ''}")
    return result.stdout or ""


def adb(*args, **kwargs): return run("adb", *args, **kwargs)


def dump_ui(name):
    remote = f"/sdcard/{name}.xml"; local = EVIDENCE / f"{name}.xml"
    adb("shell", "uiautomator", "dump", remote); adb("pull", remote, str(local))
    return ET.parse(local).getroot()


def all_text(root): return [n.attrib.get("text", "") for n in root.iter("node") if n.attrib.get("text", "")]

def all_visible_strings(root):
    vals=[]
    for n in root.iter("node"):
        for a in ("text","content-desc"):
            v=n.attrib.get(a,"")
            if v: vals.append(v)
    return vals


def require_text(root, expected, *, contains=False):
    texts=all_text(root)
    if not any((expected in t) if contains else (t==expected) for t in texts): raise AssertionError(f"Missing UI text {expected!r}. Visible texts: {texts}")


def require_content_desc(root, expected):
    ds=[n.attrib.get("content-desc","") for n in root.iter("node") if n.attrib.get("content-desc","")]
    if expected not in ds: raise AssertionError(f"Missing semantic Mushaf marker {expected!r}. Content descriptions: {ds}")


def reject_webview_error(root):
    joined="\n".join(all_visible_strings(root)); forbidden=("Webpage not available","ERR_HTTP_","ERR_FAILED","could not be loaded","Erreur de chargement du Mushaf")
    hits=[v for v in forbidden if v.lower() in joined.lower()]
    if hits: raise AssertionError(f"WebView error content is visible ({hits}). UI strings: {all_visible_strings(root)}")


def parse_bounds(value):
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",value or "")
    if not m: raise ValueError(f"Malformed bounds: {value!r}")
    x1,y1,x2,y2=map(int,m.groups()); return (x1+x2)//2,(y1+y2)//2


def tap_text(root, expected):
    for n in root.iter("node"):
        if n.attrib.get("text")==expected:
            x,y=parse_bounds(n.attrib.get("bounds")); adb("shell","input","tap",str(x),str(y)); return
    raise AssertionError(f"Cannot tap missing UI text: {expected!r}")


def screenshot(name):
    remote=f"/sdcard/{name}.png"; local=EVIDENCE/f"{name}.png"
    adb("shell","screencap","-p",remote); adb("pull",remote,str(local))
    image=Image.open(local).convert("L"); w,h=image.size; crop=image.crop((int(w*.04),int(h*.06),int(w*.96),int(h*.94)))
    stat=ImageStat.Stat(crop); std=stat.stddev[0]; pixels=list(crop.getdata()); dark_ratio=sum(1 for p in pixels if p<110)/max(1,len(pixels))
    if std<8.0 or dark_ratio<.002: raise AssertionError(f"Screenshot {name} looks blank/empty: std={std:.2f}, dark_ratio={dark_ratio:.5f}")
    (EVIDENCE/f"{name}-metrics.txt").write_text(f"width={w}\nheight={h}\ncenter_stddev={std:.4f}\ncenter_dark_ratio={dark_ratio:.6f}\n",encoding="utf-8")


def assert_foreground():
    activities=adb("shell","dumpsys","activity","activities"); (EVIDENCE/"activities.txt").write_text(activities,encoding="utf-8")
    if PACKAGE not in activities: raise AssertionError(f"{PACKAGE} is not present in current activity stack")


def wait_ui(seconds=3): time.sleep(seconds); adb("shell","input","keyevent","KEYCODE_WAKEUP",check=False)


def main():
    if len(sys.argv)!=2: raise SystemExit("usage: runtime_hifz_smoke.py <apk>")
    apk=Path(sys.argv[1])
    if not apk.is_file() or apk.stat().st_size==0: raise SystemExit(f"APK not found: {apk}")
    adb("wait-for-device"); adb("install","-r",str(apk)); adb("logcat","-c",check=False)
    launch=adb("shell","am","start","-W","-n",f"{PACKAGE}/{ACTIVITY}"); (EVIDENCE/"launch.txt").write_text(launch,encoding="utf-8")
    if "Status: ok" not in launch and "Complete" not in launch: raise AssertionError(f"Launch was not reported successful:\n{launch}")
    wait_ui(); assert_foreground()
    home=dump_ui("home")
    for expected in ("Quran Hifz TEST","Lecture / Étude · Tafsir","Mémorisation libre","Sabqi · 5 lignes · 37 répétitions","Itqān · cycle sans fin · ×30","Murājaʿah · révision en blocs de versets"): require_text(home,expected)
    screenshot("home")
    tap_text(home,"Lecture / Étude · Tafsir"); wait_ui(); study=dump_ui("study")
    require_text(study,"Lecture / Étude ·",contains=True); require_text(study,"Tafsir"); require_text(study,"‹ Page"); require_text(study,"Page ›"); reject_webview_error(study); require_content_desc(study,"Mushaf page 1"); screenshot("study")
    adb("shell","input","keyevent","KEYCODE_BACK"); wait_ui(1)
    home_again=dump_ui("home-again"); tap_text(home_again,"Sabqi · 5 lignes · 37 répétitions"); wait_ui(); sabqi=dump_ui("sabqi")
    require_text(sabqi,"Sabqi"); require_text(sabqi,"5 lignes réelles · 37 répétitions",contains=True); require_text(sabqi,"Répétition faite"); reject_webview_error(sabqi); screenshot("sabqi"); assert_foreground()
    package_dump=adb("shell","dumpsys","package",PACKAGE); (EVIDENCE/"package.txt").write_text(package_dump,encoding="utf-8")
    logcat=adb("logcat","-d","-v","threadtime"); (EVIDENCE/"logcat.txt").write_text(logcat,encoding="utf-8")
    if any(p in logcat for p in (f"ANR in {PACKAGE}",f"Process: {PACKAGE}")):
        excerpt="\n".join(line for line in logcat.splitlines() if PACKAGE in line or "FATAL EXCEPTION" in line); raise AssertionError(f"Crash/ANR evidence found in logcat:\n{excerpt[-12000:]}")
    print("PASS Quran Hifz TEST runtime smoke")

if __name__=="__main__": main()
