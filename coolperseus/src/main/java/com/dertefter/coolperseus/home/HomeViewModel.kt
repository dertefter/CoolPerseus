package com.dertefter.coolperseus.home

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dertefter.coolperseus.R
import com.dertefter.shared.data.SettingsRepository
import com.dertefter.shared.data.model.SoundType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null

    private val availableSounds = listOf(
        "keji",
        "jixie",
        "zippo",
        "jianghu",
        "lingdong",
        null
    )

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.soundType,
        MutableStateFlow(availableSounds)
    ) { soundType, sounds ->
        HomeUiState(
            selectedSound = when (soundType) {
                is SoundType.Keji -> "keji"
                is SoundType.Jixie -> "jixie"
                is SoundType.Zippo -> "zippo"
                is SoundType.Jianghu -> "jianghu"
                is SoundType.Lingdong -> "lingdong"
                else -> null
            },
            sounds = sounds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(null, emptyList())
    )

    fun onEvent(homeEvent: HomeEvent) {
        if (homeEvent !is HomeEvent.OnSelectSound) return

        viewModelScope.launch {
            val soundType = when (homeEvent.sound) {
                "keji" -> SoundType.Keji
                "jixie" -> SoundType.Jixie
                "zippo" -> SoundType.Zippo
                "jianghu" -> SoundType.Jianghu
                "lingdong" -> SoundType.Lingdong
                else -> null
            }
            settingsRepository.setSelectedSound(soundType)
            playSoundPreview(homeEvent.sound)
        }
    }

    private fun playSoundPreview(soundKey: String?) {
        val soundResId = when (soundKey) {
            "keji" -> R.raw.slide_keji_open
            "jixie" -> R.raw.slide_jiguan_open
            "zippo" -> R.raw.slide_zippo_open
            "jianghu" -> R.raw.slide_jianghu_open
            "lingdong" -> R.raw.slide_lingdong_open
            else -> 0
        }

        mediaPlayer?.release()
        mediaPlayer = null

        if (soundResId != 0) {
            try {
                mediaPlayer = MediaPlayer.create(context, soundResId)?.apply {
                    start()
                    setOnCompletionListener { player ->
                        player.release()
                        if (mediaPlayer == player) mediaPlayer = null
                    }
                }
        } catch (_: Exception) {
        }
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
