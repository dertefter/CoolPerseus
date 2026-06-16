package com.dertefter.coolperseus.overlay

import androidx.lifecycle.ViewModel
import com.dertefter.coolperseus.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class AnimationOverlayViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val soundType = settingsRepository.soundType

}