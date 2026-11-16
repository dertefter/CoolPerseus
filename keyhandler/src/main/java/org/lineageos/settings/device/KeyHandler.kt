package org.lineageos.settings.device

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import java.io.File
import android.graphics.PixelFormat
import android.util.TypedValue
import android.media.AudioAttributes
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
import com.dertefter.shared.data.model.SoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

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
            fitInsetsTypes = 0
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val primaryColorInt = try {
            val sysUiContext = try { mContext.createPackageContext("com.android.systemui", 0) } catch(e: Exception) { null }
            val sysUiAccent = sysUiContext?.let {
                val tv = TypedValue()
                it.theme.resolveAttribute(android.R.attr.colorAccent, tv, true)
                if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    tv.data
                } else 0
            } ?: 0

            if (sysUiAccent != 0 && (sysUiAccent and 0xFFFFFF) != 0x1A73E8) {
                sysUiAccent
            } else {
                val isDark = (userContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val finalId = if (isDark) {
                    userContext.resources.getIdentifier("system_accent1_200", "color", "android")
                } else {
                    userContext.resources.getIdentifier("system_accent1_600", "color", "android")
                }
                userContext.getColor(finalId)
            }
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to resolve dynamic color", e)
            "#6200EE".toColorInt()
        }

        val overlayView = EdgesGlow(userContext).apply {
            startAnimation(isOpen, primaryColorInt) {
                removeOverlay(this)
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

    private fun getApkPath(): String? {
        try {
            val location = KeyHandler::class.java.protectionDomain?.codeSource?.location?.path
            if (!location.isNullOrEmpty()) {
                val cleanPath = location.removePrefix("file:").split("!").first()
                if (File(cleanPath).exists()) {
                    return cleanPath
                }
            }
        } catch (e: Exception) {
            Log.w("DeviceKeyHandler", "Failed to get path from CodeSource", e)
        }

        try {
            val classLoaderStr = KeyHandler::class.java.classLoader?.toString() ?: ""
            val regex = Regex("""(/[^"]+\.apk)""")
            val match = regex.find(classLoaderStr)
            if (match != null) {
                val path = match.groupValues[1]
                if (File(path).exists()) {
                    return path
                }
            }
        } catch (e: Exception) {
            Log.w("DeviceKeyHandler", "Failed to get path from ClassLoader string", e)
        }

        val commonPaths = listOf(
            "/system/priv-app/DeviceSettings/DeviceSettings.apk",
            "/system/app/DeviceSettings/DeviceSettings.apk",
            "/product/priv-app/DeviceSettings/DeviceSettings.apk",
            "/system_ext/priv-app/DeviceSettings/DeviceSettings.apk"
        )
        for (path in commonPaths) {
            if (File(path).exists()) {
                return path
            }
        }

        return null
    }

    private fun getTargetResources(): Resources? {
        // 1. Try mContext.resources first
        try {
            val testFd = mContext.resources.openRawResourceFd(R.raw.slide_keji_open)
            testFd?.close()
            return mContext.resources
        } catch (_: Exception) {}

        // 2. Try createPackageContext with common package names
        val packageNames = listOf(
            "org.lineageos.settings.device",
            "com.cyanogenmod.settings.device",
            "org.lineageos.settings",
            "com.dertefter.coolperseus"
        )
        for (pkg in packageNames) {
            try {
                val ctx = mContext.createPackageContext(pkg, 0)
                val testFd = ctx.resources.openRawResourceFd(R.raw.slide_keji_open)
                testFd?.close()
                return ctx.resources
            } catch (_: Exception) {}
        }

        // 3. Load resources directly from APK file
        val apkPath = getApkPath()
        if (apkPath != null) {
            try {
                val pm = mContext.packageManager
                val appInfo = pm.getPackageArchiveInfo(apkPath, 0)?.applicationInfo
                if (appInfo != null) {
                    appInfo.sourceDir = apkPath
                    appInfo.publicSourceDir = apkPath
                    val res = pm.getResourcesForApplication(appInfo)
                    val testFd = res.openRawResourceFd(R.raw.slide_keji_open)
                    testFd?.close()
                    Log.d("DeviceKeyHandler", "Successfully loaded Resources from APK path: $apkPath")
                    return res
                }
            } catch (e: Exception) {
                Log.e("DeviceKeyHandler", "Failed to create Resources from APK path: $apkPath", e)
            }
        }

        return null
    }

    private fun playSound(isOpen: Boolean) {
        scope.launch {
            val soundType = settingsRepository.soundType.first() ?: return@launch

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (soundType is SoundType.Custom) {
                val path = if (isOpen) soundType.openSoundPath else soundType.closeSoundPath
                if (path.isNotEmpty()) {
                    try {
                        val uri: Uri = path.toUri()
                        val player = MediaPlayer().apply {
                            setAudioAttributes(audioAttributes)
                            setDataSource(mContext, uri)
                            prepare()
                            setOnCompletionListener { p -> p.release() }
                            setOnErrorListener { p, what, extra ->
                                Log.e("DeviceKeyHandler", "MediaPlayer error for custom sound: what=$what, extra=$extra")
                                p.release()
                                true
                            }
                            start()
                        }
                    } catch (e: Exception) {
                        Log.e("DeviceKeyHandler", "Failed to create MediaPlayer for custom uri $path", e)
                    }
                }
                return@launch
            }

            val soundResId = if (!isOpen) {
                when (soundType) {
                    is SoundType.Keji -> R.raw.slide_keji_open
                    is SoundType.Zippo -> R.raw.slide_zippo_open
                    is SoundType.Jianghu -> R.raw.slide_jianghu_open
                    is SoundType.Lingdong -> R.raw.slide_lingdong_open
                    is SoundType.Jixie -> R.raw.slide_jiguan_open
                }
            } else {
                when (soundType) {
                    is SoundType.Keji -> R.raw.slide_keji_close
                    is SoundType.Zippo -> R.raw.slide_zippo_close
                    is SoundType.Jianghu -> R.raw.slide_jianghu_close
                    is SoundType.Lingdong -> R.raw.slide_lingdong_close
                    is SoundType.Jixie -> R.raw.slide_jiguan_close
                }
            }

            if (soundResId == 0) return@launch

            val targetResources = getTargetResources()
            if (targetResources == null) {
                Log.e("DeviceKeyHandler", "Failed to resolve target Resources for soundResId $soundResId")
                return@launch
            }

            try {
                val afd = targetResources.openRawResourceFd(soundResId)
                if (afd != null) {
                    val player = MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        prepare()
                        setOnCompletionListener { p -> p.release() }
                        setOnErrorListener { p, what, extra ->
                            Log.e("DeviceKeyHandler", "MediaPlayer error: what=$what, extra=$extra")
                            p.release()
                            true
                        }
                        start()
                    }
                    Log.d("DeviceKeyHandler", "Successfully started MediaPlayer for soundResId=$soundResId")
                } else {
                    Log.e("DeviceKeyHandler", "openRawResourceFd returned null for soundResId $soundResId")
                }
            } catch (e: Exception) {
                Log.e("DeviceKeyHandler", "Failed to play sound for soundResId $soundResId", e)
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
