from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def edit(rel, replacements):
    path = ROOT / rel
    text = path.read_text()
    for old, new in replacements:
        if old not in text:
            raise SystemExit(f'missing expected source in {rel}: {old[:100]!r}')
        text = text.replace(old, new, 1)
    path.write_text(text)

edit('app/src/main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt', [
    ('    const val FREE_READER_MEMORIZATION = "reader109"\n',
     '    const val FREE_READER_READING = "reader109_reading"\n    const val FREE_READER_MEMORIZATION = "reader109"\n'),
    ('        val namespaces = setOf(\n            FREE_READER_MEMORIZATION,\n',
     '        val namespaces = setOf(\n            FREE_READER_READING,\n            FREE_READER_MEMORIZATION,\n'),
    ('        check(namespaces.size == 4)', '        check(namespaces.size == 5)'),
])

edit('app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt', [
    ('''    private val readerStateKey: String by lazy { hifzTaskId?.let { "task:$it" } ?: "state" }\n    private val prefs by lazy {\n        getSharedPreferences(\n            if (hifzMode) QuranPersistenceNamespaces.HIFZ_READER\n            else QuranPersistenceNamespaces.FREE_READER_MEMORIZATION,\n            MODE_PRIVATE\n        )\n    }\n''',
     '''    private val requestedMemorization: Boolean by lazy {\n        intent.getBooleanExtra(EXTRA_MEMORIZATION, false)\n    }\n    private val readerStateKey: String by lazy { hifzTaskId?.let { "task:$it" } ?: "state" }\n    private val prefs by lazy {\n        val namespace = when {\n            hifzMode -> QuranPersistenceNamespaces.HIFZ_READER\n            requestedMemorization -> QuranPersistenceNamespaces.FREE_READER_MEMORIZATION\n            else -> QuranPersistenceNamespaces.FREE_READER_READING\n        }\n        getSharedPreferences(namespace, MODE_PRIVATE)\n    }\n'''),
    ('''        val persistedMode = runCatching {\n            JSONObject(prefs.getString(readerStateKey, null) ?: "{}")\n                .optJSONObject("ui")?.optString("mode")\n        }.getOrNull()\n        memoryMode = hifzMode || (!contextual && (\n            intent.getBooleanExtra(EXTRA_MEMORIZATION, false) ||\n                persistedMode == "MEMORIZATION"\n            ))\n''',
     '''        memoryMode = hifzMode || (!contextual && requestedMemorization)\n'''),
])

edit('app/src/main/assets/reader109/reader.js', [
    ("memory=!!initial.memory||(!initial.contextual&&state.ui.mode==='MEMORIZATION')", "memory=!!initial.memory"),
    ("renderMarks();renderMemory();save();if(focus)focusVerse(focus);requestAnimationFrame(()=>{document.body.classList.remove('booting');requestAnimationFrame(()=>visual('PAGE'))})",
     "renderMarks();renderMemory();save();if(focus)focusVerse(focus);requestAnimationFrame(()=>{document.body.classList.remove('booting');window.QsgRuntimeGuard?.verifyRenderedPage?.();chrome();requestAnimationFrame(()=>visual('PAGE'))})"),
    ("b.append(button(memory?'Reprendre la lecture':'Mémorisation',()=>{closeSheet();memory?exitMemory():enterMemory()}));",
     "if(memory)b.append(button('Quitter la mémorisation',()=>{closeSheet();exitMemory()}));"),
])

edit('app/src/main/assets/reader109/index.html', [
    ('<body class="hidden booting">', '<body class="booting">'),
])

