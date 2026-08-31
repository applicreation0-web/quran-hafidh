#!/usr/bin/env python3
import json
from pathlib import Path

ROOT=Path("app/src/main/assets/hikam")
ACTIVE=ROOT/"al_hikam_verified.json"
CANDIDATES=ROOT/"al_hikam_candidates.json"
PENDING=ROOT/"pending_verification.json"

def load(path):
    assert path.is_file(), f"Missing corpus file: {path}"
    data=json.loads(path.read_text(encoding="utf-8"))
    assert isinstance(data,list), f"{path} must be a JSON array"
    return data

def common(item):
    required=(
        "id","collection","author","arabic","french","source_title",
        "source_edition","source_number","verification_status",
        "verification_sources","text_type","themes",
        "estimated_reading_seconds","translation_status",
        "translation_sources","translation_method"
    )
    for key in required:
        assert key in item, f"{item.get('id')} missing {key}"
    assert item["collection"]=="al_hikam_al_ataiyya"
    assert item["author"]=="Ibn Ata Allah al-Iskandari"
    assert item["text_type"]=="author_wisdom"
    assert str(item["arabic"]).strip()
    assert str(item["french"]).strip(), f"{item['id']} has no French translation"
    assert str(item["source_title"]).strip()
    assert str(item["source_edition"]).strip()
    assert str(item["source_number"]).strip()
    assert isinstance(item["verification_sources"],list) and item["verification_sources"]
    assert isinstance(item["translation_sources"],list) and item["translation_sources"]
    assert str(item["translation_method"]).strip()
    assert isinstance(item["themes"],list) and item["themes"]
    assert int(item["estimated_reading_seconds"]) > 0
    # Hikam are author wisdom, never hadith-graded.
    assert "hadith_grade" not in item
    assert "authenticity" not in item

active=load(ACTIVE)
candidates=load(CANDIDATES)
pending=load(PENDING)

for collection in (active,candidates,pending):
    ids=[x.get("id") for x in collection]
    assert len(ids)==len(set(ids)), "Duplicate Hikam IDs"
    nums=[str(x.get("source_number")) for x in collection]
    assert len(nums)==len(set(nums)), "Duplicate source numbers in same corpus"
    for item in collection:
        common(item)

# Candidate corpus is the edition workbench and must cover 1..264 with French.
assert len(candidates)==264, f"Expected 264 extracted candidates, got {len(candidates)}"
assert sorted(int(x["source_number"]) for x in candidates)==list(range(1,265))

for item in active:
    assert item["verification_status"]=="verified", item["id"]
    assert item["translation_status"]=="verified", item["id"]

for item in pending:
    assert (
        item["verification_status"]!="verified" or
        item["translation_status"]!="verified"
    ), f"Verified entry incorrectly left pending: {item['id']}"

candidate_ids={x["id"] for x in candidates}
assert {x["id"] for x in active}.issubset(candidate_ids)
assert {x["id"] for x in pending}.issubset(candidate_ids)
assert not ({x["id"] for x in active} & {x["id"] for x in pending})

print(json.dumps({
    "candidates":len(candidates),
    "active_verified":len(active),
    "pending":len(pending),
    "all_active_have_french":all(bool(x["french"].strip()) for x in active)
},ensure_ascii=False))
