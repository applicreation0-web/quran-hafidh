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

class HikamDetailActivity : ComponentActivity() {
    companion object { const val EXTRA_HIKMA_ID = "hikma_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hikma = HikamRepository.byId(intent.getStringExtra(EXTRA_HIKMA_ID).orEmpty())
        if (hikma == null) {
            finish()
            return
        }
        setContent { QuranSafeguardTheme { HikmaDetailScreen(hikma) } }
    }
}

@Composable
private fun HikmaDetailScreen(hikma: HikmaEntry) {
    var showCommentary by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
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
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Ibn ʿAṭāʾ Allāh al-Iskandarī",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        hikma.arabicText,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = TextDirection.Rtl
                        ),
                        textAlign = TextAlign.Right
                    )
                    Text(
                        hikma.frenchText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Source vérifiée",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Authenticité documentaire : texte et attribution vérifiés.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Traduction française : interne Quran Safeguard, relue contre l’arabe ; pas de certification éditoriale externe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val commentary = hikma.commentary
            if (commentary != null && commentary.displayEligible) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showCommentary = !showCommentary }
                ) {
                    Text(
                        if (showCommentary) "Masquer le commentaire classique"
                        else "Approfondir — commentaire classique"
                    )
                }

                if (showCommentary) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Commentaire classique — Ibn ʿAjība",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Texte du commentateur présenté sans reformulation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Commentaire arabe",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                commentary.arabicText,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                                textAlign = TextAlign.Right
                            )
                            Text(
                                "Traduction française",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(commentary.frenchText)
                            if (commentary.isExcerpt) {
                                Text(
                                    "Extrait — suite dans la source",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HikamSourceBlock(commentary.source)
                        }
                    }
                }
            }

            HikamSourceBlock(hikma.source)
        }
    }
}

@Composable
private fun HikamSourceBlock(source: ClassicalSource) {
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
