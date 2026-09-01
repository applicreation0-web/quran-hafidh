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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Applications",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Choisissez les réseaux sociaux et navigateurs où Quran Safeguard intervient.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selected.size.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "applications sélectionnées",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            (ProtectedApps.selectableScopePackages.size - selected.size)
                                .coerceAtLeast(0)
                                .toString() + " non sélectionnées",
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
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            selected.clear()
                            selected.addAll(ProtectedApps.selectableScopePackages.sorted())
                            GuardPrefs.saveProtectedPackages(
                                this@ApplicationsActivity,
                                selected.toSet()
                            )
                        }
                    ) { Text("Tout activer") }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
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
                    subtitle = "Applications sociales et messageries retenues",
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
                    subtitle = "Les huit navigateurs pris en charge",
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
    subtitle: String,
    targets: List<SafeguardTarget>,
    selected: MutableList<String>,
    onSave: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            targets.forEachIndexed { index, target ->
                if (index == 0) {
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            target.label,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (target.category == SafeguardTargetCategory.BROWSER) {
                                "Navigateur"
                            } else {
                                "Réseau / messagerie"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                }
                if (index < targets.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
