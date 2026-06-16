package com.dertefter.coolperseus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                val navController = rememberNavController()
                val items = listOf(
                    NavigationItem(
                        label = stringResource(R.string.app_sounds),
                        iconRes = R.drawable.ic_music_note,
                        route = "sounds"
                    ),
                    NavigationItem(
                        label = stringResource(R.string.app_actions),
                        iconRes = R.drawable.ic_mobile_hand,
                        route = "actions"
                    )
                )

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(painterResource(item.iconRes), contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "sounds",
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        composable("sounds") {
                            SoundSelectionRoute()
                        }
                        composable("actions") {
                            ActionSelectionRoute()
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val iconRes: Int,
    val route: String
)
