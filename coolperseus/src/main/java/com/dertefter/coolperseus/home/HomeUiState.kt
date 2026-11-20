package com.dertefter.coolperseus.home

import com.dertefter.shared.data.model.SoundType

data class HomeUiState(
    val selectedSound: SoundType?,
    val sounds: List<SoundType?>,
    val error: String? = null
)
