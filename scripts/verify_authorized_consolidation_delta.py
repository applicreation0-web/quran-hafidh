#!/usr/bin/env python3
"""Exact J4 exception: two stabilization evening quotas; all other cycle logic frozen."""
from pathlib import Path
import subprocess

if (subprocess.check_output(["git","hash-object",
      "hifz-app/src/main/java/com/quransafeguard/hifz/preview/ConsolidationCycleEngine.java"],text=True).strip()
    == "40b871eabb0b9dde10dd2e4e2a8ee1bca2fdd45c"):
    subprocess.run(["python3","scripts/verify_1175_post_nas_weekly_delta.py"],check=True)
    print("PASS: exact approved BOOX/weekly delta with historic finals frozen")
    raise SystemExit(0)

BASE="7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21"
FILE="hifz-app/src/main/java/com/quransafeguard/hifz/preview/ConsolidationCycleEngine.java"
old=subprocess.check_output(["git","show",f"{BASE}:{FILE}"]).decode("utf-8")
pairs=[
    ("SNOWBALL, SNOWBALL_FINAL, SNOWBALL_EXTENDED }","SNOWBALL, SNOWBALL_FINAL, SNOWBALL_EXTENDED, SNOWBALL_STABILIZATION_SEVEN, SNOWBALL_CONTINUOUS }"),
    ("        if (protocol == Protocol.SNOWBALL_EXTENDED) return 30;","        if (protocol == Protocol.SNOWBALL_EXTENDED) return 30;\n        if (protocol == Protocol.SNOWBALL_STABILIZATION_SEVEN || protocol == Protocol.SNOWBALL_CONTINUOUS) return 31; // up to 30 composite subblocks + old continuous"),
    ("            case SNOWBALL:\n                return copy(10, 0, 0, 0, 0);","            case SNOWBALL:\n                return copy(10, 0, 0, 0, 0); // legacy and Renforcement, frozen\n            case SNOWBALL_STABILIZATION_SEVEN:\n                return copy(7, 0, 0, 0, 0); // only newly stabilized block on this evening\n            case SNOWBALL_CONTINUOUS:\n                return copy(1, 0, 0, 0, 0); // previous blocks once continuously"),
    ("        if (unit.protocol() == Protocol.SNOWBALL || unit.protocol() == Protocol.SNOWBALL_FINAL\n                || unit.protocol() == Protocol.SNOWBALL_EXTENDED) return;","        if (unit.protocol() == Protocol.SNOWBALL || unit.protocol() == Protocol.SNOWBALL_FINAL\n                || unit.protocol() == Protocol.SNOWBALL_EXTENDED) return;\n        if (unit.protocol() == Protocol.SNOWBALL_STABILIZATION_SEVEN\n                || unit.protocol() == Protocol.SNOWBALL_CONTINUOUS) {\n            if(family!=Family.STABILIZATION)throw new IllegalArgumentException(\n                \"Seven-times/continuous protocols belong to Stabilisation only\");\n            return;\n        }"),
]
for before,after in pairs:
    assert old.count(before)==1,"Official ConsolidationCycleEngine baseline unexpectedly differs"
    old=old.replace(before,after)
# 1.17.5's next explicitly approved Sunday-page protocol retains all legacy
# engine vectors, including SNOWBALL_EXTENDED for persisted OPEN sessions.
for before, after in (
    ("SNOWBALL_EXTENDED, SNOWBALL_STABILIZATION_SEVEN, SNOWBALL_CONTINUOUS }",
     "SNOWBALL_EXTENDED, SNOWBALL_STABILIZATION_SEVEN, SNOWBALL_CONTINUOUS, SUNDAY_PAGE }"),
    ("if (protocol == Protocol.SNOWBALL_EXTENDED) return 30;",
     "if (protocol == Protocol.SNOWBALL_EXTENDED || protocol == Protocol.SUNDAY_PAGE) return 30;"),
    ("            case SNOWBALL_EXTENDED:\\n                return copy(3, 0, 0, 0, 0);",
     "            case SNOWBALL_EXTENDED:\\n                return copy(3, 0, 0, 0, 0);\\n            case SUNDAY_PAGE:\\n                return copy(3, 0, 0, 0, 0); // one visible + two anchored masked passes"),
    ("|| unit.protocol() == Protocol.SNOWBALL_EXTENDED) return;",
     "|| unit.protocol() == Protocol.SNOWBALL_EXTENDED\\n                || unit.protocol() == Protocol.SUNDAY_PAGE) return;"),
):
    before = before.replace("\\n", "\n")
    after = after.replace("\\n", "\n")
    assert old.count(before)==1, "Authorized Sunday page engine baseline drift: " + before
    old=old.replace(before,after)
assert Path(FILE).read_text(encoding="utf-8")==old,"UNAUTHORIZED ConsolidationCycleEngine change"
print("PASS: Renforcement, old sessions, all frozen cycle logic unchanged; only J4 ×7/continuous introduced")
