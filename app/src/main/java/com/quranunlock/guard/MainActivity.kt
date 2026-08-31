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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    private val serviceEnabledState = mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        BrowserDetector.refresh()
        serviceEnabledState.value = AccessibilityStatus.isEnabled(this)
    }

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
                    val installedApps = remember {
                        AppCatalog.launchableApps(this@MainActivity)
                    }
                    val protectedPackages = remember {
                        mutableStateListOf<String>().apply {
                            addAll(GuardPrefs.protectedPackages(this@MainActivity).sorted())
                        }
                    }
                    var unlockMinutes by remember {
                        mutableStateOf(GuardPrefs.unlockMinutes(this@MainActivity))
                    }
                    var saved by remember { mutableStateOf(false) }

                    val currentQuranSelection =
                        if (mode == QuranSelectionMode.JUZ) selectedJuz else selectedHizb
                    val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
                    val unitLabel = if (mode == QuranSelectionMode.JUZ) "Juz" else "Hizb"
                    val durationChoices = listOf(1, 5, 10, 15, 30, 60, 120)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        Text("Quran Unlock", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Réglages de protection")
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (serviceEnabledState.value) {
                                "Protection active ✓"
                            } else {
                                "Protection inactive — active le service d’accessibilité"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tous les navigateurs HTTP/HTTPS détectés sont protégés automatiquement, " +
                                "même s’ils ne figurent pas dans la liste ci-dessous."
                        )
                        Spacer(Modifier.height(24.dp))

                        Text("1. Pages de lecture", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Choisis la zone du Coran dans laquelle les pages obligatoires seront tirées.")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Rotation anti-triche : les pages récemment attribuées sont évitées autant que possible " +
                                "(jusqu’aux 30 dernières), avec adaptation automatique pour les petites sélections.",
                            style = MaterialTheme.typography.bodySmall
                        )

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

                        Spacer(Modifier.height(8.dp))
                        (1..maxUnit).chunked(3).forEach { rowUnits ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowUnits.forEach { unit ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = unit in currentQuranSelection,
                                            onCheckedChange = { checked ->
                                                saved = false
                                                if (checked) {
                                                    if (unit !in currentQuranSelection) currentQuranSelection.add(unit)
                                                } else if (currentQuranSelection.size > 1) {
                                                    currentQuranSelection.remove(unit)
                                                }
                                            }
                                        )
                                        Text("$unitLabel $unit")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(24.dp))

                        Text("2. Applications protégées", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Coche les applications qui devront demander une page. " +
                                "Paramètres Android reste toujours protégé. Google Play et Quran restent toujours autorisés."
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    protectedPackages.clear()
                                    protectedPackages.addAll(installedApps.map { it.packageName })
                                    saved = false
                                }
                            ) { Text("Tout protéger") }

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    protectedPackages.clear()
                                    saved = false
                                }
                            ) { Text("Tout décocher") }
                        }

                        Spacer(Modifier.height(8.dp))
                        installedApps.forEach { app ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = app.packageName in protectedPackages,
                                    onCheckedChange = { checked ->
                                        saved = false
                                        if (checked) {
                                            if (app.packageName !in protectedPackages) {
                                                protectedPackages.add(app.packageName)
                                            }
                                        } else {
                                            protectedPackages.remove(app.packageName)
                                        }
                                    }
                                )
                                Column {
                                    Text(app.label)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(24.dp))

                        Text("3. Récurrence", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Après avoir lu une page, l’application choisie reste accessible pendant cette durée. " +
                                "À l’expiration, une nouvelle page sera demandée."
                        )

                        durationChoices.forEach { minutes ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = unlockMinutes == minutes,
                                    onClick = {
                                        unlockMinutes = minutes
                                        saved = false
                                    }
                                )
                                Text(
                                    when (minutes) {
                                        1 -> "1 minute — très strict"
                                        60 -> "1 heure"
                                        120 -> "2 heures"
                                        else -> "$minutes minutes"
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(24.dp))

                        Text("4. Jokers", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "3 jokers sont disponibles chaque jour. Un joker évite la lecture d’une page " +
                                "et ouvre l’application pour la durée de récurrence choisie. " +
                                "Jokers restants aujourd’hui : " +
                                GuardPrefs.remainingJokers(this@MainActivity) +
                                "/3."
                        )

                        Spacer(Modifier.height(24.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                GuardPrefs.saveSelectionMode(this@MainActivity, mode)
                                when (mode) {
                                    QuranSelectionMode.JUZ ->
                                        GuardPrefs.saveSelectedJuz(this@MainActivity, selectedJuz.toSet())
                                    QuranSelectionMode.HIZB ->
                                        GuardPrefs.saveSelectedHizb(this@MainActivity, selectedHizb.toSet())
                                }
                                GuardPrefs.saveProtectedPackages(
                                    this@MainActivity,
                                    protectedPackages.toSet()
                                )
                                GuardPrefs.saveUnlockMinutes(this@MainActivity, unlockMinutes)
                                BrowserDetector.refresh()
                                saved = true
                            }
                        ) {
                            Text(if (saved) "Réglages enregistrés ✓" else "Enregistrer les réglages")
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        ) {
                            Text(
                                if (serviceEnabledState.value) {
                                    "Vérifier le service d’accessibilité"
                                } else {
                                    "Activer la protection"
                                }
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
