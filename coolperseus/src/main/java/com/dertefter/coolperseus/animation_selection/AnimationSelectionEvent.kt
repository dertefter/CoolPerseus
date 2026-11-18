package com.dertefter.coolperseus.animation_selection

import com.dertefter.shared.slider_animations.EdgesGlowSettings
import com.dertefter.shared.slider_animations.PixelGlowSettings

sealed class AnimationSelectionEvent {

    object OnNavigateBack : AnimationSelectionEvent()

    data class OnSelectAnimation(val animation: String?) : AnimationSelectionEvent()

    data class OnUpdateEdgesGlowSettings(val settings: EdgesGlowSettings) : AnimationSelectionEvent()

    object OnResetEdgesGlowSettings : AnimationSelectionEvent()

    data class OnUpdatePixelGlowSettings(val settings: PixelGlowSettings) : AnimationSelectionEvent()

    object OnResetPixelGlowSettings : AnimationSelectionEvent()

}
