#!/usr/bin/env python3
from pathlib import Path

p = Path(__file__).resolve().parents[1] / "app/build.gradle.kts"
s = p.read_text(encoding="utf-8")
old = s

# 0.10.4 private APK used versionCode 23. 0.10.5 must therefore be strictly
# higher so it can update both 0.10.3 (22) and 0.10.4 (23) in place.
s = s.replace("versionCode = 22", "versionCode = 24")
s = s.replace("versionCode = 23", "versionCode = 24")
s = s.replace('versionName = "0.10.3"', 'versionName = "0.10.5"')
s = s.replace('versionName = "0.10.4"', 'versionName = "0.10.5"')

# Update source-string guards embedded in the release migration audit.
for previous in ("0.10.3", "0.10.4"):
    s = s.replace(
        f'versionName = \\"{previous}\\"',
        'versionName = \\"0.10.5\\"',
    )
s = s.replace(
    "0.10.3 must use versionCode 22 for an in-place update over 0.10.2.",
    "0.10.5 must use versionCode 24 for an in-place update over 0.10.4.",
)
s = s.replace(
    "0.10.4 must use versionCode 23 for an in-place update over 0.10.3.",
    "0.10.5 must use versionCode 24 for an in-place update over 0.10.4.",
)
s = s.replace(
    "Expected audited personal Plus update 0.10.3.",
    "Expected audited Quran Safeguard update 0.10.5.",
)
s = s.replace(
    "Expected audited Quran Safeguard update 0.10.4.",
    "Expected audited Quran Safeguard update 0.10.5.",
)

required = (
    "versionCode = 24",
    'versionName = "0.10.5"',
    'versionName = \\"0.10.5\\"',
)
for marker in required:
    if marker not in s:
        raise SystemExit(f"0.10.5 version preparation missing required marker: {marker}")

for stale in (
    'versionName = \\"0.10.3\\"',
    'versionName = \\"0.10.4\\"',
):
    if stale in s:
        raise SystemExit(f"0.10.5 version preparation left stale migration guard: {stale}")

if s != old:
    p.write_text(s, encoding="utf-8")
    print("Prepared release metadata: versionCode 24 / versionName 0.10.5")
else:
    print("0.10.5 release metadata already prepared")
