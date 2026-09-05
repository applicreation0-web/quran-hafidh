#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def remove_braced_block(text: str, marker: str) -> str:
    """Remove the complete brace-delimited statement/function containing marker."""
    idx = text.find(marker)
    if idx < 0:
        return text
    line_start = text.rfind("\n", 0, idx) + 1
    brace = text.find("{", idx)
    if brace < 0:
        raise SystemExit(f"FAIL: no opening brace after {marker!r}")
    depth = 0
    in_string = False
    escaped = False
    i = brace
    while i < len(text):
        ch = text[i]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
        else:
            if ch == '"':
                in_string = True
            elif ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    end = i + 1
                    if end < len(text) and text[end] == "\n":
                        end += 1
                    return text[:line_start] + text[end:]
        i += 1
    raise SystemExit(f"FAIL: no closing brace after {marker!r}")


def strip_taddabur_comments(text: str) -> str:
    # Historical prose in compiled Kotlin would otherwise survive in DEX metadata/string tables.
    return "\n".join(
        line for line in text.splitlines()
        if not (line.lstrip().startswith("//") and "taddabur" in line.lower())
    ) + "\n"


def clean_accessibility_service() -> None:
    rel = "app/src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt"
    text = read(rel)

    # Usage ticker deadline interception.
    text = re.sub(
        r"\n\s*val taddaburTarget = foregroundPackage\n"
        r"\s*\?\.takeIf \{ ProtectedApps\.isProtected\(this@QuranAccessibilityService, it\) \}\n"
        r"\s*if \(!callFreezeActive &&\n"
        r"\s*taddaburTarget != null &&\n"
        r"\s*TaddaburEdition\.shouldBlockNow\(this@QuranAccessibilityService\)\n"
        r"\s*\) \{\n"
        r"\s*triggerTaddaburGateIfNeeded\(taddaburTarget\)\n"
        r"\s*return\n"
        r"\s*\}\n",
        "\n",
        text,
        count=1,
        flags=re.MULTILINE,
    )

    # Startup scheduling hook.
    text = re.sub(
        r"\n\s*startupStep\(\"TADDABUR_SCHEDULE_INIT_FAILED\"\) \{\n"
        r"\s*TaddaburEdition\.scheduleReminder\(this\)\n"
        r"\s*\}\n",
        "\n",
        text,
        count=1,
        flags=re.MULTILINE,
    )

    # It was an additional condition only; preserve call/screen/protected-app conditions.
    text = re.sub(
        r"^\s*!TaddaburEdition\.shouldBlockNow\(this(?:@QuranAccessibilityService)?\) &&\n",
        "",
        text,
        flags=re.MULTILINE,
    )

    # Foreground-budget policy: remove only the third OR term.
    text = re.sub(
        r"(callFrozen\s*=\s*callFreezeActive\s*\|\|\s*\n\s*whatsappCallUiActive)\s*\|\|\s*\n\s*TaddaburEdition\.shouldBlockNow\(this\)",
        r"\1",
        text,
        count=1,
        flags=re.MULTILINE,
    )

    # Main event deadline gate.
    text = remove_braced_block(text, "if (TaddaburEdition.shouldBlockNow(this))")

    # Dedicated deadline helper.
    text = remove_braced_block(text, "private fun triggerTaddaburGateIfNeeded")

    # launchGate used to allow an already-unlocked target through only when not deadline-blocked.
    text = re.sub(
        r"\s*val taddaburBlocked = TaddaburEdition\.shouldBlockNow\(this\)\n"
        r"\s*if \(!taddaburBlocked && GuardPrefs\.isUnlocked\(this, packageName\)\) return",
        "\n        if (GuardPrefs.isUnlocked(this, packageName)) return",
        text,
        count=1,
        flags=re.MULTILINE,
    )

    text = strip_taddabur_comments(text)
    write(rel, text)


def clean_gate_activity() -> None:
    rel = "app/src/main/java/com/quranunlock/guard/GateActivity.kt"
    text = read(rel)

    # Resume path: ordinary unlock is sufficient again.
    text = re.sub(
        r"^\s*!TaddaburEdition\.shouldBlockNow\(this\) &&\n",
        "",
        text,
        flags=re.MULTILINE,
    )

    # Remove the flavor replacement gate.
    text = remove_braced_block(text, "if (TaddaburEdition.renderBlockingGate")

    # Remove three deadline checks inside reader polling, joker and openReader.
    for marker in (
        "if (TaddaburEdition.shouldBlockNow(this@GateActivity))",
        "if (TaddaburEdition.shouldBlockNow(this))",
    ):
        while marker in text:
            text = remove_braced_block(text, marker)

    text = strip_taddabur_comments(text)
    write(rel, text)


def clean_dashboard() -> None:
    rel = "app/src/main/java/com/quranunlock/guard/DashboardActivity.kt"
    text = read(rel)
    text = text.replace("                TaddaburEdition.DashboardCard()\n\n", "")
    text = strip_taddabur_comments(text)
    write(rel, text)


