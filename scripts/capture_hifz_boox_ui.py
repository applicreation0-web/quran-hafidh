#!/usr/bin/env python3
"""Capture and verify the running Android app at BOOX-like viewport sizes."""
from __future__ import annotations

from datetime import date
import hashlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

OUTPUT = Path("boox-real-renders")
PACKAGE = "com.quransafeguard.hifz.ibnkathirtest"
ACTIVITY = PACKAGE + "/com.quransafeguard.hifz.preview.MainActivity"
DUMP = "/data/local/tmp/haafidh-boox-ui.xml"
TITLES = ("Apprendre", "Renforcement", "Stabiliser",
          "Consolidation", "Révision active", "Révision passive")


def adb(*args: str, check=True, timeout=30):
    return subprocess.run(("adb", *args), check=check, capture_output=True, timeout=timeout)


def diagnostics(tag):
    for suffix, args in (
        ("png", ("exec-out", "screencap", "-p")),
        ("activity.txt", ("shell", "dumpsys", "activity", "activities")),
        ("logcat.txt", ("logcat", "-d", "-t", "2000", "-v", "threadtime")),
    ):
        path = OUTPUT / f"{tag}.{suffix}"
        try:
            result = adb(*args, check=False)
            path.write_bytes(result.stdout if suffix == "png" else result.stdout + result.stderr)
        except (OSError, subprocess.SubprocessError) as exc:
            (OUTPUT / f"{tag}.{suffix}.error.txt").write_text(str(exc))


def hierarchy(tag, accept=lambda root: True):
    last = None
    for attempt in range(1, 5):
        raw = b""
        try:
            adb("shell", "rm", "-f", DUMP)
            dump = adb("shell", "uiautomator", "dump", DUMP, check=False, timeout=20)
            (OUTPUT / f"{tag}.dump-{attempt}.txt").write_bytes(dump.stdout + dump.stderr)
            if dump.returncode:
                raise ValueError(f"uiautomator exit status {dump.returncode}")
            raw = adb("exec-out", "cat", DUMP).stdout
            root = ET.fromstring(raw.decode("utf-8-sig").strip())
            if root.tag != "hierarchy" or not any(
                n.get("package") == PACKAGE for n in root.iter("node")
            ):
                raise ValueError("Accessibility tree does not contain the installed app")
            (OUTPUT / f"{tag}.xml").write_bytes(raw)
            if not accept(root):
                raise ValueError("Expected accessible control is not ready")
            return root
        except (ET.ParseError, ValueError, subprocess.SubprocessError) as exc:
            last = exc
            (OUTPUT / f"{tag}.dump-{attempt}.raw").write_bytes(raw)
            time.sleep(2)
    raise RuntimeError(f"Could not inspect Android hierarchy for {tag}: {last}") from last


def names(node):
    return node.get("text", ""), node.get("content-desc", "")


def bounds(node):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    return tuple(map(int, match.groups())) if match else None


def tap(node):
    x1, y1, x2, y2 = bounds(node)
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def find_control(root, match):
    return next((node for node in root.iter("node")
                 if node.get("package") == PACKAGE and node.get("clickable") == "true"
                 and node.get("enabled") == "true" and bounds(node)
                 and any(match(label) for label in names(node))), None)


def sunday_station(label):
    match = re.search(r"\b\d{4}-\d{2}-\d{2}\b", label)
    return bool(match and "ouvrir le détail" in label and date.fromisoformat(match[0]).weekday() == 6)


def capture_dialog(tag, root, control, expected):
    node = find_control(root, control)
    if node is None:
        raise AssertionError(f"Missing dialog control for {tag}")
    tap(node)
    root = hierarchy(tag, lambda tree: any(expected in names(n) for n in tree.iter("node")))
    diagnostics(tag)
    close = find_control(root, lambda label: label.lower() == "fermer")
    if close is None:
        raise AssertionError(f"Dialog has no close control: {tag}")
    tap(close)


def capture(tag, size, density, font):
    try:
        adb("shell", "wm", "size", size)
        adb("shell", "wm", "density", str(density))
        adb("shell", "settings", "put", "system", "font_scale", str(font))
        adb("shell", "am", "force-stop", PACKAGE)
        launch = adb("shell", "am", "start", "-W", "-n", ACTIVITY, check=False)
        (OUTPUT / f"{tag}.launch.txt").write_bytes(launch.stdout + launch.stderr)
        if launch.returncode or b"Status: ok" not in launch.stdout:
            raise RuntimeError("Launcher did not report a successful activity start")
        diagnostics(f"{tag}-home")
        home = hierarchy(f"{tag}-home", lambda tree: find_control(tree, lambda label: label == "Parcours") is not None)
        parcours = find_control(home, lambda label: label == "Parcours")
        tap(parcours)
        time.sleep(2)
        diagnostics(tag)
        root = hierarchy(tag)
        width, height = map(int, size.split("x"))
        for title in TITLES:
            aliases = {"Apprendre": ("Apprendre", "Apprentissage"),
                       "Stabiliser": ("Stabiliser", "Stabilisation")}.get(title, (title,))
            matches = [n for n in root.iter("node")
                       if n.get("package") == PACKAGE
                       and any(label == alias or label.startswith(alias + " ")
                               for label in names(n) for alias in aliases)]
            if not matches or not any(
                0 <= x1 < x2 <= width and 0 <= y1 < y2 <= height
                for x1, y1, x2, y2 in filter(None, map(bounds, matches))
            ):
                raise AssertionError(f"Missing or empty BOOX tile bounds: {title}")
        if any(n.get("package") == PACKAGE
               and (n.get("scrollable") == "true" or n.get("class") == "android.widget.ScrollView")
               for n in root.iter("node")):
            raise AssertionError("BOOX Parcours still contains a scrollable view")
        if not (OUTPUT / f"{tag}.png").read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
            raise AssertionError("ADB did not return a PNG")
        capture_dialog(f"{tag}-dimanche", root,
                       sunday_station, "Matin · détail")
        root = hierarchy(f"{tag}-after-day")
        capture_dialog(f"{tag}-bilan", root,
                       lambda label: label == "Ouvrir le bilan et les lectures cumulatives", "Bilan de la semaine")
        print(tag, size, f"font={font}", "six visible tiles, no scrollable view, day and balance dialogs", flush=True)
    except Exception:
        diagnostics(f"{tag}-failure")
        raise


if __name__ == "__main__":
    OUTPUT.mkdir(exist_ok=True)
    apks = sorted(Path("hifz-app/build/outputs/apk/debug").glob("*.apk"))
    if len(apks) != 1:
        raise RuntimeError(f"Expected one Hifz debug APK, found {len(apks)}")
    apk = apks[0]
    metadata = subprocess.check_output(("git", "rev-parse", "HEAD")).decode().strip()
    with apk.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    (OUTPUT / "build.txt").write_text(f"commit={metadata}\napk={apk.name}\nsha256={digest}\n")
    adb("install", "-r", str(apk), timeout=120)
    failures = []
    for size in ("1404x1872", "900x1200"):
        for font in (1.0, 1.25, 1.5):
            tag = f"boox-{size}-font{int(font * 100)}"
            try:
                capture(tag, size, 220 if size == "900x1200" else 240, font)
            except (OSError, ValueError, RuntimeError, AssertionError, subprocess.SubprocessError) as exc:
                failures.append(f"{tag}: {exc}")
                print(failures[-1], flush=True)
    if failures:
        raise SystemExit("\n".join(failures))
