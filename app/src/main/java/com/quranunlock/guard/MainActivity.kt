package com.quranunlock.guard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var mode by remember {
                        mutableStateOf(GuardPrefs.selectionMode(this@MainActivity))
                    }
                    val selectedJuz = remember {
                        mutableStateListOf<Int>().apply {
                            addAll(GuardPrefs.selectedJuz(this@MainActivity).sorted())
                        }
                    }
                    val selectedHizb = remember {
                        mutableStateListOf<Int>().apply {
                            addAll(GuardPrefs.selectedHizb(this@MainActivity).sorted())
                        }
                    }
                    var saved by remember { mutableStateOf(false) }

                    val currentSelection =
                        if (mode == QuranSelectionMode.JUZ) selectedJuz else selectedHizb
                    val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
                    val unitLabel = if (mode == QuranSelectionMode.JUZ) "Juz" else "Hizb"

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        Text("Quran Unlock", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("Choisis comment limiter les pages qui pourront être imposées.")
                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == QuranSelectionMode.JUZ,
                                onClick = {
                                    mode = QuranSelectionMode.JUZ
                                    saved = false
                                }
                            )
                            Text("Par Juz")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == QuranSelectionMode.HIZB,
                                onClick = {
                                    mode = QuranSelectionMode.HIZB
                                    saved = false
                                }
                            )
                            Text("Par Hizb — sélection plus précise")
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (mode == QuranSelectionMode.JUZ) {
                                "Sélectionne un ou plusieurs des 30 juz."
                            } else {
                                "Sélectionne un ou plusieurs des 60 hizb. Pratique si tu ne mémorises encore qu’une petite partie du Coran."
                            }
                        )
                        Spacer(Modifier.height(16.dp))

                        (1..maxUnit).chunked(3).forEach { rowUnits ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowUnits.forEach { unit ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = unit in currentSelection,
                                            onCheckedChange = { checked ->
                                                saved = false
                                                if (checked) {
                                                    if (unit !in currentSelection) {
                                                        currentSelection.add(unit)
                                                    }
                                                } else if (currentSelection.size > 1) {
                                                    currentSelection.remove(unit)
                                                }
                                            }
                                        )
                                        Text("$unitLabel $unit")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = currentSelection.isNotEmpty(),
                            onClick = {
                                GuardPrefs.saveSelectionMode(this@MainActivity, mode)
                                when (mode) {
                                    QuranSelectionMode.JUZ ->
                                        GuardPrefs.saveSelectedJuz(
                                            this@MainActivity,
                                            selectedJuz.toSet()
                                        )
                                    QuranSelectionMode.HIZB ->
                                        GuardPrefs.saveSelectedHizb(
                                            this@MainActivity,
                                            selectedHizb.toSet()
                                        )
                                }
                                saved = true
                            }
                        ) {
                            Text(
                                if (saved) {
                                    "Sélection enregistrée ✓"
                                } else {
                                    "Enregistrer la sélection"
                                }
                            )
                        }

                        Spacer(Modifier.height(28.dp))
                        Text(
                            "À chaque nouveau blocage, une page est choisie uniquement dans les " +
                                unitLabel.lowercase() +
                                " sélectionnés. La page reste la même jusqu’à validation."
                        )
                        Spacer(Modifier.height(20.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        ) {
                            Text("Activer / vérifier la protection")
                        }
                    }
                }
            }
        }
    }
}
