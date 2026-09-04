#!/usr/bin/env python3
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/build.gradle.kts'
s=p.read_text(encoding='utf-8')
old=s
s=s.replace('versionCode = 22','versionCode = 23')
s=s.replace('versionName = "0.10.3"','versionName = "0.10.4"')
if s==old:
    raise SystemExit('0.10.4 version preparation made no changes')
if 'versionCode = 22' in s or 'versionName = "0.10.3"' in s:
    raise SystemExit('0.10.4 version preparation left stale version guards')
p.write_text(s,encoding='utf-8')
print('Prepared ephemeral release metadata: versionCode 23 / versionName 0.10.4')
