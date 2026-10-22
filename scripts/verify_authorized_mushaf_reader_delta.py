#!/usr/bin/env python3
"""Fail-closed exception for the reviewed v1.17.2 WebView lifecycle correction.

All other frozen renderer and Hifz changes remain rejected. This authorizes
only EXACT source bytes, not release readiness or a claim of Android test PASS.
"""
from pathlib import Path
import subprocess

REPO=Path(__file__).resolve().parents[1]
ORIGINAL_COMMIT="178972e0b59f1bc7336c3e2ee4eeafd7c20ad853"
PATH_JAVA="hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java"
PATH_JS="hifz-app/src/main/assets/hifzreader/reader.js"
ORIGINAL_JAVA="4473cf97fcfd080af08b0bd9790883bedb6901c4"
APPROVED_JAVA="e151e2b0b0220883ea0ec9380fac1abc3a743d91"
APPROVED_JS="7961f9a8601cd456275d9acad86ccf46a05d2806"

def git(*args):
    return subprocess.check_output(["git",*args],cwd=REPO,text=True).strip()

if git("rev-parse",f"{ORIGINAL_COMMIT}:{PATH_JAVA}") != ORIGINAL_JAVA:
    raise SystemExit("FAIL: independent pre-fix MushafView baseline changed")
if git("rev-parse","6a923350384348332bd59de2ec985bcfa4ff0ba2:"+PATH_JAVA)!=APPROVED_JAVA:
    raise SystemExit("FAIL: approved atomic first-paint source is not pinned")
if git("hash-object",PATH_JAVA)!=APPROVED_JAVA:
    raise SystemExit("FAIL: MushafView differs from reviewed pending-FIFO/generation patch")
if git("hash-object",PATH_JS)!=APPROVED_JS:
    raise SystemExit("FAIL: reader.js differs from reviewed JS-generation signalling patch")
src=(REPO/PATH_JAVA).read_text(encoding="utf-8")
js=(REPO/PATH_JS).read_text(encoding="utf-8")
for symbol in (
    "private final ArrayDeque<Runnable> pendingActions = new ArrayDeque<>();",
    "if (generation != renderGeneration || page != requestedPage) return;",
    "if (generation != renderGeneration) return;",
    "pendingActions.addLast(action);",
    '.put("renderGeneration", currentGeneration)',
    "public void showFreeActivePreparedPage(",
    "atomicFreeMaskBoot = exactGeometryReady;",
    "#mushaf{visibility:hidden}",
    "prepared.querySelector('#hifz-exact-word-holes')",
):
    if symbol not in src: raise SystemExit("FAIL: renderer guard missing "+symbol)
for symbol in ("N?.pageShown(currentPage,Number(boot.renderGeneration||0));",
               "N?.ready(Number(boot.renderGeneration||0));"):
    if symbol not in js: raise SystemExit("FAIL: JS bridge guard missing "+symbol)
print("PASS: only exact pre-reviewed WebView FIFO/generation renderer bytes authorized")
