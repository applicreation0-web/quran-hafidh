package com.quranunlock.guard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Quran Unlock", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Les applications distrayantes sont interceptées par le service d’accessibilité. " +
                                "Google Play reste toujours autorisé."
                        )
                        Spacer(Modifier.height(24.dp))
                        Text("Protection initiale : YouTube, WhatsApp, réseaux sociaux, navigateurs et Paramètres Android.")
                        Spacer(Modifier.height(24.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        ) {
                            Text("Activer la protection")
                        }
                    }
                }
            }
        }
    }
}
