package com.dertefter.coolperseus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dertefter.coolperseus.action_selection.ActionSelectionRoute
import com.dertefter.coolperseus.animation_selection.AnimationSelectionRoute
import com.dertefter.coolperseus.home.HomeRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Home : Screen
    @Serializable
    data object ActionSelection : Screen
    @Serializable
    data object AnimationSelection : Screen
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CoolPerseusTheme {
                val backStack = rememberNavBackStack(Screen.Home)

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<Screen.Home> {
                            HomeRoute(
                                navigateToActions = { backStack.add(Screen.ActionSelection) },
                                navigateToAnimations = { backStack.add(Screen.AnimationSelection) }
                            )
                        }
                        entry<Screen.ActionSelection> {
                            ActionSelectionRoute(
                                onNavigateBack = { backStack.removeLastOrNull() }
                            )
                        }
                        entry<Screen.AnimationSelection> {
                            AnimationSelectionRoute(
                                onNavigateBack = { backStack.removeLastOrNull() }
                            )
                        }
                    }
                )
            }
        }
    }
}
