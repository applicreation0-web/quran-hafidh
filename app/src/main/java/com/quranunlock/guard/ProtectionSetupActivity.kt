package com.applicreation0.quransafeguard

import android.content.Intent
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class ProtectionSetupActivity : ComponentActivity() {
    private val serviceEnabledState = mutableStateOf(false)
    private var systemSettingsOpened = false
    private var returnHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        systemSettingsOpened = savedInstanceState?.getBoolean(KEY_SETTINGS_OPENED) ?: false
        setContent {
            QuranSafeguardTheme {
                ProtectionSetupScreen(
                    serviceEnabled = serviceEnabledState.value,
                    onOpenAndroid = ::openAndroidAccessibility,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = AccessibilityStatus.isEnabled(this)
        serviceEnabledState.value = enabled

        if (systemSettingsOpened && enabled && !returnHandled) {
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
        super.onSaveInstanceState(outState)
    }

    private fun openAndroidAccessibility() {
        systemSettingsOpened = true
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "Android ne permet pas d’ouvrir cet écran sur ce téléphone.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val KEY_SETTINGS_OPENED = "settings_opened"
    }
}

@androidx.compose.runtime.Composable
private fun ProtectionSetupScreen(
    serviceEnabled: Boolean,
    onOpenAndroid: () -> Unit,
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
                if (serviceEnabled) "Safeguard est actif" else "Activer en trois étapes",
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
