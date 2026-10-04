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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class ReadingSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuranSafeguardTheme { ReadingSelectionScreen() } }
    }

    @Composable
    private fun ReadingSelectionScreen() {
        var mode by remember { mutableStateOf(GuardPrefs.selectionMode(this@ReadingSelectionActivity)) }
        val selectedJuz = remember {
            mutableStateListOf<Int>().apply {
                addAll(GuardPrefs.selectedJuz(this@ReadingSelectionActivity).sorted())
            }
        }
        val selectedHizb = remember {
            mutableStateListOf<Int>().apply {
                addAll(GuardPrefs.selectedHizb(this@ReadingSelectionActivity).sorted())
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    SafeguardButton(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        onClick = { finish() }
                    ) { Text("OK") }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Juz / Hizb",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Choisissez selon les limites réelles des versets et les repères canoniques. Un début ou une fin de Juz/Hizb peut se trouver au milieu d’une page du Muṣḥaf ; cette page de frontière appartient alors visuellement aux deux sections voisines.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SafeguardShapes.large,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (TafsirEdition.isEnabled) "Qur’an & Tafsîr" else "Lecture du Qur’an",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (TafsirEdition.isEnabled) {
                                "Ouvrez librement le Muṣḥaf de Médine et le Tafsîr disponible, sans attendre un événement de déblocage. Cette lecture ne crédite aucun quota Safeguard."
                            } else {
                                "Ouvrez librement le Muṣḥaf de Médine, sans attendre un événement de déblocage. Cette lecture ne crédite aucun quota Safeguard."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SafeguardButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { startActivity(Intent(this@ReadingSelectionActivity, FreeQuranReaderActivity::class.java)) }
                        ) { Text(if (TafsirEdition.isEnabled) "Lecture / Étude" else "Lecture") }
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SafeguardShapes.large,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == QuranSelectionMode.JUZ,
                                onClick = {
                                    mode = QuranSelectionMode.JUZ
                                    GuardPrefs.saveSelectionMode(this@ReadingSelectionActivity, mode)
                                }
                            )
                            Text("Choisir par Juz")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == QuranSelectionMode.HIZB,
                                onClick = {
                                    mode = QuranSelectionMode.HIZB
                                    GuardPrefs.saveSelectionMode(this@ReadingSelectionActivity, mode)
                                }
                            )
                            Text("Choisir par Hizb")
                        }
                        Text(
                            "Safeguard demande jusqu’à 20 pages le matin et jusqu’à 10 pages au palier de 90 minutes, sans sortir d’une section sélectionnée ni répéter une page si le pool canonique choisi est plus court.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val current = if (mode == QuranSelectionMode.JUZ) selectedJuz else selectedHizb
                val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
                val unitLabel = if (mode == QuranSelectionMode.JUZ) "Juz" else "Hizb"

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SafeguardOutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            current.clear()
                            current.addAll(1..maxUnit)
                            persistReadingSelection(mode, selectedJuz, selectedHizb)
                        }
                    ) { Text("Tout sélectionner") }
                    SafeguardOutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            current.clear()
                            persistReadingSelection(mode, selectedJuz, selectedHizb)
                        }
                    ) { Text("Tout désélectionner") }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SafeguardShapes.large,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..maxUnit).forEach { unit ->
                            val division = QuranStructureMetadata.division(mode, unit)
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = SafeguardShapes.small
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = unit in current,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (unit !in current) current.add(unit)
                                            } else current.remove(unit)
                                            persistReadingSelection(mode, selectedJuz, selectedHizb)
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "$unitLabel $unit",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            QuranStructureMetadata.selectionSubtitle(mode, division.number),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            "Repères : ${QuranStructureMetadata.SOURCE_LABEL} • pagination du Muṣḥaf de Médine (604 pages).",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    private fun persistReadingSelection(
        mode: QuranSelectionMode,
        selectedJuz: List<Int>,
        selectedHizb: List<Int>
    ) {
        when (mode) {
            QuranSelectionMode.JUZ -> GuardPrefs.saveSelectedJuz(this, selectedJuz.toSet())
            QuranSelectionMode.HIZB -> GuardPrefs.saveSelectedHizb(this, selectedHizb.toSet())
        }
    }
}