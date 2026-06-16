package org.lineageos.settings.device

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import com.android.internal.os.DeviceKeyHandler

@Suppress("unused")
class KeyHandler(private val mContext: Context) : DeviceKeyHandler {

    companion object {
        private const val KEYCODE_SLIDER_UP = 594
        private const val KEYCODE_SLIDER_DOWN = 595
        private const val KEYCODE_AI = 689

    }

    override fun handleKeyEvent(event: KeyEvent): KeyEvent? {

        Log.e("DeviceKeyHandler", "scanCode: ${event.scanCode} action: ${event.action}")

        val isMyScancode = event.scanCode in setOf(
            KEYCODE_SLIDER_UP,
            KEYCODE_SLIDER_DOWN,
            KEYCODE_AI
        )

        if (isMyScancode) {
            return event
        } else {

            if (event.action != KeyEvent.ACTION_DOWN) { return null }
            else {
                return when (event.scanCode){
                    KEYCODE_SLIDER_UP -> {
                        launchCoolPerseusOverlay("slider_up")
                        null
                    }
                    KEYCODE_SLIDER_DOWN -> {
                        launchCoolPerseusOverlay("slider_down")
                        null
                    }
                    KEYCODE_AI -> {
                        launchCoolPerseusOverlay("ai_button")
                        null
                    }
                    else -> event
                }
            }


        }
    }

    private fun launchCoolPerseusOverlay(type: String) {
        val intent = Intent().apply {
            setClassName(
                "com.dertefter.coolperseus",
                "com.dertefter.coolperseus.AnimationService"
            )
            putExtra("type", type)
        }

        try {
            mContext.stopService(intent)
            mContext.startService(intent)
        } catch (e: Exception) {
            Log.e("DeviceKeyHandler", "Failed to launch service: " + e.stackTraceToString())
        }
    }


}