def clean_reminders() -> None:
    rel = "app/src/main/java/com/quranunlock/guard/MindfulReminderScheduler.kt"
    text = read(rel)
    text = re.sub(
        r"^\s*TaddaburEdition\.scheduleReminder\(context\)\n",
        "",
        text,
        flags=re.MULTILINE,
    )

    old = '''        if (TaddaburEdition.handlesReminder(intent?.action)) {
            TaddaburEdition.handleReminder(context, intent?.action)
        } else {
            when (intent?.action) {
                MindfulReminderScheduler.ACTION_DAILY -> ReminderNotifications.showDaily(context)
                MindfulReminderScheduler.ACTION_MORNING -> ReminderNotifications.showAdhkar(context, AdhkarPeriod.MORNING)
                MindfulReminderScheduler.ACTION_EVENING -> ReminderNotifications.showAdhkar(context, AdhkarPeriod.EVENING)
            }
        }
'''
    new = '''        when (intent?.action) {
            MindfulReminderScheduler.ACTION_DAILY -> ReminderNotifications.showDaily(context)
            MindfulReminderScheduler.ACTION_MORNING -> ReminderNotifications.showAdhkar(context, AdhkarPeriod.MORNING)
            MindfulReminderScheduler.ACTION_EVENING -> ReminderNotifications.showAdhkar(context, AdhkarPeriod.EVENING)
        }
'''
    text = text.replace(old, new)
    text = strip_taddabur_comments(text)
    write(rel, text)


def delete_feature_files() -> None:
    files = [
        "app/src/light/java/com/quranunlock/guard/TaddaburEdition.kt",
        "app/src/plus/java/com/quranunlock/guard/TaddaburActivity.kt",
        "app/src/plus/java/com/quranunlock/guard/TaddaburEdition.kt",
        "app/src/plus/java/com/quranunlock/guard/TaddaburHardening.kt",
        "app/src/plus/java/com/quranunlock/guard/TaddaburLegacyCore.kt",
        "app/src/testPlus/java/com/quranunlock/guard/TaddaburPolicyTest.kt",
        "scripts/verify_0104_taddabur.py",
        "scripts/verify_light_apk_no_taddabur.py",
        "scripts/verify_plus_apk_taddabur.py",
    ]
    for rel in files:
        path = ROOT / rel
        if path.exists():
            path.unlink()


def rewrite_priority_gate() -> None:
    rel = "scripts/verify_0105_stability_priorities.py"
    write(rel, '''#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAIL: {message}")


free_reader = (ROOT / "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt").read_text()
bookmark_store = (ROOT / "app/src/main/java/com/quranunlock/guard/QuranBookmarkStore.kt").read_text()
plus_tafsir = (ROOT / "app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt").read_text()
multitafsir_repo = (ROOT / "app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt").read_text()
hikam_report = json.loads((ROOT / "app/src/main/assets/hikam/verification_report.json").read_text())

require('private const val KEY_LAST_PAGE = "last_page"' in free_reader,
        "automatic last-page resume key missing")
require("QuranBookmarkStore" in free_reader,
        "reader must use the multiple-bookmark store")
require("fun toggle(page: Int)" in bookmark_store and "fun pages(): List<Int>" in bookmark_store,
        "multiple-bookmark persistence contract missing")
require("bookmarks.contains(page)" in free_reader and "bookmarks.forEach" in free_reader,
        "multiple bookmarks must be visible and directly navigable")
require("TafsirEdition.load" in free_reader and "TafsirEdition.Panel" in free_reader,
        "bookmark work must not remove Tafsir integration")

for root in (ROOT / "app/src/main", ROOT / "app/src/light", ROOT / "app/src/plus", ROOT / "app/src/test", ROOT / "app/src/testPlus"):
    if not root.exists():
        continue
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        require("taddabur" not in path.name.lower(), f"removed feature filename remains: {path}")
        if path.suffix.lower() in {".kt", ".java", ".xml", ".kts"}:
            require("taddabur" not in path.read_text(errors="ignore").lower(),
                    f"removed feature reference remains: {path}")

require("const val isEnabled: Boolean = true" in plus_tafsir,
        "Plus Tafsir must remain enabled")
for edition in ("JALALAYN", "QUSHAYRI", "QURTUBI"):
    require(edition in multitafsir_repo.upper(), f"{edition} Tafsir integration missing")

require(hikam_report.get("reference_count") == 264, "Hikam corpus must remain 264 entries")
require(hikam_report.get("verified_and_translated") == 264, "all 264 Hikam must remain verified and translated")
require(hikam_report.get("missing_french_translation") == 0, "no Hikma may be active without French translation")

print("PASS: Tafsir critical, multiple Quran bookmarks, 264 translated Hikam, removed runtime absent")
''')


