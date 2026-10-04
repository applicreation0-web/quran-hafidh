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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

class ApplicationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCatalog.refresh()
        setContent {
            QuranSafeguardTheme {
                ApplicationsScreen()
            }
        }
    }

    @Composable
    private fun ApplicationsScreen() {
        val installedPackages = remember {
            AppCatalog.launchableApps(this@ApplicationsActivity)
                .map(InstalledApp::packageName)
                .toSet()
        }
        val installedTargets = remember(installedPackages) {
            ProtectedApps.selectableTargets.filter { it.packageName in installedPackages }
        }
        val initialSelection = remember {
            val active = GuardPrefs.protectedPackages(this@ApplicationsActivity)
            val pending = GuardPrefs.pendingProtectedRemovals(this@ApplicationsActivity)
            active to pending
        }
        val selected = remember {
            mutableStateListOf<String>().apply {
                addAll((initialSelection.first - initialSelection.second).sorted())
            }
        }
        val pendingRemovals = remember {
            mutableStateListOf<String>().apply {
                addAll(initialSelection.second.sorted())
            }
        }
        var removalRequest by remember {
            mutableStateOf<Set<String>>(emptySet())
        }

        fun reloadSelection() {
            val active = GuardPrefs.protectedPackages(this@ApplicationsActivity)
            val pending = GuardPrefs.pendingProtectedRemovals(this@ApplicationsActivity)
            selected.clear()
            selected.addAll((active - pending).sorted())
            pendingRemovals.clear()
            pendingRemovals.addAll(pending.sorted())
        }

        if (removalRequest.isNotEmpty()) {
            val labels = installedTargets
                .filter { it.packageName in removalRequest }
                .map(SafeguardTarget::label)
            AlertDialog(
                onDismissRequest = { removalRequest = emptySet() },
                title = {
                    Text(
                        if (removalRequest.size == 1) {
                            "Retirer cette application demain ?"
                        } else {
                            "Retirer ces applications demain ?"
                        }
                    )
                },
                text = {
                    Text(
                        buildString {
                            if (labels.isNotEmpty()) {
                                append(labels.joinToString(", "))
                                append(". ")
                            }
                            append(
                                "La protection reste active aujourd’hui. " +
                                    "Le retrait prendra effet au prochain jour et " +
                                    "vous pourrez l’annuler d’ici là en recochant."
                            )
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selected.removeAll(removalRequest)
                            GuardPrefs.saveProtectedPackages(
                                this@ApplicationsActivity,
                                selected.toSet()
                            )
                            removalRequest = emptySet()
                            reloadSelection()
                        }
                    ) {
                        Text("Confirmer pour demain")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { removalRequest = emptySet() }) {
                        Text("Garder la protection")
                    }
                }
            )
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
                    "Choisissez uniquement les réseaux sociaux et navigateurs où Quran Safeguard intervient.",
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
                                (selected.size + pendingRemovals.size).toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "applications protégées aujourd’hui",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (pendingRemovals.isNotEmpty()) {
                                pendingRemovals.size.toString() +
                                    if (pendingRemovals.size == 1) {
                                        " retrait demain"
                                    } else {
                                        " retraits demain"
                                    }
                            } else {
                                (installedTargets.size - selected.size)
                                    .coerceAtLeast(0)
                                    .toString() + " non sélectionnées"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SafeguardOutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            selected.clear()
                            selected.addAll(installedTargets.map(SafeguardTarget::packageName).sorted())
                            GuardPrefs.saveProtectedPackages(
                                this@ApplicationsActivity,
                                selected.toSet()
                            )
                            reloadSelection()
                        }
                    ) { Text("Tout activer") }

                    SafeguardOutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            removalRequest = (selected + pendingRemovals).toSet()
                        }
                    ) { Text("Tout désactiver") }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SafeguardShapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        "Une application ajoutée est protégée immédiatement. Un retrait est confirmé puis appliqué le lendemain, avec annulation possible. Désactiver Safeguard dans Android ou désinstaller l’application reste toujours libre.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TargetGroup(
                    title = "Réseaux sociaux",
                    subtitle = "WhatsApp texte, X, Instagram, Facebook, YouTube et TikTok",
                    targets = installedTargets.filter {
                        it.category == SafeguardTargetCategory.SOCIAL
                    },
                    selected = selected,
                    pendingRemovals = pendingRemovals.toSet(),
                    onToggle = { target, checked ->
                        if (checked) {
                            if (target.packageName !in selected) {
                                selected.add(target.packageName)
                            }
                            GuardPrefs.saveProtectedPackages(
                                this@ApplicationsActivity,
                                selected.toSet()
                            )
                            reloadSelection()
                        } else {
                            removalRequest = setOf(target.packageName)
                        }
                    }
                )

                TargetGroup(
                    title = "Navigateurs",
                    subtitle = "Les huit navigateurs pris en charge",
                    targets = installedTargets.filter {
                        it.category == SafeguardTargetCategory.BROWSER
                    },
                    selected = selected,
                    pendingRemovals = pendingRemovals.toSet(),
                    onToggle = { target, checked ->
                        if (checked) {
                            if (target.packageName !in selected) {
                                selected.add(target.packageName)
                            }
                            GuardPrefs.saveProtectedPackages(
                                this@ApplicationsActivity,
                                selected.toSet()
                            )
                            reloadSelection()
                        } else {
                            removalRequest = setOf(target.packageName)
                        }
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
    pendingRemovals: Set<String>,
    onToggle: (SafeguardTarget, Boolean) -> Unit
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

            if (targets.isEmpty()) {
                Text(
                    "Aucune application installée dans cette catégorie.",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                            when {
                                target.packageName in pendingRemovals ->
                                    "Retrait prévu demain · cochez pour annuler"
                                target.category == SafeguardTargetCategory.BROWSER ->
                                    "Navigateur"
                                else ->
                                    "Réseau / messagerie"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = target.packageName in selected,
                        onCheckedChange = { checked ->
                            onToggle(target, checked)
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
