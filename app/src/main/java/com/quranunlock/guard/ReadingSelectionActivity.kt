package com.applicreation0.quransafeguard

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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
        var mode by remember {
            mutableStateOf(GuardPrefs.selectionMode(this@ReadingSelectionActivity))
        }
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
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = { finish() }
                    ) {
                        Text("OK")
                    }
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
                    "Choisissez les zones du Mushaf éligibles pour les prochaines lectures.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == QuranSelectionMode.JUZ,
                                onClick = {
                                    mode = QuranSelectionMode.JUZ
                                    GuardPrefs.saveSelectionMode(
                                        this@ReadingSelectionActivity,
                                        mode
                                    )
                                }
                            )
                            Text("Choisir par Juz")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == QuranSelectionMode.HIZB,
                                onClick = {
                                    mode = QuranSelectionMode.HIZB
                                    GuardPrefs.saveSelectionMode(
                                        this@ReadingSelectionActivity,
                                        mode
                                    )
                                }
                            )
                            Text("Choisir par Hizb")
                        }
                    }
                }

                val current =
                    if (mode == QuranSelectionMode.JUZ) selectedJuz else selectedHizb
                val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
                val unitLabel = if (mode == QuranSelectionMode.JUZ) "Juz" else "Hizb"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            current.clear()
                            current.addAll(1..maxUnit)
                            persistReadingSelection(mode, selectedJuz, selectedHizb)
                        }
                    ) { Text("Tout sélectionner") }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            current.clear()
                            persistReadingSelection(mode, selectedJuz, selectedHizb)
                        }
                    ) { Text("Tout désélectionner") }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        (1..maxUnit).chunked(3).forEach { rowUnits ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowUnits.forEach { unit ->
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = unit in current,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (unit !in current) current.add(unit)
                                                } else {
                                                    current.remove(unit)
                                                }
                                                persistReadingSelection(
                                                    mode,
                                                    selectedJuz,
                                                    selectedHizb
                                                )
                                            }
                                        )
                                        Text("$unitLabel $unit")
                                    }
                                }
                            }
                        }
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
            QuranSelectionMode.JUZ ->
                GuardPrefs.saveSelectedJuz(this, selectedJuz.toSet())
            QuranSelectionMode.HIZB ->
                GuardPrefs.saveSelectedHizb(this, selectedHizb.toSet())
        }
    }
}
