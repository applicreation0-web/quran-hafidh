#!/usr/bin/env python3
"""Set exact Android version metadata for the audited 0.10.6 release."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
path = root / "app" / "build.gradle.kts"
text = path.read_text(encoding="utf-8")

pattern = re.compile(
    r'(defaultConfig\s*\{.*?\bversionCode\s*=\s*)\d+(.*?\bversionName\s*=\s*")[^"]+(".*?\n\s*\})',
    re.S,
)
match = pattern.search(text)
if not match:
    raise SystemExit("Could not locate Android defaultConfig version metadata")

replacement = match.group(1) + "25" + match.group(2) + "0.10.6" + match.group(3)
updated = text[:match.start()] + replacement + text[match.end():]
if updated.count('versionCode = 25') != 1:
    raise SystemExit("0.10.6 versionCode 25 was not set exactly once")
if updated.count('versionName = "0.10.6"') != 1:
    raise SystemExit("0.10.6 versionName was not set exactly once")

path.write_text(updated, encoding="utf-8")
print("Prepared Quran Safeguard 0.10.6: versionCode 25 / Plus suffix -plus.1")
