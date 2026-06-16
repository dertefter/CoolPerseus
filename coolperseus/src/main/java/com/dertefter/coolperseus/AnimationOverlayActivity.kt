package com.dertefter.coolperseus

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AnimationOverlayActivity : ComponentActivity() {

    @Inject
    lateinit var cameraStateTracker: CameraStateTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = intent.getStringExtra("type") ?: ""


        when (intent.getStringExtra("type")) {
            "slider_down" -> {
                if (!cameraStateTracker.isCameraInUse) {
                    openCamera(frontCamera = true)
                }
            }

            "slider_up" -> {
                openCamera(frontCamera = false)
            }
        }


        val intent = Intent(this, AnimationService::class.java).apply {
            putExtra("type", type)
        }

        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to launch service: " + e.stackTraceToString())
        }
        finish()


    }
}


fun Context.openCamera(frontCamera: Boolean) {
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