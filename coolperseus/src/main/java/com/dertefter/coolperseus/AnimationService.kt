package com.dertefter.coolperseus

import android.app.KeyguardManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dertefter.coolperseus.data.SettingsRepository
import com.dertefter.coolperseus.data.model.DeviceAction
import com.dertefter.coolperseus.design.theme.CoolPerseusTheme
import com.dertefter.coolperseus.overlay.AnimationOverlayRoute
import com.dertefter.coolperseus.overlay.AnimationOverlayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AnimationService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var cameraStateTracker: CameraStateTracker

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = intent?.getStringExtra("type") ?: ""

        lifecycleScope.launch {
            val action = when (type) {
                "slider_down" -> settingsRepository.actionSliderDown.first()
                "slider_up" -> settingsRepository.actionSliderUp.first()
                "ai_button" -> settingsRepository.actionAiButton.first()
                else -> DeviceAction.None
            }
            executeAction(action)
        }

        if (composeView == null && type != "ai_button") {
            showOverlay(type)
        }
        return START_NOT_STICKY
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
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("AnimationService", "Failed to launch app: $packageName", e)
        }
    }
    private fun launchCustomIntent(actionString: String) {
        try {
            val intent = Intent(actionString).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("AnimationService", "Failed to launch custom intent: $actionString", e)
        }
    }

    private fun openCamera(frontCamera: Boolean) {
        val keyguardManager = getSystemService(KeyguardManager::class.java)

        val intent = Intent(
            if (keyguardManager?.isDeviceLocked == true) {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
            } else {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            }
        ).apply {
            putExtra(
                "android.intent.extra.USE_FRONT_CAMERA",
                frontCamera
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        }

        startActivity(intent)
    }

    private fun showOverlay(type: String) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.FILL
            setFitInsetsTypes(0)
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AnimationService)
            setViewTreeViewModelStoreOwner(this@AnimationService)
            setViewTreeSavedStateRegistryOwner(this@AnimationService)
            
            setContent {
                CoolPerseusTheme {
                    val viewModel = remember { AnimationOverlayViewModel(settingsRepository) }
                    AnimationOverlayRoute(
                        type = type,
                        onAnimationFinished = {
                            stopSelf()
                        },
                        viewModel = viewModel
                    )
                }
            }
        }

        windowManager.addView(composeView, params)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.let {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
