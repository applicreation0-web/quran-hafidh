#!/usr/bin/env python3
"""J4 static map audit, not an on-device gesture certificate."""
from pathlib import Path
s=(Path(__file__).resolve().parents[1]/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirMapActivity.java").read_text(encoding="utf-8")
required=["IbnKathirGroupIndex.shared()", "group.navigationRange()", "group.id",
          "setOnItemLongClickListener", "showBlockDetails()", "IbnKathirQuranComSource.urlForGroup(group)",
          "Lire Ibn Kathīr", "Titres originaux anglais vérifiés",
          "advancePreviewFromCaption()", "previewCaption.setOnClickListener",
          "previewPage = previewPage>=last ? first : previewPage+1",
          "previewCaption.setContentDescription",
          "openPreviewInMushaf(verse)", "tappedVerse.getAyah()>=group.startAyah",
          "g.versesOnLines(g.lineIdsOnPage(page)).contains(tappedVerse)",
          "EXTRA_MAP_PREVIEW,true", 'out.putInt("selectedGroupIndex",selectedGroupIndex)',
          'out.putInt("previewPage",previewPage)', "setHighlightVerses(exact)"]
for token in required:
    if token not in s:
        raise SystemExit("MAP J4 FAIL: "+token)
for token in ("prefs.set", "hifzPrefs.set", "writeProgress", "getSharedPreferences("):
    if token in s:
        raise SystemExit("MAP J4 FAIL: mutated reader/progression state")
source=Path(__file__).resolve().parents[1]/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirQuranComSource.java"
s2=source.read_text(encoding="utf-8")
for token in ("https://quran.com/en/", "/tafsirs/en-tafisr-ibn-kathir",
              "verifiedQafHeadings", "group.startAyah", "new String[0]"):
    if token not in s2:raise SystemExit("SOURCE LINK J4 FAIL: "+token)
for forbidden in ("getSharedPreferences(", "prefs.set", "writeProgress", "setSemanticCues("):
    if forbidden in s2:raise SystemExit("J4 SOURCE LINK MUTATES HIFZ: "+forbidden)
print("PASS: J4 external Quran.com source/navigation contracts, no Hifz mutations (not on-device).")
