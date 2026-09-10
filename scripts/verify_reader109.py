from pathlib import Path
import json, subprocess

r = Path(__file__).resolve().parents[1]
src = r / 'app/src'
a = src / 'main/assets/reader109'
g = json.loads((a / 'geometry.json').read_text())
assert len(g['pages']) == 604 and len(g['verses']) == 6236
assert len(g['juz']) == 30 and len(g['hizb']) == 60
for v, pages in g['verses'].items():
    assert all(any(v in line['verses'] for line in g['pages'][str(p)]['lines']) for p in pages), (v, pages)

manifest = (src / 'main/AndroidManifest.xml').read_text()
audio_meta = json.loads((a / 'audio.json').read_text())
assert 'RECORD_AUDIO' not in manifest
assert 'android.permission.INTERNET' in manifest
assert audio_meta['redistributionApproved'] is False
assert audio_meta['bundledInApk'] is False
assert audio_meta['delivery'] == 'user-initiated-local-download'
assert audio_meta['baseUrl'] == 'https://everyayah.com/data/Husary_Muallim_128kbps'

reader = (src / 'main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt').read_text()
reader_js = (a / 'reader.js').read_text()
hifz_guard = (a / 'hifz_guard.js').read_text()
protocol = (a / 'protocol.js').read_text()
html = (a / 'index.html').read_text()
display = (src / 'main/java/com/quranunlock/guard/DisplayProfile.kt').read_text()
refresh = (src / 'main/java/com/quranunlock/guard/EInkRefreshController.kt').read_text()
audio = (src / 'main/java/com/quranunlock/guard/QuranAudioController.kt').read_text()
audio_source = (src / 'main/java/com/quranunlock/guard/QuranAudioSource.kt').read_text()
selection = (src / 'main/java/com/quranunlock/guard/ReadingSelectionActivity.kt').read_text()
hub = (src / 'main/java/com/quranunlock/guard/QuranHubActivity.kt').read_text()
hifz_ui = (src / 'main/java/com/quranunlock/guard/HifzJourneyActivity.kt').read_text()
hifz_time = (src / 'main/java/com/quranunlock/guard/HifzActiveTimeRecorder.kt').read_text()
namespaces = (src / 'main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt').read_text()

assert '!memoryMode&&TafsirEdition.isEnabled' in reader.replace(' ', '')
assert 'GuardPrefs.' not in reader and 'SafeguardCyclePrefs.' not in reader
assert 'settings.allowFileAccess = false' in reader and 'settings.blockNetworkLoads = true' in reader
assert 'statusBarsPadding()' in reader and 'navigationBarsPadding()' in reader
assert 'FLAG_KEEP_SCREEN_ON' in reader and 'ReaderComfortPrefs.applyBrightness' in reader
assert "'Valider — '" in protocol and 'Valider le bloc mémorisé' in protocol
assert 'function disableAudio(s)' in protocol
assert all(label in reader_js for label in ('Refaire', 'Afficher brièvement', 'Réécouter', 'Indices', 'Reprise ciblée'))
assert "memory?'Mémorisation':'Signet',sessionId" in reader_js
assert "body.eink" in html and "transition:none" in html and "animation:none" in html
assert all(token in display for token in ('AUTOMATIC', 'STANDARD', 'EINK', 'looksLikeEInkDevice'))
assert 'profile == DisplayProfile.STANDARD' in refresh and 'FULL_REFRESH_THRESHOLD' in refresh
assert 'Class.forName("com.onyx.android.sdk' in refresh and 'getOrDefault(false)' in refresh
assert all(token not in refresh for token in ('counts[', 'wins[', 'milestones', 'success', 'activeLine', 'setMode('))

# Structured Hifz must never share free-memorisation state. The scheduled task/range is
# authoritative, Tafsir is unreachable, native Hifz progress owns attempts/reveals, and
# only foreground elapsed time is charged.
assert 'HIFZ_READER' in namespaces and 'FREE_READER_MEMORIZATION' in namespaces
assert 'EXTRA_HIFZ_TASK_ID' in reader and 'QuranPersistenceNamespaces.HIFZ_READER' in reader
assert 'hifzActiveStartedAtMs' in reader and 'SystemClock.elapsedRealtime()' in reader
assert 'HifzActiveTimeRecorder.record' in reader and 'override fun onPause()' in reader
assert all(token in reader for token in (
    '@JavascriptInterface\n        fun hifzStatus()',
    'fun hifzAttempt(correct: Boolean)',
    'fun hifzReveal()',
    'fun hifzAdvance()',
    'HifzTrainingProgressPolicy.canValidate',
))
assert 'EXTRA_HIFZ_TASK_ID, task.id' in hifz_ui
assert '<script src="hifz_guard.js"></script>' in html
assert all(token in hifz_guard for token in (
    'targetStart', 'targetEnd', 'targetKeys', 'showPage=async function',
    'verseTap=function', 'memory=true', 'N?.setMode(true)',
    'N?.hifzStatus', 'N?.hifzAttempt', 'N?.hifzReveal', 'N?.hifzAdvance',
    'nativeRevealVisible', 'renderMasks=function',
))
assert 'begin(keys' not in hifz_guard and 'restart(s)' not in hifz_guard
assert "state.sessions=[]" in hifz_guard and "state.active=null" in hifz_guard
assert "N?.exit()" in hifz_guard
assert 'HifzTrainingEngine.recordActiveSeconds' in hifz_time

# Local-only Al-Husary Muallim contract. Reader WebView itself remains network-blocked;
# only the native audio controller can fetch pinned HTTPS ayah files after user action.
assert 'val available: Boolean = true' in audio
assert all(token in audio for token in ('downloadVerse(', 'downloadSurah(', 'deleteSurah(', 'isDownloaded(', 'looksLikeMp3', 'QuranAudioSource.url'))
assert all(token in audio_source for token in ('Husary_Muallim_128kbps', 'isAllowedHttpsUrl', 'fileName(', 'STORAGE_VERSION'))
assert all(token in reader for token in ('@JavascriptInterface fun isDownloaded', '@JavascriptInterface fun downloadVerse', '@JavascriptInterface fun downloadSurah', '@JavascriptInterface fun deleteSurah'))
assert 'Audio Al-Husary Muʿallim' in reader_js and 'Télécharger la sourate' in reader_js
assert "classList.toggle('audio',playing&&p.dataset.verse===e.surah+':'+e.ayah)" in reader_js
assert 'localAudioReady' in reader_js and 'P.disableAudio(s)' in reader_js
assert 'audioCountsProgress' in reader_js
assert 'visual(' not in reader_js[reader_js.index('window.audioEvent='):reader_js.index('window.setOcclusion=')]
assert 'Audio Al-Husary Muʿallim' in hifz_guard and 'playTarget' in hifz_guard

assert not any('eink' in part.lower() for path in src.rglob('*') if path.is_dir() for part in path.parts[-1:])
assert 'if (TafsirEdition.isEnabled)' in selection and 'if (TafsirEdition.isEnabled)' in hub
assert 'Tafsîr al-Jalalayn' not in selection

subprocess.run(['node', '--check', str(a / 'reader.js')], check=True)
subprocess.run(['node', '--check', str(a / 'hifz_guard.js')], check=True)
subprocess.run(['node', str(r / 'scripts/test_reader109_protocol.js')], check=True)
print('PASS 604 pages, 6236 verses, authoritative isolated Hifz reader state, STANDARD/EINK, Tafsir isolation, no microphone, private local Al-Husary audio')
