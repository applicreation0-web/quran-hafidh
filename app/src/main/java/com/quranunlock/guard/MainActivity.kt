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
                    val selected = remember {
                        mutableStateListOf<Int>().apply {
                            addAll(GuardPrefs.selectedJuz(this@MainActivity).sorted())
                        }
                    }
                    var saved by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        Text("Quran Unlock", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("Choisis les juz dans lesquels Quran Unlock pourra sélectionner la page obligatoire.")
                        Spacer(Modifier.height(20.dp))

                        (1..30).chunked(3).forEach { rowJuz ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowJuz.forEach { juz ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = juz in selected,
                                            onCheckedChange = { checked ->
                                                saved = false
                                                if (checked) {
                                                    if (juz !in selected) selected.add(juz)
                                                } else if (selected.size > 1) {
                                                    selected.remove(juz)
                                                }
                                            }
                                        )
                                        Text("Juz $juz")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selected.isNotEmpty(),
                            onClick = {
                                GuardPrefs.saveSelectedJuz(this@MainActivity, selected.toSet())
                                saved = true
                            }
                        ) {
                            Text(if (saved) "Juz enregistrés ✓" else "Enregistrer les juz")
                        }

                        Spacer(Modifier.height(28.dp))
                        Text(
                            "À chaque nouveau blocage, une seule page est tirée au sort parmi ces juz. " +
                                "La même page reste imposée jusqu’à validation."
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
