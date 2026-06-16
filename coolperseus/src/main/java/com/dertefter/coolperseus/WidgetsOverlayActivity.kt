package com.dertefter.coolperseus

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dertefter.coolperseus.design.theme.CoolPerseusTheme
import com.dertefter.coolperseus.widgets.Widgets
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class WidgetsOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        val type = intent.getStringExtra("type") ?: ""

        setContent {
            CoolPerseusTheme {

            }
        }
    }
}
