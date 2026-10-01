package com.applicreation0.quransafeguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp

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
    var contextArabic by remember { mutableStateOf(true) }

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
                "AL-GHAZÂLÎ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                entry.source.workTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                entry.source.locator,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 19.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(17.dp)
                ) {
                    Text(
                        entry.arabicText,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = TextDirection.Rtl,
                            lineHeight = 39.sp
                        ),
                        textAlign = TextAlign.Right
                    )
                    HorizontalDivider()
                    Text(
                        entry.frenchText,
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
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "Source et attribution vérifiées",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Traduction interne Quran Safeguard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val context = entry.context
            if (context != null && context.displayEligible) {
                SafeguardButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(16.dp),
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
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Contexte dans l’œuvre",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Passage de l’auteur présenté sans reformulation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (contextArabic) {
                                    SafeguardButton(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        onClick = { contextArabic = true }
                                    ) { Text("Arabe") }
                                    SafeguardOutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        onClick = { contextArabic = false }
                                    ) { Text("Français") }
                                } else {
                                    SafeguardOutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        onClick = { contextArabic = true }
                                    ) { Text("Arabe") }
                                    SafeguardButton(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        onClick = { contextArabic = false }
                                    ) { Text("Français") }
                                }
                            }

                            if (contextArabic) {
                                Text(
                                    context.arabicText,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.Rtl,
                                        lineHeight = 30.sp
                                    ),
                                    textAlign = TextAlign.Right
                                )
                            } else {
                                Text(
                                    context.frenchText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 25.sp
                                )
                            }
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
            source.volume?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text(source.edition, style = MaterialTheme.typography.bodySmall)
            source.editor?.let {
                Text(
                    "Éditeur/établissement du texte : " + it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(source.locator, style = MaterialTheme.typography.bodySmall)
            source.translator?.let {
                Text("Traducteur : " + it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
