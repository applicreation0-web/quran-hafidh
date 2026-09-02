package com.applicreation0.quransafeguard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class GateActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    private var challengeKey: String = ""

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        if (challengeKey.isNotBlank() &&
            (!ProtectedApps.isProtected(this, challengeKey) ||
                GuardPrefs.isUnlocked(this, challengeKey) ||
                GuardRuntime.externalForegroundPackage() != challengeKey)
        ) {
            GuardRuntime.interception.reset()
            finishAndRemoveTask()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        challengeKey = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        if (challengeKey.isBlank()) {
            finish()
            return
        }

        // Never render a gate for a target that is now permanently excluded,
        // including stale intents/configuration left by an older app version.
        if (!ProtectedApps.isProtected(this, challengeKey) ||
            GuardRuntime.externalForegroundPackage() != challengeKey
        ) {
            GuardRuntime.interception.reset()
            finishAndRemoveTask()
            return
        }

        GuardRuntime.interception.markGateVisible(challengeKey)
        GuardDiagnostics.log(this, "GATE_VISIBLE", challengeKey)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    GuardDiagnostics.log(
                        this@GateActivity,
                        "GATE_BACK_TO_HOME",
                        challengeKey
                    )
                    startActivity(
                        Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    finish()
                }
            }
        )

        val page = GuardPrefs.challengePage(this, challengeKey)
        GuardPrefs.ensureReadingSession(this, challengeKey, page)

        val mode = GuardPrefs.selectionMode(this)
        val sectionLabel = when (mode) {
            QuranSelectionMode.JUZ ->
                QuranPageSelector.juzForPage(page).joinToString(" / ") { "Juz $it" }
            QuranSelectionMode.HIZB ->
                QuranPageSelector.hizbForPage(page).joinToString(" / ") { "Hizb $it" }
        }

        setContent {
            QuranSafeguardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var readingMs by remember {
                        mutableLongStateOf(
                            GuardPrefs.readingElapsedMs(
                                this@GateActivity,
                                challengeKey,
                                page
                            )
                        )
                    }
                    var jokersRemaining by remember {
                        mutableStateOf(GuardPrefs.remainingJokers(this@GateActivity))
                    }
                    var bottomReached by remember {
                        mutableStateOf(
                            GuardPrefs.hasReachedReadingBottom(
                                this@GateActivity,
                                challengeKey,
                                page
                            )
                        )
                    }

                    LaunchedEffect(challengeKey, page) {
                        while (true) {
                            readingMs = GuardPrefs.readingElapsedMs(
                                this@GateActivity,
                                challengeKey,
                                page
                            )
                            bottomReached = GuardPrefs.hasReachedReadingBottom(
                                this@GateActivity,
                                challengeKey,
                                page
                            )
                            delay(500)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Une page avant de continuer",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Mushaf de Médine • Page $page",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(sectionLabel, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Le chrono mesure votre temps réel de lecture. Il se met en pause si vous quittez la page.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        SafeguardButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { openReader(page) }
                        ) { Text("Lire la page $page") }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Temps de lecture : " + formatGateDuration(readingMs) +
                                if (bottomReached) " • bas de page atteint" else ""
                        )

                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (bottomReached && readingMs >= GuardPrefs.MIN_READING_MS) {
                                "La validation se fait directement dans la page du Mushaf."
                            } else {
                                "Parcourez la page et lisez-la activement pendant au moins 60 secondes."
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(Modifier.height(20.dp))
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = jokersRemaining > 0,
                            onClick = {
                                if (GuardPrefs.consumeJoker(this@GateActivity)) {
                                    jokersRemaining =
                                        GuardPrefs.remainingJokers(this@GateActivity)
                                    GuardPrefs.unlockWithJoker(
                                        this@GateActivity,
                                        challengeKey
                                    )
                                    GuardRuntime.interception.markUnlocked(challengeKey)
                                    GuardDiagnostics.log(
                                        this@GateActivity,
                                        "JOKER_UNLOCKED",
                                        challengeKey
                                    )
                                    setResult(Activity.RESULT_OK)
                                    finishAndRemoveTask()
                                } else {
                                    jokersRemaining =
                                        GuardPrefs.remainingJokers(this@GateActivity)
                                }
                            }
                        ) {
                            Text(
                                if (jokersRemaining > 0) {
                                    "Utiliser 1 joker — $jokersRemaining/${GuardPrefs.DAILY_JOKERS} • ${GuardPrefs.JOKER_MAX_UNLOCK_MINUTES} min max"
                                } else {
                                    "Aucun joker restant aujourd’hui"
                                }
                            )
                        }
                    }
                }
            }
        }

        // Open the Quran page immediately. If the reader is closed too early,
        // this control screen remains behind it with the accumulated progress.
        window.decorView.post { openReader(page) }
    }

    override fun onStart() {
        super.onStart()
        if (challengeKey.isNotBlank()) {
            GuardRuntime.interception.markGateVisible(challengeKey)
        }
    }

    override fun onStop() {
        if (challengeKey.isNotBlank()) {
            GuardRuntime.interception.markGateHidden(challengeKey)
        }
        super.onStop()
    }

    private fun openReader(page: Int) {
        if (isFinishing || challengeKey.isBlank()) return
        startActivity(
            Intent(this, MushafReaderActivity::class.java).apply {
                putExtra(MushafReaderActivity.EXTRA_PAGE, page)
                putExtra(MushafReaderActivity.EXTRA_CHALLENGE_KEY, challengeKey)
            }
        )
    }
}


private fun formatGateDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutesPart = seconds / 60L
    val secondsPart = seconds % 60L
    return "%02d:%02d".format(minutesPart, secondsPart)
}
