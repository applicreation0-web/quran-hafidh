#!/usr/bin/env python3
"""CP-03 historical graphical map; CP02 Lecture now uses only approved private Ayah Themes titles."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=root/"hifz-app/src/main/java/com/quransafeguard/hifz/preview"
lecture=(java/"StudyReaderActivity.java").read_text(encoding="utf-8")
titles=(java/"IbnKathirQuranComPrivateTitles.java").read_text(encoding="utf-8")
manifest=(root/"hifz-app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
home=(java/"MainActivity.java").read_text(encoding="utf-8")
map_source=(java/"IbnKathirMapActivity.java").read_text(encoding="utf-8")
for token in ('onSemanticCueTap(String passageId)',
              'AyahThemesCueIndex.load(this)',
              'SemanticTitlePopup.showEnglish(this,label,semanticTitleDialog)',
              'readerActions.addView(semanticButton)',
              'ayahThemesCues.forPage(page)', '"Masquer les ancres"'):
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
for name in ('IbnKathirQuranComSource.java','QuizActivity.java'):
    assert not (java/name).exists(), "Retired runtime class persists: "+name
assert '<activity android:name=".IbnKathirMapActivity"' in manifest
assert 'new Intent(this, IbnKathirMapActivity.class)' in home
assert 'QuizActivity' not in manifest
assert 'android.permission.RECORD_AUDIO' not in manifest
for token in ('buildGraphicalMap', 'new MushafView(this)', 'setHighlightVerses(exact)',
              'IbnKathirQuranComPrivateTitles.approvedTitle(this, group)', 'Sans titre',
              'StudyReaderActivity.EXTRA_JUMP_PAGE', 'StudyReaderActivity.EXTRA_JUMP_VERSE',
              'StudyReaderActivity.EXTRA_MAP_PREVIEW', 'onSaveInstanceState', 'scrollY'):
    assert token in map_source, "Graphical map contract missing: "+token
for forbidden in ('ListView', 'ArrayAdapter', 'IbnKathirOfflineTitles',
                  'IbnKathirTitlePack', 'Intent.ACTION_VIEW', 'https://'):
    assert forbidden not in map_source, "Old list/network/title source returned: "+forbidden
for token in ('ibn_kathir_qurancom_private','IK_APPROVED_FINAL_V2',
              'M7B_1903_2026-10-13','validateApprovedTitle','FAILED.put(chunk,true)'):
    assert token in titles, "Private fail-closed title contract missing: "+token
for token in ('getSharedPreferences(', 'prefs.set', 'startActivity(', 'https://'):
    assert token not in titles, "Private title reader must never mutate Hifz/network state"
import subprocess
tracked=subprocess.check_output(
    ["git","ls-files","--","hifz-app/src/main/assets/ibn_kathir_qurancom_private"],
    cwd=root,text=True).strip()
assert not tracked, "PRIVATE source is tracked in PUBLIC repo"
assert 'lastQuranComHeadingIndex' not in lecture, "Carousel state must be absent"
assert 'sourceHeadings(this,group)' not in lecture, "Legacy title source must be retired"
assert 'firstThematicIndex' not in titles, "V1 display-order sorting must be retired"
print("PASS: CP02 Ayah Themes Lecture + historical CP03 graphical map kept isolated; no old Lecture headings or network")
