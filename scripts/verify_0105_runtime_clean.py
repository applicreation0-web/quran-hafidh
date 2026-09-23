#!/usr/bin/env python3
from pathlib import Path
import argparse
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NEEDLE = b"taddabur"
# Release-blocking: neither edition may acquire direct-call/phone-state access via manifest merging.
FORBIDDEN_FINAL_PERMISSIONS = (
    "android.permission.CALL_PHONE",
    "android.permission.READ_PHONE_STATE",
)


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


def check_final_manifest_permissions(path: Path) -> None:
    apkanalyzer = shutil.which("apkanalyzer")
    if not apkanalyzer:
        fail("apkanalyzer is required to verify final APK permissions")
    result = subprocess.run(
        [apkanalyzer, "manifest", "print", str(path)],
        check=False,
        text=True,
        capture_output=True,
    )
    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        fail(f"cannot inspect final manifest for {path.name}: {detail}")
    manifest = result.stdout
    for permission in FORBIDDEN_FINAL_PERMISSIONS:
        if permission in manifest:
            fail(f"forbidden final permission remains in {path.name}: {permission}")


def check_apk(path: Path) -> None:
    if not path.is_file():
        fail(f"APK not found: {path}")
    with zipfile.ZipFile(path) as apk:
        for info in apk.infolist():
            if "taddabur" in info.filename.lower():
                fail(f"removed feature entry remains in {path.name}: {info.filename}")
            if info.file_size <= 32 * 1024 * 1024 and NEEDLE in apk.read(info).lower():
                fail(f"removed feature bytecode/string remains in {path.name}: {info.filename}")
    check_final_manifest_permissions(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", action="append", default=[])
    args = parser.parse_args()
    check_source()
    for raw in args.apk:
        check_apk(Path(raw))
    print(
        "PASS: removed runtime absent from source"
        + (" and APKs; final phone permissions absent" if args.apk else "")
    )


if __name__ == "__main__":
    main()