edit('app/src/main/assets/reader109/runtime_guard.js', [
    ('  let sizeWatch=null;\n  let verifyRaf1=null,verifyRaf2=null;\n',
     '  let sizeWatch=null;\n  let layoutRetryTimer=null;\n  let verifyRaf1=null,verifyRaf2=null;\n'),
    ('    if(sizeWatch){sizeWatch.disconnect();sizeWatch=null}\n    cancelFrame(verifyRaf1);cancelFrame(verifyRaf2);verifyRaf1=verifyRaf2=null;\n',
     '    if(sizeWatch){sizeWatch.disconnect();sizeWatch=null}\n    if(layoutRetryTimer!==null){clearTimeout(layoutRetryTimer);layoutRetryTimer=null}\n    cancelFrame(verifyRaf1);cancelFrame(verifyRaf2);verifyRaf1=verifyRaf2=null;\n'),
    ('''  function waitForLayout(mushaf,generation){\n    if(!isCurrent(generation)||sizeWatch)return;\n    sizeWatch=new ResizeObserver(()=>{\n      if(!isCurrent(generation)){sizeWatch?.disconnect();sizeWatch=null;return}\n      const svg=mushaf.querySelector('svg'),r=svg?.getBoundingClientRect();\n      if(r&&r.width>1&&r.height>1){sizeWatch.disconnect();sizeWatch=null;verifyRenderedPage(generation)}\n    });\n    sizeWatch.observe(mushaf);\n  }\n''',
     '''  function waitForLayout(mushaf,generation){\n    if(!isCurrent(generation))return;\n    const retry=()=>{\n      if(!isCurrent(generation))return;\n      layoutRetryTimer=null;\n      verifyRenderedPage(generation);\n    };\n    if(layoutRetryTimer===null)layoutRetryTimer=setTimeout(retry,100);\n    if(sizeWatch||typeof ResizeObserver!=='function')return;\n    sizeWatch=new ResizeObserver(()=>{\n      if(!isCurrent(generation)){sizeWatch?.disconnect();sizeWatch=null;return}\n      const svg=mushaf.querySelector('svg'),r=svg?.getBoundingClientRect();\n      if(r&&r.width>1&&r.height>1){\n        sizeWatch.disconnect();sizeWatch=null;\n        if(layoutRetryTimer!==null){clearTimeout(layoutRetryTimer);layoutRetryTimer=null}\n        verifyRenderedPage(generation);\n      }\n    });\n    sizeWatch.observe(mushaf);\n    const svg=mushaf.querySelector('svg');if(svg)sizeWatch.observe(svg);\n  }\n'''),
])

edit('app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt', [
    ('''            val needsSetup = config.bounds == null ||\n                config.pace.sabqiMinutesPerPage == null ||\n                config.pace.itqanMinutesPerPage == null ||\n                config.availableMinutes.sabqi == null ||\n''',
     '''            val needsSetup = config.bounds == null ||\n                config.availableMinutes.sabqi == null ||\n'''),
    ('''        var sabqiPace by remember { mutableStateOf(existing.pace.sabqiMinutesPerPage?.toString() ?: "") }\n        var itqanPace by remember { mutableStateOf(existing.pace.itqanMinutesPerPage?.toString() ?: "") }\n        var murajaahPace by remember {\n            mutableStateOf(\n                existing.pace.murajaahMinutesPerPage?.toString()\n                    ?: MurajaahPolicy.INITIAL_MINUTES_PER_PAGE_REFERENCE.toString()\n            )\n        }\n''',
     '''        var murajaahMinutesPerJuz by remember {\n            mutableStateOf(\n                (\n                    (existing.pace.murajaahMinutesPerPage\n                        ?: MurajaahPolicy.INITIAL_MINUTES_PER_PAGE_REFERENCE) *\n                        MurajaahPolicy.INITIAL_REFERENCE_PAGES\n                    ).toString()\n            )\n        }\n'''),
    ('''                NumberField("Sabqi min/page observées", sabqiPace) { sabqiPace = it }\n                NumberField("Itqān min/page observées", itqanPace) { itqanPace = it }\n                NumberField("Murājaʿah min/page (2,25 = 45 min/juz)", murajaahPace) { murajaahPace = it }\n                NumberField("Minutes disponibles Sabqi", sabqiMinutes) { sabqiMinutes = it }\n''',
     '''                Text(\n                    "Aucune vitesse Sabqi ou Itqān à estimer : Sabqi suit 5 lignes réelles " +\n                        "et Itqān suit 1 page ×30.",\n                    style = MaterialTheme.typography.bodySmall,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant\n                )\n                NumberField(\n                    "Référence Murājaʿah : minutes pour 1 juz",\n                    murajaahMinutesPerJuz\n                ) { murajaahMinutesPerJuz = it }\n                Text(\n                    "45 min/juz par défaut. Vous pouvez garder cette valeur.",\n                    style = MaterialTheme.typography.bodySmall,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant\n                )\n                NumberField("Minutes disponibles Sabqi", sabqiMinutes) { sabqiMinutes = it }\n'''),
    ('''                            pace = HifzPaceProfile(\n                                sabqiMinutesPerPage = sabqiPace.toDouble(),\n                                itqanMinutesPerPage = itqanPace.toDouble(),\n                                murajaahMinutesPerPage = murajaahPace.toDouble()\n                            ),\n''',
     '''                            pace = existing.pace.copy(\n                                murajaahMinutesPerPage =\n                                    murajaahMinutesPerJuz.replace(',', '.').toDouble() /\n                                        MurajaahPolicy.INITIAL_REFERENCE_PAGES\n                            ),\n'''),
    ('                        localError = "Vérifiez les sourates, versets, durées et l’ordre des bornes."\n',
     '                        localError =\n                            "Vérifiez les 4 bornes Qur’an, les minutes disponibles " +\n                                "et la référence Murājaʿah."\n'),
])

