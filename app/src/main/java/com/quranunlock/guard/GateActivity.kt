package com.quranunlock.guard

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
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

    private data class Verse(val arabic: String, val translation: String, val reference: String)

    private val verses = listOf(
        Verse("وَالْعَصْرِ", "Par le Temps !", "Al-ʿAsr 103:1"),
        Verse("إِنَّ الْإِنسَانَ لَفِي خُسْرٍ", "L’être humain est certes en perdition.", "Al-ʿAsr 103:2"),
        Verse("إِنَّ مَعَ الْعُسْرِ يُسْرًا", "Avec la difficulté est certes une facilité.", "Ash-Sharh 94:6")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (targetPackage.isNullOrBlank()) {
            finish()
            return
        }

        val verse = verses[(SystemClock.elapsedRealtime() % verses.size).toInt()]

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var secondsLeft by remember { mutableIntStateOf(12) }

                    LaunchedEffect(Unit) {
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
                        Text("Lis avant de continuer", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(32.dp))
                        Text(
                            verse.arabic,
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(verse.translation, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(verse.reference, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(28.dp))

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                QuranReaderLauncher.open(this@GateActivity)
                            }
                        ) {
                            Text("Lire dans Quran for Android")
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = secondsLeft == 0,
                            onClick = {
                                GuardPrefs.unlock(this@GateActivity, targetPackage)
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        ) {
                            Text(if (secondsLeft > 0) "Lecture… " + secondsLeft + "s" else "J’ai lu — déverrouiller")
                        }
                    }
                }
            }
        }
    }
}
