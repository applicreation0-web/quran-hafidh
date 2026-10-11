#!/usr/bin/env python3
"""Strict source pin for the audited P0 Itqan rotation writer."""
import subprocess
from pathlib import Path
path="hifz-app/src/main/java/com/quransafeguard/hifz/preview/ItqanRegimeStore.java"
source=Path(path).read_text(encoding="utf-8")
expected="f64ba264dc87762681e80d8abd97eb71fff6e41b"
assert subprocess.check_output(["git","hash-object",path],text=True).strip()==expected, "Unreviewed Itqan rotation change"
assert 'static SharedPreferences.Editor appendRotationState(' in source
assert 'return appendRotationState(p,p.edit(),state).commit();' in source
assert 'return e.putString(LEG,latched.leg.name())' in source
print("PASS: exact Itqan writer with atomic append semantics")
