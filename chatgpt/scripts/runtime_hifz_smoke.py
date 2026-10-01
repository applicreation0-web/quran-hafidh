#!/usr/bin/env python3
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
    result = subprocess.run(list(args), text=True,
                            stdout=subprocess.PIPE if capture else None,
                            stderr=subprocess.STDOUT if capture else None)
    if check and result.returncode != 0:
        raise RuntimeError(f"Command failed ({result.returncode}): {' '.join(args)}\n{result.stdout or ''}")
    return result.stdout or ""


def adb(*args, **kwargs): return run("adb", *args, **kwargs)


def dump_ui(name):
    remote=f"/sdcard/{name}.xml"; local=EVIDENCE/f"{name}.xml"
    diagnostics=[]
    for attempt in range(1,5):
        local.unlink(missing_ok=True)
        adb("shell","rm","-f",remote,check=False)
        output=adb("shell","uiautomator","dump","--compressed",remote,check=False)
        diagnostics.append(f"attempt={attempt}: {output.strip()}")
        adb("pull",remote,str(local),check=False)
        if local.is_file() and local.stat().st_size>32:
            try:
                return ET.parse(local).getroot()
            except ET.ParseError as error:
                diagnostics.append(f"parse={error}")
        time.sleep(0.8)
    raise AssertionError("UI dump unavailable after 4 attempts. " + " | ".join(diagnostics))


def all_text(root): return [n.attrib.get("text","") for n in root.iter("node") if n.attrib.get("text","")]


def all_visible_strings(root):
    vals=[]
    for n in root.iter("node"):
        for attr in ("text","content-desc"):
            value=n.attrib.get(attr,"")
            if value: vals.append(value)
    return vals


def require_text(root, expected, *, contains=False):
    texts=all_text(root)
    if not any((expected in t) if contains else (t==expected) for t in texts):
        raise AssertionError(f"Missing UI text {expected!r}. Visible texts: {texts}")


def reject_text(root, expected, *, contains=False):
    texts=all_text(root)
    if any((expected in t) if contains else (t==expected) for t in texts):
        raise AssertionError(f"Unexpected UI text {expected!r}. Visible texts: {texts}")


def require_content_desc(root, expected):
    descriptions=[n.attrib.get("content-desc","") for n in root.iter("node") if n.attrib.get("content-desc","")]
    if expected not in descriptions:
        raise AssertionError(f"Missing semantic Mushaf marker {expected!r}. Content descriptions: {descriptions}")


def require_enabled(root, text, expected):
    for node in root.iter("node"):
        if node.attrib.get("text")==text:
            actual=node.attrib.get("enabled","false").lower()=="true"
            if actual!=expected: raise AssertionError(f"{text!r} enabled={actual}, expected {expected}")
            return
    raise AssertionError(f"Cannot inspect missing UI text: {text!r}")


def reject_webview_error(root):
    joined="\n".join(all_visible_strings(root))
    forbidden=("Webpage not available","ERR_HTTP_","ERR_FAILED","could not be loaded","Erreur Mushaf","Mushaf indisponible")
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
    image=Image.open(local).convert("L"); w,h=image.size
    crop=image.crop((int(w*.04),int(h*.06),int(w*.96),int(h*.94)))
    stat=ImageStat.Stat(crop); std=stat.stddev[0]; pixels=list(crop.getdata()); dark_ratio=sum(1 for p in pixels if p<110)/max(1,len(pixels))
    if std<8.0 or dark_ratio<.002: raise AssertionError(f"Screenshot {name} looks blank/empty: std={std:.2f}, dark_ratio={dark_ratio:.5f}")
    (EVIDENCE/f"{name}-metrics.txt").write_text(f"width={w}\nheight={h}\ncenter_stddev={std:.4f}\ncenter_dark_ratio={dark_ratio:.6f}\n",encoding="utf-8")


