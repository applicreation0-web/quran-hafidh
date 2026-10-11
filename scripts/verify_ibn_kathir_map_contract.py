#!/usr/bin/env python3
"""Mushaf-first private Quran.com Ibn Kathir titles: no public corpus, no browser."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=root/"hifz-app/src/main/java/com/quransafeguard/hifz/preview"
lecture=(java/"StudyReaderActivity.java").read_text(encoding="utf-8")
titles=(java/"IbnKathirQuranComPrivateTitles.java").read_text(encoding="utf-8")
manifest=(root/"hifz-app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
for token in ('onSemanticCueTap(String passageId)',
              'IbnKathirQuranComPrivateTitles.sourceHeadings(this,group)',
              'SemanticTitlePopup.showEnglish(this,label,semanticTitleDialog)',
              'readerActions.addView(semanticButton)',
              'ibnKathirCues.readerCuesForPage(page)', '"Masquer les amorces"'):
    assert token in lecture, "Missing Mushaf-first role: "+token
for legacy in ('IbnKathirOfflineTitles.verifiedHeadings(group)',
               'IbnKathirTitlePack.sourceHeadings(this,group)'):
    assert legacy not in lecture, "Old spa5k title fallback forbidden: "+legacy
popup=(java/"SemanticTitlePopup.java").read_text(encoding="utf-8")
assert "private static Dialog create(" in popup
assert "attrs.alpha=0f;" in popup and "attrs.alpha=1f;" in popup
assert "getLocationOnScreen(iconTop)" in popup
for forbidden in ('openIbnKathirMap','Intent.ACTION_VIEW',
                  'new Intent(this,IbnKathirMapActivity.class)',
                  'IbnKathirQuranComSource.urlForGroup'):
    assert forbidden not in lecture, "External list/network dependency: "+forbidden
for name in ('IbnKathirMapActivity.java','IbnKathirQuranComSource.java','QuizActivity.java'):
    assert not (java/name).exists(), "Retired runtime class persists: "+name
assert 'IbnKathirMapActivity' not in manifest and 'QuizActivity' not in manifest
assert 'android.permission.RECORD_AUDIO' not in manifest
for token in ('ibn_kathir_qurancom_private','IK_QURANCOM_PRIVATE_V1',
              'source_resource_id','FAILED.put(chunk, true)','return new String[0]'):
    assert token in titles, "Private fail-closed title contract missing: "+token
for token in ('getSharedPreferences(', 'prefs.set', 'startActivity(', 'https://'):
    assert token not in titles, "Private title reader must never mutate Hifz/network state"
import subprocess
tracked=subprocess.check_output(
    ["git","ls-files","--","hifz-app/src/main/assets/ibn_kathir_qurancom_private"],
    cwd=root,text=True).strip()
assert not tracked, "PRIVATE source is tracked in PUBLIC repo"
print("PASS: private Quran.com title loader, old spa5k display disabled, 1903 index intact, no Quiz, NO APK")
