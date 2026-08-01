package com.dertefter.coolperseus.overlay

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun EdgesGlow(
    modifier: Modifier = Modifier,
    percent: Float = 1f,
    closed: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary
) {

    val maxV = if (closed) 16f else 40f

    val maxAlpha = if (closed) 0.6f else 1.0f

    val animatedWidth = when {
        percent < 0.15f -> (percent / 0.15f) * maxV
        else -> ((1f - percent) / 0.85f) * maxV
    }.coerceIn(0f, maxV).dp

    val alpha = when {
        percent < 0.15f -> (percent / 0.15f) * maxAlpha
        else -> ((1f - percent) / 0.85f) * maxAlpha
    }.coerceIn(0f, maxAlpha)

    val vOffset = if (closed) (-8).dp else 20.dp

    Box(
        modifier = modifier
            .innerShadow(
                shape = RoundedCornerShape(28.dp),
                shadow = Shadow(
                    radius = animatedWidth,
                    color = color,
                    alpha = alpha,
                    offset = DpOffset(0.dp, vOffset),
                )
            )
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ){}
}

@Preview(backgroundColor = 0xFFF8EFEF)
@Composable
fun EdgesGlowPreview() {
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")

    val percent by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "percent"
    )

    val closedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000),
            repeatMode = RepeatMode.Restart
        ),
        label = "closedPhase"
    )
    val closed = closedPhase < 1f

    MaterialTheme {
        EdgesGlow(percent = percent, closed = closed)
    }
}
