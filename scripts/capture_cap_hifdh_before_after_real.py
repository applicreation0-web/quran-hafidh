#!/usr/bin/env python3
"""Capture REAL Android BOOX home before and after projected An-Nas.
Only uses the installed debug application and its genuine forecast; no image
generation, drawing, synthetic credits, fake date or user data mutation.
"""
from __future__ import annotations
import re
import struct
import time
from pathlib import Path
from capture_hifz_boox_ui import (
    adb, hierarchy, names, bounds, find_control, tap, PACKAGE, ACTIVITY
)

OUT = Path("boox-cap-real-before-after")
OUT.mkdir(parents=True, exist_ok=True)

def read_graph(tag):
    root = hierarchy(tag, lambda root: find_control(
        root, lambda label: label.startswith(
            "Feuille de route prévisionnelle interactive ")) is not None)
    graph = find_control(root, lambda label: label.startswith(
        "Feuille de route prévisionnelle interactive "))
    assert graph is not None, "No real Cap Hifdh timeline"
    spoken = " ".join(names(graph))
    match = re.search(r"interactive (\d{4})", spoken)
    assert match, "Accessible year not found in actual Android timeline"
    assert "Acquisition, " in spoken, "Acquisition frise must be present"
    assert not any(n.get("scrollable") == "true"
                   for n in root.iter("node")), "BOOX home must never scroll"
    graph_bounds = bounds(graph)
    assert graph_bounds and graph_bounds[3] < 1872, "Graph clipped"
    return root, int(match.group(1)), spoken, graph_bounds

def screenshot(tag, root, year, spoken, rect):
    png = adb("exec-out", "screencap", "-p").stdout
    assert png.startswith(b"\x89PNG\r\n\x1a\n"), "Not an Android PNG"
    width, height = struct.unpack(">II", png[16:24])
    assert (width, height) == (1404, 1872), "Wrong emulator viewport"
    assert len(png) >= 70000, "Suspiciously blank screenshot"
    (OUT / f"{tag}.png").write_bytes(png)
    (OUT / f"{tag}.txt").write_text(
        "Real Android emulator screenshot\n"
        f"year={year}\n"
        f"graphBounds={rect}\n"
        f"accessibility={spoken}\n", encoding="utf-8")
    print(f"ACTUAL_SCREENSHOT {tag}: year={year}, size={width}x{height}, "
          f"bytes={len(png)}, bounds={rect}", flush=True)

def main():
    adb("shell", "wm", "size", "1404x1872")
    adb("shell", "wm", "density", "240")
    adb("shell", "settings", "put", "system", "font_scale", "1.5")
    adb("shell", "am", "force-stop", PACKAGE)
    launched = adb("shell", "am", "start", "-W", "-n", ACTIVITY, check=False)
    assert launched.returncode == 0, "Cannot launch real Android APK"
    time.sleep(3)

    before, year, spoken, rect = read_graph("cap-real-before-nas-viewport")
    assert "Maîtrise, " in spoken and "Objectif final An-Nās" in spoken, (
        "Before screenshot does not show both Acquisition and Maîtrise")
    screenshot("01-before-nas-acquisition-and-maitrise", before, year, spoken, rect)

    # Select the FIRST year in which the actual forecast removes the Maîtrise
    # frise following predicted An-Nās. Never fake its completion flag.
    initial_year = year
    after = None
    for step in range(25):
        next_year = find_control(before,
            lambda label: label == "Prévision : année suivante")
        assert next_year is not None, (
            f"No post-Nas year reachable from {year}; did not fabricate a capture")
        tap(next_year)
        time.sleep(.5)
        before, year, spoken, rect = read_graph(f"cap-real-nas-year-{step}")
        assert year > initial_year
        if "Maîtrise, " not in spoken:
            assert "Objectif final An-Nās" not in spoken
            assert "Entretien :" not in spoken, (
                "An-Nas must remove the second frise, not replace it")
            after = (before, year, spoken, rect)
            break
    assert after is not None, "Could not capture after projected An-Nās"
    time.sleep(.8)
    root, year, spoken, rect = after
    screenshot("02-after-nas-acquisition-only", root, year, spoken, rect)
    (OUT / "screenproof.txt").write_text(
        f"Both screenshots are real screenshots from the SAME Android debug APK.\n"
        f"Requested size: 1404x1872, Android font scale 150%.\n"
        f"Before: {initial_year} with two rails.\n"
        f"After: {year} with Acquisition only, no Entretien replacement.\n"
        f"Read-only forecast year selection; no earned credits are generated.\n",
        encoding="utf-8")
    print("SUCCESS: TWO REAL ANDROID BOOX CAP HIFDH SCREENSHOTS", flush=True)

if __name__ == "__main__":
    main()
