package com.dertefter.shared.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.dertefter.shared.data.model.DeviceAction
import com.dertefter.shared.data.model.SoundType
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

    private val contentResolver = context.contentResolver

    private fun <T> createSettingsFlow(key: String, getValue: () -> T): Flow<T> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(getValue())
            }
        }
        val uri = Settings.System.getUriFor(key)
        contentResolver.registerContentObserver(uri, false, observer)
        trySend(getValue())
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    override val soundType: Flow<SoundType?> = createSettingsFlow(PREF_SELECTED_SOUND) { getSelectedSound() }

    override val customSounds: Flow<List<SoundType>> = createSettingsFlow(PREF_CUSTOM_SOUNDS) { getCustomSoundsList() }

    override val actionSliderUp: Flow<DeviceAction> = createSettingsFlow(PREF_ACTION_SLIDER_UP) { getAction(PREF_ACTION_SLIDER_UP) }

    override val actionSliderDown: Flow<DeviceAction> = createSettingsFlow(PREF_ACTION_SLIDER_DOWN) { getAction(PREF_ACTION_SLIDER_DOWN) }

    override val actionAiButton: Flow<DeviceAction> = createSettingsFlow(PREF_ACTION_AI_BUTTON) { getAction(PREF_ACTION_AI_BUTTON) }

    override val selectedAnimation: Flow<String?> = createSettingsFlow(PREF_SELECTED_ANIMATION) { getSelectedAnimation() }

    override suspend fun setSelectedSound(sound: SoundType?) {
        val value = if (sound == null) "none" else serializeSound(sound)
        Settings.System.putString(contentResolver, PREF_SELECTED_SOUND, value)
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
        Settings.System.putString(contentResolver, PREF_ACTION_SLIDER_UP, action.toStringRepresentation())
    }

    override suspend fun setActionSliderDown(action: DeviceAction) {
        Settings.System.putString(contentResolver, PREF_ACTION_SLIDER_DOWN, action.toStringRepresentation())
    }

    override suspend fun setActionAiButton(action: DeviceAction) {
        Settings.System.putString(contentResolver, PREF_ACTION_AI_BUTTON, action.toStringRepresentation())
    }

    override suspend fun setSelectedAnimation(animation: String?) {
        val value = animation ?: "none"
        Settings.System.putString(contentResolver, PREF_SELECTED_ANIMATION, value)
    }

    private fun getAction(key: String): DeviceAction {
        val serialized = Settings.System.getString(contentResolver, key)
        return DeviceAction.fromString(serialized)
    }

    private fun getSelectedSound(): SoundType? {
        val serialized = Settings.System.getString(contentResolver, PREF_SELECTED_SOUND) ?: return SoundType.Keji
        return deserializeSound(serialized)
    }

    private fun getCustomSoundsList(): List<SoundType> {
        val serialized = Settings.System.getString(contentResolver, PREF_CUSTOM_SOUNDS) ?: ""
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(";").mapNotNull { deserializeSound(it) }
    }

    private fun saveCustomSounds(sounds: List<SoundType>) {
        val serialized = sounds.joinToString(";") { serializeSound(it) }
        Settings.System.putString(contentResolver, PREF_CUSTOM_SOUNDS, serialized)
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

    private fun getSelectedAnimation(): String? {
        val value = Settings.System.getString(contentResolver, PREF_SELECTED_ANIMATION) ?: "edges_glow"
        return if (value == "none") null else value
    }

    companion object {
        private const val PREF_SELECTED_SOUND = "coolperseus_selected_sound"
        private const val PREF_CUSTOM_SOUNDS = "coolperseus_custom_sounds"
        private const val PREF_ACTION_SLIDER_UP = "coolperseus_action_slider_up"
        private const val PREF_ACTION_SLIDER_DOWN = "coolperseus_action_slider_down"
        private const val PREF_ACTION_AI_BUTTON = "coolperseus_action_ai_button"
        private const val PREF_SELECTED_ANIMATION = "coolperseus_selected_animation"
    }
}
