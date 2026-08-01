package org.lineageos.settings.device

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.util.TypedValue
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.android.internal.os.DeviceKeyHandler
import com.dertefter.coolperseus.data.SettingsRepository
import com.dertefter.coolperseus.data.SettingsRepositoryImpl
import com.dertefter.coolperseus.data.model.DeviceAction
import com.dertefter.coolperseus.data.model.SoundType
import com.dertefter.coolperseus.overlay.EdgesGlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("unused")
class KeyHandler(private val mContext: Context) : DeviceKeyHandler, LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var activeOverlayCount = 0
    private var windowManager: WindowManager? = null

    private val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(mContext)
    }

    init {
        handler.post {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
    }

    companion object {
        private const val KEYCODE_SLIDER_UP = 594
        private const val KEYCODE_SLIDER_DOWN = 595
        private const val KEYCODE_AI = 689

        private const val ACTION_SLIDER_EVENT = "com.dertefter.coolperseus.ACTION_SLIDER_EVENT"
        private const val EXTRA_EVENT_TYPE = "event_type"
    }

    override fun handleKeyEvent(event: KeyEvent): KeyEvent? {
        val isMyScancode = event.scanCode in setOf(
            KEYCODE_SLIDER_UP,
            KEYCODE_SLIDER_DOWN,
            KEYCODE_AI
        )

        if (!isMyScancode) return event

        if (event.action != KeyEvent.ACTION_DOWN) return null

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
        }

        if (type != "ai_button") {
            showOverlay(type)
        }
    }

    private fun executeAction(action: DeviceAction) {
        when (action) {
            DeviceAction.None -> {}
            is DeviceAction.LaunchIntent -> {
                when (action.action) {
                    "open_front_camera" -> openCamera(frontCamera = true)
                    "open_camera" -> openCamera(frontCamera = false)
                    "open_widgets" -> openWidgets()
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

    private fun openWidgets() {
        try {
            val intent = Intent().apply {
                component = ComponentName("com.dertefter.coolperseus", "com.dertefter.coolperseus.widgets.WidgetsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            startActivityAsUser(intent)
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to launch WidgetsActivity", e)
        }
    }

    private fun sendSliderEventBroadcast(type: String) {
        val intent = Intent(ACTION_SLIDER_EVENT).apply {
            putExtra(EXTRA_EVENT_TYPE, type)
            // Ensure the broadcast is explicit for our app if possible, or just send it
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

    private fun showOverlay(type: String) {
        if (windowManager == null) {
            windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        val isOpen = type == "slider_up"
        
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
            }
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val overlayView = ComposeView(userContext).apply {
            setViewTreeLifecycleOwner(this@KeyHandler)
            setViewTreeViewModelStoreOwner(this@KeyHandler)
            setViewTreeSavedStateRegistryOwner(this@KeyHandler)
            
            setContent {
                val isDark = (userContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                
                val primaryColor = try {
                    // SystemUI context is the most reliable source for the actual applied dynamic accent
                    val sysUiContext = try { mContext.createPackageContext("com.android.systemui", 0) } catch(e: Exception) { null }
                    val sysUiAccent = sysUiContext?.let {
                        val tv = TypedValue()
                        it.theme.resolveAttribute(android.R.attr.colorAccent, tv, true)
                        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                            tv.data
                        } else 0
                    } ?: 0

                    if (sysUiAccent != 0 && (sysUiAccent and 0xFFFFFF) != 0x1A73E8) {
                        Color(sysUiAccent)
                    } else if (android.os.Build.VERSION.SDK_INT >= 31) {
                        val finalId = if (isDark) {
                            userContext.resources.getIdentifier("system_accent1_200", "color", "android")
                        } else {
                            userContext.resources.getIdentifier("system_accent1_600", "color", "android")
                        }
                        Color(userContext.getColor(finalId))
                    } else {
                        Color(0xFF6200EE)
                    }
                } catch (e: Exception) {
                    Log.e("DeviceKeyHandler", "Failed to resolve dynamic color", e)
                    Color(0xFF6200EE)
                }

                val percent = remember { Animatable(0f) }
                
                LaunchedEffect(Unit) {
                    percent.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(600, easing = LinearEasing)
                    )
                    removeOverlay(this@apply)
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EdgesGlow(
                        percent = percent.value,
                        closed = isOpen,
                        color = primaryColor
                    )
                }
            }
        }

        try {
            windowManager?.addView(overlayView, params)
            if (activeOverlayCount == 0) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
            activeOverlayCount++
            playSound(isOpen)
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to add overlay view", e)
        }
    }

    private fun playSound(isOpen: Boolean) {
        scope.launch {
            val soundType = settingsRepository.soundType.first() ?: return@launch
            
            val resContext = try {
                mContext.createPackageContext("org.lineageos.settings.device", 0)
            } catch (e: Exception) {
                try {
                    mContext.createPackageContext("com.dertefter.coolperseus", 0)
                } catch (e2: Exception) {
                    null
                }
            }

            if (resContext == null) return@launch

            val resName = if (isOpen) {
                when (soundType) {
                    is SoundType.Keji -> "slide_keji_close"
                    is SoundType.Zippo -> "slide_zippo_close"
                    is SoundType.Jianghu -> "slide_jianghu_close"
                    is SoundType.Lingdong -> "slide_lingdong_close"
                    is SoundType.Jixie -> "slide_jiguan_close"
                    else -> null
                }
            } else {
                when (soundType) {
                    is SoundType.Keji -> "slide_keji_open"
                    is SoundType.Zippo -> "slide_zippo_open"
                    is SoundType.Jianghu -> "slide_jianghu_open"
                    is SoundType.Lingdong -> "slide_lingdong_open"
                    is SoundType.Jixie -> "slide_jiguan_open"
                    else -> null
                }
            }

            val soundResId = if (resName != null) {
                resContext.resources.getIdentifier(resName, "raw", resContext.packageName)
            } else 0

            val player = if (soundResId != 0) {
                MediaPlayer.create(resContext, soundResId)
            } else if (soundType is SoundType.Custom) {
                val path = if (isOpen) soundType.closeSoundPath else soundType.openSoundPath
                if (path.isNotEmpty()) {
                    try {
                        val uri: Uri = path.toUri()
                        MediaPlayer.create(mContext, uri)
                    } catch (_: Exception) {
                        null
                    }
                } else null
            } else null

            player?.let {
                it.start()
                it.setOnCompletionListener { p ->
                    p.release()
                }
            }
        }
    }

    private fun removeOverlay(view: View) {
        handler.post {
            try {
                windowManager?.removeView(view)
                activeOverlayCount--
                if (activeOverlayCount == 0) {
                    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                }
            } catch (e: Exception) {
                Log.e("DeviceKeyHandler", "Failed to remove overlay view", e)
            }
        }
    }
}
