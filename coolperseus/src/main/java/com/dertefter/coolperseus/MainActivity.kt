package com.dertefter.coolperseus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dertefter.coolperseus.action_selection.ActionSelectionRoute
import dagger.hilt.android.AndroidEntryPoint
import com.dertefter.coolperseus.design.theme.CoolPerseusTheme
import com.dertefter.coolperseus.sound_selection.SoundSelectionRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            CoolPerseusTheme {
                SoundSelectionRoute({})
            }
        }
    }
}
