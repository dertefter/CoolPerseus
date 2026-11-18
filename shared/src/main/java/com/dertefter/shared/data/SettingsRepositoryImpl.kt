package com.dertefter.shared.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.dertefter.shared.data.model.DeviceAction
import com.dertefter.shared.data.model.SoundType
import com.dertefter.shared.slider_animations.EdgesGlowSettings
import com.dertefter.shared.slider_animations.PixelGlowSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
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
    }.buffer(Channel.UNLIMITED)

    override val soundType: Flow<SoundType?> = createSettingsFlow(PREF_SELECTED_SOUND) { getSelectedSound() }

    override val customSounds: Flow<List<SoundType>> = createSettingsFlow(PREF_CUSTOM_SOUNDS) { getCustomSoundsList() }

    override val actionSliderUp: Flow<DeviceAction> = createSettingsFlow(PREF_ACTION_SLIDER_UP) { getAction(PREF_ACTION_SLIDER_UP) }

    override val actionSliderDown: Flow<DeviceAction> = createSettingsFlow(PREF_ACTION_SLIDER_DOWN) { getAction(PREF_ACTION_SLIDER_DOWN) }

    override val actionAiButton: Flow<DeviceAction> = createSettingsFlow(PREF_ACTION_AI_BUTTON) { getAction(PREF_ACTION_AI_BUTTON) }

    override val selectedAnimation: Flow<String?> = createSettingsFlow(PREF_SELECTED_ANIMATION) { getSelectedAnimation() }

    override val edgesGlowSettings: Flow<EdgesGlowSettings> = createSettingsFlow(PREF_EDGES_GLOW_SETTINGS) { getEdgesGlowSettings() }

    override val pixelGlowSettings: Flow<PixelGlowSettings> = createSettingsFlow(PREF_PIXEL_GLOW_SETTINGS) { getPixelGlowSettings() }

    override fun getSelectedAnimationSync(): String? = getSelectedAnimation()

    override fun getEdgesGlowSettingsSync(): EdgesGlowSettings = getEdgesGlowSettings()

    override fun getPixelGlowSettingsSync(): PixelGlowSettings = getPixelGlowSettings()

    override suspend fun setSelectedSound(sound: SoundType?) {
        val value = if (sound == null) "none" else serializeSound(sound)
        Settings.System.putString(contentResolver, PREF_SELECTED_SOUND, value)
        contentResolver.notifyChange(Settings.System.getUriFor(PREF_SELECTED_SOUND), null)
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
        contentResolver.notifyChange(Settings.System.getUriFor(PREF_ACTION_SLIDER_UP), null)
    }

    override suspend fun setActionSliderDown(action: DeviceAction) {
        Settings.System.putString(contentResolver, PREF_ACTION_SLIDER_DOWN, action.toStringRepresentation())
        contentResolver.notifyChange(Settings.System.getUriFor(PREF_ACTION_SLIDER_DOWN), null)
    }

    override suspend fun setActionAiButton(action: DeviceAction) {
        Settings.System.putString(contentResolver, PREF_ACTION_AI_BUTTON, action.toStringRepresentation())
        contentResolver.notifyChange(Settings.System.getUriFor(PREF_ACTION_AI_BUTTON), null)
    }

    override suspend fun setSelectedAnimation(animation: String?) {
        val value = animation ?: "none"
        Settings.System.putString(contentResolver, PREF_SELECTED_ANIMATION, value)
        contentResolver.notifyChange(Settings.System.getUriFor(PREF_SELECTED_ANIMATION), null)
    }

    override suspend fun setEdgesGlowSettings(settings: EdgesGlowSettings) {
        putString(PREF_EDGES_GLOW_SETTINGS, serializeEdgesGlowSettings(settings))
    }

    override suspend fun resetEdgesGlowSettings() {
        setEdgesGlowSettings(EdgesGlowSettings())
    }

    override suspend fun setPixelGlowSettings(settings: PixelGlowSettings) {
        putString(PREF_PIXEL_GLOW_SETTINGS, serializePixelGlowSettings(settings))
    }

    override suspend fun resetPixelGlowSettings() {
        setPixelGlowSettings(PixelGlowSettings())
    }

    private fun putString(key: String, value: String) {
        Settings.System.putString(contentResolver, key, value)
        contentResolver.notifyChange(Settings.System.getUriFor(key), null)
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
        contentResolver.notifyChange(Settings.System.getUriFor(PREF_CUSTOM_SOUNDS), null)
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

    private fun getEdgesGlowSettings(): EdgesGlowSettings {
        val raw = Settings.System.getString(contentResolver, PREF_EDGES_GLOW_SETTINGS)
        return deserializeEdgesGlowSettings(raw)
    }

    private fun serializeEdgesGlowSettings(s: EdgesGlowSettings): String = serializeValues(
        tag = "v${EdgesGlowSettings.STORAGE_VERSION}",
        values = listOf(
            s.cornerRadius, s.glowRadius, s.glowPower, s.pulseSpeed,
            s.pulseAmount, s.maxVerticalTop, s.maxVerticalBottom
        )
    )

    private fun deserializeEdgesGlowSettings(raw: String?): EdgesGlowSettings {
        val v = parseValues(raw, tag = "v${EdgesGlowSettings.STORAGE_VERSION}", count = 7)
            ?: return EdgesGlowSettings()
        return EdgesGlowSettings(
            cornerRadius = v[0],
            glowRadius = v[1],
            glowPower = v[2],
            pulseSpeed = v[3],
            pulseAmount = v[4],
            maxVerticalTop = v[5],
            maxVerticalBottom = v[6]
        )
    }

    private fun getPixelGlowSettings(): PixelGlowSettings {
        val raw = Settings.System.getString(contentResolver, PREF_PIXEL_GLOW_SETTINGS)
        return deserializePixelGlowSettings(raw)
    }

    private fun serializePixelGlowSettings(s: PixelGlowSettings): String = serializeValues(
        tag = "v${PixelGlowSettings.STORAGE_VERSION}",
        values = listOf(
            s.cornerRadius, s.glowRadius, s.glowPower, s.pulseSpeed, s.pulseAmount,
            s.verticalTop, s.verticalBottom, s.glowStrength,
            s.dotCell, s.dotSize, s.dotDensity, s.dotFlicker
        )
    )

    private fun deserializePixelGlowSettings(raw: String?): PixelGlowSettings {
        val v = parseValues(raw, tag = "v${PixelGlowSettings.STORAGE_VERSION}", count = 12)
            ?: return PixelGlowSettings()
        return PixelGlowSettings(
            cornerRadius = v[0],
            glowRadius = v[1],
            glowPower = v[2],
            pulseSpeed = v[3],
            pulseAmount = v[4],
            verticalTop = v[5],
            verticalBottom = v[6],
            glowStrength = v[7],
            dotCell = v[8],
            dotSize = v[9],
            dotDensity = v[10],
            dotFlicker = v[11]
        )
    }

    /** Формат хранения: `tag|f0|f1|...` */
    private fun serializeValues(tag: String, values: List<Float>): String =
        (listOf(tag) + values).joinToString("|")

    private fun parseValues(raw: String?, tag: String, count: Int): List<Float>? {
        if (raw.isNullOrEmpty()) return null
        val parts = raw.split("|")
        if (parts.size != count + 1 || parts[0] != tag) return null
        return try {
            parts.drop(1).map { it.toFloat() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREF_SELECTED_SOUND = "coolperseus_selected_sound"
        private const val PREF_CUSTOM_SOUNDS = "coolperseus_custom_sounds"
        private const val PREF_ACTION_SLIDER_UP = "coolperseus_action_slider_up"
        private const val PREF_ACTION_SLIDER_DOWN = "coolperseus_action_slider_down"
        private const val PREF_ACTION_AI_BUTTON = "coolperseus_action_ai_button"
        private const val PREF_SELECTED_ANIMATION = "coolperseus_selected_animation"
        private const val PREF_EDGES_GLOW_SETTINGS = "coolperseus_edges_glow_settings"
        private const val PREF_PIXEL_GLOW_SETTINGS = "coolperseus_pixel_glow_settings"
    }
}
