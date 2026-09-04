#!/usr/bin/env python3
"""Release-readiness audit for the 0.10.4 Al-Hikam contract.

This is intentionally stricter than the ordinary display gate. It is expected to FAIL
until the requested 264/264 matn-only boundary audit has been completed and its evidence
has been written into the corpus. No source comparison is inferred from a URL alone.
"""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
SOURCE_REGISTRY = ROOT / "editorial/hikam_0104_source_registry.json"
SHA256 = re.compile(r"^[0-9a-f]{64}$")
MATHESON_PRIMARY_ID = "matheson_2014_original_arabic"
# Frozen from the complete 0.10.3 Hikma 16 matn which was positively verified against
# the Matheson numbering: no length heuristic is used.
HIKMA_16_APPROVED_CANONICAL_SHA256 = (
    "db04f93fb5502b4b62526f97a8953d8e718366db41487b41cfeed5ed054cce15"
)


def canonical_matn_fingerprint(item: dict) -> str:
    payload = (
        f'{int(item["source_number"])}\n'
        f'{str(item["arabic"]).strip()}'
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def vocalized_fingerprint(item: dict) -> str:
    payload = (
        f'{int(item["source_number"])}\n'
        f'{str(item["arabic_vocalized"]).strip()}'
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def load_source_registry() -> dict[str, dict]:
    document = json.loads(SOURCE_REGISTRY.read_text(encoding="utf-8"))
    assert document.get("schema_version") == 1
    sources = document.get("sources")
    assert isinstance(sources, list) and sources
    by_id: dict[str, dict] = {}
    for source in sources:
        source_id = str(source.get("source_id", "")).strip()
        assert source_id and source_id not in by_id
        by_id[source_id] = source
    return by_id


entries = json.loads(CORPUS.read_text(encoding="utf-8"))
assert len(entries) == 264
assert [int(item["source_number"]) for item in entries] == list(range(1, 265))
registry = load_source_registry()

primary = registry.get(MATHESON_PRIMARY_ID)
assert primary is not None
assert primary.get("source_kind") == "matn_only"
assert primary.get("boundary_authority") is True
assert primary.get("counts_as_independent_witness") is False

eligible_witness_ids = {
    source_id
    for source_id, source in registry.items()
    if source.get("counts_as_independent_witness") is True
    and source_id != MATHESON_PRIMARY_ID
}

failures: list[str] = []
for item in entries:
    number = int(item["source_number"])
    if item.get("matn_boundary_status") != "verified":
        failures.append(f"{number}: boundary not verified from matn-only source")

    primary_id = str(item.get("matn_primary_source_id", "")).strip()
    if primary_id != MATHESON_PRIMARY_ID:
        failures.append(f"{number}: unapproved primary boundary source id")
    if item.get("matn_primary_source_kind") != "matn_only":
        failures.append(f"{number}: primary boundary source is not marked matn_only")
    if not str(item.get("matn_primary_source_url", "")).strip():
        failures.append(f"{number}: primary source URL missing")

    witnesses = item.get("independent_witness_ids")
    witness_ids = set(witnesses) if isinstance(witnesses, list) else set()
    if len(witness_ids) < 2:
        failures.append(f"{number}: fewer than two independent witnesses")
    unknown_or_ineligible = witness_ids - eligible_witness_ids
    if unknown_or_ineligible:
        failures.append(
            f"{number}: witness ids are not independently eligible: "
            + ", ".join(sorted(unknown_or_ineligible))
        )
    if primary_id and primary_id in witness_ids:
        failures.append(f"{number}: primary source incorrectly counted as independent witness")

    expected_matn = str(item.get("matn_snapshot_sha256", "")).strip()
    actual_matn = canonical_matn_fingerprint(item)
    if not SHA256.fullmatch(expected_matn) or expected_matn != actual_matn:
        failures.append(f"{number}: canonical matn snapshot mismatch")

    expected_vocalized = str(item.get("vocalized_snapshot_sha256", "")).strip()
    actual_vocalized = vocalized_fingerprint(item)
    if not SHA256.fullmatch(expected_vocalized) or expected_vocalized != actual_vocalized:
        failures.append(f"{number}: vocalized snapshot mismatch")

    if item.get("translation_status") == "verified":
        translation_hash = str(
            item.get("translation_matn_snapshot_sha256", "")
        ).strip()
        if translation_hash != expected_matn:
            failures.append(
                f"{number}: French translation audited against stale/unfrozen canonical matn"
            )

# Positive exact regression. A shorter/longer/different Hikma 16 does not pass merely because
# somebody updates a matching metadata hash: its independently frozen canonical fingerprint
# must remain the approved one until a documented source-variant review changes this gate.
h16 = entries[15]
if canonical_matn_fingerprint(h16) != HIKMA_16_APPROVED_CANONICAL_SHA256:
    failures.append("16: approved complete long matn changed or was truncated")
if int(entries[16]["source_number"]) != 17:
    failures.append("17: boundary immediately after the approved Hikma 16 is missing")

expected_french = {
    174: "La survenue des privations est une fête pour les aspirants.",
    175: "Il se peut que tu trouves dans les privations un surcroît que tu ne trouves ni dans le jeûne ni dans la prière.",
    176: "Les privations sont les tapis des dons.",
}
for number, expected in expected_french.items():
    if str(entries[number - 1].get("french", "")).strip() != expected:
        failures.append(f"{number}: required 0.10.3 French correction regressed")

# The main asset must remain pure matn/translation metadata. Even future schema additions may
# not place sharh text or commentator attribution inside the canonical Hikam entries.
for item in entries:
    forbidden = {
        "commentary",
        "sharh",
        "commentator",
        "commentary_arabic",
        "commentary_french",
        "commentary_work_id",
    } & set(item)
    if forbidden:
        failures.append(
            f'{item.get("source_number", "?")}: sharh fields leaked into main corpus: '
            + ", ".join(sorted(forbidden))
        )

if failures:
    print(f"0.10.4 Hikam release audit: FAIL ({len(failures)} findings)")
    for finding in failures[:60]:
        print("-", finding)
    if len(failures) > 60:
        print(f"- ... {len(failures) - 60} additional findings")
    raise SystemExit(1)

print("0.10.4 Hikam release audit: PASS")
