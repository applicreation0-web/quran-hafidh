#!/usr/bin/env python3
"""Set exact Android version metadata and its release-audit tuple for 0.10.6."""
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

old_audit = '''        val preparedReleaseMetadata =
            buildFile.contains("versionCode = 24") &&
                buildFile.contains("versionName = \\"0.10.5\\"")
        check(auditedBaselineMetadata || preparedReleaseMetadata) {
            "Expected either the audited 0.10.3 baseline metadata or prepared 0.10.5 release metadata."
        }'''
new_audit = '''        val preparedReleaseMetadata =
            buildFile.contains("versionCode = 25") &&
                buildFile.contains("versionName = \\"0.10.6\\"")
        check(auditedBaselineMetadata || preparedReleaseMetadata) {
            "Expected either the audited 0.10.3 baseline metadata or prepared 0.10.6 release metadata."
        }'''
if updated.count(old_audit) != 1:
    raise SystemExit(
        "Could not locate the exact 0.10.5 prepared-release audit tuple; refusing to weaken the gate"
    )
updated = updated.replace(old_audit, new_audit, 1)

if updated.count('versionCode = 25') != 2:
    raise SystemExit("0.10.6 versionCode 25 must appear exactly in defaultConfig and release audit")
if updated.count('versionName = "0.10.6"') != 1:
    raise SystemExit("0.10.6 defaultConfig versionName must appear exactly once")
if updated.count('versionName = \\"0.10.6\\"') != 1:
    raise SystemExit("0.10.6 escaped release-audit versionName must appear exactly once")
if 'versionCode = 24' in updated or 'versionName = \\"0.10.5\\"' in updated:
    raise SystemExit("Obsolete prepared 0.10.5 release tuple survived")

path.write_text(updated, encoding="utf-8")
print("Prepared Quran Safeguard 0.10.6: versionCode 25 / Plus suffix -plus.1 / exact release-audit tuple")