def write_runtime_clean_gate() -> None:
    rel = "scripts/verify_0105_runtime_clean.py"
    write(rel, '''#!/usr/bin/env python3
from pathlib import Path
import argparse
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NEEDLE = b"taddabur"


def fail(message: str) -> None:
    raise SystemExit("FAIL: " + message)


def check_source() -> None:
    for root in (ROOT / "app/src/main", ROOT / "app/src/light", ROOT / "app/src/plus", ROOT / "app/src/test", ROOT / "app/src/testPlus"):
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            if "taddabur" in path.name.lower():
                fail(f"removed runtime filename remains: {path.relative_to(ROOT)}")
            if path.suffix.lower() in {".kt", ".java", ".xml", ".kts"} and NEEDLE in path.read_bytes().lower():
                fail(f"removed runtime reference remains: {path.relative_to(ROOT)}")


def check_apk(path: Path) -> None:
    if not path.is_file():
        fail(f"APK not found: {path}")
    with zipfile.ZipFile(path) as apk:
        for info in apk.infolist():
            if "taddabur" in info.filename.lower():
                fail(f"removed feature entry remains in {path.name}: {info.filename}")
            if info.file_size <= 32 * 1024 * 1024 and NEEDLE in apk.read(info).lower():
                fail(f"removed feature bytecode/string remains in {path.name}: {info.filename}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", action="append", default=[])
    args = parser.parse_args()
    check_source()
    for raw in args.apk:
        check_apk(Path(raw))
    print("PASS: removed runtime absent from source" + (" and APKs" if args.apk else ""))


if __name__ == "__main__":
    main()
''')


def clean_workflows() -> None:
    workflow_dir = ROOT / ".github/workflows"
    for path in workflow_dir.glob("*.yml"):
        text = path.read_text(encoding="utf-8")
        lines = [line for line in text.splitlines() if not any(old in line for old in (
            "verify_0104_taddabur.py",
            "verify_light_apk_no_taddabur.py",
            "verify_plus_apk_taddabur.py",
        ))]
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    stability = workflow_dir / "android-0.10.5-stability-core.yml"
    text = stability.read_text(encoding="utf-8")
    if "Verify removed runtime stays absent" not in text:
        anchor = "      - name: Verify sensitive-app scope remains isolated\n        run: python3 scripts/verify_0105_sensitive_scope.py\n"
        text = text.replace(anchor, anchor + "\n      - name: Verify removed runtime stays absent\n        run: python3 scripts/verify_0105_runtime_clean.py\n", 1)
    if 'verify_0105_runtime_clean.py --apk "${light}" --apk "${plus}"' not in text:
        anchor = '          python3 scripts/verify_plus_apk_multitafsir.py "${plus}"\n'
        text = text.replace(anchor, anchor + '          python3 scripts/verify_0105_runtime_clean.py --apk "${light}" --apk "${plus}"\n', 1)
    stability.write_text(text, encoding="utf-8")

    publish = workflow_dir / "publish-0.10.5.yml"
    text = publish.read_text(encoding="utf-8")
    if "python3 scripts/verify_0105_runtime_clean.py" not in text:
        anchor = "          python3 scripts/verify_0105_sensitive_scope.py\n"
        text = text.replace(anchor, anchor + "          python3 scripts/verify_0105_runtime_clean.py\n", 1)
    if 'verify_0105_runtime_clean.py --apk "${light}" --apk "${plus}"' not in text:
        anchor = '          python3 scripts/verify_plus_apk_multitafsir.py "${plus}"\n'
        text = text.replace(anchor, anchor + '          python3 scripts/verify_0105_runtime_clean.py --apk "${light}" --apk "${plus}"\n', 1)
    if 'verify_0105_runtime_clean.py --apk "${light_apk}" --apk "${plus_apk}"' not in text:
        anchor = '          python3 scripts/verify_plus_apk_multitafsir.py "${plus_apk}"\n'
        text = text.replace(anchor, anchor + '          python3 scripts/verify_0105_runtime_clean.py --apk "${light_apk}" --apk "${plus_apk}"\n', 1)
    publish.write_text(text, encoding="utf-8")


def assert_clean() -> None:
    bad = []
    for root in (ROOT / "app/src/main", ROOT / "app/src/light", ROOT / "app/src/plus", ROOT / "app/src/test", ROOT / "app/src/testPlus"):
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            if "taddabur" in path.name.lower():
                bad.append(str(path.relative_to(ROOT)))
            elif path.suffix.lower() in {".kt", ".java", ".xml", ".kts"} and "taddabur" in path.read_text(errors="ignore").lower():
                bad.append(str(path.relative_to(ROOT)))
    if bad:
        raise SystemExit("FAIL residual runtime refs:\n" + "\n".join(sorted(set(bad))))


def main() -> None:
    clean_accessibility_service()
    clean_gate_activity()
    clean_dashboard()
    clean_reminders()
    delete_feature_files()
    rewrite_priority_gate()
    write_runtime_clean_gate()
    clean_workflows()
    assert_clean()
    print("PASS: Taddabur fully removed from 0.10.5 compiled runtime and stale gates")


if __name__ == "__main__":
    main()
