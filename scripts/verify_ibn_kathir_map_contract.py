#!/usr/bin/env python3
"""J4 static map audit, not an on-device gesture certificate."""
from pathlib import Path
s=(Path(__file__).resolve().parents[1]/"hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirMapActivity.java").read_text(encoding="utf-8")
required=["IbnKathirGroupIndex.shared()", "group.navigationRange()", "group.id",
          "setOnItemLongClickListener", "showBlockDetails()", "contenu non disponible",
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
print("PASS: J4 static map source/navigation/return contract (not instrumented).")
