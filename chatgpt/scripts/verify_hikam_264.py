#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
REPORT = ROOT / "app/src/main/assets/hikam/verification_report.json"

assert CORPUS.is_file(), f"Missing Hikam corpus: {CORPUS}"
assert REPORT.is_file(), f"Missing Hikam verification report: {REPORT}"

entries = json.loads(CORPUS.read_text(encoding="utf-8"))
report = json.loads(REPORT.read_text(encoding="utf-8"))

assert isinstance(entries, list), "Hikam corpus must be a JSON array"
assert len(entries) == 264, f"Expected 264 Hikam, got {len(entries)}"

numbers = [int(item["source_number"]) for item in entries]
assert numbers == list(range(1, 265)), "Hikam source_number must be exactly 1..264 in order"

ids = [str(item["id"]) for item in entries]
assert len(ids) == len(set(ids)), "Duplicate Hikam IDs"

for item in entries:
    number = int(item["source_number"])
    prefix = f"Hikma {number}: "

    assert str(item.get("arabic", "")).strip(), prefix + "missing Arabic"
    assert str(item.get("french", "")).strip(), prefix + "missing French"
    assert item.get("author") == "Ibn Ata Allah al-Iskandari", prefix + "wrong author"
    assert item.get("collection") == "al_hikam_al_ataiyya", prefix + "wrong collection"
    assert item.get("text_type") == "author_wisdom", prefix + "wrong text type"
    assert item.get("verification_status") == "verified", prefix + "Arabic/source not verified"
    assert item.get("translation_status") == "verified", prefix + "translation not verified"
    assert str(item.get("source_title", "")).strip(), prefix + "missing source title"
    assert str(item.get("source_edition", "")).strip(), prefix + "missing source edition"
    assert isinstance(item.get("verification_sources"), list) and item["verification_sources"], (
        prefix + "missing verification sources"
    )
    assert isinstance(item.get("translation_sources"), list) and item["translation_sources"], (
        prefix + "missing translation provenance"
    )
    assert str(item.get("translation_method", "")).strip(), prefix + "missing translation method"
    assert str(item.get("translator_note", "")).strip(), prefix + "missing translator note"
    assert not str(item.get("explanation", "")).strip(), prefix + "AI/editorial explanation forbidden"

    forbidden = {"simpleExplanation", "aiSummary", "meaning"}
    assert forbidden.isdisjoint(item.keys()), prefix + "forbidden interpretation field present"

assert report.get("reference_count") == 264
assert report.get("verified_and_translated") == 264
assert report.get("pending_verification") == 0
assert report.get("missing_french_translation") == 0

print(json.dumps({
    "hikam": len(entries),
    "range": "1-264",
    "verified_and_translated": report.get("verified_and_translated"),
    "pending": report.get("pending_verification")
}, ensure_ascii=False))
