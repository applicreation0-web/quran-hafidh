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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
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
        val sharhAvailability = if (HikamSharhEdition.isEnabled) {
            HikamSharhEdition.forHikma(this, hikma.sourceNumber)
        } else {
            emptyList()
        }

        setContent {
            QuranSafeguardTheme {
                HikmaDetailScreen(
                    hikma = hikma,
                    sharhAvailability = sharhAvailability
                )
            }
        }
    }
}

@Composable
private fun HikmaDetailScreen(
    hikma: HikmaEntry,
    sharhAvailability: List<HikamSharhAvailability>
) {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "ḤIKMA — TEXTE ARABE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
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
                    HorizontalDivider()
                    Text(
                        "TRADUCTION FRANÇAISE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        hikma.frenchText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 25.sp
                    )
                }
            }

            HikamTerminologyBlock(hikma)

            if (HikamSharhEdition.isEnabled && sharhAvailability.any { it.available }) {
                DualSharhBlock(sharhAvailability)
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
                        "L’arabe reste l’autorité. La traduction anglaise et son glossaire " +
                            "servent uniquement de contrôle terminologique et de sens.",
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
private fun HikamTerminologyBlock(hikma: HikmaEntry) {
    val terms = remember(hikma.canonicalId) {
        HikamTechnicalLexicon.forHikma(hikma)
    }
    if (terms.isEmpty()) return

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                "AIDE TERMINOLOGIQUE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Repères éditoriaux séparés du texte de la Ḥikma et du commentaire classique.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            terms.forEach { term ->
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("[")
                            append(term.transliteration)
                            append("]")
                        }
                        append(" — ")
                        append(term.frenchMeaning)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
private fun DualSharhBlock(
    availability: List<HikamSharhAvailability>
) {
    val sharnubi = availability.firstOrNull {
        it.commentator == HikamCommentator.SHARNUBI
    } ?: HikamSharhAvailability(HikamCommentator.SHARNUBI, null)
    val ibnAbbad = availability.firstOrNull {
        it.commentator == HikamCommentator.IBN_ABBAD
    } ?: HikamSharhAvailability(HikamCommentator.IBN_ABBAD, null)

    var selected by remember(availability) {
        mutableStateOf(
            when {
                sharnubi.available -> HikamCommentator.SHARNUBI
                ibnAbbad.available -> HikamCommentator.IBN_ABBAD
                else -> HikamCommentator.SHARNUBI
            }
        )
    }

    val selectedAvailability = when (selected) {
        HikamCommentator.SHARNUBI -> sharnubi
        HikamCommentator.IBN_ABBAD -> ibnAbbad
    }

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
                "COMMENTAIRE CLASSIQUE — SHARḤ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choisir le commentateur. Aucun commentaire n’est fusionné avec la Ḥikma.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selected == HikamCommentator.SHARNUBI && sharnubi.available) {
                    SafeguardButton(
                        modifier = Modifier.weight(1f),
                        onClick = { selected = HikamCommentator.SHARNUBI }
                    ) {
                        Text("al-Sharnūbī")
                    }
                } else {
                    SafeguardOutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = sharnubi.available,
                        onClick = { selected = HikamCommentator.SHARNUBI }
                    ) {
                        Text("al-Sharnūbī")
                    }
                }

                if (selected == HikamCommentator.IBN_ABBAD && ibnAbbad.available) {
                    SafeguardButton(
                        modifier = Modifier.weight(1f),
                        onClick = { selected = HikamCommentator.IBN_ABBAD }
                    ) {
                        Text("Ibn ʿAbbād")
                    }
                } else {
                    SafeguardOutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = ibnAbbad.available,
                        onClick = { selected = HikamCommentator.IBN_ABBAD }
                    ) {
                        Text("Ibn ʿAbbād")
                    }
                }
            }

            selectedAvailability.entry?.takeIf(HikamSharhEntry::displayEligible)?.let { entry ->
                Text(
                    entry.commentator.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    entry.workTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    richSharhText(
                        text = entry.arabicText,
                        spans = entry.richSpansArabic,
                        accent = MaterialTheme.colorScheme.secondary,
                        quoteColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = TextDirection.Rtl,
                        lineHeight = 31.sp
                    ),
                    textAlign = TextAlign.Right
                )
                Text(
                    richSharhText(
                        text = entry.frenchText,
                        spans = entry.richSpansFrench,
                        accent = MaterialTheme.colorScheme.secondary,
                        quoteColor = MaterialTheme.colorScheme.primary
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 25.sp
                )
                Text(
                    entry.printLocator,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.technicalTerms.isNotEmpty()) {
                    Text(
                        "Lexique du commentaire : " +
                            entry.technicalTerms.sorted().joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } ?: Text(
                "Ce sharḥ n’est pas encore marqué comme vérifié pour cette Hikma. " +
                    "Aucun texte de remplacement n’est généré.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

private fun richSharhText(
    text: String,
    spans: List<HikamRichSpan>,
    accent: Color,
    quoteColor: Color
): AnnotatedString = buildAnnotatedString {
    append(text)
    spans.forEach { span ->
        if (span.start < 0 ||
            span.endExclusive <= span.start ||
            span.endExclusive > text.length
        ) {
            return@forEach
        }
        val style = when (span.role) {
            HikamRichRole.TECHNICAL_TERM -> SpanStyle(
                fontWeight = FontWeight.Bold,
                color = accent
            )
            HikamRichRole.AUTHOR_EMPHASIS -> SpanStyle(
                fontWeight = FontWeight.Bold
            )
            HikamRichRole.COMMENTATOR_EMPHASIS -> SpanStyle(
                fontStyle = FontStyle.Italic
            )
            HikamRichRole.QURAN_QUOTE,
            HikamRichRole.HADITH_QUOTE -> SpanStyle(
                fontWeight = FontWeight.SemiBold,
                color = quoteColor
            )
            HikamRichRole.EDITORIAL_BRACKET -> SpanStyle(
                fontStyle = FontStyle.Italic,
                color = accent
            )
            HikamRichRole.SOURCE_NOTE -> SpanStyle(
                fontStyle = FontStyle.Italic
            )
        }
        addStyle(style, span.start, span.endExclusive)
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
