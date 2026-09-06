package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.brotli.dec.BrotliInputStream
import kotlin.math.roundToInt

/**
 * Voluntary Qur'an reader used outside Safeguard challenges.
 *
 * This activity deliberately has no challenge key and never calls GuardPrefs reading
 * validation or unlock APIs. Reading here therefore cannot credit an app-unlock quota.
 * Tafsir Quran references open a second internal, read-only instance of this reader;
 * the original activity remains alive so Back returns to the exact commentary state.
 */
class FreeQuranReaderActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE = "page"
        private const val EXTRA_REFERENCE_MODE = "reference_mode"
        private const val EXTRA_REFERENCE_SURAH = "reference_surah"
        private const val EXTRA_REFERENCE_START_AYAH = "reference_start_ayah"
        private const val EXTRA_REFERENCE_END_AYAH = "reference_end_ayah"
        private const val PREFS = "free_quran_reader"
        private const val KEY_LAST_PAGE = "last_page"
        private const val FIRST_PAGE = 1
        private const val LAST_PAGE = 604
    }

    private var selectedTafsirVerse by mutableStateOf<VerseRef?>(null)
    private var tafsirLoadState by mutableStateOf<TafsirLoadState>(TafsirLoadState.Closed)
    private var preserveTafsirOnNextPause = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reference = if (intent.getBooleanExtra(EXTRA_REFERENCE_MODE, false)) {
            val surah = intent.getIntExtra(EXTRA_REFERENCE_SURAH, 0)
            val start = intent.getIntExtra(EXTRA_REFERENCE_START_AYAH, 0)
            val end = intent.getIntExtra(EXTRA_REFERENCE_END_AYAH, start)
            if (surah > 0 && start > 0 && end >= start) {
                QuranReferenceRef(surah, start, end)
            } else {
                null
            }
        } else {
            null
        }
        val referenceMode = reference != null
        val readerPrefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val requestedPage = intent.getIntExtra(EXTRA_PAGE, 0)
        val storedPage = readerPrefs.getInt(KEY_LAST_PAGE, FIRST_PAGE)
        val storedBookmarks = QuranBookmarkStore.load(this)
        val initialPage = when {
            requestedPage in FIRST_PAGE..LAST_PAGE -> requestedPage
            !referenceMode && storedPage in FIRST_PAGE..LAST_PAGE -> storedPage
            else -> FIRST_PAGE
        }

        setContent {
            QuranSafeguardTheme {
                val coroutineScope = rememberCoroutineScope()
                var page by remember { mutableIntStateOf(initialPage) }
                var bookmarkPages by remember { mutableStateOf(storedBookmarks) }
                var quickNavOpen by remember { mutableStateOf(false) }
                var quickNavPage by remember { mutableIntStateOf(initialPage) }
                var quickNavInput by remember { mutableStateOf(initialPage.toString()) }
                var comfortOpen by remember { mutableStateOf(false) }
                var pureReading by remember { mutableStateOf(false) }
                var tafsirExpanded by remember { mutableStateOf(false) }
                var visualMode by remember {
                    mutableStateOf(ReaderComfortPrefs.visualMode(this@FreeQuranReaderActivity))
                }
                var readerBrightness by remember {
                    mutableFloatStateOf(ReaderComfortPrefs.brightness(this@FreeQuranReaderActivity))
                }
                var message by remember {
                    mutableStateOf(
                        if (referenceMode) {
                            "Référence coranique ${reference!!.label} • retour pour reprendre le commentaire."
                        } else {
                            "Mushaf arabe : balayez vers la droite pour avancer, " +
                                "vers la gauche pour revenir."
                        }
                    )
                }

                val darkShell = visualMode == ReaderVisualMode.DARK
                val shellColor = when (visualMode) {
                    ReaderVisualMode.COMFORT -> SafeguardReadingSurface
                    ReaderVisualMode.LIGHT -> Color(0xFFFCFBF7)
                    ReaderVisualMode.DARK -> Color(0xFF171A18)
                }
                val shellPrimary = if (darkShell) Color(0xFFE8E0D2) else MaterialTheme.colorScheme.primary
                val shellSecondary = if (darkShell) Color(0xFFBDB5A9) else MaterialTheme.colorScheme.secondary
                val shellMuted = if (darkShell) Color(0xFFB9B8B2) else MaterialTheme.colorScheme.onSurfaceVariant
                val chromeHidden = pureReading && !referenceMode && !quickNavOpen && !comfortOpen

                LaunchedEffect(readerBrightness) {
                    ReaderComfortPrefs.applyBrightness(window, readerBrightness)
                }

                BackHandler(enabled = comfortOpen && selectedTafsirVerse == null) {
                    comfortOpen = false
                }

                BackHandler(enabled = quickNavOpen && selectedTafsirVerse == null) {
                    quickNavOpen = false
                    quickNavPage = page
                    quickNavInput = page.toString()
                }

                BackHandler(enabled = selectedTafsirVerse != null) {
                    closeTafsir()
                }

                LaunchedEffect(selectedTafsirVerse) {
                    tafsirExpanded = false
                    val requestedVerse = selectedTafsirVerse ?: return@LaunchedEffect
                    tafsirLoadState = TafsirLoadState.Loading
                    val entry = TafsirEdition.load(
                        this@FreeQuranReaderActivity,
                        requestedVerse
                    )
                    if (selectedTafsirVerse == requestedVerse) {
                        tafsirLoadState = if (entry == null) {
                            TafsirLoadState.Unavailable
                        } else {
                            TafsirLoadState.Available(entry)
                        }
                    }
                }

                fun showPage(nextPage: Int) {
                    if (referenceMode || selectedTafsirVerse != null) return
                    if (nextPage !in FIRST_PAGE..LAST_PAGE) {
                        message = if (nextPage < FIRST_PAGE) {
                            "Vous êtes sur la première page du Mushaf."
                        } else {
                            "Vous êtes sur la dernière page du Mushaf."
                        }
                        return
                    }
                    page = nextPage
                    quickNavOpen = false
                    quickNavPage = nextPage
                    quickNavInput = nextPage.toString()
                    readerPrefs.edit()
                        .putInt(KEY_LAST_PAGE, page)
                        .apply()
                    message = "Lecture libre • appuyez sur un verset pour ouvrir le Tafsîr."
                }

                fun toggleBookmark() {
                    if (referenceMode || selectedTafsirVerse != null) return
                    val wasMarked = page in bookmarkPages
                    bookmarkPages = QuranBookmarkStore.toggle(
                        this@FreeQuranReaderActivity,
                        page
                    )
                    message = if (wasMarked) {
                        "Marque-page retiré • page $page."
                    } else {
                        "Marque-page ajouté • page $page."
                    }
                }

                fun openReference(referenceToOpen: QuranReferenceRef) {
                    if (referenceMode || !TafsirEdition.isEnabled) return
                    coroutineScope.launch {
                        val referencePage = TafsirEdition.referencePage(
                            this@FreeQuranReaderActivity,
                            referenceToOpen
                        )
                        if (referencePage == null) {
                            message = "Référence ${referenceToOpen.label} : page du Mushaf introuvable."
                            return@launch
                        }
                        preserveTafsirOnNextPause = true
                        startActivity(
                            Intent(
                                this@FreeQuranReaderActivity,
                                FreeQuranReaderActivity::class.java
                            ).apply {
                                putExtra(EXTRA_PAGE, referencePage)
                                putExtra(EXTRA_REFERENCE_MODE, true)
                                putExtra(EXTRA_REFERENCE_SURAH, referenceToOpen.surah)
                                putExtra(EXTRA_REFERENCE_START_AYAH, referenceToOpen.startAyah)
                                putExtra(EXTRA_REFERENCE_END_AYAH, referenceToOpen.endAyah)
                            }
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = shellColor
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(vertical = if (chromeHidden) 0.dp else 2.dp)
                        ) {
                            if (!chromeHidden) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        if (referenceMode) {
                                            "Référence Qur’an • ${reference!!.label}"
                                        } else {
                                            "Qur’an & Tafsîr"
                                        },
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = shellPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!referenceMode && selectedTafsirVerse == null) {
                                        Text(
                                            "Aa / ☼",
                                            modifier = Modifier
                                                .clickable {
                                                    comfortOpen = !comfortOpen
                                                    if (comfortOpen) quickNavOpen = false
                                                }
                                                .padding(horizontal = 7.dp, vertical = 5.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = shellSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Masquer",
                                            modifier = Modifier
                                                .clickable {
                                                    pureReading = true
                                                    comfortOpen = false
                                                    quickNavOpen = false
                                                }
                                                .padding(horizontal = 7.dp, vertical = 5.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = shellMuted,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Text(
                                    if (referenceMode) {
                                        "Mushaf de Médine • Page $page / $LAST_PAGE"
                                    } else {
                                        "Mushaf de Médine • Page $page / $LAST_PAGE ${if (quickNavOpen) "▴" else "▾"}"
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            enabled = !referenceMode &&
                                                selectedTafsirVerse == null &&
                                                !comfortOpen
                                        ) {
                                            if (quickNavOpen) {
                                                quickNavOpen = false
                                            } else {
                                                quickNavPage = page
                                                quickNavInput = page.toString()
                                                quickNavOpen = true
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = shellSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (comfortOpen && !referenceMode && selectedTafsirVerse == null) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = shellColor,
                                        tonalElevation = 0.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "Confort de lecture",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                ReaderVisualMode.entries.forEach { mode ->
                                                    val label = when (mode) {
                                                        ReaderVisualMode.COMFORT -> "Confort"
                                                        ReaderVisualMode.LIGHT -> "Clair"
                                                        ReaderVisualMode.DARK -> "Sombre"
                                                    }
                                                    if (visualMode == mode) {
                                                        SafeguardButton(
                                                            modifier = Modifier.weight(1f),
                                                            onClick = {}
                                                        ) { Text(label) }
                                                    } else {
                                                        SafeguardOutlinedButton(
                                                            modifier = Modifier.weight(1f),
                                                            onClick = {
                                                                visualMode = mode
                                                                ReaderComfortPrefs.setVisualMode(
                                                                    this@FreeQuranReaderActivity,
                                                                    mode
                                                                )
                                                            }
                                                        ) { Text(label) }
                                                    }
                                                }
                                            }
                                            Text(
                                                if (readerBrightness < 0f) {
                                                    "Luminosité : téléphone"
                                                } else {
                                                    "Luminosité : ${(readerBrightness * 100).toInt()} %"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Slider(
                                                value = if (readerBrightness < 0f) 0.72f else readerBrightness,
                                                onValueChange = { value ->
                                                    readerBrightness = value.coerceIn(0.12f, 1f)
                                                    ReaderComfortPrefs.setBrightness(
                                                        this@FreeQuranReaderActivity,
                                                        readerBrightness
                                                    )
                                                },
                                                valueRange = 0.12f..1f,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                SafeguardOutlinedButton(
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        readerBrightness = -1f
                                                        ReaderComfortPrefs.setBrightness(
                                                            this@FreeQuranReaderActivity,
                                                            null
                                                        )
                                                    }
                                                ) { Text("Auto") }
                                                SafeguardOutlinedButton(
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { comfortOpen = false }
                                                ) { Text("Fermer") }
                                            }
                                        }
                                    }
                                }

                                if (quickNavOpen && !referenceMode && selectedTafsirVerse == null) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = shellColor,
                                        tonalElevation = 0.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                "Navigation rapide • page $quickNavPage",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Slider(
                                                value = quickNavPage.toFloat(),
                                                onValueChange = { value ->
                                                    val candidate = value.roundToInt()
                                                        .coerceIn(FIRST_PAGE, LAST_PAGE)
                                                    quickNavPage = candidate
                                                    quickNavInput = candidate.toString()
                                                },
                                                onValueChangeFinished = {
                                                    val target = quickNavPage
                                                    showPage(target)
                                                    message = "Navigation rapide • page $target."
                                                },
                                                valueRange = FIRST_PAGE.toFloat()..LAST_PAGE.toFloat(),
                                                steps = LAST_PAGE - FIRST_PAGE - 1,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SafeguardOutlinedButton(
                                                    enabled = quickNavPage > FIRST_PAGE,
                                                    onClick = {
                                                        quickNavPage = (quickNavPage - 1)
                                                            .coerceAtLeast(FIRST_PAGE)
                                                        quickNavInput = quickNavPage.toString()
                                                    }
                                                ) {
                                                    Text("−1")
                                                }
                                                OutlinedTextField(
                                                    value = quickNavInput,
                                                    onValueChange = { value ->
                                                        if (
                                                            value.length <= LAST_PAGE.toString().length &&
                                                            value.all(Char::isDigit)
                                                        ) {
                                                            quickNavInput = value
                                                            value.toIntOrNull()
                                                                ?.takeIf { it in FIRST_PAGE..LAST_PAGE }
                                                                ?.let { quickNavPage = it }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    label = { Text("Page") },
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Number
                                                    )
                                                )
                                                SafeguardOutlinedButton(
                                                    enabled = quickNavPage < LAST_PAGE,
                                                    onClick = {
                                                        quickNavPage = (quickNavPage + 1)
                                                            .coerceAtMost(LAST_PAGE)
                                                        quickNavInput = quickNavPage.toString()
                                                    }
                                                ) {
                                                    Text("+1")
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            val typedPage = quickNavInput.toIntOrNull()
                                            SafeguardButton(
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = typedPage != null &&
                                                    typedPage in FIRST_PAGE..LAST_PAGE,
                                                onClick = {
                                                    typedPage
                                                        ?.takeIf { it in FIRST_PAGE..LAST_PAGE }
                                                        ?.let { target ->
                                                            showPage(target)
                                                            message = "Navigation rapide • page $target."
                                                        }
                                                }
                                            ) {
                                                Text("Aller à la page $quickNavPage")
                                            }
                                        }
                                    }
                                }

                                Text(
                                    if (TafsirEdition.isEnabled) message else
                                        "Lecture libre du Mushaf de Médine.",
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = shellMuted
                                )
                                Spacer(Modifier.height(2.dp))
                            }

                            AnimatedContent(
                                targetState = page,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInHorizontally { width -> -width } togetherWith
                                            slideOutHorizontally { width -> width }
                                    } else {
                                        slideInHorizontally { width -> width } togetherWith
                                            slideOutHorizontally { width -> -width }
                                    }
                                },
                                label = "free-mushaf-rtl-page"
                            ) { pageNumber ->
                                val svgContent = remember(pageNumber) {
                                    loadMushafPage(pageNumber)
                                }
                                if (svgContent.isNullOrBlank()) {
                                    Text(
                                        "La page du Mushaf n’est pas disponible.",
                                        modifier = Modifier.padding(18.dp),
                                        color = shellPrimary
                                    )
                                } else {
                                    key(visualMode) {
                                        FreeMushafPageWebView(
                                            svgContent = svgContent,
                                            pageNumber = pageNumber,
                                            pageBackground = ReaderComfortPrefs.pageBackground(visualMode),
                                            tafsirOpen = selectedTafsirVerse != null ||
                                                quickNavOpen || comfortOpen,
                                            referenceHighlight = reference?.startVerse,
                                            modifier = Modifier.fillMaxSize(),
                                            onSwipePrevious = { showPage(page - 1) },
                                            onSwipeNext = { showPage(page + 1) },
                                            onVerseTapped = { verse ->
                                                if (!referenceMode && !quickNavOpen && !comfortOpen) {
                                                    openTafsir(verse)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            if (!chromeHidden) {
                                Spacer(Modifier.height(2.dp))
                                if (referenceMode) {
                                    SafeguardOutlinedButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        onClick = { finish() }
                                    ) {
                                        Text("← Retour au commentaire")
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SafeguardOutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            enabled = selectedTafsirVerse == null && page > FIRST_PAGE,
                                            onClick = { showPage(page - 1) }
                                        ) { Text("Préc.") }
                                        SafeguardOutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            enabled = selectedTafsirVerse == null,
                                            onClick = { toggleBookmark() }
                                        ) { Text(if (page in bookmarkPages) "Signet ✓" else "Signet") }
                                        SafeguardOutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            enabled = selectedTafsirVerse == null && page < LAST_PAGE,
                                            onClick = { showPage(page + 1) }
                                        ) { Text("Suiv.") }
                                    }
                                    if (bookmarkPages.isNotEmpty()) {
                                        Spacer(Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState())
                                                .padding(horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Signets ${bookmarkPages.size} :",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = shellSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            bookmarkPages.sorted().forEach { bookmarkedPage ->
                                                SafeguardOutlinedButton(
                                                    enabled = selectedTafsirVerse == null &&
                                                        bookmarkedPage != page,
                                                    onClick = {
                                                        showPage(bookmarkedPage)
                                                        message =
                                                            "Ouverture du marque-page • page $bookmarkedPage."
                                                    }
                                                ) {
                                                    Text("p. $bookmarkedPage")
                                                }
                                            }
                                        }
                                    }
                                    TextButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        enabled = selectedTafsirVerse == null,
                                        onClick = { finish() }
                                    ) { Text("Fermer") }
                                }
                            }
                        }

                        if (chromeHidden && !referenceMode && selectedTafsirVerse == null) {
                            Text(
                                "Afficher",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clickable { pureReading = false }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = shellMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!referenceMode) {
                            selectedTafsirVerse?.let { verse ->
                                TafsirEdition.Panel(
                                    verse = verse,
                                    state = tafsirLoadState,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    maxPanelHeight = maxHeight * if (tafsirExpanded) 0.84f else 0.42f,
                                    expanded = tafsirExpanded,
                                    onExpandedChange = { tafsirExpanded = it },
                                    onPanelTopInWindow = { top ->
                                        TafsirEdition.revealAbove(top)
                                    },
                                    onQuranReferenceSelected = ::openReference
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openTafsir(verse: VerseRef) {
        if (!TafsirEdition.isEnabled) return
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

    override fun onPause() {
        if (preserveTafsirOnNextPause) {
            preserveTafsirOnNextPause = false
        } else {
            closeTafsir()
        }
        super.onPause()
    }

    private fun loadMushafPage(page: Int): String? {
        if (page !in FIRST_PAGE..LAST_PAGE) return null
        val assetPath = "mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(page)
        return runCatching {
            assets.open(assetPath).use { compressed ->
                BrotliInputStream(compressed)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }
        }.getOrNull()
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@androidx.compose.runtime.Composable
private fun FreeMushafPageWebView(
    svgContent: String,
    pageNumber: Int,
    pageBackground: String,
    tafsirOpen: Boolean,
    referenceHighlight: VerseRef?,
    modifier: Modifier = Modifier,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit
) {
    val currentTafsirOpen = rememberUpdatedState(tafsirOpen)
    val currentReferenceHighlight = rememberUpdatedState(referenceHighlight)
    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.parseColor(pageBackground))
                settings.javaScriptEnabled = TafsirEdition.isEnabled
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                val swipeThreshold = 72f * resources.displayMetrics.density
                val gestureClassifier = ReaderGestureClassifier(swipeThreshold)
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> gestureClassifier.onDown(
                            event.x,
                            event.y,
                            event.pointerCount
                        )
                        MotionEvent.ACTION_MOVE -> gestureClassifier.onMove(
                            event.x,
                            event.y,
                            event.pointerCount
                        )
                        MotionEvent.ACTION_POINTER_DOWN -> gestureClassifier.onAdditionalPointer()
                        MotionEvent.ACTION_CANCEL -> gestureClassifier.onCancel()
                        MotionEvent.ACTION_UP -> {
                            when (
                                gestureClassifier.onUp(
                                    event.x,
                                    event.y,
                                    gesturesEnabled =
                                        !currentTafsirOpen.value &&
                                            currentReferenceHighlight.value == null
                                )
                            ) {
                                ReaderSwipe.NEXT -> currentOnSwipeNext.value()
                                ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()
                                null -> Unit
                            }
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = true

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val highlight = currentReferenceHighlight.value ?: return
                        view?.evaluateJavascript(
                            "window.qsgTafsir && window.qsgTafsir.select(" +
                                "${highlight.surah},${highlight.ayah});",
                            null
                        )
                    }
                }

                val referenceLockStyle = if (referenceHighlight != null) {
                    "<style>.ayahPolygon{pointer-events:none!important;cursor:default!important}</style>"
                } else {
                    ""
                }
                val html = """
                    <!doctype html>
                    <html dir="rtl">
                    <head>
                      <meta name="viewport"
                            content="width=device-width, initial-scale=1.0">
                      <style>
                        html, body {
                          margin: 0;
                          padding: 0;
                          background: $pageBackground;
                          width: 100%;
                          min-height: 100%;
                          overflow-x: hidden;
                        }
                        svg {
                          display: block;
                          width: 100%;
                          height: auto;
                          max-width: 100%;
                        }
                      </style>
                    </head>
                    <body>
                      ${TafsirEdition.prepareHtml(svgContent, pageNumber)}
                      $referenceLockStyle
                    </body>
                    </html>
                """.trimIndent()

                if (referenceHighlight == null) {
                    TafsirEdition.configureWebView(
                        webView = this,
                        pageNumber = pageNumber,
                        verseIndex = MushafVerseIndex.fromSvg(svgContent),
                        onVerseTapped = { verse -> currentOnVerseTapped.value(verse) }
                    )
                }

                loadDataWithBaseURL(
                    "https://quran-safeguard.local/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}
