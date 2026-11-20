package com.dertefter.shared.data.model

data class SoundType(
    val id: String,
    val name: String,
    val openSounds: List<String>,
    val closeSounds: List<String>,
    val imagePath: String? = null,
) {
    val isPlayable: Boolean
        get() = openSounds.isNotEmpty() && closeSounds.isNotEmpty()
}
