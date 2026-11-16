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



def verify_boox_home_icon_sizes(home, width):
    """Protect the 2x2 approved BOOX icons from short-screen compression."""
    floor = max(64, int(width * 0.075))
    for label in ("Parcours", "Progression", "Lecture", "Atlas"):
        tile = find_control(home, lambda value: value == label)
        if tile is None:
            raise AssertionError(f"Missing direct-access tile: {label}")
        icon = next((node for node in tile.iter("node")
                     if node.get("class") == "android.widget.ImageView"
                     and bounds(node) is not None), None)
        if icon is None:
            raise AssertionError(f"No approved icon for {label}")
        x1, y1, x2, y2 = bounds(icon)
        if y2 - y1 < floor or x2 <= x1:
            raise AssertionError(
                f"BOOX {label} icon collapsed to {x2-x1}x{y2-y1}px; "
                f"minimum height {floor}px")



def verify_boox_parcours_icon_sizes(root, width):
    """All six approved Hifdh action icons stay legible, including Stabiliser
    with the separate (Maintien) subtitle at fontScale 150%."""
    floor = max(44, int(width * .05))
    for label in ("Apprentissage", "Renforcement", "Stabilisation",
                  "Consolidation", "Révision active", "Révision passive"):
        node = next((n for n in root.iter("node")
                     if n.get("package") == PACKAGE
                     and any(value == label or value.startswith(label + " · ")
                             for value in names(n))
                     and n.get("class") == "android.widget.LinearLayout"
                     and bounds(n) is not None), None)
        if node is None:
            raise AssertionError(f"Missing Parcours activity tile: {label}")
        icon = next((n for n in node.iter("node")
                     if n.get("class") == "android.widget.ImageView"
                     and bounds(n) is not None), None)
        if icon is None:
            raise AssertionError(f"No approved pictogram visible in {label}")
        x1, y1, x2, y2 = bounds(icon)
        if y2 - y1 < floor:
            raise AssertionError(
                f"BOOX Parcours {label} icon is {y2-y1}px high; minimum {floor}px")



def verify_calendar_day_bounds(root):
    """No cut-off date labels even with concentric selection at 150% font."""
    stations = [n for n in root.iter("node")
                if n.get("package") == PACKAGE
                and "ouvrir le détail matin et soir" in n.get("content-desc", "")]
    if len(stations) != 7:
        raise AssertionError(f"Expected seven actual dated stations, got {len(stations)}")
    for station in stations:
        sx1, sy1, sx2, sy2 = bounds(station)
        visible_dates = [n for n in station.iter("node")
                         if n.get("class") == "android.widget.TextView"
                         and n.get("text") and bounds(n) is not None]
        if len(visible_dates) != 2:
            raise AssertionError("Every day needs weekday initial and date text")
        for child in visible_dates:
            x1, y1, x2, y2 = bounds(child)
            if y1 < sy1 or y2 > sy2 or x1 < sx1 or x2 > sx2:
                raise AssertionError(
                    f"Calendar label {child.get('text')} clips its station: "
                    f"{(x1,y1,x2,y2)} vs {(sx1,sy1,sx2,sy2)}")


