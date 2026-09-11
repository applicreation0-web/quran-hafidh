package com.applicreation0.quransafeguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Qur’an",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (TafsirEdition.isEnabled) "Lecture, Tafsîr, mémorisation et Hifz." else "Lecture, mémorisation et Hifz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    QuranHubRow(
                        title = if (TafsirEdition.isEnabled) "Lecture libre & Tafsîr" else "Lecture du Qur’an",
                        subtitle = "Muṣḥaf de Médine • signets • navigation",
                        onClick = {
                            startActivity(Intent(this@QuranHubActivity, FreeQuranReaderActivity::class.java))
                        }
                    )
                    QuranHubRow(
                        title = "Mémorisation",
                        subtitle = "Mode libre et ponctuel • sourate, page ou passage",
                        onClick = {
                            startActivity(
                                Intent(this@QuranHubActivity, FreeQuranReaderActivity::class.java)
                                    .putExtra(FreeQuranReaderActivity.EXTRA_MEMORIZATION, true)
                            )
                        }
                    )
                    QuranHubRow(
                        title = "Parcours Hifz",
                        subtitle = "Sabqi • Itqān • Murājaʿah • suivi structuré indépendant",
                        onClick = {
                            startActivity(Intent(this@QuranHubActivity, HifzJourneyActivity::class.java))
                        }
                    )
                    QuranHubRow(
                        title = "Parcours Juz / Hizb",
                        subtitle = "Choisir le parcours quotidien Safeguard",
                        onClick = {
                            startActivity(Intent(this@QuranHubActivity, ReadingSelectionActivity::class.java))
                        }
                    )
                    QuranHubRow(
                        title = "Historique de lecture",
                        subtitle = "Pages, temps de lecture et progression locale",
                        onClick = {
                            startActivity(Intent(this@QuranHubActivity, ReadingHistoryActivity::class.java))
                        }
                    )

                    Text(
                        "Lecture libre, Mémorisation, Parcours Hifz et Challenge gardent des états séparés : aucune lecture libre ne crédite un déblocage ni ne déplace le planning Hifz.",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranHubRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 2.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
