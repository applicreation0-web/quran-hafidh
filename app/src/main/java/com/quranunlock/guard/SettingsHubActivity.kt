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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Stable top-level settings architecture.
 * Existing detailed screens remain available behind these categories while the
 * dashboard no longer duplicates every destination.
 */
class SettingsHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranSafeguardTheme {
                var visualMode by remember {
                    mutableStateOf(ReaderComfortPrefs.visualMode(this@SettingsHubActivity))
                }
                var brightness by remember {
                    mutableFloatStateOf(ReaderComfortPrefs.brightness(this@SettingsHubActivity))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Réglages",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Chaque fonction a maintenant une seule place principale.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingsSection(
                        title = "Protection",
                        subtitle = "Activation, règles 15 / 90 min et diagnostic"
                    ) {
                        SafeguardButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(this@SettingsHubActivity, MainActivity::class.java))
                            }
                        ) { Text("Protection & règles") }
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(this@SettingsHubActivity, ApplicationsActivity::class.java))
                            }
                        ) { Text("Applications protégées") }
                    }

                    SettingsSection(
                        title = "Qur’an",
                        subtitle = "Parcours quotidien et historique"
                    ) {
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(this@SettingsHubActivity, ReadingSelectionActivity::class.java))
                            }
                        ) { Text("Choix Juz / Hizb") }
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(this@SettingsHubActivity, ReadingHistoryActivity::class.java))
                            }
                        ) { Text("Historique de lecture") }
                    }

                    SettingsSection(
                        title = "Rappels",
                        subtitle = "Pensée du jour, adhkâr et horaires locaux"
                    ) {
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(this@SettingsHubActivity, MainActivity::class.java))
                            }
                        ) { Text("Rappels & horaires") }
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(this@SettingsHubActivity, AdhkarActivity::class.java))
                            }
                        ) { Text("Voir les adhkâr") }
                    }

                    SettingsSection(
                        title = "Apparence & confort",
                        subtitle = "Le Muṣḥaf n’est jamais inversé ni recoloré artificiellement"
                    ) {
                        Text(
                            "Ambiance de lecture",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReaderVisualMode.entries.forEach { mode ->
                                val label = when (mode) {
                                    ReaderVisualMode.COMFORT -> "Confort"
                                    ReaderVisualMode.LIGHT -> "Clair"
                                    ReaderVisualMode.DARK -> "Sombre"
                                }
                                val selected = visualMode == mode
                                if (selected) {
                                    SafeguardButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = {}
                                    ) { Text(label) }
                                } else {
                                    SafeguardOutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            visualMode = mode
                                            ReaderComfortPrefs.setVisualMode(
                                                this@SettingsHubActivity,
                                                mode
                                            )
                                        }
                                    ) { Text(label) }
                                }
                            }
                        }

                        Text(
                            if (brightness < 0f) {
                                "Luminosité : suivre le téléphone"
                            } else {
                                "Luminosité de la lecture : ${(brightness * 100).toInt()} %"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = if (brightness < 0f) 0.72f else brightness,
                            onValueChange = { value ->
                                brightness = value.coerceIn(0.12f, 1f)
                                ReaderComfortPrefs.setBrightness(
                                    this@SettingsHubActivity,
                                    brightness
                                )
                                ReaderComfortPrefs.applyBrightness(window, brightness)
                            },
                            valueRange = 0.12f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                brightness = -1f
                                ReaderComfortPrefs.setBrightness(this@SettingsHubActivity, null)
                                ReaderComfortPrefs.applyBrightness(window, -1f)
                            }
                        ) { Text("Suivre la luminosité du téléphone") }
                        Text(
                            "Ces réglages concernent la lecture libre. Aucun rappel de repos visuel n’est ajouté.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SettingsSection(
                        title = "Textes & contenu",
                        subtitle = "Hadiths, Ḥikam et autres textes vérifiés"
                    ) {
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(this@SettingsHubActivity, SpiritualLibraryActivity::class.java))
                            }
                        ) { Text("Bibliothèque") }
                    }

                    SettingsSection(
                        title = "À propos",
                        subtitle = "Installation privée, version et informations techniques"
                    ) {
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(this@SettingsHubActivity, MainActivity::class.java))
                            }
                        ) { Text("Informations & réglages détaillés") }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
