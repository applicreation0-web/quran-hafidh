#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAIL: {message}")


free_reader = (ROOT / "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt").read_text()
bookmark_store = (ROOT / "app/src/main/java/com/quranunlock/guard/QuranBookmarkStore.kt").read_text()
plus_tafsir = (ROOT / "app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt").read_text()
multitafsir_repo = (ROOT / "app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt").read_text()
hikam_report = json.loads((ROOT / "app/src/main/assets/hikam/verification_report.json").read_text())

require('private const val KEY_LAST_PAGE = "last_page"' in free_reader,
        "automatic last-page resume key missing")
require("QuranBookmarkStore" in free_reader,
        "reader must use the multiple-bookmark store")
require("fun toggle(page: Int)" in bookmark_store and "fun pages(): List<Int>" in bookmark_store,
        "multiple-bookmark persistence contract missing")
require("bookmarks.contains(page)" in free_reader and "bookmarks.forEach" in free_reader,
        "multiple bookmarks must be visible and directly navigable")
require("TafsirEdition.load" in free_reader and "TafsirEdition.Panel" in free_reader,
        "bookmark work must not remove Tafsir integration")

for root in (ROOT / "app/src/main", ROOT / "app/src/light", ROOT / "app/src/plus", ROOT / "app/src/test", ROOT / "app/src/testPlus"):
    if not root.exists():
        continue
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        require("taddabur" not in path.name.lower(), f"removed feature filename remains: {path}")
        if path.suffix.lower() in {".kt", ".java", ".xml", ".kts"}:
            require("taddabur" not in path.read_text(errors="ignore").lower(),
                    f"removed feature reference remains: {path}")

require("const val isEnabled: Boolean = true" in plus_tafsir,
        "Plus Tafsir must remain enabled")
for edition in ("JALALAYN", "QUSHAYRI", "QURTUBI"):
    require(edition in multitafsir_repo.upper(), f"{edition} Tafsir integration missing")

require(hikam_report.get("reference_count") == 264, "Hikam corpus must remain 264 entries")
require(hikam_report.get("verified_and_translated") == 264, "all 264 Hikam must remain verified and translated")
require(hikam_report.get("missing_french_translation") == 0, "no Hikma may be active without French translation")

print("PASS: Tafsir critical, multiple Quran bookmarks, 264 translated Hikam, removed runtime absent")
