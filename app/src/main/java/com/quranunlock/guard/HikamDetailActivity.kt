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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

class HikamDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_HIKMA_ID = "hikma_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hikma = HikamRepository.byId(
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
    val context = LocalContext.current
    var showTransliteration by remember {
        mutableStateOf(HikamPrefs.transliterationEnabled(context))
    }
    var showCommentary by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Hikma " + hikma.sourceNumber,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showTransliteration,
                    onCheckedChange = { enabled ->
                        showTransliteration = enabled
                        HikamPrefs.setTransliterationEnabled(context, enabled)
                    }
                )
                Text(
                    "Translittération",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showTransliteration) {
                Text(
                    hikma.transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            hikma.commentary?.let { commentary ->
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showCommentary = !showCommentary }
                ) {
                    Text(
                        if (showCommentary) {
                            "Masquer le commentaire classique"
                        } else {
                            "Approfondir — commentaire classique"
                        }
                    )
                }

                if (showCommentary) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Commentaire classique — " + commentary.commentator,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Texte du commentateur présenté sans reformulation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                "COMMENTAIRE ARABE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                commentary.arabicExcerpt,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDirection = TextDirection.Rtl
                                ),
                                textAlign = TextAlign.Right
                            )

                            Text(
                                "TRADUCTION FRANÇAISE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(commentary.frenchTranslation)

                            if (commentary.isExcerpt) {
                                Text(
                                    "Extrait — suite dans la source",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                "SOURCE COMPLÈTE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                commentary.commentator + " • " +
                                    commentary.workTitle + "\n" +
                                    commentary.edition + "\n" +
                                    commentary.locator,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Traduction française produite pour Quran Safeguard ; aucune traduction française commerciale moderne n’est reproduite.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Source de la Hikma",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Ibn ʿAṭāʾ Allāh al-Iskandarī • Al-Hikam al-ʿAṭāʾiyya • " +
                            "Hikma " + hikma.sourceNumber,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        hikma.sourceNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
