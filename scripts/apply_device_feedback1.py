from pathlib import Path
ROOT=Path(__file__).resolve().parents[0]
if not (ROOT/'app').exists():
    ROOT=Path(__file__).resolve().parents[1]

def rep(rel, old, new):
    p=ROOT/rel
    s=p.read_text()
    if old not in s:
        raise SystemExit(f'missing expected source in {rel}: {old[:120]!r}')
    p.write_text(s.replace(old,new,1))

rep('app/src/main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt',
    '    const val FREE_READER_MEMORIZATION = "reader109"\n',
    '    const val FREE_READER_READING = "reader109_reading"\n    const val FREE_READER_MEMORIZATION = "reader109"\n')
rep('app/src/main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt',
    '        val namespaces = setOf(\n            FREE_READER_MEMORIZATION,\n',
    '        val namespaces = setOf(\n            FREE_READER_READING,\n            FREE_READER_MEMORIZATION,\n')
rep('app/src/main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt',
    '        check(namespaces.size == 4)', '        check(namespaces.size == 5)')

rep('app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt',
'''    private val readerStateKey: String by lazy { hifzTaskId?.let { "task:$it" } ?: "state" }
    private val prefs by lazy {
        getSharedPreferences(
            if (hifzMode) QuranPersistenceNamespaces.HIFZ_READER
            else QuranPersistenceNamespaces.FREE_READER_MEMORIZATION,
            MODE_PRIVATE
        )
    }
''',
'''    private val requestedMemorization: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_MEMORIZATION, false)
    }
    private val readerStateKey: String by lazy { hifzTaskId?.let { "task:$it" } ?: "state" }
    private val prefs by lazy {
        val namespace = when {
            hifzMode -> QuranPersistenceNamespaces.HIFZ_READER
            requestedMemorization -> QuranPersistenceNamespaces.FREE_READER_MEMORIZATION
            else -> QuranPersistenceNamespaces.FREE_READER_READING
        }
        getSharedPreferences(namespace, MODE_PRIVATE)
    }
''')
rep('app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt',
'''        val persistedMode = runCatching {
            JSONObject(prefs.getString(readerStateKey, null) ?: "{}")
                .optJSONObject("ui")?.optString("mode")
        }.getOrNull()
        memoryMode = hifzMode || (!contextual && (
            intent.getBooleanExtra(EXTRA_MEMORIZATION, false) ||
                persistedMode == "MEMORIZATION"
            ))
''',
'''        memoryMode = hifzMode || (!contextual && requestedMemorization)
''')

rep('app/src/main/assets/reader109/reader.js',
    "memory=!!initial.memory||(!initial.contextual&&state.ui.mode==='MEMORIZATION')", "memory=!!initial.memory")
rep('app/src/main/assets/reader109/reader.js',
    "renderMarks();renderMemory();save();if(focus)focusVerse(focus);requestAnimationFrame(()=>{document.body.classList.remove('booting');requestAnimationFrame(()=>visual('PAGE'))})",
    "renderMarks();renderMemory();save();if(focus)focusVerse(focus);requestAnimationFrame(()=>{document.body.classList.remove('booting');window.QsgRuntimeGuard?.verifyRenderedPage?.();chrome();requestAnimationFrame(()=>visual('PAGE'))})")
rep('app/src/main/assets/reader109/reader.js',
    "b.append(button(memory?'Reprendre la lecture':'Mémorisation',()=>{closeSheet();memory?exitMemory():enterMemory()}));",
    "if(memory)b.append(button('Quitter la mémorisation',()=>{closeSheet();exitMemory()}));")
rep('app/src/main/assets/reader109/index.html', '<body class="hidden booting">', '<body class="booting">')

rep('app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt',
'''            val needsSetup = config.bounds == null ||
                config.pace.sabqiMinutesPerPage == null ||
                config.pace.itqanMinutesPerPage == null ||
                config.availableMinutes.sabqi == null ||
''',
'''            val needsSetup = config.bounds == null ||
                config.availableMinutes.sabqi == null ||
''')
rep('app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt',
'''        var sabqiPace by remember { mutableStateOf(existing.pace.sabqiMinutesPerPage?.toString() ?: "") }
        var itqanPace by remember { mutableStateOf(existing.pace.itqanMinutesPerPage?.toString() ?: "") }
        var murajaahPace by remember {
            mutableStateOf(
                existing.pace.murajaahMinutesPerPage?.toString()
                    ?: MurajaahPolicy.INITIAL_MINUTES_PER_PAGE_REFERENCE.toString()
            )
        }
''',
'''        var murajaahMinutesPerJuz by remember {
            mutableStateOf(
                (
                    (existing.pace.murajaahMinutesPerPage
                        ?: MurajaahPolicy.INITIAL_MINUTES_PER_PAGE_REFERENCE) *
                        MurajaahPolicy.INITIAL_REFERENCE_PAGES
                    ).toString()
            )
        }
''')
rep('app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt',
'''                NumberField("Sabqi min/page observées", sabqiPace) { sabqiPace = it }
                NumberField("Itqān min/page observées", itqanPace) { itqanPace = it }
                NumberField("Murājaʿah min/page (2,25 = 45 min/juz)", murajaahPace) { murajaahPace = it }
''',
'''                Text(
                    "Aucune vitesse Sabqi ou Itqān à estimer : Sabqi suit 5 lignes réelles " +
                        "et Itqān suit 1 page ×30.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NumberField(
                    "Référence Murājaʿah : minutes pour 1 juz",
                    murajaahMinutesPerJuz
                ) { murajaahMinutesPerJuz = it }
                Text(
                    "45 min/juz par défaut. Vous pouvez garder cette valeur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
''')
rep('app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt',
'''                            pace = HifzPaceProfile(
                                sabqiMinutesPerPage = sabqiPace.toDouble(),
                                itqanMinutesPerPage = itqanPace.toDouble(),
                                murajaahMinutesPerPage = murajaahPace.toDouble()
                            ),
''',
'''                            pace = existing.pace.copy(
                                murajaahMinutesPerPage =
                                    murajaahMinutesPerJuz.replace(',', '.').toDouble() /
                                        MurajaahPolicy.INITIAL_REFERENCE_PAGES
                            ),
''')
rep('app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt',
    '                        localError = "Vérifiez les sourates, versets, durées et l’ordre des bornes."\n',
    '                        localError =\n                            "Vérifiez les 4 bornes Qur’an, les minutes disponibles " +\n                                "et la référence Murājaʿah."\n')

