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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

class SensitiveAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProtectedApps.clearClassificationCache()
        setContent {
            QuranSafeguardTheme {
                SensitiveAppsScreen()
            }
        }
    }

    @Composable
    private fun SensitiveAppsScreen() {
        val apps = remember { ProtectedApps.launchableApps(this@SensitiveAppsActivity) }
        val userAllowed = remember {
            mutableStateListOf<String>().apply {
                addAll(GuardPrefs.userAlwaysAllowedPackages(this@SensitiveAppsActivity))
            }
        }
        var search by remember { mutableStateOf("") }
        val visibleApps = remember(apps, search) {
            val needle = search.trim()
            if (needle.isBlank()) apps else apps.filter {
                it.label.contains(needle, ignoreCase = true)
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
                    "TOUJOURS ACCESSIBLES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Banques, paiements et identité",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Ces applications ne déclenchent jamais la lecture. Safeguard exclut automatiquement les applications sensibles reconnues ; vous pouvez ajouter ici une banque locale non reconnue.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SafeguardShapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "Garantie de sécurité",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Une application cochée ici reste toujours accessible, même si elle avait été sélectionnée auparavant. Les passages nécessaires par Android ou par une page d’authentification sont autorisés temporairement.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "N’ajoutez que des applications de banque, paiement, identité, mots de passe, 2FA, sécurité ou VPN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Rechercher une application") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                if (visibleApps.isEmpty()) {
                    Text(
                        "Aucune application correspondante.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SafeguardShapes.large,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            visibleApps.forEachIndexed { index, app ->
                                val checked = app.automaticallySensitive ||
                                    app.packageName in userAllowed
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            app.label,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            if (app.automaticallySensitive) {
                                                "Exclue automatiquement"
                                            } else if (app.packageName in userAllowed) {
                                                "Toujours accessible"
                                            } else {
                                                "Protection normale"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (checked) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    Checkbox(
                                        checked = checked,
                                        enabled = !app.automaticallySensitive,
                                        onCheckedChange = { allow ->
                                            if (allow) {
                                                if (app.packageName !in userAllowed) {
                                                    userAllowed.add(app.packageName)
                                                }
                                            } else {
                                                userAllowed.remove(app.packageName)
                                            }
                                            GuardPrefs.saveUserAlwaysAllowedPackages(
                                                this@SensitiveAppsActivity,
                                                userAllowed.toSet()
                                            )
                                        }
                                    )
                                }
                                if (index < visibleApps.lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                SafeguardButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { finish() }
                ) {
                    Text("Terminer")
                }
            }
        }
    }
}
