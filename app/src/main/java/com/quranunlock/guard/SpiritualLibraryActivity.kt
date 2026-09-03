package com.applicreation0.quransafeguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

private enum class LibrarySection {
    HADITH,
    HIKAM
}

class SpiritualLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranSafeguardTheme {
                SpiritualLibraryScreen(
                    hadiths = ReminderLibrary.all(this)
                        .filter { it.type == ReminderType.HADITH },
                    hikamEntries = HikamRepository.entries(this)
                        .filter { it.displayEligible },
                    onOpenHikma = { id ->
                        startActivity(
                            Intent(this, HikamDetailActivity::class.java)
                                .putExtra(HikamDetailActivity.EXTRA_HIKMA_ID, id)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SpiritualLibraryScreen(
    hadiths: List<DailyReminder>,
    hikamEntries: List<HikmaEntry>,
    onOpenHikma: (String) -> Unit
) {
    var section by remember { mutableStateOf(LibrarySection.HADITH) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "RAPPEL / TEXTES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Bibliothèque spirituelle",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Texte arabe, traduction française et source, sans interprétation ajoutée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LibrarySection.entries.forEach { candidate ->
                    val label = when (candidate) {
                        LibrarySection.HADITH -> "Hadiths"
                        LibrarySection.HIKAM -> "Al-Hikam"
                    }
                    if (candidate == section) {
                        SafeguardButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            onClick = { section = candidate }
                        ) { Text(label) }
                    } else {
                        SafeguardOutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            onClick = { section = candidate }
                        ) { Text(label) }
                    }
                }
            }

            when (section) {
                LibrarySection.HADITH -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hadiths, key = { it.id }) { reminder ->
                            DailyReminderCard(reminder)
                        }
                    }
                }

                LibrarySection.HIKAM -> {
                    val entries = hikamEntries
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "Ḥikam vocalisées, avec leur traduction française et leur source vérifiée.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(entries, key = { it.canonicalId }) { hikma ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.elevatedCardElevation(
                                    defaultElevation = 1.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(17.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Text(
                                        "Hikma " + hikma.sourceNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        hikma.arabicText,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            textDirection = TextDirection.Rtl
                                        ),
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        hikma.frenchText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Ibn ʿAṭāʾ Allāh • " + hikma.source.locator,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    SafeguardButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        onClick = { onOpenHikma(hikma.canonicalId) }
                                    ) {
                                        Text("Lire")
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}
