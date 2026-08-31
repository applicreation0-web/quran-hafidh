package com.quranunlock.guard

import android.app.Activity
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
import androidx.compose.runtime.mutableIntStateOf
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
        private const val MIN_READING_SECONDS = 60
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (targetPackage.isNullOrBlank()) {
            finish()
            return
        }

        val page = GuardPrefs.challengePage(this, targetPackage)
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
                    var secondsLeft by remember { mutableIntStateOf(MIN_READING_SECONDS) }
                    var quranOpened by remember { mutableStateOf(false) }
                    var jokersRemaining by remember {
                        mutableIntStateOf(GuardPrefs.remainingJokers(this@GateActivity))
                    }

                    LaunchedEffect(quranOpened) {
                        if (!quranOpened) return@LaunchedEffect
                        while (secondsLeft > 0) {
                            delay(1000)
                            secondsLeft--
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
                        Spacer(Modifier.height(28.dp))
                        Text(
                            "Page " + page,
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(sectionLabel, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Lis entièrement cette page dans Quran for Android. " +
                                "Utilise « Go to page / Aller à la page » et saisis le numéro ci-dessus, puis reviens ici.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(28.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                quranOpened = true
                                QuranReaderLauncher.open(this@GateActivity)
                            }
                        ) {
                            Text("Ouvrir Quran puis aller à la page " + page)
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = quranOpened && secondsLeft == 0,
                            onClick = {
                                GuardPrefs.unlock(this@GateActivity, targetPackage)
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        ) {
                            val label = when {
                                !quranOpened -> "Ouvre d’abord Quran"
                                secondsLeft > 0 -> "Lecture en cours… " + secondsLeft + "s"
                                else -> "J’ai lu entièrement la page " + page
                            }
                            Text(label)
                        }

                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = jokersRemaining > 0 && targetPackage != ProtectedApps.ANDROID_SETTINGS,
                            onClick = {
                                if (GuardPrefs.consumeJoker(this@GateActivity)) {
                                    jokersRemaining = GuardPrefs.remainingJokers(this@GateActivity)
                                    GuardPrefs.unlock(this@GateActivity, targetPackage)
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                } else {
                                    jokersRemaining = 0
                                }
                            }
                        ) {
                            Text(
                                if (targetPackage == ProtectedApps.ANDROID_SETTINGS) {
                                    "Jokers désactivés pour les Paramètres Android"
                                } else if (jokersRemaining > 0) {
                                    "Utiliser 1 joker — " +
                                        jokersRemaining +
                                        "/" +
                                        GuardPrefs.DAILY_JOKERS +
                                        " restants"
                                } else {
                                    "Aucun joker restant aujourd’hui"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