def assert_foreground():
    activities=adb("shell","dumpsys","activity","activities"); (EVIDENCE/"activities.txt").write_text(activities,encoding="utf-8")
    if PACKAGE not in activities: raise AssertionError(f"{PACKAGE} is not present in current activity stack")


def wait_ui(seconds=1.2): time.sleep(seconds); adb("shell","input","keyevent","KEYCODE_WAKEUP",check=False)


def main():
    if len(sys.argv)!=2: raise SystemExit("usage: runtime_hifz_smoke.py <apk>")
    apk=Path(sys.argv[1])
    if not apk.is_file() or apk.stat().st_size==0: raise SystemExit(f"APK not found: {apk}")

    adb("wait-for-device"); adb("install","-r",str(apk)); adb("logcat","-c",check=False)
    launch=adb("shell","am","start","-W","-n",f"{PACKAGE}/{ACTIVITY}"); (EVIDENCE/"launch.txt").write_text(launch,encoding="utf-8")
    if "Status: ok" not in launch and "Complete" not in launch: raise AssertionError(f"Launch was not reported successful:\n{launch}")
    wait_ui(2.0); assert_foreground()

    home=dump_ui("home")
    for expected in ("Quran Hifz","Parcours Hifz · séance du jour","Lecture / Étude · Tafsir","Mémorisation libre","Paramètres"):
        require_text(home,expected)
    screenshot("home")

    tap_text(home,"Lecture / Étude · Tafsir"); wait_ui(1.0)
    study=dump_ui("study-page1")
    require_text(study,"Lecture / Étude ·",contains=True); require_text(study,"Tafsir"); require_content_desc(study,"Mushaf page 1"); reject_webview_error(study)
    screenshot("study-page1")
    tap_text(study,"Page ›"); wait_ui(1.0)
    study2=dump_ui("study-page2"); require_content_desc(study2,"Mushaf page 2"); reject_webview_error(study2)
    tap_text(study2,"‹ Page"); wait_ui(1.0)
    study1b=dump_ui("study-page1-return"); require_content_desc(study1b,"Mushaf page 1"); screenshot("study")
    adb("shell","input","keyevent","KEYCODE_BACK"); wait_ui(1.0)

    home_again=dump_ui("home-again"); tap_text(home_again,"Parcours Hifz · séance du jour"); wait_ui(1.2)
    sabqi=dump_ui("sabqi-before")
    require_text(sabqi,"Sabqi"); require_text(sabqi,"5 lignes réelles · 37 répétitions",contains=True); require_text(sabqi,"Répétition faite")
    require_enabled(sabqi,"‹ Page",False); require_enabled(sabqi,"Page ›",False); reject_webview_error(sabqi)
    tap_text(sabqi,"Répétition faite"); tap_text(sabqi,"Répétition faite"); wait_ui(1.0)
    sabqi_after=dump_ui("sabqi-after-doubletap")
    require_text(sabqi_after,"Répétition suivante : 2 / 37",contains=True)
    reject_text(sabqi_after,"Répétition suivante : 3 / 37",contains=True)
    screenshot("sabqi"); assert_foreground()

    package_dump=adb("shell","dumpsys","package",PACKAGE); (EVIDENCE/"package.txt").write_text(package_dump,encoding="utf-8")
    logcat=adb("logcat","-d","-v","threadtime"); (EVIDENCE/"logcat.txt").write_text(logcat,encoding="utf-8")
    if any(pattern in logcat for pattern in (f"ANR in {PACKAGE}",f"Process: {PACKAGE}")):
        excerpt="\n".join(line for line in logcat.splitlines() if PACKAGE in line or "FATAL EXCEPTION" in line)
        raise AssertionError(f"Crash/ANR evidence found in logcat:\n{excerpt[-12000:]}")

    print("PASS Quran Hifz runtime smoke")
    print("- fresh install + launch")
    print("- Mushaf page 1 -> 2 -> 1 bridge proof")
    print("- Sabqi passage navigation locked")
    print("- double tap counted once")
    print("- screenshots non-blank and no crash/ANR signature")


if __name__=="__main__": main()
