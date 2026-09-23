#!/usr/bin/env python3
"""Set exact Android version metadata and release-audit tuple for Quran Safeguard 0.10.7."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "app" / "build.gradle.kts"
text = path.read_text(encoding="utf-8")

# Freeze the actual packaged Android metadata.
default_pattern = re.compile(
    r'(defaultConfig\s*\{.*?\bversionCode\s*=\s*)\d+(.*?\bversionName\s*=\s*")[^"]+(".*?\n\s*\})',
    re.S,
)
match = default_pattern.search(text)
if not match:
    raise SystemExit("Could not locate Android defaultConfig version metadata")
text = text[:match.start()] + match.group(1) + "26" + match.group(2) + "0.10.7" + match.group(3) + text[match.end():]

# Keep the audited 0.10.3 source baseline accepted while making 0.10.7 the only prepared release tuple.
prepared_pattern = re.compile(
    r'''        val preparedReleaseMetadata =\n\s*buildFile\.contains\("versionCode = \d+"\) &&\n\s*buildFile\.contains\("versionName = \\\"[^\"]+\\\""\)\n        check\(auditedBaselineMetadata \|\| preparedReleaseMetadata\) \{\n            "Expected either the audited 0\.10\.3 baseline metadata or prepared [^"]+ release metadata\."\n        \}'''
)
prepared_replacement = '''        val preparedReleaseMetadata =
            buildFile.contains("versionCode = 26") &&
                buildFile.contains("versionName = \\\"0.10.7\\\"")
        check(auditedBaselineMetadata || preparedReleaseMetadata) {
            "Expected either the audited 0.10.3 baseline metadata or prepared 0.10.7 release metadata."
        }'''
text, count = prepared_pattern.subn(prepared_replacement, text, count=1)
if count != 1:
    raise SystemExit("Could not locate the prepared-release audit tuple; refusing to weaken migration checks")

if text.count('versionCode = 26') != 2:
    raise SystemExit("0.10.7 versionCode 26 must appear exactly in defaultConfig and release audit")
if text.count('versionName = "0.10.7"') != 1:
    raise SystemExit("0.10.7 defaultConfig versionName must appear exactly once")
if text.count('versionName = \\\"0.10.7\\\"') != 1:
    raise SystemExit("0.10.7 escaped release-audit versionName must appear exactly once")

path.write_text(text, encoding="utf-8")
print("Prepared Quran Safeguard 0.10.7: versionCode 26 / Plus suffix -plus.1 / exact release-audit tuple")
