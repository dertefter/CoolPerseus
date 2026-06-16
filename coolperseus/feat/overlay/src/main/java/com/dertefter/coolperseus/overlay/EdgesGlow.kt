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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun EdgesGlow(
    modifier: Modifier = Modifier,
    percent: Float = 1f,
    closed: Boolean = false
) {

    val maxV = if (closed) 12f else 30f

    val maxAlpha = if (closed) 0.4f else 0.8f

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
                    color = MaterialTheme.colorScheme.primary,
                    alpha = alpha,
                    offset = DpOffset(0.dp, vOffset),
                )
            )
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ){}
}

@Preview
@Composable
fun EdgesGlowPreview() {
    val infiniteTransition = rememberInfiniteTransition(label = "percent")
    val percent by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "percent"
    )
    MaterialTheme {
        EdgesGlow(percent = 0.4f, closed = false)
    }
}
