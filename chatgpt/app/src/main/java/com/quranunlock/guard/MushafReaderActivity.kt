package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.brotli.dec.BrotliInputStream

class MushafReaderActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE = "page"
        const val EXTRA_CHALLENGE_KEY = "challenge_key"
    }

    private var challengeKey: String = ""
    private var activeReadingPage = 0
    private var displayedPage = 0
    private var pageReady = false
    private var activityResumed = false
    private var activityTopResumed =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    private var selectedTafsirVerse by mutableStateOf<VerseRef?>(null)
    private var tafsirLoadState by mutableStateOf<TafsirLoadState>(TafsirLoadState.Closed)

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

    private fun mayCountActiveReading(): Boolean =
        activityResumed &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                activityTopResumed)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        challengeKey = intent.getStringExtra(EXTRA_CHALLENGE_KEY).orEmpty()
        if (challengeKey.isBlank()) {
            finish()
            return
        }

        val initialPlan = runCatching {
            SafeguardCyclePrefs.currentPlan(this)
        }.getOrNull()
        if (initialPlan == null || initialPlan.first.isEmpty()) {
            finish()
            return
        }

        val initialPages = initialPlan.first
        val initialActiveIndex = initialPlan.second
        activeReadingPage = initialPages[initialActiveIndex]
        displayedPage = activeReadingPage

        GuardRuntime.interception.markReaderVisible(challengeKey)
        GuardDiagnostics.log(
            this,
            "READER_VISIBLE",
            challengeKey,
            "level=${GuardPrefs.challengeLevel(this).name} " +
                "page=$activeReadingPage " +
                "progress=${initialActiveIndex + 1}/${initialPages.size}"
        )

        setContent {
            QuranSafeguardTheme {
                var planPages by remember {
                    mutableStateOf(initialPages)
                }
                var activeIndex by remember {
                    mutableIntStateOf(initialActiveIndex)
                }
                var displayedIndex by remember {
                    mutableIntStateOf(initialActiveIndex)
                }
                var readingMs by remember {
                    mutableLongStateOf(
                        GuardPrefs.readingElapsedMs(
                            this@MushafReaderActivity,
                            challengeKey,
                            planPages[activeIndex]
                        )
                    )
                }
                var bottomReached by remember {
                    mutableStateOf(
                        GuardPrefs.hasReachedReadingBottom(
                            this@MushafReaderActivity,
                            challengeKey,
                            planPages[activeIndex]
                        )
                    )
                }
                var gestureMessage by remember {
                    mutableStateOf(
                        "Balayez vers la droite pour avancer, " +
                            "vers la gauche pour revenir."
                    )
                }
                var quotaReached by remember {
                    mutableStateOf(false)
                }
                var brightnessOpen by remember { mutableStateOf(false) }
                var chromeVisible by remember { mutableStateOf(true) }
                var chromeInteraction by remember { mutableIntStateOf(0) }
                var readerBrightness by remember {
                    mutableFloatStateOf(ReaderComfortPrefs.brightness(this@MushafReaderActivity))
                }
                val quotaPageCount = remember { initialPages.size }
                val level = remember {
                    GuardPrefs.challengeLevel(this@MushafReaderActivity)
                }
                val sectionMode = remember(level) {
                    if (level == ChallengeLevel.MICRO) {
                        GuardPrefs.selectionMode(this@MushafReaderActivity)
                    } else {
                        QuranSelectionMode.HIZB
                    }
                }

                LaunchedEffect(readerBrightness) {
                    ReaderComfortPrefs.applyBrightness(window, readerBrightness)
                }

                LaunchedEffect(
                    chromeVisible,
                    chromeInteraction,
                    brightnessOpen,
                    selectedTafsirVerse
                ) {
                    if (chromeVisible && !brightnessOpen && selectedTafsirVerse == null) {
                        delay(2_500L)
                        chromeVisible = false
                    }
                }

                BackHandler(enabled = brightnessOpen && selectedTafsirVerse == null) {
                    brightnessOpen = false
                }

                BackHandler(enabled = selectedTafsirVerse != null) {
                    closeTafsir()
                }

                LaunchedEffect(selectedTafsirVerse) {
                    val requestedVerse = selectedTafsirVerse ?: return@LaunchedEffect
                    tafsirLoadState = TafsirLoadState.Loading
                    val entry = TafsirEdition.load(
                        this@MushafReaderActivity,
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

                fun pauseActiveReading() {
                    if (pageReady && activeReadingPage in 1..604) {
                        GuardPrefs.endReadingForeground(
                            this@MushafReaderActivity,
                            challengeKey,
                            activeReadingPage
                        )
                    }
                    pageReady = false
                }

                fun showPage(index: Int) {
                    if (selectedTafsirVerse != null ||
                        index !in planPages.indices ||
                        (!quotaReached && index > activeIndex) ||
                        index == displayedIndex
                    ) {
                        return
                    }
                    pauseActiveReading()
                    displayedIndex = index
                    displayedPage = planPages[index]
                    gestureMessage = when {
                        quotaReached ->
                            "Lecture libre • vous pouvez sortir à tout moment."
                        index < activeIndex ->
                            "Page déjà validée. Balayez vers la droite " +
                                "pour revenir à la lecture en cours."
                        else ->
                            "Lisez cette page pendant 60 secondes, puis avancez."
                    }
                }

                fun refreshPlanAfterValidation() {
                    val fresh = SafeguardCyclePrefs.currentPlan(
                        this@MushafReaderActivity
                    )
                    planPages = fresh.first
                    activeIndex = fresh.second
                    activeReadingPage = planPages[activeIndex]
                    displayedIndex = activeIndex
                    displayedPage = activeReadingPage
                    readingMs = GuardPrefs.readingElapsedMs(
                        this@MushafReaderActivity,
                        challengeKey,
                        activeReadingPage
                    )
                    bottomReached = GuardPrefs.hasReachedReadingBottom(
                        this@MushafReaderActivity,
                        challengeKey,
                        activeReadingPage
                    )
                    gestureMessage =
                        "Page suivante prête. Balayez vers la droite après 60 secondes."
                }

                fun continueFreely() {
                    pauseActiveReading()
                    activeReadingPage = 0

                    if (displayedIndex < planPages.lastIndex) {
                        displayedIndex += 1
                        displayedPage = planPages[displayedIndex]
                    } else if (planPages.last() < 604) {
                        val nextPage = planPages.last() + 1
                        planPages = planPages + nextPage
                        displayedIndex = planPages.lastIndex
                        displayedPage = nextPage
                    } else {
                        gestureMessage =
                            "Vous êtes sur la dernière page du Mushaf."
                        return
                    }

                    gestureMessage =
                        "Lecture libre • quota atteint, sortie possible à tout moment."
                }

                fun validateAndAdvance(continueAfterQuota: Boolean = false) {
                    if (quotaReached) {
                        continueFreely()
                        return
                    }
                    if (displayedIndex < activeIndex) {
                        showPage(displayedIndex + 1)
                        return
                    }

                    val currentPage = planPages[activeIndex]
                    val persistedReadingMs = GuardPrefs.readingElapsedMs(
                        this@MushafReaderActivity,
                        challengeKey,
                        currentPage
                    )
                    if (!ReadingValidationPolicy.canValidate(
                            activeReadingMs = persistedReadingMs
                        )
                    ) {
                        gestureMessage = "Encore " +
                            formatRemainingSeconds(
                                GuardPrefs.MIN_READING_MS - persistedReadingMs
                            ) +
                            " avant la page suivante."
                        return
                    }

                    pauseActiveReading()
                    val elapsed = GuardPrefs.completeReadingAndUnlock(
                        this@MushafReaderActivity,
                        challengeKey,
                        currentPage
                    )

                    if (GuardPrefs.isUnlocked(
                            this@MushafReaderActivity,
                            challengeKey
                        )
                    ) {
                        GuardRuntime.interception.markUnlocked(challengeKey)
                        GuardDiagnostics.log(
                            this@MushafReaderActivity,
                            "READING_QUOTA_REACHED",
                            challengeKey,
                            "page=$currentPage elapsedMs=$elapsed"
                        )
                        if (!continueAfterQuota) {
                            TargetReturnCoordinator.returnImmediately(
                                this@MushafReaderActivity,
                                challengeKey,
                                "reading_${level.name.lowercase()}"
                            )
                            return
                        }
                        quotaReached = true
                        activeReadingPage = 0
                        readingMs = GuardPrefs.MIN_READING_MS
                        bottomReached = true
                        gestureMessage =
                            "Quota atteint. Vous pouvez ouvrir l’application " +
                                "cible ou continuer librement."
                    } else {
                        GuardDiagnostics.log(
                            this@MushafReaderActivity,
                            "READING_NEXT_PAGE",
                            challengeKey,
                            "page=$currentPage"
                        )
                        refreshPlanAfterValidation()
                    }
                }

                LaunchedEffect(activeIndex, challengeKey, quotaReached) {
                    while (!quotaReached) {
                        val currentPage = planPages[activeIndex]
                        readingMs = GuardPrefs.readingElapsedMs(
                            this@MushafReaderActivity,
                            challengeKey,
                            currentPage
                        )
                        bottomReached =
                            GuardPrefs.hasReachedReadingBottom(
                                this@MushafReaderActivity,
                                challengeKey,
                                currentPage
                            )
                        delay(200L)
                    }
                }

                val currentDisplayedPage = planPages[displayedIndex]
                val viewingCompletedPage =
                    quotaReached || displayedIndex < activeIndex
                val canValidate = !quotaReached &&
                    ReadingValidationPolicy.canValidate(
                        activeReadingMs = readingMs
                    )
                val sectionDivisions =
                    QuranStructureMetadata.divisionsForPage(
                        sectionMode,
                        currentDisplayedPage
                    )
                val sectionContext = sectionDivisions.joinToString(" • ") {
                    QuranStructureMetadata.unitLabel(sectionMode, it.number) +
                        " · " + it.verseRangeLabel
                }
                val boundaryContext = sectionDivisions.mapNotNull {
                    QuranStructureMetadata.boundaryNotice(
                        sectionMode,
                        it.number,
                        currentDisplayedPage
                    )
                }.joinToString(" ")

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SafeguardReadingSurface
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 0.dp, vertical = 2.dp)
                        ) {
                        if (chromeVisible || brightnessOpen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Qur’an & Tafsîr",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "☼",
                                    modifier = Modifier
                                        .clickable {
                                            brightnessOpen = !brightnessOpen
                                            chromeVisible = true
                                            chromeInteraction += 1
                                        }
                                        .padding(horizontal = 7.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "Mushaf de Médine • Page $currentDisplayedPage / 604",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (brightnessOpen) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = SafeguardReadingSurface,
                                    tonalElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Luminosité",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
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
                                                    this@MushafReaderActivity,
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
                                                        this@MushafReaderActivity,
                                                        null
                                                    )
                                                }
                                            ) { Text("Auto") }
                                            SafeguardOutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                onClick = { brightnessOpen = false }
                                            ) { Text("Fermer") }
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            when {
                                quotaReached ->
                                    "Quota de $quotaPageCount pages atteint • lecture libre"
                                level == ChallengeLevel.MORNING ->
                                    "Filtre matinal • page ${displayedIndex + 1}/$quotaPageCount"
                                level == ChallengeLevel.MICRO ->
                                    "Pause 15 minutes • page 1/1"
                                else ->
                                    "Palier 90 minutes • page ${displayedIndex + 1}/$quotaPageCount"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (sectionContext.isNotBlank()) {
                            Text(
                                sectionContext,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            when {
                                quotaReached -> "Quota atteint • sortie libre"
                                viewingCompletedPage -> "Page validée"
                                readingMs >= GuardPrefs.MIN_READING_MS -> "01:00 atteint • prêt à valider"
                                else -> formatReadingDuration(readingMs) + " / 01:00"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (viewingCompletedPage || canValidate) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.height(3.dp))

                        AnimatedContent(
                            targetState = displayedIndex,
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
                            label = "mushaf-page-swipe"
                        ) { pageIndex ->
                            val pageNumber = planPages[pageIndex]
                            val svgContent = remember(pageNumber) {
                                loadMushafPage(pageNumber)
                            }
                            var loadFailed by remember(pageNumber) {
                                mutableStateOf(svgContent.isNullOrBlank())
                            }

                            if (!svgContent.isNullOrBlank() && !loadFailed) {
                                MushafPageWebView(
                                    svgContent = svgContent,
                                    pageNumber = pageNumber,
                                    tafsirOpen = selectedTafsirVerse != null,
                                    modifier = Modifier.fillMaxSize(),
                                    onReady = {
                                        if (!quotaReached &&
                                            pageNumber == activeReadingPage &&
                                            displayedPage == activeReadingPage
                                        ) {
                                            markPageReady(pageNumber)
                                        }
                                    },
                                    onBottomReached = {
                                        if (!quotaReached &&
                                            pageNumber == activeReadingPage &&
                                            displayedPage == activeReadingPage
                                        ) {
                                            GuardPrefs.markReadingBottomReached(
                                                this@MushafReaderActivity,
                                                challengeKey,
                                                pageNumber
                                            )
                                            bottomReached = true
                                        }
                                    },
                                    onSwipePrevious = {
                                        if (displayedIndex > 0) {
                                            showPage(displayedIndex - 1)
                                        } else {
                                            gestureMessage =
                                                "Vous êtes sur la première page."
                                        }
                                    },
                                    onSwipeNext = {
                                        validateAndAdvance()
                                    },
                                    onReaderTap = {
                                        chromeVisible = true
                                        chromeInteraction += 1
                                    },
                                    onVerseTapped = { verse ->
                                        if (pageNumber == currentDisplayedPage &&
                                            displayedPage == pageNumber
                                        ) {
                                            openTafsir(verse)
                                        }
                                    },
                                    onFailure = {
                                        loadFailed = true
                                        if (pageNumber == activeReadingPage) {
                                            markPageUnavailable(pageNumber)
                                        }
                                    }
                                )
                            } else {
                                Text(
                                    "La page du Mushaf n’est pas disponible. " +
                                        "La lecture n’est pas comptabilisée.",
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(7.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SafeguardOutlinedButton(
                                modifier = Modifier.weight(1f),
                                enabled = selectedTafsirVerse == null && displayedIndex > 0,
                                onClick = {
                                    showPage(displayedIndex - 1)
                                }
                            ) {
                                Text("Page précédente")
                            }
                            SafeguardButton(
                                modifier = Modifier.weight(1f),
                                enabled = selectedTafsirVerse == null &&
                                    if (quotaReached) {
                                        currentDisplayedPage < 604
                                    } else {
                                        viewingCompletedPage || canValidate
                                    },
                                onClick = {
                                    validateAndAdvance()
                                }
                            ) {
                                Text(
                                    when {
                                        quotaReached -> "Continuer à lire"
                                        viewingCompletedPage -> "Page suivante"
                                        activeIndex < planPages.lastIndex ->
                                            "Valider et avancer"
                                        else -> "Débloquer et ouvrir"
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        if (!quotaReached &&
                            !viewingCompletedPage &&
                            activeIndex == planPages.lastIndex &&
                            canValidate
                        ) {
                            SafeguardOutlinedButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                enabled = selectedTafsirVerse == null,
                                onClick = {
                                    validateAndAdvance(continueAfterQuota = true)
                                }
                            ) {
                                Text("Valider et continuer à lire")
                            }
                            Spacer(Modifier.height(5.dp))
                        }
                        if (quotaReached) {
                            SafeguardButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                enabled = selectedTafsirVerse == null,
                                onClick = {
                                    pauseActiveReading()
                                    TargetReturnCoordinator.returnImmediately(
                                        this@MushafReaderActivity,
                                        challengeKey,
                                        "continued_reading_complete"
                                    )
                                }
                            ) {
                                Text("Ouvrir l’application cible")
                            }
                        } else {
                            SafeguardOutlinedButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                enabled = selectedTafsirVerse == null,
                                onClick = {
                                    pauseActiveReading()
                                    finish()
                                }
                            ) {
                                Text("Quitter sans valider")
                            }
                        }
                        }

                        selectedTafsirVerse?.let { verse ->
                            TafsirEdition.Panel(
                                verse = verse,
                                state = tafsirLoadState,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                maxPanelHeight = maxHeight * 0.5f,
                                onPanelTopInWindow = { top ->
                                    TafsirEdition.revealAbove(top)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadMushafPage(page: Int): String? {
        if (page !in 1..604) return null
        val assetPath =
            "mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(page)
        return runCatching {
            assets.open(assetPath).use { compressed ->
                BrotliInputStream(compressed)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }
        }.getOrNull()
    }

    override fun onStart() {
        super.onStart()
        if (challengeKey.isNotBlank()) {
            GuardRuntime.interception.markReaderVisible(challengeKey)
        }
    }

    override fun onStop() {
        if (challengeKey.isNotBlank()) {
            GuardRuntime.interception.markReaderHidden(challengeKey)
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        if (pageReady &&
            displayedPage == activeReadingPage &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ) {
            GuardPrefs.beginReadingForeground(
                this,
                challengeKey,
                activeReadingPage
            )
        }
    }

    override fun onTopResumedActivityChanged(
        isTopResumedActivity: Boolean
    ) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            activityTopResumed == isTopResumedActivity
        ) {
            return
        }

        activityTopResumed = isTopResumedActivity
        if (!pageReady || displayedPage != activeReadingPage) return

        if (mayCountActiveReading()) {
            GuardPrefs.beginReadingForeground(
                this,
                challengeKey,
                activeReadingPage
            )
        } else {
            GuardPrefs.endReadingForeground(
                this,
                challengeKey,
                activeReadingPage
            )
        }
    }

    override fun onPause() {
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

    private fun markPageReady(page: Int) {
        if (page != activeReadingPage ||
            displayedPage != activeReadingPage
        ) {
            return
        }
        if (pageReady) return
        pageReady = true
        if (mayCountActiveReading()) {
            GuardPrefs.beginReadingForeground(
                this,
                challengeKey,
                page
            )
        }
    }

    private fun markPageUnavailable(page: Int) {
        if (pageReady && page == activeReadingPage) {
            GuardPrefs.endReadingForeground(
                this,
                challengeKey,
                page
            )
        }
        pageReady = false
    }
}

private fun formatReadingDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutesPart = seconds / 60L
    val secondsPart = seconds % 60L
    return "%02d:%02d".format(minutesPart, secondsPart)
}

private fun formatRemainingSeconds(milliseconds: Long): String {
    val seconds = ((milliseconds + 999L) / 1000L).coerceAtLeast(0L)
    return if (seconds <= 1L) "1 seconde" else "$seconds secondes"
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@androidx.compose.runtime.Composable
private fun MushafPageWebView(
    svgContent: String,
    pageNumber: Int,
    tafsirOpen: Boolean,
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
    onBottomReached: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onReaderTap: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit,
    onFailure: () -> Unit
) {
    val currentTafsirOpen = rememberUpdatedState(tafsirOpen)
    val currentOnReady = rememberUpdatedState(onReady)
    val currentOnBottomReached = rememberUpdatedState(onBottomReached)
    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnReaderTap = rememberUpdatedState(onReaderTap)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)
    val currentOnFailure = rememberUpdatedState(onFailure)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(
                    android.graphics.Color.parseColor(ReaderComfortPrefs.pageBackground())
                )
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
                        MotionEvent.ACTION_DOWN -> {
                            gestureClassifier.onDown(
                                event.x,
                                event.y,
                                event.pointerCount
                            )
                        }
                        MotionEvent.ACTION_MOVE -> {
                            gestureClassifier.onMove(
                                event.x,
                                event.y,
                                event.pointerCount
                            )
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            gestureClassifier.onAdditionalPointer()
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            gestureClassifier.onCancel()
                        }
                        MotionEvent.ACTION_UP -> {
                            when (
                                gestureClassifier.onUp(
                                    event.x,
                                    event.y,
                                    gesturesEnabled = !currentTafsirOpen.value
                                )
                            ) {
                                ReaderSwipe.NEXT -> currentOnSwipeNext.value()
                                ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()
                                null -> currentOnReaderTap.value()
                            }
                        }
                    }
                    false
                }

                setOnScrollChangeListener { view, _, scrollY, _, _ ->
                    val webView = view as WebView
                    val contentHeightPx =
                        (webView.contentHeight * webView.scale).toInt()
                    val noLongerScrollsDown =
                        !webView.canScrollVertically(1)
                    val needsScroll =
                        ReadingValidationPolicy.requiresScroll(
                            contentHeightPx = contentHeightPx,
                            viewportHeightPx = webView.height
                        )
                    if (contentHeightPx > 0 &&
                        noLongerScrollsDown &&
                        (scrollY > 0 || !needsScroll)
                    ) {
                        currentOnBottomReached.value()
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = true

                    override fun onPageFinished(
                        view: WebView?,
                        url: String?
                    ) {
                        currentOnReady.value()
                        view?.post {
                            val contentHeightPx =
                                (view.contentHeight * view.scale).toInt()
                            if (contentHeightPx > 0 &&
                                view.height > 0 &&
                                !ReadingValidationPolicy.requiresScroll(
                                    contentHeightPx = contentHeightPx,
                                    viewportHeightPx = view.height
                                )
                            ) {
                                currentOnBottomReached.value()
                            }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            currentOnFailure.value()
                        }
                    }
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
                          background: ${ReaderComfortPrefs.READER_CREAM_HEX};
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
                    </body>
                    </html>
                """.trimIndent()

                TafsirEdition.configureWebView(
                    webView = this,
                    pageNumber = pageNumber,
                    verseIndex = MushafVerseIndex.fromSvg(svgContent),
                    onVerseTapped = { verse ->
                        currentOnVerseTapped.value(verse)
                    }
                )

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