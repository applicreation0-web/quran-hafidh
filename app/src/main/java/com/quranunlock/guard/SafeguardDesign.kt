package com.applicreation0.quransafeguard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private val baseTypography = Typography()

/** Warm, low-glare surface reserved for sustained reading. */
internal val SafeguardReadingSurface = Color(0xFFF4F0E6)

internal val SafeguardShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
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
    val gold = MaterialTheme.colorScheme.secondary
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .sahelianButtonOrnament(gold, enabled),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, gold.copy(alpha = if (enabled) 0.95f else 0.35f)),
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
    val gold = MaterialTheme.colorScheme.secondary
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .sahelianButtonOrnament(gold, enabled),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, gold.copy(alpha = if (enabled) 0.8f else 0.28f)),
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
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.secondary)
        )
    }
}

private fun Modifier.sahelianButtonOrnament(
    accent: Color,
    enabled: Boolean
): Modifier = drawWithContent {
    drawContent()
    if (!enabled || size.width < 64.dp.toPx() || size.height < 32.dp.toPx()) {
        return@drawWithContent
    }

    val lineWidth = 1.dp.toPx()
    val diamondRadius = 3.dp.toPx()
    val sideInset = 10.dp.toPx()
    val centerY = size.height / 2f

    fun drawDiamond(center: Offset) {
        val path = Path().apply {
            moveTo(center.x, center.y - diamondRadius)
            lineTo(center.x + diamondRadius, center.y)
            lineTo(center.x, center.y + diamondRadius)
            lineTo(center.x - diamondRadius, center.y)
            close()
        }
        drawPath(
            path = path,
            color = accent.copy(alpha = 0.88f),
            style = Stroke(width = lineWidth)
        )
    }

    drawDiamond(Offset(sideInset, centerY))
    drawDiamond(Offset(size.width - sideInset, centerY))

    val chevronHalf = 5.dp.toPx()
    val chevronDepth = 2.5.dp.toPx()
    val centerX = size.width / 2f
    drawLine(
        color = accent.copy(alpha = 0.72f),
        start = Offset(centerX - chevronHalf, lineWidth),
        end = Offset(centerX, lineWidth + chevronDepth),
        strokeWidth = lineWidth
    )
    drawLine(
        color = accent.copy(alpha = 0.72f),
        start = Offset(centerX, lineWidth + chevronDepth),
        end = Offset(centerX + chevronHalf, lineWidth),
        strokeWidth = lineWidth
    )
    drawLine(
        color = accent.copy(alpha = 0.72f),
        start = Offset(centerX - chevronHalf, size.height - lineWidth),
        end = Offset(centerX, size.height - lineWidth - chevronDepth),
        strokeWidth = lineWidth
    )
    drawLine(
        color = accent.copy(alpha = 0.72f),
        start = Offset(centerX, size.height - lineWidth - chevronDepth),
        end = Offset(centerX + chevronHalf, size.height - lineWidth),
        strokeWidth = lineWidth
    )
}
