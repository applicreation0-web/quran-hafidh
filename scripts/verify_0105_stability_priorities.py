#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"FAIL: {message}")


free_reader = (ROOT / "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt").read_text()
bookmark_store = (ROOT / "app/src/main/java/com/quranunlock/guard/QuranBookmarkStore.kt").read_text()
plus_taddabur = (ROOT / "app/src/plus/java/com/quranunlock/guard/TaddaburEdition.kt").read_text()
plus_manifest = (ROOT / "app/src/plus/AndroidManifest.xml").read_text()
plus_tafsir = (ROOT / "app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt").read_text()
multitafsir_repo = (ROOT / "app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt").read_text()
hikam_report = json.loads((ROOT / "app/src/main/assets/hikam/verification_report.json").read_text())

# Qur'an reader: automatic last-page resume and multiple explicit user bookmarks are distinct.
require('private const val KEY_LAST_PAGE = "last_page"' in free_reader,
        "automatic last-page resume key missing")
require('private const val KEY_BOOKMARK_PAGES = "bookmark_pages"' in bookmark_store,
        "multiple-bookmark persistence key missing")
require("getStringSet(KEY_BOOKMARK_PAGES" in bookmark_store and
        "putStringSet(KEY_BOOKMARK_PAGES" in bookmark_store,
        "multiple bookmarks must be persisted as a set")
require("fun toggle(context: Context, page: Int): Set<Int>" in bookmark_store,
        "bookmark add/remove action missing")
require("var bookmarkPages" in free_reader and "bookmarkPages.sorted().forEach" in free_reader,
        "reader must expose all saved bookmarks")
require("Ajouter un marque-page" in free_reader and "Retirer le marque-page" in free_reader,
        "multiple-bookmark user feedback missing")
require("TafsirEdition.load" in free_reader and "TafsirEdition.Panel" in free_reader,
        "bookmark work must not remove Tafsir integration")

# Taddabur is deliberately deferred from 0.10.5 runtime, while its core remains in source.
require("const val isEnabled: Boolean = false" in plus_taddabur,
        "Plus Taddabur runtime must be disabled")
require("fun shouldBlockNow(context: Context): Boolean = false" in plus_taddabur,
        "Taddabur must never block protected apps in 0.10.5")
require("fun scheduleReminder(context: Context) = Unit" in plus_taddabur,
        "Taddabur alarm scheduling must be disabled")
require("TaddaburActivity" not in plus_manifest,
        "deferred Taddabur activity must not be declared in Plus manifest")
require((ROOT / "app/src/plus/java/com/quranunlock/guard/TaddaburLegacyCore.kt").exists(),
        "Taddabur core must remain preserved for later work")

# Tafsir is a release-critical Plus feature.
require("const val isEnabled: Boolean = true" in plus_tafsir,
        "Plus Tafsir must remain enabled")
for edition in ("JALALAYN", "QUSHAYRI", "QURTUBI"):
    require(edition in multitafsir_repo.upper(), f"{edition} Tafsir integration missing")

# Hikam: comments are optional, but 264/264 verified Arabic+French translations are mandatory.
require(hikam_report.get("reference_count") == 264,
        "Hikam reference corpus must remain 264 entries")
require(hikam_report.get("verified_and_translated") == 264,
        "all 264 Hikam must remain verified and translated")
require(hikam_report.get("missing_french_translation") == 0,
        "no Hikma may be active without French translation")

print("PASS: 0.10.5 priorities locked — Tafsir critical, Quran multi-bookmarks persistent, Hikam translations complete, Taddabur runtime deferred")
