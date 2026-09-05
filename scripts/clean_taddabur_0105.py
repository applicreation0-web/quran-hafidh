#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.write_text(text, encoding="utf-8")


def replace_required(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"FAIL {label}: expected 1 exact match, found {count}")
    return text.replace(old, new, 1)


def remove_braced_block_at_marker(text: str, marker: str, label: str) -> str:
    marker_index = text.find(marker)
    if marker_index < 0:
        return text
    line_start = text.rfind("\n", 0, marker_index) + 1
    brace_start = text.find("{", marker_index)
    if brace_start < 0:
        raise SystemExit(f"FAIL {label}: opening brace not found")
    depth = 0
    in_string = False
    escaped = False
    i = brace_start
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
    raise SystemExit(f"FAIL {label}: closing brace not found")


def clean_accessibility_service() -> None:
    rel = "app/src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt"
    text = read(rel)

    ticker = re.compile(
        r"\n\s*val taddaburTarget = foregroundPackage\n"
        r"\s*\?\.takeIf \{ ProtectedApps\.isProtected\(this@QuranAccessibilityService, it\) \}\n"
        r"\s*if \(!callFreezeActive &&\n"
        r"\s*taddaburTarget != null &&\n"
        r"\s*TaddaburEdition\.shouldBlockNow\(this@QuranAccessibilityService\)\n"
        r"\s*\) \{\n"
        r"\s*triggerTaddaburGateIfNeeded\(taddaburTarget\)\n"
        r"\s*return\n"
        r"\s*\}\n",
        re.MULTILINE,
    )
    text, count = ticker.subn("\n", text, count=1)
    if count != 1:
        raise SystemExit(f"FAIL accessibility ticker cleanup: expected 1 match, found {count}")

    startup = re.compile(
        r"\n\s*startupStep\(\"TADDABUR_SCHEDULE_INIT_FAILED\"\) \{\n"
        r"\s*TaddaburEdition\.scheduleReminder\(this\)\n"
        r"\s*\}\n",
        re.MULTILINE,
    )
    text, count = startup.subn("\n", text, count=1)
    if count != 1:
        raise SystemExit(f"FAIL accessibility startup cleanup: expected 1 match, found {count}")

    # Taddabur was only an extra conjunct. Removing this line preserves the
    # existing call-freeze, screen, scope and unlock recovery conditions.
    text, count = re.subn(
        r"^\s*!TaddaburEdition\.shouldBlockNow\(this\) &&\n",
        "",
        text,
        flags=re.MULTILINE,
    )
    if count < 2:
        raise SystemExit(f"FAIL accessibility recovery cleanup: expected >=2 conjuncts, found {count}")

    text = remove_braced_block_at_marker(
        text,
        "if (TaddaburEdition.shouldBlockNow(this))",
        "accessibility foreground Taddabur gate",
    )
    text = remove_braced_block_at_marker(
        text,
        "private fun triggerTaddaburGateIfNeeded",
        "accessibility Taddabur helper",
    )

    if "Taddabur" in text or "taddabur" in text:
        lines = [
            f"{i}: {line}"
            for i, line in enumerate(text.splitlines(), 1)
            if "taddabur" in line.lower()
        ]
        raise SystemExit("FAIL accessibility residual Taddabur refs:\n" + "\n".join(lines))
    write(rel, text)


def clean_dashboard() -> None:
    rel = "app/src/main/java/com/quranunlock/guard/DashboardActivity.kt"
    text = read(rel)
    text = replace_required(
        text,
        "                TaddaburEdition.DashboardCard()\n\n",
        "",
        "dashboard card",
    )
    write(rel, text)


def clean_gate() -> None:
    rel = "app/src/main/java/com/quranunlock/guard/GateActivity.kt"
    text = read(rel)
    # The blocking-gate call is a complete if block; remove only that block.
    marker = "if (TaddaburEdition.renderBlockingGate"
    if marker in text:
        text = remove_braced_block_at_marker(text, marker, "GateActivity Taddabur block")
    if "Taddabur" in text or "taddabur" in text:
        lines = [
            f"{i}: {line}"
            for i, line in enumerate(text.splitlines(), 1)
            if "taddabur" in line.lower()
        ]
        raise SystemExit("FAIL GateActivity residual Taddabur refs:\n" + "\n".join(lines))
    write(rel, text)


