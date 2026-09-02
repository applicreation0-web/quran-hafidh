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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.unit.sp

class HikamDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_HIKMA_ID = "hikma_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hikma = HikamRepository.byId(
            this,
            intent.getStringExtra(EXTRA_HIKMA_ID).orEmpty()
        )
        if (hikma == null) {
            finish()
            return
        }
        setContent {
            QuranSafeguardTheme {
                HikmaDetailScreen(hikma)
            }
        }
    }
}

@Composable
private fun HikmaDetailScreen(hikma: HikmaEntry) {
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
                "AL-HIKAM AL-ʿAṬĀʾIYYA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Hikma " + hikma.sourceNumber,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Ibn ʿAṭāʾ Allāh al-Iskandarī",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 19.dp,
                        vertical = 22.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        hikma.arabicText,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = TextDirection.Rtl,
                            lineHeight = 39.sp
                        ),
                        textAlign = TextAlign.Right,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        hikma.frenchText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 25.sp
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "TEXTE ET SOURCE VÉRIFIÉS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        hikma.source.locator +
                            " • traduction interne Quran Safeguard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Le tashkīl provient d’une édition vocalisée. " +
                            "Aucun signe n’est généré automatiquement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HikamSourceBlock(
                source = hikma.source,
                hasVocalizationSource =
                    !hikma.vocalizationSourceUrl.isNullOrBlank()
            )
        }
    }
}

@Composable
private fun HikamSourceBlock(
    source: ClassicalSource,
    hasVocalizationSource: Boolean
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "SOURCE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                source.author + " • " + source.workTitle,
                fontWeight = FontWeight.SemiBold
            )
            source.volume?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                source.edition,
                style = MaterialTheme.typography.bodySmall
            )
            source.editor?.let {
                Text(
                    "Éditeur/établissement du texte : " + it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                source.locator,
                style = MaterialTheme.typography.bodySmall
            )
            if (hasVocalizationSource) {
                Text(
                    "Contrôle vocalisé : édition arabe Al-Ḥikam, " +
                        "The Matheson Trust, v. 1.0 (2014).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            source.translator?.let {
                Text(
                    "Traducteur : " + it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
