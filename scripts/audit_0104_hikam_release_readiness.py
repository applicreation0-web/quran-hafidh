#!/usr/bin/env python3
"""Release-readiness audit for the 0.10.4 Al-Hikam contract.

Unlike the scaffold gate, this script is expected to FAIL until the 264/264 matn-only
boundary audit has populated the new provenance fields. It must pass before 0.10.4 is
merged/released.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"


def fingerprint(item: dict) -> str:
    payload = (
        f'{int(item["source_number"])}\n'
        f'{str(item["arabic"]).strip()}\n'
        f'{str(item["arabic_vocalized"]).strip()}'
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


entries = json.loads(CORPUS.read_text(encoding="utf-8"))
assert len(entries) == 264
assert [int(item["source_number"]) for item in entries] == list(range(1, 265))

failures: list[str] = []
for item in entries:
    number = int(item["source_number"])
    if item.get("matn_boundary_status") != "verified":
        failures.append(f"{number}: boundary not verified from matn-only source")
    if item.get("matn_primary_source_kind") != "matn_only":
        failures.append(f"{number}: primary boundary source is not marked matn_only")
    witnesses = item.get("independent_witness_ids")
    if not isinstance(witnesses, list) or len(set(witnesses)) < 2:
        failures.append(f"{number}: fewer than two independent witnesses")
    expected = str(item.get("matn_snapshot_sha256", "")).strip()
    if expected != fingerprint(item):
        failures.append(f"{number}: matn snapshot mismatch")
    if item.get("translation_status") == "verified":
        translation_hash = str(
            item.get("translation_matn_snapshot_sha256", "")
        ).strip()
        if translation_hash != expected:
            failures.append(f"{number}: French translation audited against stale/unfrozen matn")

# Positive long-matn regression: never shorten Hikma 16 heuristically.
h16 = entries[15]
if int(h16["source_number"]) != 16 or len(str(h16["arabic"]).strip()) < 400:
    failures.append("16: long canonical matn appears truncated")

expected_french = {
    174: "La survenue des privations est une fête pour les aspirants.",
    175: "Il se peut que tu trouves dans les privations un surcroît que tu ne trouves ni dans le jeûne ni dans la prière.",
    176: "Les privations sont les tapis des dons.",
}
for number, expected in expected_french.items():
    if str(entries[number - 1].get("french", "")).strip() != expected:
        failures.append(f"{number}: required 0.10.3 French correction regressed")

if failures:
    print(f"0.10.4 Hikam release audit: FAIL ({len(failures)} findings)")
    for finding in failures[:40]:
        print("-", finding)
    if len(failures) > 40:
        print(f"- ... {len(failures) - 40} additional findings")
    raise SystemExit(1)

print("0.10.4 Hikam release audit: PASS")
