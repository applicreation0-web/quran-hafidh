package com.applicreation0.quransafeguard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class ReadingCompleteActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE = "page"
        const val EXTRA_ELAPSED_MS = "elapsed_ms"
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = intent.getIntExtra(EXTRA_PAGE, 0)
        val elapsedMs = intent.getLongExtra(EXTRA_ELAPSED_MS, 0L)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()

        if (page !in 1..604 || targetPackage.isBlank() ||
            !GuardPrefs.hasPendingCompletedReading(this, targetPackage, page)
        ) {
            finish()
            return
        }

        val reminder = runCatching { DailyReminderManager.today(this) }.getOrNull()
        val today = GuardPrefs.dailyReadingSummary(this)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    GuardDiagnostics.log(
                        this@ReadingCompleteActivity,
                        "READING_SUMMARY_LEFT_WITHOUT_UNLOCK",
                        targetPackage,
                        "page=$page"
                    )
                    startActivity(
                        Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    finishAndRemoveTask()
                }
            }
        )

        setContent {
            QuranSafeguardTheme {
                ReadingCompleteScreen(
                    page = page,
                    elapsedMs = elapsedMs,
                    today = today,
                    reminder = reminder,
                    onContinue = {
                        if (GuardPrefs.unlockAfterCompletedReading(
                                this@ReadingCompleteActivity,
                                targetPackage,
                                page
                            )
                        ) {
                            GuardRuntime.interception.markUnlocked(targetPackage)
                            GuardDiagnostics.log(
                                this@ReadingCompleteActivity,
                                "READING_UNLOCKED_AFTER_SUMMARY",
                                targetPackage,
                                "page=$page elapsedMs=$elapsedMs"
                            )
                            finishAndRemoveTask()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ReadingCompleteScreen(
    page: Int,
    elapsedMs: Long,
    today: DailyReadingSummary,
    reminder: DailyReminder?,
    onContinue: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Lecture terminée 🌿",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                buildString {
                    if (page in 1..604) {
                        append("Page ")
                        append(page)
                        append(" • ")
                    }
                    append(formatCompletionDuration(elapsedMs))
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (today.pages > 0) {
                Text(
                    "Aujourd’hui : " + today.pages + " page(s) • " +
                        formatCompletionDuration(today.totalMs) +
                        " • moyenne " + formatCompletionDuration(today.averageMs) + " / page",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (reminder != null) {
                DailyReminderCard(reminder = reminder)
            } else {
                Text(
                    "Le rappel du jour n’est pas disponible. Aucun contenu non vérifié n’est affiché.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinue
            ) {
                Text("Continuer")
            }

            Text(
                "Un seul rappel principal est conservé pour toute la journée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCompletionDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        minutes.toString() + " min " + seconds + " s"
    } else {
        seconds.toString() + " s"
    }
}
