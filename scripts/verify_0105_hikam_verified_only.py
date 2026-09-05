#!/usr/bin/env python3
"""Fail-closed release gate for the 0.10.5 Hikam maxims-only scope."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
REPORT = ROOT / "app/src/main/assets/hikam/verification_report.json"
FORBIDDEN_PRODUCTION_SHARH = ROOT / "app/src/plus/assets/hikam/hikam_sharh_dual.json"


def fail(message: str) -> None:
    raise SystemExit("0.10.5 HIKAM VERIFIED-ONLY FAILURE: " + message)


entries = json.loads(CORPUS.read_text(encoding="utf-8"))
report = json.loads(REPORT.read_text(encoding="utf-8"))
special_recovery_numbers = {int(n) for n in report.get("special_recovery_numbers", [])}

if len(entries) != 264:
    fail(f"expected 264 verified maxims, found {len(entries)}")

numbers = [int(item.get("source_number", 0)) for item in entries]
if numbers != list(range(1, 265)):
    fail("source numbering is not the exact ordered range 1..264")

required_nonempty = (
    "arabic",
    "french",
    "source_title",
    "source_edition",
    "verification_status",
    "translation_status",
)
for item in entries:
    number = int(item["source_number"])
    for field in required_nonempty:
        if not str(item.get(field, "")).strip():
            fail(f"Hikma {number}: missing {field}")
    sources = item.get("verification_sources") or []
    source_page = str(item.get("source_page", "")).strip()
    if not source_page:
        # Hikma 233 is explicitly documented by the corpus verification report
        # as a special recovery outside the primary digital-page extraction.
        # Do not invent a primary page locator: require the exact documented
        # exception, three independent Arabic controls, and a recovery note.
        if number not in special_recovery_numbers:
            fail(f"Hikma {number}: missing source_page without documented special recovery")
        if len(sources) < 3:
            fail(f"Hikma {number}: special recovery needs at least three Arabic controls")
        if "recovered only after independent confirmation" not in item.get("verification_notes", ""):
            fail(f"Hikma {number}: special recovery explanation missing")
    elif number in special_recovery_numbers:
        fail(f"Hikma {number}: documented special recovery unexpectedly gained an unproved primary page")
    if item["verification_status"] != "verified":
        fail(f"Hikma {number}: Arabic matn is not verified")
    if item["translation_status"] != "verified":
        fail(f"Hikma {number}: French translation is not verified")
    if str(item.get("explanation", "")).strip():
        fail(f"Hikma {number}: explanation/commentary must be empty in 0.10.5")
    if item.get("text_type") != "author_wisdom":
        fail(f"Hikma {number}: text_type is not author_wisdom")
    if len(sources) < 2:
        fail(f"Hikma {number}: fewer than two Arabic verification controls")
    cross = item.get("translation_cross_audit") or {}
    if cross.get("status") != "cross_audited_against_english_reference":
        fail(f"Hikma {number}: English semantic cross-audit marker missing")
    if cross.get("authority_rule") != "verified_arabic_remains_authoritative":
        fail(f"Hikma {number}: Arabic authority rule changed")

if special_recovery_numbers != {233}:
    fail(f"unexpected special-recovery set: {sorted(special_recovery_numbers)}")

if FORBIDDEN_PRODUCTION_SHARH.exists():
    fail("commentary/sharh production asset must not ship in the maxims-only release")

if report.get("reference_count") != 264:
    fail("verification report reference_count changed")
if report.get("verified_and_translated") != 264:
    fail("verification report does not certify all 264 rows")
if report.get("pending_verification") != 0:
    fail("verification report still has pending Arabic rows")
if report.get("missing_french_translation") != 0:
    fail("verification report still has missing French translations")
if report.get("memory_generated_arabic") is not False:
    fail("Arabic source provenance rule changed")

print("0.10.5 Hikam verified-only gate: PASS")
print("- 264/264 ordered author maxims")
print("- Arabic and French present and verified for every row")
print("- Hikma 233 remains an explicit three-source special recovery; no locator invented")
print("- English reference is secondary cross-audit only; Arabic remains authoritative")
print("- no explanation/commentary text and no production sharh asset")
