package com.dertefter.coolperseus

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraStateTracker @Inject constructor(
    @ApplicationContext context: Context
) : CameraManager.AvailabilityCallback() {

    private val cameraManager =
        context.getSystemService(CameraManager::class.java)

    @Volatile
    var isCameraInUse = false
        private set

    init {
        cameraManager.registerAvailabilityCallback(this, null)
    }

    override fun onCameraUnavailable(cameraId: String) {
        isCameraInUse = true
    }

    override fun onCameraAvailable(cameraId: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            isCameraInUse = false
        }, 100)
    }
}