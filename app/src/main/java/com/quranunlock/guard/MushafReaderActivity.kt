package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.brotli.dec.BrotliInputStream
import kotlin.math.abs

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

    private fun mayCountActiveReading(): Boolean =
        activityResumed &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                activityTopResumed)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        "Balayez vers la gauche pour avancer, " +
                            "vers la droite pour revenir."
                    )
                }
                val level = remember {
                    GuardPrefs.challengeLevel(this@MushafReaderActivity)
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
                    if (index !in planPages.indices ||
                        index > activeIndex ||
                        index == displayedIndex
                    ) {
                        return
                    }
                    pauseActiveReading()
                    displayedIndex = index
                    displayedPage = planPages[index]
                    gestureMessage = if (index < activeIndex) {
                        "Page déjà validée. Balayez vers la gauche " +
                            "pour revenir à la lecture en cours."
                    } else {
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
                        "Page suivante prête. Balayez après 60 secondes."
                }

                fun validateAndAdvance() {
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
                    val persistedBottom =
                        GuardPrefs.hasReachedReadingBottom(
                            this@MushafReaderActivity,
                            challengeKey,
                            currentPage
                        )

                    if (!ReadingValidationPolicy.canValidate(
                            activeReadingMs = persistedReadingMs,
                            bottomReached = persistedBottom
                        )
                    ) {
                        gestureMessage = when {
                            persistedReadingMs < GuardPrefs.MIN_READING_MS ->
                                "Encore " +
                                    formatRemainingSeconds(
                                        GuardPrefs.MIN_READING_MS -
                                            persistedReadingMs
                                    ) +
                                    " avant la page suivante."
                            !persistedBottom ->
                                "60 secondes atteintes. Faites défiler " +
                                    "jusqu’au bas de la page."
                            else -> "Cette page n’est pas encore validable."
                        }
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
                            "READING_UNLOCKED_DIRECTLY",
                            challengeKey,
                            "page=$currentPage elapsedMs=$elapsed"
                        )
                        finishAndRemoveTask()
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

                LaunchedEffect(activeIndex, challengeKey) {
                    while (true) {
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
                val viewingCompletedPage = displayedIndex < activeIndex
                val canValidate = ReadingValidationPolicy.canValidate(
                    activeReadingMs = readingMs,
                    bottomReached = bottomReached
                )

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Mushaf de Médine • Page $currentDisplayedPage",
                            modifier = Modifier.padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when (level) {
                                ChallengeLevel.MORNING ->
                                    "Filtre matinal • page " +
                                        "${displayedIndex + 1}/${planPages.size}"
                                ChallengeLevel.MICRO ->
                                    "Pause 15 minutes • page 1/1"
                                ChallengeLevel.HIZB ->
                                    "Palier 90 minutes • page " +
                                        "${displayedIndex + 1}/${planPages.size}"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when {
                                viewingCompletedPage ->
                                    "Page validée • retour libre"
                                readingMs >= GuardPrefs.MIN_READING_MS &&
                                    bottomReached ->
                                    "01:00 atteint • balayez pour valider"
                                readingMs >= GuardPrefs.MIN_READING_MS ->
                                    "01:00 atteint • parcourez le bas de page"
                                bottomReached ->
                                    "Page parcourue • " +
                                        formatReadingDuration(readingMs) +
                                        " / 01:00"
                                else ->
                                    "Lecture active • " +
                                        formatReadingDuration(readingMs) +
                                        " / 01:00"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (viewingCompletedPage || canValidate) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            gestureMessage,
                            modifier = Modifier.padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(3.dp))

                        AnimatedContent(
                            targetState = displayedIndex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { width -> width } togetherWith
                                        slideOutHorizontally { width -> -width }
                                } else {
                                    slideInHorizontally { width -> -width } togetherWith
                                        slideOutHorizontally { width -> width }
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
                                    modifier = Modifier.fillMaxSize(),
                                    onReady = {
                                        if (pageNumber == activeReadingPage &&
                                            displayedPage == activeReadingPage
                                        ) {
                                            markPageReady(pageNumber)
                                        }
                                    },
                                    onBottomReached = {
                                        if (pageNumber == activeReadingPage &&
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
                                enabled = displayedIndex > 0,
                                onClick = {
                                    showPage(displayedIndex - 1)
                                }
                            ) {
                                Text("Page précédente")
                            }
                            SafeguardButton(
                                modifier = Modifier.weight(1f),
                                enabled = viewingCompletedPage || canValidate,
                                onClick = {
                                    validateAndAdvance()
                                }
                            ) {
                                Text(
                                    when {
                                        viewingCompletedPage -> "Page suivante"
                                        activeIndex < planPages.lastIndex ->
                                            "Valider et avancer"
                                        else -> "Valider le palier"
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        SafeguardOutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            onClick = {
                                pauseActiveReading()
                                finish()
                            }
                        ) {
                            Text("Quitter sans valider")
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
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
    onBottomReached: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onFailure: () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                var downX = 0f
                var downY = 0f
                val swipeThreshold = 72f * resources.displayMetrics.density
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                            downY = event.y
                        }
                        MotionEvent.ACTION_UP -> {
                            val deltaX = event.x - downX
                            val deltaY = event.y - downY
                            if (abs(deltaX) >= swipeThreshold &&
                                abs(deltaX) > abs(deltaY) * 1.25f
                            ) {
                                if (deltaX < 0f) {
                                    onSwipeNext()
                                } else {
                                    onSwipePrevious()
                                }
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
                        onBottomReached()
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
                        onReady()
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
                                onBottomReached()
                            }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onFailure()
                        }
                    }
                }

                val html = """
                    <!doctype html>
                    <html>
                    <head>
                      <meta name="viewport"
                            content="width=device-width, initial-scale=1.0">
                      <style>
                        html, body {
                          margin: 0;
                          padding: 0;
                          background: #ffffff;
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
                      $svgContent
                    </body>
                    </html>
                """.trimIndent()

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
