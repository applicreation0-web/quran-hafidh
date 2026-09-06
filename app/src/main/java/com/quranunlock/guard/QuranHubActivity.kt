package com.applicreation0.quransafeguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One clear home for Qur'an reading features; no challenge state is mutated here. */
class QuranHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranSafeguardTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Qur’an",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Lire, choisir votre parcours et retrouver votre progression.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    QuranHubCard(
                        title = if (TafsirEdition.isEnabled) "Lecture libre & Tafsîr" else "Lecture du Qur’an",
                        subtitle = "Muṣḥaf de Médine • navigation rapide • marque-pages",
                        primary = true,
                        onClick = {
                            startActivity(
                                Intent(
                                    this@QuranHubActivity,
                                    if (TafsirEdition.isEnabled) {
                                        FreeQuranReaderActivity::class.java
                                    } else {
                                        ReadingSelectionActivity::class.java
                                    }
                                )
                            )
                        }
                    )
                    QuranHubCard(
                        title = "Parcours Juz / Hizb",
                        subtitle = "Choisir les zones utilisées pour le parcours quotidien Safeguard",
                        onClick = {
                            startActivity(Intent(this@QuranHubActivity, ReadingSelectionActivity::class.java))
                        }
                    )
                    QuranHubCard(
                        title = "Historique de lecture",
                        subtitle = "Pages validées, temps de lecture et progression enregistrée sur ce téléphone",
                        onClick = {
                            startActivity(Intent(this@QuranHubActivity, ReadingHistoryActivity::class.java))
                        }
                    )
                    Text(
                        "La lecture libre reste séparée du parcours quotidien et ne crédite jamais un déblocage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun QuranHubCard(
    title: String,
    subtitle: String,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (primary) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
