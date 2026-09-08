from pathlib import Path
import json,hashlib,subprocess
r=Path(__file__).resolve().parents[1];src=r/'app/src';a=src/'main/assets/reader109'
g=json.loads((a/'geometry.json').read_text());assert len(g['pages'])==604 and len(g['verses'])==6236
assert len(g['juz'])==30 and len(g['hizb'])==60
for v,pages in g['verses'].items():
 assert all(any(v in l['verses'] for l in g['pages'][str(p)]['lines']) for p in pages),(v,pages)
manifest=(src/'main/AndroidManifest.xml').read_text();assert 'RECORD_AUDIO' not in manifest
assert not json.loads((a/'audio.json').read_text())['redistributionApproved']
reader=(src/'main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt').read_text()
reader_js=(a/'reader.js').read_text();protocol=(a/'protocol.js').read_text();html=(a/'index.html').read_text()
display=(src/'main/java/com/quranunlock/guard/DisplayProfile.kt').read_text()
refresh=(src/'main/java/com/quranunlock/guard/EInkRefreshController.kt').read_text()
audio=(src/'main/java/com/quranunlock/guard/QuranAudioController.kt').read_text()
selection=(src/'main/java/com/quranunlock/guard/ReadingSelectionActivity.kt').read_text()
hub=(src/'main/java/com/quranunlock/guard/QuranHubActivity.kt').read_text()
assert '!memoryMode&&TafsirEdition.isEnabled' in reader
assert 'GuardPrefs.' not in reader and 'SafeguardCyclePrefs.' not in reader
assert 'settings.allowFileAccess = false' in reader and 'settings.blockNetworkLoads = true' in reader
assert 'statusBarsPadding()' in reader and 'navigationBarsPadding()' in reader
assert 'FLAG_KEEP_SCREEN_ON' in reader and 'ReaderComfortPrefs.applyBrightness' in reader
assert "'Valider — '" in protocol and 'Valider le bloc mémorisé' in protocol
assert all(label in reader_js for label in ('Refaire','Afficher brièvement','Réécouter','Indices','Reprise ciblée'))
assert "memory?'Mémorisation':'Signet',sessionId" in reader_js
assert "body.eink" in html and "transition:none" in html and "animation:none" in html
assert all(token in display for token in ('AUTOMATIC','STANDARD','EINK','looksLikeEInkDevice'))
assert 'profile == DisplayProfile.STANDARD' in refresh and 'FULL_REFRESH_THRESHOLD' in refresh
assert 'Class.forName("com.onyx.android.sdk' in refresh and 'getOrDefault(false)' in refresh
assert all(token not in refresh for token in ('counts[','wins[','milestones','success','activeLine','setMode('))
assert 'if(!available){emit("unavailable");return}' in audio
assert 'audioCountsProgress' in reader_js
assert 'visual(' not in reader_js[reader_js.index('window.audioEvent='):reader_js.index('window.setOcclusion=')]
assert not any('eink' in part.lower() for path in src.rglob('*') if path.is_dir() for part in path.parts[-1:])
assert 'if (TafsirEdition.isEnabled)' in selection and 'if (TafsirEdition.isEnabled)' in hub
assert 'Tafsîr al-Jalalayn' not in selection
subprocess.run(['node',str(r/'scripts/test_reader109_protocol.js')],check=True)
print('PASS 604 pages, 6236 verses, memory protocol, STANDARD/EINK, Tafsir isolation, no microphone, closed audio gate')
