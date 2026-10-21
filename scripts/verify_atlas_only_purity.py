#!/usr/bin/env python3
"""No runtime or test-linked Correspondances code in the Atlas-only BOOX build."""
from pathlib import Path
import subprocess, re
root=Path("hifz-app/src")
entries=subprocess.check_output(["git","ls-files","hifz-app/src/main","hifz-app/src/test","hifz-app/src/androidTest","scripts"],text=True).splitlines()
live=[x for x in entries if Path(x).exists()]
bad=[x for x in live if re.search(r"Correspondence|correspondence|ic_corr",x) and
     ("/src/" in x or x.startswith("scripts/"))]
if bad: raise SystemExit("Correspondances feature files remain: "+repr(bad[:30]))
for path in [
 "hifz-app/src/main/AndroidManifest.xml",
 "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java",
 "hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java",
 "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java",
 "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MawdudiMapScreen.java",
 "hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java",
 "hifz-app/src/test/java/com/quransafeguard/hifz/preview/UiIconMappingTest.java",
]:
 data=Path(path).read_text()
 if re.search(r"Correspondence|correspondences|correspondance|ic_corr",data,re.I):
  raise SystemExit("Active feature reference remains: "+path)
print("PASS: Atlas-only source, manifest, runtime, icon and test inventory.")