def capture_cap_hifz(tag, home, width, height, fixed, density=None):
    visible = lambda tree, label: next((
        node for node in tree.iter("node") if node.get("package") == PACKAGE
        and any(label(value) for value in names(node))
        and (area := bounds(node)) and 0 <= area[0] < area[2] <= width
        and 0 <= area[1] < area[3] <= height), None)

    def is_graph(value):
        return value.startswith("Feuille de route prévisionnelle")

    if fixed:
        home = hierarchy(f"{tag}-cap-viewport", lambda tree: visible(tree, is_graph) is not None)
        if (visible(home, lambda value: value == "Cap Hifdh") is None
                or visible(home, is_graph) is None):
            raise AssertionError("Cap Hifdh title and timeline must both fit on the BOOX home")
        if visible(home, lambda value: value == "Acquis ›") is None:
            raise AssertionError("The Cap Hifdh history footer is clipped on the BOOX home")
        if any(node.get("scrollable") == "true" for node in home.iter("node")):
            raise AssertionError("BOOX home must not scroll")
    else:
        adb("shell", "input", "swipe", str(width // 2), str(int(height * .78)),
            str(width // 2), str(int(height * .34)), "440")
    suffix = f"{tag}-cap-hifdh-90"
    screen = hierarchy(suffix, lambda tree:
        visible(tree, is_graph) is not None and visible(
            tree, lambda value: value == "Prévision · 90 % d’assiduité") is not None)
    if any("Simulation à " in label for node in screen.iter("node")
           for label in names(node)):
        raise AssertionError("Legacy 80/90/100 selector is still present")
    diagnostics(suffix)
    if tag.endswith("font150") and fixed:
        verify_cap_milestone_opens_mushaf(tag)
        verify_direct_cap_milestone_taps(tag, density)
        verify_post_nas_forecast_year(tag)



def verify_cap_milestone_opens_mushaf(tag):
    """Check the all-inline Cap Hifdh navigator then open the real QCF
    Mushaf from its selected marker, never using an Android AlertDialog."""
    home = hierarchy(f"{tag}-cap-nav-ready")
    history = find_control(home, lambda value: value.startswith("Parcourir les acquis."))
    if history is None:
        raise AssertionError("Real acquired-line history should be inline on BOOX home")
    tap(history)
    home = hierarchy(f"{tag}-cap-inline-history", lambda tree:
        find_control(tree, lambda value: value == "Quitter la consultation des acquis") is not None)
    diagnostics(f"{tag}-cap-inline-history")
    if not any("Acquis · revenir à la projection" in label
               for node in home.iter("node") for label in names(node)):
        raise AssertionError("History must reuse the existing inline row, not open a dialog")
    tap(find_control(home, lambda value: value == "Quitter la consultation des acquis"))
    home = hierarchy(f"{tag}-cap-inline-return", lambda tree:
        find_control(tree, lambda value:
            value.startswith("Ouvrir le Mushaf depuis Cap Hifdh :")) is not None)
    selected = find_control(home, lambda value:
        value.startswith("Ouvrir le Mushaf depuis Cap Hifdh :"))
    if selected is None:
        raise AssertionError("Missing inline, directly clickable checkpoint")
    graph = find_control(home, lambda value:
        value.startswith("Feuille de route prévisionnelle interactive"))
    if graph is None:
        raise AssertionError("Missing interactive Cap Hifdh annual ribbon")
    # Single chevrons now visibly browse forecast milestones.
    # Double chevrons remain reserved for calendar years.
    forward = find_control(home, lambda value: value == "Jalon prévisionnel suivant")
    backward = next((node for node in home.iter("node")
        if node.get("package") == PACKAGE
        and "Jalon prévisionnel précédent" in names(node) and bounds(node)), None)
    next_year = find_control(home, lambda value: value == "Prévision : année suivante")
    if forward is None or backward is None or next_year is None:
        raise AssertionError("Missing year/step controls on Cap Hifdh")
    # At the first forecast checkpoint, going backward is correctly disabled.
    if backward.get("enabled") == "true":
        raise AssertionError("Initial step-back arrow must be disabled")
    selected_before = " ".join(names(selected))
    tap(forward)
    home = hierarchy(f"{tag}-cap-step-chevron", lambda tree:
        find_control(tree, lambda value:
            value.startswith("Ouvrir le Mushaf depuis Cap Hifdh :")) is not None)
    selected_after = find_control(home, lambda value:
        value.startswith("Ouvrir le Mushaf depuis Cap Hifdh :"))
    if selected_after is None or " ".join(names(selected_after)) == selected_before:
        raise AssertionError("Step chevron did not navigate to a new forecast milestone")
    diagnostics(f"{tag}-cap-step-chevron")

    # Touch empty timeline ink: the same step cursor advances without popup.
    graph = find_control(home, lambda value:
        value.startswith("Feuille de route prévisionnelle interactive"))
    x1, y1, x2, y2 = bounds(graph)
    adb("shell", "input", "tap", str((x1 + x2) // 2), str(y2 - 5))
    home = hierarchy(f"{tag}-cap-inline-step", lambda tree:
        find_control(tree, lambda value:
            value.startswith("Ouvrir le Mushaf depuis Cap Hifdh :")) is not None)
    diagnostics(f"{tag}-cap-inline-step")
    if any(node.get("text", "").startswith("Juz et Hizb ·") for node in home.iter("node")):
        raise AssertionError("Modal Juz / Hizb explorer regressed into the BOOX home")
    selected = find_control(home, lambda value:
        value.startswith("Ouvrir le Mushaf depuis Cap Hifdh :"))
    tap(selected)
    state = ""
    for _ in range(6):
        time.sleep(2)
        state = adb("shell", "dumpsys", "activity", "activities").stdout.decode(
            "utf-8", errors="replace")
        if re.search(r"(?:topResumedActivity|mResumedActivity).*StudyReaderActivity", state):
            break
    if not re.search(r"(?:topResumedActivity|mResumedActivity).*StudyReaderActivity", state):
        raise AssertionError("Inline Cap Hifdh marker did not open the real Mushaf")
    time.sleep(6)
    diagnostics(f"{tag}-cap-mushaf-from-milestone")
    screenshot = (OUTPUT / f"{tag}-cap-mushaf-from-milestone.png").read_bytes()
    if len(screenshot) < 40000:
        raise AssertionError("Mushaf is still blank after rendering; page must be verified")
    adb("shell", "input", "keyevent", "4")
    hierarchy(f"{tag}-cap-home-return", lambda tree:
              find_control(tree, lambda label: label == "Parcours") is not None)



def verify_post_nas_forecast_year(tag):
    """Real emulator portrait screenshot / accessibility test for owner photo.
    With the source fixture An-Nās is forecast for 2028; on a selected 2031
    timeline the Maîtrise progress lane must be absent, not merely empty.
    It is NOT a real user completion or a fictitious maintenance Juz.
    """
    for year in range(2027, 2032):
        before = hierarchy(f"{tag}-cap-before-{year}")
        arrow = find_control(before, lambda label:
            label == "Prévision : année suivante")
        if arrow is None:
            raise AssertionError("Cap Hifdh year navigation stopped before 2031")
        tap(arrow)
    root = hierarchy(f"{tag}-cap-hifdh-post-nas-2031", lambda tree:
        find_control(tree, lambda label:
            label.startswith("Feuille de route prévisionnelle interactive 2031")) is not None)
    graph = find_control(root, lambda label:
        label.startswith("Feuille de route prévisionnelle interactive 2031"))
    spoken = " ".join(names(graph))
    if "Entretien : cycle sans échéance" not in spoken:
        raise AssertionError("After projected An-Nās, Entretien is missing in 2031")
    if "Maîtrise, position de travail" in spoken or "Objectif final An-Nās" in spoken:
        raise AssertionError("The 2031 Cap Hifdh still draws a finished Maîtrise lane")
    if any(node.get("scrollable") == "true" for node in root.iter("node")):
        raise AssertionError("After projected An-Nās, BOOX home must stay unscrolled")
    diagnostics(f"{tag}-cap-hifdh-post-nas-2031")


def verify_direct_cap_milestone_taps(tag, density):
    """Tap EVERY future Juz/Hizb icon plus both terminal diamonds on the
    actual Canvas surface. Validate Activity navigation and a nonblank QCF page.
    No optimistic credit, mocked timeline click, or selector fallback."""
    scale = density / 160.0
    for lane, station in ((0, 1), (0, 2), (1, 1), (1, 2), (0, 3), (1, 3)):
        home = hierarchy(f"{tag}-cap-direct-{lane}-{station}-ready",
            lambda tree: find_control(tree, lambda label:
                label.startswith("Feuille de route prévisionnelle interactive")) is not None)
        graph = find_control(home, lambda label:
            label.startswith("Feuille de route prévisionnelle interactive"))
        gx1, gy1, gx2, gy2 = bounds(graph)
        margin = 16 * scale
        span = (gx2 - gx1) - 2 * margin
        stations = (margin + 6 * scale, margin + span * .34,
                    margin + span * .64, margin + span - 6 * scale)
        # Follow the actual view-height-dependent BOOX lane separation;
        # using a fixed 160dp/84dp offset falsely taps empty ink after the
        # home starts using its previously wasted bottom viewport.
        spacious = (gx2 - gx1) >= 720 * scale
        available_dp = (gy2 - gy1) / scale
        lane_separation = max(160 if spacious else 88,
                              available_dp - (154 if spacious else 92))
        row = lane_separation if lane else 0
        px = int(gx1 + stations[station])
        py = int(gy1 + (row + (60 if spacious else 34)) * scale)
        if not (gx1 < px < gx2 and gy1 < py < gy2):
            raise AssertionError(f"Direct icon {lane}/{station} outside BOOX viewport")
        adb("shell", "input", "tap", str(px), str(py))
        for _ in range(8):
            time.sleep(1)
            state = adb("shell", "dumpsys", "activity", "activities").stdout.decode(
                "utf-8", errors="replace")
            if re.search(r"(?:topResumedActivity|mResumedActivity).*StudyReaderActivity", state):
                break
        else:
            raise AssertionError(f"Direct Juz/Hizb or goal tap {lane}/{station} did not open Mushaf")
        # A foreground Reader initially paints its blank WebView shell.
        # Fail closed until the authentic QCF page has visibly rendered.
        rendered = False
        for _ in range(10):
            time.sleep(2)
            shot = adb("exec-out", "screencap", "-p").stdout
            if shot[:4] == bytes((137, 80, 78, 71)) and len(shot) >= 45000:
                rendered = True
                break
        if not rendered:
            raise AssertionError(f"Direct milestone {lane}/{station} opened a blank Mushaf page")
        if station in (1, 3):
            diagnostics(f"{tag}-cap-direct-{lane}-{station}-mushaf")
        adb("shell", "input", "keyevent", "4")
        hierarchy(f"{tag}-cap-direct-{lane}-{station}-return",
            lambda tree: find_control(tree, lambda label: label == "Parcours") is not None)


def verify_home_balance(home, width, height, density):
    """Use actual Android node rectangles, not source-only layout assertions."""
    scale = density / 160.0
    tiles = [find_control(home, lambda value, label=label: value == label)
             for label in ("Parcours", "Progression", "Lecture", "Atlas")]
    if any(tile is None for tile in tiles):
        raise AssertionError("Missing one of the four BOOX destinations")
    first, third = bounds(tiles[0]), bounds(tiles[2])
    vertical = bounds(tiles[2])[3] - bounds(tiles[0])[1]
    if vertical > height * .34:
        raise AssertionError(f"Home icons still monopolize {vertical / height:.0%} of BOOX")
    for tile, label in zip(tiles, ("Parcours", "Progression", "Lecture", "Atlas")):
        caption = next((n for n in tile.iter("node")
                        if n.get("class") == "android.widget.TextView"
                        and n.get("text") == label and bounds(n)), None)
        if caption is None:
            raise AssertionError(f"{label} has no visible, one-word caption")
        tx1,ty1,tx2,ty2=bounds(tile)
        x1,y1,x2,y2=bounds(caption)
        if not(tx1<=x1<x2<=tx2 and ty1<=y1<y2<=ty2):
            raise AssertionError(f"Caption {label} is clipped")
    history = find_control(home, lambda value:
        value.startswith("Parcourir les acquis."))
    if history is None:
        raise AssertionError("Missing directly reachable authentic Acquis history")
    bx=bounds(history)
    if bx[3]-bx[1] < 47 * scale:
        raise AssertionError("Acquis history tap area is smaller than 48dp")
    graph = find_control(home, lambda value:
        value.startswith("Feuille de route prévisionnelle interactive"))
    if graph is None or bounds(graph)[3] > height - 30:
        raise AssertionError("Cap Hifdh is clipped by the BOOX viewport")


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
        home = hierarchy(f"{tag}-home", lambda tree: find_control(
            tree, lambda label: label == "Parcours") is not None)
        diagnostics(f"{tag}-home")
        width, height = map(int, size.split("x"))
        verify_boox_home_icon_sizes(home, width)
        verify_home_balance(home, width, height, density)
        capture_cap_hifz(tag, home, width, height, fixed=True, density=density)
        parcours = find_control(home, lambda label: label == "Parcours")
        tap(parcours)
        time.sleep(2)
        diagnostics(tag)
        root = hierarchy(tag)
        width, height = map(int, size.split("x"))
        verify_boox_parcours_icon_sizes(root, width)
        verify_calendar_day_bounds(root)
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
    try:
        installation = adb("install", "-r", str(apk), check=False, timeout=300)
        (OUTPUT / "install.txt").write_bytes(installation.stdout + installation.stderr)
        if installation.returncode or b"Success" not in installation.stdout:
            raise RuntimeError("ADB could not install the BOOX test APK")
    except subprocess.TimeoutExpired as exc:
        (OUTPUT / "install.txt").write_bytes((exc.stdout or b"") + (exc.stderr or b""))
        diagnostics("install-failure")
        raise
    except (OSError, RuntimeError):
        diagnostics("install-failure")
        raise
    failures = []
    for size in ("1404x1872", "900x1200"):
        for font in (1.0, 1.25, 1.5):
            tag = f"boox-{size}-font{int(font * 100)}"
            try:
                capture(tag, size, 220 if size == "900x1200" else 240, font)
            except (OSError, ValueError, RuntimeError, AssertionError, subprocess.SubprocessError) as exc:
                failures.append(f"{tag}: {exc}")
                print(failures[-1], flush=True)
    for font in (1.0, 1.25, 1.5):
        tag = f"phone-font{int(font * 100)}"
        try:
            adb("shell", "wm", "size", "1080x2400")
            adb("shell", "wm", "density", "440")
            adb("shell", "settings", "put", "system", "font_scale", str(font))
            adb("shell", "am", "force-stop", PACKAGE)
            adb("shell", "am", "start", "-W", "-n", ACTIVITY)
            home = hierarchy(f"{tag}-home", lambda root: find_control(
                root, lambda label: label == "Parcours") is not None)
            diagnostics(f"{tag}-home")
            capture_cap_hifz(tag, home, 1080, 2400, fixed=False)
        except (OSError, ValueError, RuntimeError, AssertionError, subprocess.SubprocessError) as exc:
            failures.append(f"{tag}: {exc}")
    if failures:
        raise SystemExit("\n".join(failures))
