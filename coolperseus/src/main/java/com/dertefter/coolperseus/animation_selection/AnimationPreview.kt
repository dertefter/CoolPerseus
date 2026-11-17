package com.dertefter.coolperseus.animation_selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.shared.slider_animations.EdgesGlow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AnimationPreview(
    modifier: Modifier = Modifier,
    isLoop: Boolean = true,
) {
    val glowView = remember { mutableStateOf<EdgesGlow?>(null) }
    val color = android.graphics.Color.rgb(0, 255, 98)

    AndroidView(
        factory = { ctx ->
            EdgesGlow(ctx, zOrderOnTop = true).apply {
                glowView.value = this
                startAnimation(isOpen = true, color = color) {}
            }
        },
        update = { view ->
            glowView.value = view
        },
        modifier = modifier,
    )

    if (isLoop) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(600.milliseconds) // Animation duration (open)
                delay(200.milliseconds) // Pause 0.2s
                glowView.value?.startAnimation(isOpen = false, color = color) {}
                delay(600.milliseconds) // Animation duration (close)
                delay(1000.milliseconds) // Pause 1s
                glowView.value?.startAnimation(isOpen = true, color = color) {}
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnimationPreviewPreview() {
    CoolPerseusTheme {
        AnimationPreview()
    }
}
