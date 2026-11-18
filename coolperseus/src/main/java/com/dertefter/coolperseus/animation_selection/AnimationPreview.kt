package com.dertefter.coolperseus.animation_selection

import android.content.Context
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.dertefter.coolperseus.CoolPerseusTheme
import com.dertefter.shared.slider_animations.EdgesGlow
import com.dertefter.shared.slider_animations.EdgesGlowSettings
import com.dertefter.shared.slider_animations.PixelGlow
import com.dertefter.shared.slider_animations.PixelGlowSettings
import com.dertefter.shared.slider_animations.SliderAnimation
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AnimationPreview(
    animation: String?,
    modifier: Modifier = Modifier,
    edgesGlowSettings: EdgesGlowSettings = EdgesGlowSettings(),
    pixelGlowSettings: PixelGlowSettings = PixelGlowSettings(),
    isLoop: Boolean = true,
) {
    when (animation) {
        "edges_glow" -> key(animation) {
            SliderAnimationPreview(
                modifier = modifier,
                isLoop = isLoop,
                factory = { ctx ->
                    EdgesGlow(ctx, zOrderOnTop = true).apply { applySettings(edgesGlowSettings) }
                },
                onSettingsUpdate = { view ->
                    (view as? EdgesGlow)?.applySettings(edgesGlowSettings)
                }
            )
        }

        "pixel_glow" -> key(animation) {
            SliderAnimationPreview(
                modifier = modifier,
                isLoop = isLoop,
                factory = { ctx ->
                    PixelGlow(ctx, zOrderOnTop = true).apply { applySettings(pixelGlowSettings) }
                },
                onSettingsUpdate = { view ->
                    (view as? PixelGlow)?.applySettings(pixelGlowSettings)
                }
            )
        }
    }
}

@Composable
private fun SliderAnimationPreview(
    factory: (Context) -> View,
    onSettingsUpdate: (View) -> Unit,
    modifier: Modifier = Modifier,
    isLoop: Boolean = true,
) {
    val animationView = remember { mutableStateOf<SliderAnimation?>(null) }
    val color = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        factory = { ctx ->
            factory(ctx).also { view ->
                animationView.value = view as? SliderAnimation
                animationView.value?.startAnimation(isOpen = true, color = color) {}
            }
        },
        update = { view ->
            animationView.value = view as? SliderAnimation
            onSettingsUpdate(view)
        },
        modifier = modifier,
    )

    if (isLoop) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(600.milliseconds) // Animation duration (open)
                delay(200.milliseconds) // Pause 0.2s
                animationView.value?.startAnimation(isOpen = false, color = color) {}
                delay(600.milliseconds) // Animation duration (close)
                delay(1000.milliseconds) // Pause 1s
                animationView.value?.startAnimation(isOpen = true, color = color) {}
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnimationPreviewPreview() {
    CoolPerseusTheme {
        AnimationPreview(animation = "edges_glow")
    }
}

@Preview(showBackground = true)
@Composable
fun PixelGlowPreview() {
    CoolPerseusTheme {
        AnimationPreview(animation = "pixel_glow")
    }
}
