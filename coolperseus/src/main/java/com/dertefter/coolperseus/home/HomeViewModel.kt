package com.dertefter.coolperseus.home

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dertefter.coolperseus.R
import com.dertefter.coolperseus.sounds.SoundPackStore
import com.dertefter.shared.data.SettingsRepository
import com.dertefter.shared.data.model.SoundType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private val errorState = MutableStateFlow<String?>(null)

    private val ioErrorLogger = CoroutineExceptionHandler { _, error ->
        Log.e(TAG, "Sound pack operation failed", error)
        errorState.value = context.getString(R.string.error_invalid_sound_pack)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.soundType,
        settingsRepository.customSounds,
        errorState
    ) { selectedSound, customSounds, error ->
        HomeUiState(
            selectedSound = selectedSound,
            sounds = customSounds + listOf(null),
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(null, listOf(null))
    )

    init {
        viewModelScope.launch(Dispatchers.IO + ioErrorLogger) {
            val stored = settingsRepository.customSounds.first()
            if (stored.isNotEmpty()) migrateCustomSounds(stored)
        }
    }

    fun onEvent(homeEvent: HomeEvent) {
        when (homeEvent) {
            is HomeEvent.OnSelectSound -> {
                viewModelScope.launch { settingsRepository.setSelectedSound(homeEvent.sound) }
                playSoundPreview(homeEvent.sound)
            }

            is HomeEvent.OnImportSoundPack -> importSoundPack(homeEvent.uri)
            is HomeEvent.OnDeleteSoundPack -> deleteSoundPack(homeEvent.sound)
            HomeEvent.OnDismissError -> errorState.value = null
        }
    }

    private fun importSoundPack(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO + ioErrorLogger) {
            val contents = readZipEntries(uri)
            val image = contents.images.firstOrNull { it.looksLikeCover } ?: contents.images.firstOrNull()

            if (contents.open.isEmpty() || contents.close.isEmpty() || image == null) {
                Log.w(
                    TAG,
                    "Invalid pack $uri: open=${contents.open.size}, close=${contents.close.size}, images=${contents.images.size}"
                )
                errorState.value = context.getString(R.string.error_invalid_sound_pack)
                return@launch
            }

            val packDir = SoundPackStore.packDir(context, nextPackId())
            val pack = SoundType(
                id = packDir.name,
                name = getFileNameFromUri(uri),
                openSounds = contents.open.map { storePackFile(packDir, it) },
                closeSounds = contents.close.map { storePackFile(packDir, it) },
                imagePath = storePackFile(packDir, image)
            )

            settingsRepository.addCustom(pack)
            settingsRepository.setSelectedSound(pack)
            withContext(Dispatchers.Main) { playSoundPreview(pack) }
        }
    }

    private fun readZipEntries(uri: Uri): ZipContents {
        val open = mutableListOf<ZipEntryData>()
        val close = mutableListOf<ZipEntryData>()
        val images = mutableListOf<ZipEntryData>()

        context.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val data = ZipEntryData(entry.name.substringAfterLast('/'), zis.readBytes())
                        when {
                            data.isImage -> images += data
                            data.isOpenSound -> open += data
                            data.isCloseSound -> close += data
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        }

        return ZipContents(open, close, images)
    }

    private fun storePackFile(packDir: File, data: ZipEntryData): String {
        val target = SoundPackStore.uniqueFile(packDir, SoundPackStore.safeFileName(data.fileName))
        target.outputStream().use { it.write(data.bytes) }
        return SoundPackStore.contentUri(context, target)
    }

    private fun nextPackId(): String = "pack_${System.currentTimeMillis()}"

    private fun deleteSoundPack(pack: SoundType) {
        viewModelScope.launch(Dispatchers.IO + ioErrorLogger) {
            settingsRepository.removeCustom(pack)

            if (settingsRepository.soundType.first() == pack) {
                settingsRepository.setSelectedSound(null)
            }

            SoundPackStore.deletePack(context, pack.id)
            val legacyPaths = pack.openSounds + pack.closeSounds + listOfNotNull(pack.imagePath)
            for (path in legacyPaths.filterNot { SoundPackStore.isContentUri(it) }) {
                runCatching { File(path).delete() }
            }
        }
    }

    // ----------------------------------------------------------------- миграция

    private suspend fun migrateCustomSounds(stored: List<SoundType>) {
        val migrated = stored.mapNotNull { pack ->
            if (pack.isShared()) pack else relocate(pack)
        }

        if (migrated == stored) return
        settingsRepository.replaceCustomSounds(migrated)

        val selected = settingsRepository.soundType.first()
        if (selected != null && migrated.none { it.id == selected.id }) {
            settingsRepository.setSelectedSound(null)
        }
    }

    private fun SoundType.isShared(): Boolean =
        (openSounds + closeSounds).all { SoundPackStore.isContentUri(it) }

    private fun relocate(pack: SoundType): SoundType? {
        val packDir = SoundPackStore.packDir(context, pack.id)
        val open = pack.openSounds.mapNotNull { relocateInto(it, packDir) }
        val close = pack.closeSounds.mapNotNull { relocateInto(it, packDir) }

        if (open.isEmpty() || close.isEmpty()) {
            SoundPackStore.deletePack(context, pack.id)
            return null
        }

        return pack.copy(
            openSounds = open,
            closeSounds = close,
            imagePath = pack.imagePath?.let { relocateInto(it, packDir) }
        )
    }

    private fun relocateInto(path: String, packDir: File): String? {
        val source = SoundPackStore.localFile(context, path) ?: return null
        val target = File(packDir, source.name)
        if (target.path != source.path) {
            runCatching { source.copyTo(target, overwrite = true) }.onFailure { return null }
            runCatching { source.delete() }
        }
        return SoundPackStore.contentUri(context, target)
    }

    private fun playSoundPreview(pack: SoundType?) {
        releasePlayer()
        val source = pack?.openSounds?.randomOrNull() ?: return

        try {
            val local = SoundPackStore.localFile(context, source)?.absolutePath
            mediaPlayer = MediaPlayer().apply {
                if (local != null) {
                    setDataSource(local)
                } else {
                    setDataSource(context, source.toUri())
                }
                setOnCompletionListener { releaseOnComplete(it) }
                setOnErrorListener { player, _, _ ->
                    releaseOnComplete(player)
                    true
                }
                prepareAsync()
                setOnPreparedListener { it.start() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound preview", e)
        }
    }

    private fun releaseOnComplete(player: MediaPlayer) {
        runCatching { player.release() }
        if (mediaPlayer === player) mediaPlayer = null
    }

    private fun releasePlayer() {
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) name = cursor.getString(index)
                }
            }
        }
        if (name == null) name = uri.lastPathSegment ?: uri.path?.substringAfterLast('/')

        return name
            ?.removeSuffix(".zip")
            ?.removeSuffix(".ZIP")
            ?.ifBlank { null }
            ?: "Sound Pack"
    }

    override fun onCleared() {
        releasePlayer()
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }

    private data class ZipContents(
        val open: List<ZipEntryData>,
        val close: List<ZipEntryData>,
        val images: List<ZipEntryData>
    )

    private class ZipEntryData(val fileName: String, val bytes: ByteArray) {
        private val lower: String = fileName.lowercase()

        val isImage: Boolean = IMAGE_EXTENSIONS.any { lower.endsWith(it) } || lower.contains("image")
        val looksLikeCover: Boolean = lower.startsWith("image") || lower.contains("image")
        val isOpenSound: Boolean = !isImage && lower.contains("_open")
        val isCloseSound: Boolean = !isImage && lower.contains("_close")

        companion object {
            private val IMAGE_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp", ".bmp", ".gif")
        }
    }
}
