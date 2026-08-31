package com.applicreation0.quransafeguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

class HikamLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranSafeguardTheme {
                HikamLibraryScreen(HikamCorpus.verified(this@HikamLibraryActivity))
            }
        }
    }
}

@Composable
private fun HikamLibraryScreen(allHikam: List<HikamEntry>) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim().lowercase()
    val visible = remember(allHikam, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            allHikam
        } else {
            allHikam.filter { hikma ->
                hikma.sourceNumber == normalizedQuery ||
                    hikma.french.lowercase().contains(normalizedQuery) ||
                    hikma.arabic.contains(query.trim()) ||
                    hikma.author.lowercase().contains(normalizedQuery) ||
                    hikma.themes.any { it.lowercase().contains(normalizedQuery) }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Al-Ḥikam al-ʿAṭāʾiyya",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${allHikam.size} Ḥikam vérifiées • arabe + français • hors ligne",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Rechercher : n°, mot, thème ou auteur") }
            )
            Text(
                "${visible.size} résultat(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = visible,
                    key = { it.id }
                ) { hikma ->
                    HikmaLibraryCard(hikma)
                }
            }
        }
    }
}

@Composable
private fun HikmaLibraryCard(hikma: HikamEntry) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Ḥikma n° ${hikma.sourceNumber}",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    hikma.themes.take(2).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                hikma.arabic,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium.copy(
                    textDirection = TextDirection.Rtl
                ),
                textAlign = TextAlign.Right
            )

            Text(
                hikma.french,
                style = MaterialTheme.typography.bodyLarge
            )

            hikma.explanation?.takeIf { it.isNotBlank() }?.let { explanation ->
                Text(
                    "Explication",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    explanation,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                "Ibn ʿAṭāʾ Allāh al-Iskandarī • Al-Ḥikam al-ʿAṭāʾiyya • " +
                    "Ḥikma n° ${hikma.sourceNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
