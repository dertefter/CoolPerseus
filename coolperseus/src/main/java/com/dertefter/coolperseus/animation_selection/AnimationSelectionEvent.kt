package com.dertefter.coolperseus.animation_selection

sealed class AnimationSelectionEvent {

    object OnNavigateBack : AnimationSelectionEvent()

    data class OnSelectAnimation(val animation: String?) : AnimationSelectionEvent()

}
