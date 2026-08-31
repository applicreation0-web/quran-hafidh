package com.quranunlock.guard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
        const val EXTRA_PURPOSE = "purpose"
        const val PURPOSE_ACCESS = "access"
        const val PURPOSE_UNINSTALL = "uninstall"
        private const val MIN_READING_MS = 60_000L
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val challengeKey = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (challengeKey.isNullOrBlank()) {
            finish()
            return
        }

        val purpose = intent.getStringExtra(EXTRA_PURPOSE) ?: PURPOSE_ACCESS
        val uninstallFlow = purpose == PURPOSE_UNINSTALL

        val page = GuardPrefs.challengePage(this, challengeKey)
        GuardPrefs.ensureReadingSession(this, challengeKey, page)

        val mode = GuardPrefs.selectionMode(this)
        val sectionLabel = when (mode) {
            QuranSelectionMode.JUZ ->
                QuranPageSelector.juzForPage(page).joinToString(" / ") { "Juz " + it }
            QuranSelectionMode.HIZB ->
                QuranPageSelector.hizbForPage(page).joinToString(" / ") { "Hizb " + it }
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
                            if (uninstallFlow) {
                                "Une page avant la désinstallation"
                            } else {
                                "Une page avant de continuer"
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Mushaf de Médine • Page " + page,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(sectionLabel, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "La page est intégrée à Quran Unlock. Le compteur avance uniquement " +
                                "pendant que cette page est réellement affichée au premier plan.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@GateActivity,
                                        MushafReaderActivity::class.java
                                    ).apply {
                                        putExtra(MushafReaderActivity.EXTRA_PAGE, page)
                                        putExtra(
                                            MushafReaderActivity.EXTRA_CHALLENGE_KEY,
                                            challengeKey
                                        )
                                    }
                                )
                            }
                        ) {
                            Text("Lire la page " + page)
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Temps de lecture validé : " +
                                secondsValidated +
                                " / 60 s"
                        )

                        Spacer(Modifier.height(12.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = readingComplete,
                            onClick = {
                                if (uninstallFlow) {
                                    GuardPrefs.completeChallengeWithoutUnlock(
                                        this@GateActivity,
                                        challengeKey
                                    )
                                    openSystemUninstall()
                                } else {
                                    GuardPrefs.unlock(this@GateActivity, challengeKey)
                                }

                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        ) {
                            Text(
                                when {
                                    !readingComplete -> "Terminer la lecture de la page"
                                    uninstallFlow -> "Page lue — désinstaller"
                                    else -> "Page lue — continuer"
                                }
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uninstallFlow &&
                                challengeKey != ProtectedApps.ANDROID_SETTINGS &&
                                jokersRemaining > 0,
                            onClick = {
                                if (GuardPrefs.consumeJoker(this@GateActivity)) {
                                    jokersRemaining =
                                        GuardPrefs.remainingJokers(this@GateActivity)
                                    GuardPrefs.unlock(this@GateActivity, challengeKey)
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                } else {
                                    jokersRemaining = 0
                                }
                            }
                        ) {
                            Text(
                                when {
                                    uninstallFlow ->
                                        "Pas de joker pour la désinstallation"
                                    challengeKey == ProtectedApps.ANDROID_SETTINGS ->
                                        "Jokers désactivés pour les Paramètres Android"
                                    jokersRemaining > 0 ->
                                        "Utiliser 1 joker — " +
                                            jokersRemaining +
                                            "/" +
                                            GuardPrefs.DAILY_JOKERS +
                                            " restants"
                                    else ->
                                        "Aucun joker restant aujourd’hui"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openSystemUninstall() {
        val uninstall = Intent(
            Intent.ACTION_DELETE,
            Uri.parse("package:" + packageName)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching {
            startActivity(uninstall)
        }.getOrElse {
            val appDetails = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(appDetails)
        }
    }
}