edit('app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt', [
    ('private const val GATE_TAFSIR_ENABLED = false', 'private const val GATE_TAFSIR_ENABLED = true'),
    ('''    private fun openTafsir(verse: VerseRef) {\n        if (!GATE_TAFSIR_ENABLED || !TafsirEdition.isEnabled) return\n        selectedTafsirVerse = verse\n        tafsirLoadState = TafsirLoadState.Loading\n        TafsirEdition.selectVerse(verse)\n    }\n\n    private fun closeTafsir() {\n        if (selectedTafsirVerse == null) return\n        TafsirEdition.closeAndRestore()\n        selectedTafsirVerse = null\n        tafsirLoadState = TafsirLoadState.Closed\n    }\n\n    private fun mayCountActiveReading(): Boolean =\n        activityResumed &&\n            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||\n                activityTopResumed)\n''',
     '''    private fun openTafsir(verse: VerseRef) {\n        if (!GATE_TAFSIR_ENABLED || !TafsirEdition.isEnabled) return\n        if (pageReady && displayedPage == activeReadingPage) {\n            GuardPrefs.endReadingForeground(this, challengeKey, activeReadingPage)\n        }\n        selectedTafsirVerse = verse\n        tafsirLoadState = TafsirLoadState.Loading\n        TafsirEdition.selectVerse(verse)\n    }\n\n    private fun closeTafsir() {\n        if (selectedTafsirVerse == null) return\n        TafsirEdition.closeAndRestore()\n        selectedTafsirVerse = null\n        tafsirLoadState = TafsirLoadState.Closed\n        if (pageReady &&\n            displayedPage == activeReadingPage &&\n            mayCountActiveReading()\n        ) {\n            GuardPrefs.beginReadingForeground(this, challengeKey, activeReadingPage)\n        }\n    }\n\n    private fun mayCountActiveReading(): Boolean =\n        activityResumed &&\n            selectedTafsirVerse == null &&\n            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||\n                activityTopResumed)\n'''),
    ('                                    "Qur’an",\n', '                                    if (TafsirEdition.isEnabled) "Qur’an & Tafsîr" else "Qur’an",\n'),
    ('''                            Text(\n                                "Mushaf de Médine • Page $currentDisplayedPage / 604",\n                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),\n                                style = MaterialTheme.typography.bodyMedium,\n                                color = MaterialTheme.colorScheme.secondary,\n                                fontWeight = FontWeight.SemiBold\n                            )\n''',
     '''                            Text(\n                                "Mushaf de Médine • Page $currentDisplayedPage / 604",\n                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),\n                                style = MaterialTheme.typography.bodyMedium,\n                                color = MaterialTheme.colorScheme.secondary,\n                                fontWeight = FontWeight.SemiBold\n                            )\n                            if (TafsirEdition.isEnabled && selectedTafsirVerse == null) {\n                                Text(\n                                    "Touchez un verset pour ouvrir le Tafsîr.",\n                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp),\n                                    style = MaterialTheme.typography.bodySmall,\n                                    color = MaterialTheme.colorScheme.onSurfaceVariant\n                                )\n                            }\n'''),
    ('''    override fun onPause() {\n        closeTafsir()\n        if (pageReady &&\n            displayedPage == activeReadingPage &&\n            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||\n                activityTopResumed)\n        ) {\n            GuardPrefs.endReadingForeground(\n                this,\n                challengeKey,\n                activeReadingPage\n            )\n        }\n        activityResumed = false\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {\n            activityTopResumed = false\n        }\n        super.onPause()\n    }\n''',
     '''    override fun onPause() {\n        val activePageWasReady = pageReady && displayedPage == activeReadingPage\n        activityResumed = false\n        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {\n            activityTopResumed = false\n        }\n        closeTafsir()\n        if (activePageWasReady) {\n            GuardPrefs.endReadingForeground(\n                this,\n                challengeKey,\n                activeReadingPage\n            )\n        }\n        super.onPause()\n    }\n'''),
])

reader = (ROOT / 'app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt').read_text()
hifz = (ROOT / 'app/src/main/java/com/quranunlock/guard/HifzJourneyActivity.kt').read_text()
gate = (ROOT / 'app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt').read_text()
js = (ROOT / 'app/src/main/assets/reader109/reader.js').read_text()
ns = (ROOT / 'app/src/main/java/com/quranunlock/guard/QuranPersistenceNamespaces.kt').read_text()
assert 'FREE_READER_READING' in ns
assert 'persistedMode == "MEMORIZATION"' not in reader
assert "memory=!!initial.memory||" not in js
assert 'Sabqi min/page observées' not in hifz and 'Itqān min/page observées' not in hifz
assert 'Référence Murājaʿah : minutes pour 1 juz' in hifz
assert 'GATE_TAFSIR_ENABLED = true' in gate
assert 'selectedTafsirVerse == null' in gate
print('device feedback1 corrections applied')
