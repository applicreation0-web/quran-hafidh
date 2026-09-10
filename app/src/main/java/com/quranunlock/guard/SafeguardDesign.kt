package com.applicreation0.quransafeguard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private val baseTypography = Typography()

/**
 * Quran Safeguard visual system.
 * Warm ivory + deep green are the base; restrained gold/brown accents give the
 * controls an oriental/sub-Saharan character without reducing readability.
 */
internal val SafeguardAppBackground = Color(0xFFF8F3E8)
internal val SafeguardSurface = Color(0xFFFFFCF6)
internal val SafeguardReadingSurface = Color(0xFFF7F2E8)
internal val SafeguardDeepGreen = Color(0xFF1D5B47)
internal val SafeguardTextGreen = Color(0xFF21382F)
internal val SafeguardSecondaryText = Color(0xFF62665F)
internal val SafeguardGold = Color(0xFFB48A3C)
internal val SafeguardBrown = Color(0xFF76563C)
internal val SafeguardSoftGreen = Color(0xFFE7F0EA)
internal val SafeguardSoftGold = Color(0xFFF3E8CE)

// Slightly asymmetric corners echo carved/architectural contours while
// remaining sober and predictable for touch targets.
internal val SafeguardShapes = Shapes(
    small = RoundedCornerShape(
        topStart = 14.dp,
        topEnd = 8.dp,
        bottomEnd = 14.dp,
        bottomStart = 8.dp
    ),
    medium = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 10.dp,
        bottomEnd = 20.dp,
        bottomStart = 10.dp
    ),
    large = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 14.dp,
        bottomEnd = 28.dp,
        bottomStart = 14.dp
    )
)

internal val SafeguardTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = FontFamily.Serif),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = FontFamily.Serif),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = FontFamily.Serif)
)

@Composable
internal fun SafeguardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SafeguardShapes.medium,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(
            1.dp,
            if (enabled) SafeguardGold.copy(alpha = 0.92f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        ),
        content = content
    )
}

@Composable
internal fun SafeguardOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SafeguardShapes.medium,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(
            1.dp,
            if (enabled) SafeguardBrown.copy(alpha = 0.68f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        ),
        content = content
    )
}

@Composable
internal fun SafeguardProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(SafeguardSoftGold)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
