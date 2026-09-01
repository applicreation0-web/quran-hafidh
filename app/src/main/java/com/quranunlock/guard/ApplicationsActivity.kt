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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class ApplicationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranSafeguardTheme {
                ApplicationsScreen()
            }
        }
    }

    @Composable
    private fun ApplicationsScreen() {
        val selected = remember {
            mutableStateListOf<String>().apply {
                addAll(GuardPrefs.protectedPackages(this@ApplicationsActivity).sorted())
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Applications protégées",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Safeguard est volontairement limité aux réseaux sociaux et aux huit navigateurs pris en charge.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Hors scope",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Banque, paiement, identité, authentification, mots de passe, sécurité, appels et alarmes ne sont jamais des cibles Safeguard.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Paramètres Android reste protégé uniquement contre le contournement du service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selected.clear()
                            selected.addAll(
                                ProtectedApps.selectableScopePackages.sorted()
                            )
                            GuardPrefs.saveProtectedPackages(
                                this@ApplicationsActivity,
                                selected.toSet()
                            )
                        }
                    ) { Text("Tout activer") }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selected.clear()
                            GuardPrefs.saveProtectedPackages(
                                this@ApplicationsActivity,
                                emptySet()
                            )
                        }
                    ) { Text("Tout désactiver") }
                }

                TargetGroup(
                    title = "Réseaux sociaux",
                    targets = ProtectedApps.socialTargets,
                    selected = selected,
                    onSave = {
                        GuardPrefs.saveProtectedPackages(
                            this@ApplicationsActivity,
                            selected.toSet()
                        )
                    }
                )

                TargetGroup(
                    title = "Navigateurs",
                    targets = ProtectedApps.browserTargets,
                    selected = selected,
                    onSave = {
                        GuardPrefs.saveProtectedPackages(
                            this@ApplicationsActivity,
                            selected.toSet()
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TargetGroup(
    title: String,
    targets: List<SafeguardTarget>,
    selected: MutableList<String>,
    onSave: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            targets.forEach { target ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = target.packageName in selected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (target.packageName !in selected) {
                                    selected.add(target.packageName)
                                }
                            } else {
                                selected.remove(target.packageName)
                            }
                            onSave()
                        }
                    )
                    Column {
                        Text(target.label)
                        Text(
                            target.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
