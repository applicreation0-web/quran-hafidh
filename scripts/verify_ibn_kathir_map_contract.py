#!/usr/bin/env python3
"""Mushaf-first offline Ibn Kathir Amorces contract; NO semantic list or external reader."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=root/"hifz-app/src/main/java/com/quransafeguard/hifz/preview"
lecture=(java/"StudyReaderActivity.java").read_text(encoding="utf-8")
titles=(java/"IbnKathirOfflineTitles.java").read_text(encoding="utf-8")
manifest=(root/"hifz-app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
for token in ['onSemanticCueTap(String passageId)','IbnKathirOfflineTitles.verifiedHeadings(group)',
             'SemanticTitlePopup.showEnglish(this,label,semanticTitleDialog)','readerActions.addView(semanticButton)',
             'ibnKathirCues.readerCuesForPage(page)','"Masquer les amorces"']:
    assert token in lecture, "Missing Mushaf-first role: "+token
assert "private static Dialog create(" in (java/"SemanticTitlePopup.java").read_text(encoding="utf-8")
for forbidden in ('openIbnKathirMap', 'Intent.ACTION_VIEW',
                  'new Intent(this,IbnKathirMapActivity.class)', 'IbnKathirQuranComSource.urlForGroup'):
    assert forbidden not in lecture, "Legacy list/network dependency: "+forbidden
for name in ('IbnKathirMapActivity.java','IbnKathirQuranComSource.java','QuizActivity.java'):
    assert not (java/name).exists(), "Retired runtime class persists: "+name
assert 'IbnKathirMapActivity' not in manifest
assert 'QuizActivity' not in manifest
assert 'android.permission.RECORD_AUDIO' not in manifest
assert titles.count('case ' )>=16
assert 'verifiedQafHeadings' in titles and 'verifiedNajmHeadings' in titles
for token in ('getSharedPreferences(', 'prefs.set', 'startActivity(', 'https://'):
    assert token not in titles, "Local English titles must never reach network/progression"
print("PASS: no semantic lists, no external Quran.com navigation, 16 offline heading groups, no Quiz")
