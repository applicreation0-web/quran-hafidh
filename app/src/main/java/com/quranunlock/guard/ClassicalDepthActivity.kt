package com.applicreation0.quransafeguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

class ClassicalDepthActivity : ComponentActivity() {
    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID).orEmpty()
        val entry = ClassicalDepthRepository.displayEntry(reminderId)

        if (entry == null) {
            finish()
            return
        }

        setContent {
            QuranSafeguardTheme {
                ClassicalDepthScreen(entry = entry, onClose = { finish() })
            }
        }
    }
}

@Composable
private fun ClassicalDepthScreen(
    entry: ClassicalDepthEntry,
    onClose: () -> Unit
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
                when (entry.kind) {
                    ClassicalDepthKind.HIKAM_CLASSICAL_COMMENTARY ->
                        "Approfondir — commentaire classique"
                    ClassicalDepthKind.GHAZALI_SAME_AUTHOR_CONTEXT ->
                        "Approfondir — contexte dans l’œuvre"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                entry.sourceAuthor + " • " + entry.sourceWork,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text(
                "Texte arabe original",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                entry.arabicText,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge.copy(
                    textDirection = TextDirection.Rtl
                ),
                textAlign = TextAlign.Right
            )

            HorizontalDivider()

            Text(
                "Traduction française",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                entry.frenchText,
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            Text(
                "Source complète",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                entry.sourceAuthor + " • " + entry.sourceWork + " • " +
                    entry.sourceReference,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                if (entry.completePassage) {
                    "Passage présenté intégralement, sans reformulation ni résumé."
                } else {
                    "Extrait continu signalé comme tel, sans reformulation ni résumé."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClose
            ) {
                Text("Retour")
            }
        }
    }
}
