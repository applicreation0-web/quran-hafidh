package com.applicreation0.quransafeguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

class GhazaliDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ID = "ghazali_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entry = GhazaliRepository.byId(intent.getStringExtra(EXTRA_ID).orEmpty())
        if (entry == null) {
            finish()
            return
        }
        setContent {
            QuranSafeguardTheme {
                GhazaliDetailScreen(entry)
            }
        }
    }
}

@Composable
private fun GhazaliDetailScreen(entry: GhazaliEntry) {
    var showContext by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Texte d’al-Ghazâlî",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                entry.arabicText,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    textDirection = TextDirection.Rtl
                ),
                textAlign = TextAlign.Right
            )
            Text(entry.frenchText, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Authenticité : source et attribution vérifiées",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val context = entry.context
            if (context != null && context.displayEligible) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showContext = !showContext }
                ) {
                    Text(
                        if (showContext) {
                            "Masquer le contexte"
                        } else {
                            "Approfondir — contexte dans l’œuvre"
                        }
                    )
                }

                if (showContext) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "TEXTE ARABE ORIGINAL",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                context.arabicText,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Rtl
                                ),
                                textAlign = TextAlign.Right
                            )
                            Text(
                                "TRADUCTION FRANÇAISE",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(context.frenchText)
                            if (context.isExcerpt) {
                                Text(
                                    "Extrait — suite dans la source",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ClassicalSourceBlock(context.source)
                        }
                    }
                }
            }

            ClassicalSourceBlock(entry.source)
        }
    }
}

@Composable
private fun ClassicalSourceBlock(source: ClassicalSource) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("SOURCE", fontWeight = FontWeight.SemiBold)
            Text(source.author + " • " + source.workTitle)
            source.volume?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(source.edition, style = MaterialTheme.typography.bodySmall)
            source.editor?.let {
                Text("Éditeur/établissement du texte : $it", style = MaterialTheme.typography.bodySmall)
            }
            Text(source.locator, style = MaterialTheme.typography.bodySmall)
            source.translator?.let {
                Text("Traducteur : $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
