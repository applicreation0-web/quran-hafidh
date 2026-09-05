#!/usr/bin/env python3
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
