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

/** Final 0.10.10 identity: warm cream, black ink and neutral grayscale only. */
internal val SafeguardAppBackground = Color(0xFFFBF7EF)
internal val SafeguardSurface = Color(0xFFFFFDF8)
internal val SafeguardReadingSurface = Color(0xFFF7F2E8)
internal val SafeguardInk = Color(0xFF171715)
internal val SafeguardSecondaryText = Color(0xFF5C5C58)
internal val SafeguardNeutralContainer = Color(0xFFEDEAE3)
internal val SafeguardOutline = Color(0xFFAAA9A4)

// Compatibility aliases for older call sites. They are deliberately monochrome and
// must never reintroduce the historical green/gold palette.
@Deprecated("Use SafeguardInk")
internal val SafeguardDeepGreen = SafeguardInk
@Deprecated("Use SafeguardInk")
internal val SafeguardTextGreen = SafeguardInk
@Deprecated("Use SafeguardInk")
internal val SafeguardGold = SafeguardInk

internal val SafeguardShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
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
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.50f else 0.24f)
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
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.48f else 0.22f)
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
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
