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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

class AdhkarActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PERIOD = "period"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val period = runCatching {
            AdhkarPeriod.valueOf(intent.getStringExtra(EXTRA_PERIOD).orEmpty())
        }.getOrDefault(AdhkarPeriod.MORNING)

        setContent {
            QuranSafeguardTheme {
                AdhkarScreen(period)
            }
        }
    }
}

@Composable
private fun AdhkarScreen(period: AdhkarPeriod) {
    val items = AuthenticAdhkarLibrary.forPeriod(period)
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (period == AdhkarPeriod.MORNING) "Adhkâr du matin 🌿" else "Adhkâr du soir 🌿",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (period == AdhkarPeriod.MORNING) {
                    "Fenêtre recommandée : de Fajr au lever du soleil."
                } else {
                    "Fenêtre recommandée : de ‘Asr à Maghrib."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Uniquement des formules dont la source et le degré ont été vérifiés. Pas de translittération affichée par défaut.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            items.forEach { item ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            if (item.repeatCount > 1) "À réciter ×" + item.repeatCount else "À réciter",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            item.arabicText,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                textDirection = TextDirection.Rtl
                            ),
                            textAlign = TextAlign.Right
                        )
                        Text(item.frenchText)
                        Text(
                            item.source + " • " + item.authenticity,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
