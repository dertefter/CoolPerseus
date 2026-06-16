package com.dertefter.coolperseus

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dertefter.coolperseus.design.theme.CoolPerseusTheme
import com.dertefter.coolperseus.overlay.AnimationOverlayRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnimationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = intent.getStringExtra("type") ?: ""

        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )


        enableEdgeToEdge()

        setContent {
            CoolPerseusTheme {
                AnimationOverlayRoute(
                    type = type,
                    onAnimationFinished = {
                        finish()
                        overrideActivityTransition(
                            OVERRIDE_TRANSITION_CLOSE,
                            0,
                            0
                            )
                    }
                )
            }
        }


    }
}