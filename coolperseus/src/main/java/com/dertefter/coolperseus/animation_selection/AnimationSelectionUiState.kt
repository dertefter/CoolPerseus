package com.dertefter.coolperseus.animation_selection

import com.dertefter.shared.slider_animations.EdgesGlowSettings
import com.dertefter.shared.slider_animations.PixelGlowSettings

data class AnimationSelectionUiState(
    val selectedAnimation: String?,
    val animations: List<String?>,
    val edgesGlowSettings: EdgesGlowSettings = EdgesGlowSettings(),
    val pixelGlowSettings: PixelGlowSettings = PixelGlowSettings()
)
