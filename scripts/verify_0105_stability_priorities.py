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

# Automatic last-page resume and explicit multiple bookmarks are separate features.
require('private const val KEY_LAST_PAGE = "last_page"' in free_reader,
        "automatic last-page resume key missing")
require("QuranBookmarkStore.load(this)" in free_reader,
        "reader must load the persistent multiple-bookmark set")
require("fun load(context: Context): Set<Int>" in bookmark_store,
        "multiple-bookmark load contract missing")
require("fun toggle(context: Context, page: Int): Set<Int>" in bookmark_store,
        "multiple-bookmark toggle contract missing")
require('private const val KEY_BOOKMARK_PAGES = "bookmark_pages"' in bookmark_store,
        "multiple bookmarks must use their own persistent set key")
require('private const val LEGACY_SINGLE_BOOKMARK = "bookmark_page"' in bookmark_store,
        "legacy single bookmark migration must remain supported")
require("QuranBookmarkStore.toggle(" in free_reader,
        "reader bookmark add/remove action missing")
require("page in bookmarkPages" in free_reader,
        "current-page bookmark state missing")
require("bookmarkPages.sorted().forEach" in free_reader,
        "saved bookmarks must be listed for direct navigation")
require("showPage(bookmarkedPage)" in free_reader,
        "saved bookmarks must navigate directly to their page")
require("TafsirEdition.load" in free_reader and "TafsirEdition.Panel" in free_reader,
        "bookmark work must not remove Tafsir integration")

# Removed feature must not survive in any compiled Android source/test source set.
for root in (
    ROOT / "app/src/main",
    ROOT / "app/src/light",
    ROOT / "app/src/plus",
    ROOT / "app/src/test",
    ROOT / "app/src/testPlus",
):
    if not root.exists():
        continue
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        require("taddabur" not in path.name.lower(),
                f"removed feature filename remains: {path}")
        if path.suffix.lower() in {".kt", ".java", ".xml", ".kts"}:
            require("taddabur" not in path.read_text(errors="ignore").lower(),
                    f"removed feature reference remains: {path}")

# Tafsir is release-critical in Plus.
require("const val isEnabled: Boolean = true" in plus_tafsir,
        "Plus Tafsir must remain enabled")
for edition in ("JALALAYN", "QUSHAYRI", "QURTUBI"):
    require(edition in multitafsir_repo.upper(), f"{edition} Tafsir integration missing")

# Hikam comments are optional; verified Arabic + French is mandatory for all 264 entries.
require(hikam_report.get("reference_count") == 264,
        "Hikam corpus must remain 264 entries")
require(hikam_report.get("verified_and_translated") == 264,
        "all 264 Hikam must remain verified and translated")
require(hikam_report.get("missing_french_translation") == 0,
        "no Hikma may be active without French translation")

print("PASS: Tafsir critical, multiple Quran bookmarks, 264 translated Hikam, removed runtime absent")
