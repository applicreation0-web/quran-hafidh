#!/usr/bin/env python3
from pathlib import Path

p = Path(__file__).resolve().parents[1] / "app/build.gradle.kts"
s = p.read_text(encoding="utf-8")
old = s

# Promote the Android package metadata used by the actual release build.
s = s.replace("versionCode = 22", "versionCode = 23")
s = s.replace('versionName = "0.10.3"', 'versionName = "0.10.4"')

# The migration self-audit contains the expected versionName inside a Kotlin
# string literal, so the quotes are escaped in the source file. Update that
# literal too; otherwise the release build legitimately becomes 0.10.4 and the
# old 0.10.3 guard rejects it.
s = s.replace('versionName = \\"0.10.3\\"', 'versionName = \\"0.10.4\\"')
s = s.replace(
    "0.10.3 must use versionCode 22 for an in-place update over 0.10.2.",
    "0.10.4 must use versionCode 23 for an in-place update over 0.10.3.",
)
s = s.replace(
    "Expected audited personal Plus update 0.10.3.",
    "Expected audited Quran Safeguard update 0.10.4.",
)

# Idempotent preparation is intentional: a retry after a previous release
# preparation must verify the desired state instead of failing merely because
# the metadata is already correct.
required = (
    "versionCode = 23",
    'versionName = "0.10.4"',
    'versionName = \\"0.10.4\\"',
)
for marker in required:
    if marker not in s:
        raise SystemExit(f"0.10.4 version preparation missing required marker: {marker}")

if 'versionName = \\"0.10.3\\"' in s:
    raise SystemExit("0.10.4 version preparation left stale migration versionName guard")

if s != old:
    p.write_text(s, encoding="utf-8")
    print("Prepared release metadata and migration gate for versionCode 23 / versionName 0.10.4")
else:
    print("0.10.4 release metadata and migration gate already prepared")
