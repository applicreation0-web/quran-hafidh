package com.applicreation0.quransafeguard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
        private const val MIN_READING_MS = 60_000L
    }

    private var challengeKey: String = ""

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        if (challengeKey.isNotBlank() && GuardPrefs.isUnlocked(this, challengeKey)) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        challengeKey = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        if (challengeKey.isBlank()) {
            finish()
            return
        }

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
            MaterialTheme {
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

                    LaunchedEffect(challengeKey, page) {
                        while (true) {
                            readingMs = GuardPrefs.readingElapsedMs(
                                this@GateActivity,
                                challengeKey,
                                page
                            )
                            delay(500)
                        }
                    }

                    val secondsValidated = (readingMs / 1000L).coerceAtMost(60L)
                    val readingComplete = readingMs >= MIN_READING_MS

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
                            "Le compteur avance uniquement lorsque cette page est réellement affichée au premier plan.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { openReader(page) }
                        ) { Text("Lire la page $page") }

                        Spacer(Modifier.height(12.dp))
                        Text("Temps de lecture validé : $secondsValidated / 60 s")

                        Spacer(Modifier.height(12.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = readingComplete,
                            onClick = {
                                GuardPrefs.unlock(this@GateActivity, challengeKey)
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        ) {
                            Text(
                                if (readingComplete) {
                                    "Page lue — continuer"
                                } else {
                                    "Terminer la lecture de la page"
                                }
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(
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
                                    setResult(Activity.RESULT_OK)
                                    finish()
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
