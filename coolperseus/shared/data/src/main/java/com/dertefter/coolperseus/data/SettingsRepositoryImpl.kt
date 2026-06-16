package com.dertefter.coolperseus.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.dertefter.coolperseus.data.model.DeviceAction
import com.dertefter.coolperseus.data.model.SoundType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("coolperseus_settings", Context.MODE_PRIVATE)


    override val soundType: Flow<SoundType?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_SELECTED_SOUND) {
                trySend(getSelectedSound())
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getSelectedSound())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val customSounds: Flow<List<SoundType>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_CUSTOM_SOUNDS) {
                trySend(getCustomSoundsList())
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getCustomSoundsList())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val actionSliderUp: Flow<DeviceAction> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_ACTION_SLIDER_UP) {
                trySend(getAction(PREF_ACTION_SLIDER_UP))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAction(PREF_ACTION_SLIDER_UP))
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val actionSliderDown: Flow<DeviceAction> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_ACTION_SLIDER_DOWN) {
                trySend(getAction(PREF_ACTION_SLIDER_DOWN))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAction(PREF_ACTION_SLIDER_DOWN))
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val actionAiButton: Flow<DeviceAction> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_ACTION_AI_BUTTON) {
                trySend(getAction(PREF_ACTION_AI_BUTTON))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAction(PREF_ACTION_AI_BUTTON))
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setSelectedSound(sound: SoundType?) {
        if (sound == null) {
            sharedPreferences.edit {
                putString(PREF_SELECTED_SOUND, "none")
            }
        } else {
            sharedPreferences.edit {
                putString(PREF_SELECTED_SOUND, serializeSound(sound))
            }
        }
    }

    override suspend fun addCustom(soundType: SoundType) {
        val current = getCustomSoundsList().toMutableList()
        val isDuplicate = current.any {
            it is SoundType.Custom &&
                    soundType is SoundType.Custom &&
                    it.openSoundPath == soundType.openSoundPath
        }
        if (!isDuplicate) {
            current.add(soundType)
            saveCustomSounds(current)
        }
    }

    override suspend fun removeCustom(soundType: SoundType) {
        val current = getCustomSoundsList().toMutableList()
        if (current.remove(soundType)) {
            saveCustomSounds(current)
        }
    }

    override suspend fun setActionSliderUp(action: DeviceAction) {
        sharedPreferences.edit {
            putString(PREF_ACTION_SLIDER_UP, action.toStringRepresentation())
        }
    }

    override suspend fun setActionSliderDown(action: DeviceAction) {
        sharedPreferences.edit {
            putString(PREF_ACTION_SLIDER_DOWN, action.toStringRepresentation())
        }
    }

    override suspend fun setActionAiButton(action: DeviceAction) {
        sharedPreferences.edit {
            putString(PREF_ACTION_AI_BUTTON, action.toStringRepresentation())
        }
    }

    private fun getAction(key: String): DeviceAction {
        val serialized = sharedPreferences.getString(key, null)
        return DeviceAction.fromString(serialized)
    }

    private fun getSelectedSound(): SoundType? {
        val serialized = sharedPreferences.getString(PREF_SELECTED_SOUND, null) ?: return SoundType.Keji
        return deserializeSound(serialized)
    }

    private fun getCustomSoundsList(): List<SoundType> {
        val serializedSet = sharedPreferences.getStringSet(PREF_CUSTOM_SOUNDS, emptySet()) ?: emptySet()
        return serializedSet.mapNotNull { deserializeSound(it) }
    }

    private fun saveCustomSounds(sounds: List<SoundType>) {
        val serializedSet = sounds.map { serializeSound(it) }.toSet()
        sharedPreferences.edit {
            putStringSet(PREF_CUSTOM_SOUNDS, serializedSet)
        }
    }

    private fun serializeSound(sound: SoundType): String {
        return when (sound) {
            is SoundType.Jianghu -> "jianghu"
            is SoundType.Jixie -> "jixie"
            is SoundType.Keji -> "keji"
            is SoundType.Lingdong -> "lingdong"
            is SoundType.Zippo -> "zippo"
            is SoundType.Custom -> "custom|${sound.openSoundPath}|${sound.closeSoundPath}"
        }
    }

    private fun deserializeSound(serialized: String): SoundType? {
        return when {
            serialized == "jianghu" -> SoundType.Jianghu
            serialized == "jixie" -> SoundType.Jixie
            serialized == "keji" -> SoundType.Keji
            serialized == "lingdong" -> SoundType.Lingdong
            serialized == "zippo" -> SoundType.Zippo
            serialized.startsWith("custom|") -> {
                val parts = serialized.split("|")
                if (parts.size == 3) {
                    SoundType.Custom(parts[1], parts[2])
                } else null
            }
            else -> null
        }
    }

    companion object {
        private const val PREF_SELECTED_SOUND = "selected_sound"
        private const val PREF_CUSTOM_SOUNDS = "custom_sounds"
        private const val PREF_WIDGET_IDS = "widget_ids"

        private const val PREF_ACTION_SLIDER_UP = "action_slider_up"
        private const val PREF_ACTION_SLIDER_DOWN = "action_slider_down"
        private const val PREF_ACTION_AI_BUTTON = "action_ai_button"
    }
}