def clean_reminder_scheduler() -> None:
    rel = "app/src/main/java/com/quranunlock/guard/MindfulReminderScheduler.kt"
    text = read(rel)
    marker = "if (TaddaburEdition.handlesReminder"
    while marker in text:
        text = remove_braced_block_at_marker(text, marker, "reminder Taddabur routing")
    # Remove any now-orphaned one-line scheduling call if present.
    text = re.sub(
        r"^\s*TaddaburEdition\.scheduleReminder\([^\n]*\)\s*$\n?",
        "",
        text,
        flags=re.MULTILINE,
    )
    if "Taddabur" in text or "taddabur" in text:
        lines = [
            f"{i}: {line}"
            for i, line in enumerate(text.splitlines(), 1)
            if "taddabur" in line.lower()
        ]
        raise SystemExit("FAIL reminder residual Taddabur refs:\n" + "\n".join(lines))
    write(rel, text)


def delete_feature_files() -> None:
    explicit = [
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
    for rel in explicit:
        path = ROOT / rel
        if path.exists():
            path.unlink()


def rewrite_priority_gate() -> None:
    rel = "scripts/verify_0105_stability_priorities.py"
    content = '''#!/usr/bin/env python3
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

# Qur'an reader: automatic last-page resume and multiple explicit bookmarks are distinct.
require('private const val KEY_LAST_PAGE = "last_page"' in free_reader,
        "automatic last-page resume key missing")
require("QuranBookmarkStore" in free_reader,
        "reader must use the dedicated multi-bookmark store")
require("fun toggle(page: Int)" in bookmark_store,
        "multi-bookmark toggle action missing")
require("fun pages(): List<Int>" in bookmark_store,
        "multi-bookmark listing missing")
require("bookmarks.contains(page)" in free_reader,
        "current-page bookmark state missing")
require("bookmarks.forEach" in free_reader,
        "saved bookmarks are not exposed for direct navigation")
require("TafsirEdition.load" in free_reader and "TafsirEdition.Panel" in free_reader,
        "bookmark work must not remove Tafsir integration")

# Taddabur is removed from every compiled Android source set and test source set.
for root in (
    ROOT / "app/src/main",
    ROOT / "app/src/light",
    ROOT / "app/src/plus",
    ROOT / "app/src/test",
    ROOT / "app/src/testPlus",
):
    if not root.exists():
        continue
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        require("taddabur" not in path.name.lower(), f"removed feature filename remains: {path}")
        if path.suffix.lower() in {".kt", ".java", ".xml", ".kts"}:
            text = path.read_text(errors="ignore")
            require("taddabur" not in text.lower(), f"removed feature reference remains: {path}")

# Tafsir is a release-critical Plus feature.
require("const val isEnabled: Boolean = true" in plus_tafsir,
        "Plus Tafsir must remain enabled")
for edition in ("JALALAYN", "QUSHAYRI", "QURTUBI"):
    require(edition in multitafsir_repo.upper(), f"{edition} Tafsir integration missing")

# Hikam: comments are optional, but 264/264 verified Arabic+French translations are mandatory.
require(hikam_report.get("reference_count") == 264,
        "Hikam reference corpus must remain 264 entries")
require(hikam_report.get("verified_and_translated") == 264,
        "all 264 Hikam must remain verified and translated")
require(hikam_report.get("missing_french_translation") == 0,
        "no Hikma may be active without French translation")

print("PASS: 0.10.5 priorities locked — Tafsir critical, multiple Quran bookmarks, Hikam translations complete, Taddabur removed")
'''
    write(rel, content)


def write_runtime_clean_gate() -> None:
    rel = "scripts/verify_0105_runtime_clean.py"
    content = '''#!/usr/bin/env python3
from pathlib import Path
import argparse
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def fail(message: str) -> None:
    raise SystemExit("FAIL: " + message)


def check_source() -> None:
    roots = [
        ROOT / "app/src/main",
        ROOT / "app/src/light",
        ROOT / "app/src/plus",
        ROOT / "app/src/test",
        ROOT / "app/src/testPlus",
    ]
    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            if "taddabur" in path.name.lower():
                fail(f"removed feature filename remains: {path.relative_to(ROOT)}")
            if path.suffix.lower() in {".kt", ".java", ".xml", ".kts"}:
                if b"taddabur" in path.read_bytes().lower():
                    fail(f"removed feature reference remains: {path.relative_to(ROOT)}")


def check_apk(path: Path) -> None:
    if not path.is_file():
        fail(f"APK not found: {path}")
    needle = b"taddabur"
    with zipfile.ZipFile(path) as apk:
        for info in apk.infolist():
            if "taddabur" in info.filename.lower():
                fail(f"removed feature entry remains in {path.name}: {info.filename}")
            if info.file_size <= 32 * 1024 * 1024:
                data = apk.read(info)
                if needle in data.lower():
                    fail(f"removed feature bytecode/string remains in {path.name}: {info.filename}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", action="append", default=[])
    args = parser.parse_args()
    check_source()
    for raw in args.apk:
        check_apk(Path(raw))
    print("PASS: Taddabur absent from compiled source sets" +
          (" and APK payloads" if args.apk else ""))


if __name__ == "__main__":
    main()
'''
    write(rel, content)


def clean_workflows() -> None:
    for path in (ROOT / ".github/workflows").glob("*.yml"):
        text = path.read_text(encoding="utf-8")
        lines = [
            line for line in text.splitlines()
            if not any(name in line for name in (
                "verify_0104_taddabur.py",
                "verify_light_apk_no_taddabur.py",
                "verify_plus_apk_taddabur.py",
            ))
        ]
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    stability = ROOT / ".github/workflows/android-0.10.5-stability-core.yml"
    text = stability.read_text(encoding="utf-8")
    if "python3 scripts/verify_0105_runtime_clean.py" not in text:
        text = text.replace(
            "      - name: Verify sensitive-app scope remains isolated\n        run: python3 scripts/verify_0105_sensitive_scope.py\n",
            "      - name: Verify sensitive-app scope remains isolated\n        run: python3 scripts/verify_0105_sensitive_scope.py\n\n"
            "      - name: Verify removed runtime stays absent\n"
            "        run: python3 scripts/verify_0105_runtime_clean.py\n",
            1,
        )
    apk_anchor = '          python3 scripts/verify_plus_apk_multitafsir.py "${plus}"\n'
    if 'verify_0105_runtime_clean.py --apk "${light}" --apk "${plus}"' not in text:
        text = text.replace(
            apk_anchor,
            apk_anchor + '          python3 scripts/verify_0105_runtime_clean.py --apk "${light}" --apk "${plus}"\n',
            1,
        )
    stability.write_text(text, encoding="utf-8")

    publish = ROOT / ".github/workflows/publish-0.10.5.yml"
    text = publish.read_text(encoding="utf-8")
    if "python3 scripts/verify_0105_runtime_clean.py" not in text:
        text = text.replace(
            "          python3 scripts/verify_0105_sensitive_scope.py\n",
            "          python3 scripts/verify_0105_sensitive_scope.py\n"
            "          python3 scripts/verify_0105_runtime_clean.py\n",
            1,
        )
    # Add APK proof after multitafsir checks in both unsigned and signed phases.
    text = text.replace(
        '          python3 scripts/verify_plus_apk_multitafsir.py "${plus}"\n',
        '          python3 scripts/verify_plus_apk_multitafsir.py "${plus}"\n'
        '          python3 scripts/verify_0105_runtime_clean.py --apk "${light}" --apk "${plus}"\n',
    )
    text = text.replace(
        '          python3 scripts/verify_plus_apk_multitafsir.py "${plus_apk}"\n',
        '          python3 scripts/verify_plus_apk_multitafsir.py "${plus_apk}"\n'
        '          python3 scripts/verify_0105_runtime_clean.py --apk "${light_apk}" --apk "${plus_apk}"\n',
    )
    publish.write_text(text, encoding="utf-8")


def assert_runtime_clean() -> None:
    bad = []
    for rel_root in ("app/src/main", "app/src/light", "app/src/plus", "app/src/test", "app/src/testPlus"):
        root = ROOT / rel_root
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            if "taddabur" in path.name.lower():
                bad.append(str(path.relative_to(ROOT)))
                continue
            if path.suffix.lower() in {".kt", ".java", ".xml", ".kts"}:
                if "taddabur" in path.read_text(errors="ignore").lower():
                    bad.append(str(path.relative_to(ROOT)))
    if bad:
        raise SystemExit("FAIL residual runtime Taddabur files/references:\n" + "\n".join(sorted(set(bad))))


def main() -> None:
    clean_accessibility_service()
    clean_dashboard()
    clean_gate()
    clean_reminder_scheduler()
    delete_feature_files()
    rewrite_priority_gate()
    write_runtime_clean_gate()
    clean_workflows()
    assert_runtime_clean()
    print("PASS: Taddabur removed from runtime source, tests and active 0.10.5 release gates")


if __name__ == "__main__":
    main()
