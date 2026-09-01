package com.applicreation0.quransafeguard

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    val depthEntry = ClassicalDepthRepository.displayEntry(reminder.id)

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

            Text(
                source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (depthEntry != null) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.startActivity(
                            Intent(context, ClassicalDepthActivity::class.java)
                                .putExtra(
                                    ClassicalDepthActivity.EXTRA_REMINDER_ID,
                                    reminder.id
                                )
                        )
                    }
                ) {
                    Text(depthEntry.buttonLabel)
                }
            }
        }
    }
}

private fun reminderLabel(reminder: DailyReminder): String =
    when (reminder.type) {
        ReminderType.HADITH -> "Rappel du jour • Hadith authentifié"
        ReminderType.GHAZALI -> "Rappel du jour • Sagesse d’al-Ghazâlî"
        ReminderType.HIKAM -> "Rappel du jour • Al-Hikam"
    }
