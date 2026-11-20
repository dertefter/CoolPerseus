package org.lineageos.settings.device

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.android.internal.os.DeviceKeyHandler
import com.dertefter.shared.data.SettingsRepository
import com.dertefter.shared.data.SettingsRepositoryImpl
import com.dertefter.shared.data.model.DeviceAction
import com.dertefter.shared.slider_animations.EdgesGlow
import com.dertefter.shared.slider_animations.PixelGlow
import com.dertefter.shared.slider_animations.SliderAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("unused")
class KeyHandler(private val mContext: Context) : DeviceKeyHandler, LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var activeOverlayCount = 0
    private var currentOverlayView: View? = null
    private var lastEventTime = 0L
    private var lastScanCode = -1
    private var windowManager: WindowManager? = null
    private val openRotator = SoundRotator()
    private val closeRotator = SoundRotator()

    private val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(mContext)
    }

    init {
        handler.post {
            savedStateRegistryController.performRestore(null)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
    }

    companion object {
        private const val TAG = "DeviceKeyHandler"

        private const val KEYCODE_SLIDER_UP = 594
        private const val KEYCODE_SLIDER_DOWN = 595
        private const val KEYCODE_AI = 689

        private const val ACTION_SLIDER_EVENT = "com.dertefter.coolperseus.ACTION_SLIDER_EVENT"
        private const val EXTRA_EVENT_TYPE = "event_type"

        private const val CONTENT_SCHEME = "content://"
    }

    override fun handleKeyEvent(event: KeyEvent): KeyEvent? {
        val isMyScancode = event.scanCode in setOf(
            KEYCODE_SLIDER_UP,
            KEYCODE_SLIDER_DOWN,
            KEYCODE_AI
        )

        if (!isMyScancode) return event

        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return null

        val now = System.currentTimeMillis()
        if (event.scanCode == lastScanCode && (now - lastEventTime) < 300) {
            Log.d("DeviceKeyHandler", "Ignoring duplicate key event for scanCode=${event.scanCode}")
            return null
        }
        lastEventTime = now
        lastScanCode = event.scanCode

        val type = when (event.scanCode) {
            KEYCODE_SLIDER_UP -> "slider_up"
            KEYCODE_SLIDER_DOWN -> "slider_down"
            KEYCODE_AI -> "ai_button"
            else -> return event
        }

        handler.post {
            handleAction(type)
            sendSliderEventBroadcast(type)
        }

        return null
    }

    private fun handleAction(type: String) {
        scope.launch {
            val action = when (type) {
                "slider_down" -> settingsRepository.actionSliderDown.first()
                "slider_up" -> settingsRepository.actionSliderUp.first()
                "ai_button" -> settingsRepository.actionAiButton.first()
                else -> DeviceAction.None
            }
            executeAction(action)
            if (type != "ai_button") {
                showOverlay(type)
            }
        }
    }

    private fun executeAction(action: DeviceAction) {
        when (action) {
            DeviceAction.None -> {}
            is DeviceAction.LaunchIntent -> {
                when (action.action) {
                    "open_front_camera" -> openCamera(frontCamera = true)
                    "open_camera" -> openCamera(frontCamera = false)
                    else -> launchCustomIntent(action.action)
                }
            }
            is DeviceAction.LaunchApp -> launchApp(action.packageName)
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = mContext.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityAsUser(intent)
            }
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to launch app: $packageName", e)
        }
    }

    private fun launchCustomIntent(actionString: String) {
        try {
            val intent = Intent(actionString).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAsUser(intent)
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to launch custom intent: $actionString", e)
        }
    }

    private fun sendSliderEventBroadcast(type: String) {
        val intent = Intent(ACTION_SLIDER_EVENT).apply {
            putExtra(EXTRA_EVENT_TYPE, type)
            setPackage("com.dertefter.coolperseus")
        }
        sendBroadcastAsUser(intent)
    }

    private fun sendBroadcastAsUser(intent: Intent) {
        try {
            val userHandleClass = Class.forName("android.os.UserHandle")
            val constructor = userHandleClass.getConstructor(Int::class.javaPrimitiveType)
            val userHandle = constructor.newInstance(-2) // UserHandle.CURRENT
            val method = Context::class.java.getMethod("sendBroadcastAsUser", Intent::class.java, userHandleClass)
            method.invoke(mContext, intent, userHandle)
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to sendBroadcastAsUser via reflection", e)
            try {
                mContext.sendBroadcast(intent)
            } catch (e2: Exception) {
                Log.e("DeviceKeyHandler", "Failed to sendBroadcast normally", e2)
            }
        }
    }

    private fun openCamera(frontCamera: Boolean) {
        val keyguardManager = mContext.getSystemService(KeyguardManager::class.java)
        val intent = Intent(
            if (keyguardManager?.isDeviceLocked == true) {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
            } else {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            }
        ).apply {
            putExtra("android.intent.extra.USE_FRONT_CAMERA", frontCamera)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivityAsUser(intent)
    }

    private fun startActivityAsUser(intent: Intent) {
        try {
            val userHandleClass = Class.forName("android.os.UserHandle")
            val constructor = userHandleClass.getConstructor(Int::class.javaPrimitiveType)
            val userHandle = constructor.newInstance(-2) // UserHandle.CURRENT
            val method = Context::class.java.getMethod("startActivityAsUser", Intent::class.java, userHandleClass)
            method.invoke(mContext, intent, userHandle)
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to startActivityAsUser via reflection", e)
            try {
                mContext.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("DeviceKeyHandler", "Failed to startActivity normally", e2)
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun showOverlay(type: String) {
        if (windowManager == null) {
            windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        val isOpen = type == "slider_up"
        val selectedAnim = settingsRepository.getSelectedAnimationSync()
        val glowSettings = settingsRepository.getEdgesGlowSettingsSync()
        val pixelSettings = settingsRepository.getPixelGlowSettingsSync()
        Log.d(
            "DeviceKeyHandler",
            "showOverlay: type=$type selectedAnim=$selectedAnim edgesGlow=$glowSettings pixelGlow=$pixelSettings"
        )

        if (selectedAnim != null && selectedAnim != "none") {
            val userContext = try {
                val userHandleClass = Class.forName("android.os.UserHandle")
                val method = Context::class.java.getMethod("createContextAsUser", userHandleClass, Int::class.javaPrimitiveType)
                val constructor = userHandleClass.getConstructor(Int::class.javaPrimitiveType)
                val userHandle = constructor.newInstance(-2) // UserHandle.CURRENT
                method.invoke(mContext, userHandle, 0) as Context
            } catch (e: Exception) {
                Log.e("DeviceKeyHandler", "Failed to create user context", e)
                mContext
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.FILL
                fitInsetsTypes = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            val primaryColorInt = resolveDynamicColor(userContext)

            val overlayView: View? = when (selectedAnim) {
                "edges_glow" -> EdgesGlow(userContext, zOrderOnTop = true).apply {
                    applySettings(glowSettings)
                }

                "pixel_glow" -> PixelGlow(userContext, zOrderOnTop = true).apply {
                    applySettings(pixelSettings)
                }

                else -> null
            }

            if (overlayView == null) {
                Log.w("DeviceKeyHandler", "Unknown animation id: $selectedAnim")
                playSound(isOpen)
                return
            }

            currentOverlayView?.let { oldView ->
                removeOverlay(oldView)
            }
            currentOverlayView = overlayView

            (overlayView as SliderAnimation).startAnimation(isOpen, primaryColorInt) {
                removeOverlay(overlayView)
            }

            try {
                windowManager?.addView(overlayView, params)
                if (activeOverlayCount == 0) {
                    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
                    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                }
                activeOverlayCount++
                Log.d("DeviceKeyHandler", "Overlay added: ${overlayView.javaClass.simpleName}, count=$activeOverlayCount")
            } catch (e: Exception) {
                Log.e("DeviceKeyHandler", "Failed to add overlay view", e)
            }
        } else {
            Log.w(
                "DeviceKeyHandler",
                "Overlay skipped: selectedAnim=$selectedAnim (в приложении выбрана анимация «Без анимации»)"
            )
        }
        playSound(isOpen)
    }

    private fun resolveDynamicColor(userContext: Context): Int {
        return try {
            val sysUiContext = try {
                mContext.createPackageContext("com.android.systemui", 0)
            } catch (e: Exception) {
                null
            }
            val tv = TypedValue()
            val resolveOk = sysUiContext?.theme
                ?.resolveAttribute(android.R.attr.colorAccent, tv, true) == true
            val isColorType = tv.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT
            val sysUiAccent = if (resolveOk && isColorType) tv.data else 0
            sysUiAccent
        } catch (e: Exception) {
            "#6200EE".toColorInt()
        }
    }

    private fun playSound(isOpen: Boolean) {
        scope.launch {
            val sound = settingsRepository.soundType.first() ?: return@launch

            val opening = !isOpen
            val sounds = if (opening) sound.openSounds else sound.closeSounds
            val source = (if (opening) openRotator else closeRotator).next(sounds) ?: return@launch

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            withContext(Dispatchers.IO) { playSoundSource(source, audioAttributes) }
        }
    }

    private class SoundRotator {
        private var lastIndex = -1

        fun next(sounds: List<String>): String? {
            if (sounds.isEmpty()) return null
            if (sounds.size == 1) return sounds.first()

            var index = sounds.indices.random()
            while (index == lastIndex) {
                index = sounds.indices.random()
            }
            lastIndex = index
            return sounds[index]
        }
    }

    private fun playSoundSource(source: String, audioAttributes: AudioAttributes) {
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(audioAttributes)

            if (source.startsWith(CONTENT_SCHEME)) {
                val descriptor = openSoundDescriptor(source)
                if (descriptor == null) {
                    Log.w(TAG, "Sound pack file is not available: $source")
                    player.release()
                    return
                }
                descriptor.use {
                    player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
            } else {
                player.setDataSource(source)
            }

            player.setOnCompletionListener { p -> p.release() }
            player.setOnErrorListener { p, what, extra ->
                Log.e(TAG, "MediaPlayer error for $source: what=$what, extra=$extra")
                p.release()
                true
            }
            player.prepare()
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound $source", e)
            runCatching { player.release() }
        }
    }

    private fun openSoundDescriptor(source: String): AssetFileDescriptor? = try {
        mContext.contentResolver.openAssetFileDescriptor(source.toUri(), "r")
    } catch (e: Exception) {
        Log.w(TAG, "SoundPackProvider did not serve $source", e)
        null
    }

    private fun removeOverlay(view: View) {
        handler.post {
            try {
                windowManager?.removeView(view)
                if (currentOverlayView == view) {
                    currentOverlayView = null
                }
                activeOverlayCount--
                if (activeOverlayCount <= 0) {
                    activeOverlayCount = 0
                    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                }
            } catch (e: Exception) {
                Log.e("DeviceKeyHandler", "Failed to remove overlay view", e)
            }
        }
    }
}
