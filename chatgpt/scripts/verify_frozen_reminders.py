#!/usr/bin/env python3
import json
from pathlib import Path

PATH = Path("app/src/main/assets/reminders/hadeethenc_snapshot.json")
with PATH.open(encoding="utf-8") as handle:
    items = json.load(handle)

assert isinstance(items, list), "Reminder snapshot must be a JSON list"
assert len(items) > 0, "No frozen hadiths"
assert len({item["id"] for item in items}) == len(items), "Duplicate reminder IDs"

weak_markers = ("faible", "weak", "daif", "da'if", "mawdu", "fabriqué", "fabrique")

for item in items:
    assert item.get("type") == "HADITH"
    assert item.get("arabicText", "").strip()
    assert item.get("frenchText", "").strip()
    assert item.get("reference", "").strip()
    assert item.get("authenticity", "").strip()
    assert item.get("translationSource") == "HadeethEnc.com Français v1.17.0"
    assert item.get("sourceSnapshotDate") == "2026-08-31"
    assert item.get("displayEligible") is True
    assert item.get("theme") in {
        "coran", "famille", "voisinage", "douceur", "patience",
        "sincerite", "gratitude", "generosite", "proprete", "entraide",
        "discipline", "bonnes_moeurs", "science", "dhikr", "priere"
    }
    assert "HadeethEnc.com Français v1.17.0" in item["reference"]
    assert "https://hadeethenc.com/fr/browse/hadith/" in item["reference"]
    assert (
        "Sahih al-Bukhari" in item.get("book", "") or
        "Sahih Muslim" in item.get("book", "")
    ), item.get("book")
    grade = item["authenticity"].lower()
    assert not any(marker in grade for marker in weak_markers), item["authenticity"]

    content = (
        item.get("frenchText", "") + " " +
        " ".join(item.get("tags", []))
    ).lower()
    forbidden = (
        "guerre", "combat", "esclave", "fornication", "adultère",
        "adultere", "lapid", "fouet", "menstrue", "rapport charnel",
        "rapports charnels", "organe génital", "organe genital",
        "sperme", "coït", "coit", "divorce", "héritage", "heritage",
        "butin", "captif", "expédition", "expedition"
    )
    assert not any(marker in content for marker in forbidden), item["id"]

print("verified frozen reminders:", len(items))
