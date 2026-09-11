package com.applicreation0.quransafeguard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class ProtectionSetupActivity : ComponentActivity() {
    companion object {
        private const val KEY_SETTINGS_OPENED = "settings_opened"
        private const val KEY_RESTRICTED_HELP = "restricted_help"
    }
    private val serviceEnabledState = mutableStateOf(false)
    private val restrictedHelpState = mutableStateOf(false)
    private var systemSettingsOpened = false
    private var returnHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        systemSettingsOpened = savedInstanceState?.getBoolean(KEY_SETTINGS_OPENED) ?: false
        restrictedHelpState.value =
            savedInstanceState?.getBoolean(KEY_RESTRICTED_HELP) ?: false
        setContent {
            QuranSafeguardTheme {
                ProtectionSetupScreen(
                    serviceEnabled = serviceEnabledState.value,
                    showRestrictedHelp = restrictedHelpState.value,
                    showRestrictedSettingsPreparation =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    onOpenAndroid = ::openAndroidAccessibility,
                    onOpenAppDetails = ::openAppDetails,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = AccessibilityStatus.isEnabled(this)
        serviceEnabledState.value = enabled

        if (systemSettingsOpened && !enabled) {
            restrictedHelpState.value = true
        }

        if (systemSettingsOpened && enabled && !returnHandled) {
            restrictedHelpState.value = false
            returnHandled = true
            Toast.makeText(this, "Protection activée", Toast.LENGTH_SHORT).show()
            window.decorView.postDelayed(
                {
                    if (!isFinishing) finish()
                },
                500L
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_SETTINGS_OPENED, systemSettingsOpened)
        outState.putBoolean(KEY_RESTRICTED_HELP, restrictedHelpState.value)
        super.onSaveInstanceState(outState)
    }

    private fun openAndroidAccessibility() {
        systemSettingsOpened = true
        restrictedHelpState.value = false
        openSystemScreen(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            "Android ne permet pas d’ouvrir les réglages d’accessibilité sur ce téléphone."
        )
    }

    private fun openAppDetails() {
        restrictedHelpState.value = true
        openSystemScreen(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            ),
            "Android ne permet pas d’ouvrir les informations de l’application."
        )
    }

    private fun openSystemScreen(intent: Intent, failureMessage: String) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, failureMessage, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, failureMessage, Toast.LENGTH_LONG).show()
        }
    }

}

@androidx.compose.runtime.Composable
private fun ProtectionSetupScreen(
    serviceEnabled: Boolean,
    showRestrictedHelp: Boolean,
    showRestrictedSettingsPreparation: Boolean,
    onOpenAndroid: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onClose: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "PROTECTION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (serviceEnabled) "Safeguard est actif" else "Activation guidée",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (serviceEnabled) {
                    "La protection fonctionne. Tous les autres réglages restent dans Safeguard."
                } else {
                    "Android demande cette confirmation système une seule fois. Cela prend généralement moins d’une minute."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = SafeguardShapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (serviceEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    if (serviceEnabled) "Protection active ✓" else "Protection en attente",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    textAlign = TextAlign.Center,
                    color = if (serviceEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!serviceEnabled) {
                if (showRestrictedSettingsPreparation && !showRestrictedHelp) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SafeguardShapes.medium,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Avant l’activation sur Android 13 ou plus",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Comme Quran Safeguard est une APK privée, Android peut griser l’interrupteur. Ouvrez les informations de l’application, touchez ⋮ puis « Autoriser les paramètres restreints ». Revenez ensuite ici.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SafeguardOutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onOpenAppDetails
                            ) {
                                Text("Préparer l’autorisation Android")
                            }
                        }
                    }
                }
                ActivationStep(
                    number = "1",
                    title = "Repérez Quran Safeguard",
                    detail = "Dans la liste Accessibilité d’Android, touchez « Quran Safeguard »."
                )
                ActivationStep(
                    number = "2",
                    title = "Activez le service",
                    detail = "Activez « Utiliser Quran Safeguard », puis confirmez la demande Android."
                )
                ActivationStep(
                    number = "3",
                    title = "Revenez avec la flèche Retour",
                    detail = "Safeguard vérifiera l’activation et vous ramènera automatiquement dans l’application."
                )
            }

            if (!serviceEnabled && showRestrictedHelp) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SafeguardShapes.medium,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Android a bloqué l’activation ?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Si « Paramètre restreint » apparaît, ouvrez les informations de l’application, touchez le menu ⋮, puis « Autoriser les paramètres restreints ». Revenez ensuite ici.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenAppDetails
                        ) {
                            Text("Ouvrir les informations de l’application")
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            SafeguardButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = if (serviceEnabled) onClose else onOpenAndroid
            ) {
                Text(if (serviceEnabled) "Terminer" else "Ouvrir Accessibilité Android")
            }
            if (!serviceEnabled) {
                SafeguardOutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClose
                ) {
                    Text("Pas maintenant")
                }
            }
            Text(
                "Safeguard ne lit pas le contenu de vos écrans et n’enregistre pas vos saisies.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun ActivationStep(
    number: String,
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
