package com.applicreation0.quransafeguard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

@Composable
fun DailyReminderCard(
    reminder: DailyReminder,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                reminderLabel(reminder),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                reminder.arabicText,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge.copy(
                    textDirection = TextDirection.Rtl
                ),
                textAlign = TextAlign.Right,
                fontWeight = FontWeight.Medium
            )

            Text(
                reminder.frenchText,
                style = MaterialTheme.typography.bodyLarge
            )

            val source = buildString {
                append(reminder.author)
                append(" • ")
                append(reminder.book)
                if (reminder.reference.isNotBlank()) {
                    append(" • ")
                    append(reminder.reference)
                }
                if (!reminder.authenticity.isNullOrBlank()) {
                    append(" • ")
                    append(reminder.authenticity)
                }
            }

            if (reminder.type == ReminderType.HIKAM) {
                SafeguardButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.startActivity(
                            Intent(context, HikamDetailActivity::class.java)
                                .putExtra(HikamDetailActivity.EXTRA_HIKMA_ID, reminder.id)
                        )
                    }
                ) {
                    Text("Voir la Hikma")
                }
            }

            if (reminder.type == ReminderType.GHAZALI) {
                SafeguardButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.startActivity(
                            Intent(context, GhazaliDetailActivity::class.java)
                                .putExtra(GhazaliDetailActivity.EXTRA_ID, reminder.id)
                        )
                    }
                ) {
                    Text("Voir le texte")
                }
            }

            if (reminder.type != ReminderType.HADITH) {
                Text(
                    "Authenticité : source et attribution vérifiées",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Traduction française : interne Quran Safeguard • revue humaine non requise",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun reminderLabel(reminder: DailyReminder): String =
    when (reminder.type) {
        ReminderType.HADITH -> "Rappel du jour • Hadith authentifié"
        ReminderType.GHAZALI -> "Rappel du jour • Texte d’al-Ghazâlî"
        ReminderType.HIKAM -> "Rappel du jour • Al-Hikam"
    }
