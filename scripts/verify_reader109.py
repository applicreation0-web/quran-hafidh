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
assert '!memoryMode&&TafsirEdition.isEnabled' in reader
assert 'GuardPrefs.' not in reader and 'SafeguardCyclePrefs.' not in reader
assert 'settings.allowFileAccess = false' in reader and 'settings.blockNetworkLoads = true' in reader
assert 'statusBarsPadding()' in reader and 'navigationBarsPadding()' in reader
subprocess.run(['node',str(r/'scripts/test_reader109_protocol.js')],check=True)
print('PASS 604 pages, 6236 verses, exact divisions, memory/Tafsir isolation, no microphone, isolated audio gate')
