#!/usr/bin/env python3
"""Inventory every icon-bearing Android screen without altering any Hifz engine.

This static audit deliberately reports ambiguous/dynamic actions instead of
pretending their final rendered pictograms have been visually certified.
"""
from __future__ import annotations

import json
import pathlib
import re
from collections import Counter

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview"
ICONS = ROOT / "hifz-app/src/main/res/drawable"
MANIFEST = ROOT / "docs/FROZEN_UI_ICONS_13_PLUS_10.tsv"

def main():
    contract = []
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        kind, role, name, filename, sha, source = line.split("\t")
        contract.append({"kind": kind, "role": role, "icon": name,
                         "drawable": filename[:-4], "git_sha": sha})
    assert len(contract) == 23
    assert all((ICONS/(c["drawable"]+".xml")).is_file() for c in contract)
    screens = []
    total_actions = 0
    for p in sorted(JAVA.glob("*.java")):
        source = p.read_text(encoding="utf-8")
        # Include icon button helpers and separately constructed Android icon buttons.
        helper_calls = [(m.group(1), m.start()) for m in re.finditer(
            r"\bUi\.(iconButton|roundAction|setButtonIcon)\s*\(", source)]
        direct_drawables = sorted(set(re.findall(r"\bR\.drawable\.(ic_[a-z0-9_]+)",source)))
        if not helper_calls and not direct_drawables:
            continue
        details = []
        for kind, pos in helper_calls:
            # Only a literal constant can be statically validated; variable labels
            # are explicitly marked as needing a runtime/visual inspection.
            fragment = source[pos:pos+210].replace("\n"," ")
            literal = re.search(
                r'Ui\.(?:iconButton|roundAction)\s*\([^,]+,\s*"(?:[^"\\]|\\.)*"\s*,\s*"([^"]+)"',
                fragment)
            details.append({"method":kind,"line":source.count("\n",0,pos)+1,
                            "static_role":literal.group(1) if literal else None})
        total_actions += len(helper_calls)
        screens.append({"class":p.stem,"helpers":len(helper_calls),
                        "static_roles":[v["static_role"] for v in details if v["static_role"]],
                        "dynamic_or_direct":len([v for v in details if not v["static_role"]]),
                        "drawables":direct_drawables})
    assert screens and total_actions > 0
    counts = Counter(c["drawable"] for c in contract)
    assert len(counts)==23 and all(n==1 for n in counts.values())
    visual_only = {
       "screens_with_icon_actions":len(screens),
       "helper_invocations":total_actions,
       "static_roles_identified":sum(len(s["static_roles"]) for s in screens),
       "dynamic_calls_needing_on_device_review":sum(s["dynamic_or_direct"] for s in screens),
       "contracted_icons":23,
       "screens":screens,
    }
    path = ROOT/"build_icon_inventory.json"
    path.write_text(json.dumps(visual_only,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:v for k,v in visual_only.items() if k!="screens"},ensure_ascii=False))
    for s in screens:
        print(f"{s['class']}: {s['helpers']} icon helper calls, "
              f"{len(s['static_roles'])} static roles, "
              f"{s['dynamic_or_direct']} dynamic/manual, "
              f"{len(s['drawables'])} direct vector refs")
    print("PASS: screen-wide inventory generated (not a physical visual certificate).")

if __name__ == "__main__":
    main()
