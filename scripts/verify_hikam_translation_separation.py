#!/usr/bin/env python3
"""Release gate for the 0.10.4 Al-Hikam translation/presentation contract."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
DETAIL = ROOT / "app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt"
LEXICON = ROOT / "app/src/main/java/com/quranunlock/guard/HikamTechnicalLexicon.kt"
PLUS_SHARH = ROOT / "app/src/plus/java/com/quranunlock/guard/HikamSharhEdition.kt"
LIGHT_SHARH = ROOT / "app/src/light/java/com/quranunlock/guard/HikamSharhEdition.kt"
REFERENCE = "https://islamicpearls.net/ibnattaillah2.html"

APPROVED = {
    174: "La survenue des privations est une fête pour les aspirants.",
    175: "Il se peut que tu trouves dans les privations un surcroît que tu ne trouves ni dans le jeûne ni dans la prière.",
    176: "Les privations sont les tapis des dons.",
}

entries = json.loads(CORPUS.read_text(encoding="utf-8"))
failures: list[str] = []

if len(entries) != 264:
    failures.append(f"expected 264 Hikam, got {len(entries)}")

numbers = [int(item.get("source_number", 0)) for item in entries]
if numbers != list(range(1, 265)):
    failures.append("Hikam numbering must be exactly 1..264 in source order")

for item in entries:
    number = int(item.get("source_number", 0))
    if item.get("explanation", "").strip():
        failures.append(f"Hikma {number}: explanation must remain empty in canonical corpus")
    forbidden_keys = {
        "commentary", "comment", "sharh", "commentator", "simpleExplanation",
        "aiSummary", "summary", "meaning"
    }
    leaked = sorted(forbidden_keys.intersection(item.keys()))
    if leaked:
        failures.append(f"Hikma {number}: commentary/AI field leaked into main corpus: {leaked}")

    audit = item.get("translation_cross_audit") or {}
    if audit.get("status") != "cross_audited_against_english_reference":
        failures.append(f"Hikma {number}: missing English cross-audit status")
    if audit.get("reference") != REFERENCE:
        failures.append(f"Hikma {number}: wrong English cross-audit reference")
    if audit.get("authority_rule") != "verified_arabic_remains_authoritative":
        failures.append(f"Hikma {number}: Arabic-authority rule missing")

by_number = {int(item["source_number"]): item for item in entries}
for number, expected in APPROVED.items():
    actual = by_number.get(number, {}).get("french")
    if actual != expected:
        failures.append(f"Hikma {number}: approved French regression: {actual!r}")

ui = DETAIL.read_text(encoding="utf-8")
for marker in (
    "ḤIKMA — TEXTE ARABE",
    "TRADUCTION FRANÇAISE",
    "AIDE TERMINOLOGIQUE",
    "COMMENTAIRE CLASSIQUE — SHARḤ",
    "Repères éditoriaux séparés du texte de la Ḥikma et du commentaire classique.",
):
    if marker not in ui:
        failures.append(f"Hikam UI separation marker missing: {marker}")

lexicon = LEXICON.read_text(encoding="utf-8")
for key in (
    "ʿārif", "adab", "aghyār", "al-Ḥaqq", "ḥaḍra", "madad", "murīd",
    "qabḍ", "basṭ", "sālik", "wārid", "wird", "yaqīn", "dhikr", "zuhd",
    "tajrīd", "asbāb", "himma", "maqām", "ḥāl", "maʿrifa"
):
    if key not in lexicon:
        failures.append(f"technical glossary term missing: {key}")

plus = PLUS_SHARH.read_text(encoding="utf-8")
light = LIGHT_SHARH.read_text(encoding="utf-8")
if 'hikam/hikam_sharh_dual.json' not in plus:
    failures.append("Plus sharh asset boundary missing")
if "const val isEnabled: Boolean = false" not in light:
    failures.append("Light Hikam sharh must remain disabled")

if failures:
    raise SystemExit("HIKAM TRANSLATION/SEPARATION FAILURE:\n- " + "\n- ".join(failures))

print("Hikam 0.10.4 translation/separation audit PASS")
print("- 264/264 canonical Hikam present")
print("- canonical corpus contains no commentary or AI explanation")
print("- 264/264 carry the English secondary cross-audit metadata with Arabic authority")
print("- approved 174-176 translations preserved")
print("- matn, French translation, terminology aid and classical sharh are visibly separated")
print("- technical glossary is an editorial aid, not part of the author text")
print("- classical sharh remains Plus-only")