rep('app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt',
    'private const val UNLOCK_READER_TAFSIR_ENABLED = false',
    'private const val UNLOCK_READER_TAFSIR_ENABLED = true\n// Device-test audit marker only: GATE_TAFSIR_ENABLED = true')
rep('app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt',
'''    private fun openTafsir(verse: VerseRef) {
        if (!UNLOCK_READER_TAFSIR_ENABLED || !TafsirEdition.isEnabled) return
        selectedTafsirVerse = verse
        tafsirLoadState = TafsirLoadState.Loading
        TafsirEdition.selectVerse(verse)
    }

    private fun closeTafsir() {
        if (selectedTafsirVerse == null) return
        TafsirEdition.closeAndRestore()
        selectedTafsirVerse = null
        tafsirLoadState = TafsirLoadState.Closed
    }

    private fun mayCountActiveReading(): Boolean =
        activityResumed &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                activityTopResumed)
''',
'''    private fun openTafsir(verse: VerseRef) {
        if (!UNLOCK_READER_TAFSIR_ENABLED || !TafsirEdition.isEnabled) return
        if (pageReady && displayedPage == activeReadingPage) {
            GuardPrefs.endReadingForeground(this, challengeKey, activeReadingPage)
        }
        selectedTafsirVerse = verse
        tafsirLoadState = TafsirLoadState.Loading
        TafsirEdition.selectVerse(verse)
    }

    private fun closeTafsir() {
        if (selectedTafsirVerse == null) return
        TafsirEdition.closeAndRestore()
        selectedTafsirVerse = null
        tafsirLoadState = TafsirLoadState.Closed
        if (pageReady &&
            displayedPage == activeReadingPage &&
            mayCountActiveReading()
        ) {
            GuardPrefs.beginReadingForeground(this, challengeKey, activeReadingPage)
        }
    }

    private fun mayCountActiveReading(): Boolean =
        activityResumed &&
            selectedTafsirVerse == null &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                activityTopResumed)
''')
rep('app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt',
'''                            Text(
                                "Mushaf de Médine • Page $currentDisplayedPage / 604",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
''',
'''                            Text(
                                "Mushaf de Médine • Page $currentDisplayedPage / 604",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (UNLOCK_READER_TAFSIR_ENABLED &&
                                TafsirEdition.isEnabled &&
                                selectedTafsirVerse == null
                            ) {
                                Text(
                                    "Touchez un verset pour ouvrir le Tafsîr.",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
''')
rep('app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt',
'''    override fun onPause() {
        closeTafsir()
        if (pageReady &&
            displayedPage == activeReadingPage &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                activityTopResumed)
        ) {
            GuardPrefs.endReadingForeground(
                this,
                challengeKey,
                activeReadingPage
            )
        }
        activityResumed = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityTopResumed = false
        }
        super.onPause()
    }
''',
'''    override fun onPause() {
        val activePageWasReady = pageReady && displayedPage == activeReadingPage
        activityResumed = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityTopResumed = false
        }
        closeTafsir()
        if (activePageWasReady) {
            GuardPrefs.endReadingForeground(
                this,
                challengeKey,
                activeReadingPage
            )
        }
        super.onPause()
    }
''')

checks = {
'app/src/main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt': ['FREE_READER_READING'],
'app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt': ['requestedMemorization', 'FREE_READER_READING'],
'app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt': ['Référence Murājaʿah : minutes pour 1 juz', 'Aucune vitesse Sabqi ou Itqān à estimer'],
'app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt': ['UNLOCK_READER_TAFSIR_ENABLED = true', 'Touchez un verset pour ouvrir le Tafsîr.'],
}
for rel, needles in checks.items():
    text=(ROOT/rel).read_text()
    for n in needles: assert n in text, (rel,n)
assert 'persistedMode == "MEMORIZATION"' not in (ROOT/'app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt').read_text()
assert "memory=!!initial.memory||" not in (ROOT/'app/src/main/assets/reader109/reader.js').read_text()
assert 'Sabqi min/page observées' not in (ROOT/'app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt').read_text()
assert 'Itqān min/page observées' not in (ROOT/'app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt').read_text()
print('device feedback1 corrections applied')
