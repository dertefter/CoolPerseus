package com.dertefter.coolperseus.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.lifecycleScope
import com.dertefter.coolperseus.data.SettingsRepository
import com.dertefter.coolperseus.design.theme.CoolPerseusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetsActivity : ComponentActivity() {
    companion object {
        const val APPWIDGET_BIND_REQUEST_CODE = 103
        const val APPWIDGET_CONFIGURE_REQUEST_CODE = 104
        
        const val ACTION_SLIDER_EVENT = "com.dertefter.coolperseus.ACTION_SLIDER_EVENT"
        const val EXTRA_EVENT_TYPE = "event_type"
    }

    private val appWidgetHost by lazy { AppWidgetHost(applicationContext, 1024) }
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(applicationContext) }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val addedWidgetIds = mutableStateListOf<Int>()
    private var showPicker by mutableStateOf(false)
    private var pendingWidgetId: Int = -1
    
    private val sliderEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SLIDER_EVENT) {
                val eventType = intent.getStringExtra(EXTRA_EVENT_TYPE)
                if (eventType == "slider_up") {
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)

        val filter = IntentFilter(ACTION_SLIDER_EVENT)
        ContextCompat.registerReceiver(this, sliderEventReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        lifecycleScope.launch {
            val savedWidgets = settingsRepository.widgets.first()
            addedWidgetIds.clear()
            addedWidgetIds.addAll(savedWidgets)

            snapshotFlow { addedWidgetIds.toList() }
                .collectLatest {
                    settingsRepository.setWidgets(it)
                }
        }

        setContent {
            val widgetHeights by settingsRepository.widgetHeights.collectAsState(initial = emptyMap())
            CoolPerseusTheme() {
                WidgetHostScreen(
                    addedWidgetIds = addedWidgetIds,
                    widgetHeights = widgetHeights,
                    showPicker = showPicker,
                    onShowPickerChange = { showPicker = it },
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager,
                    onAddAppWidget = { addAppWidget(it) },
                    onHeightChange = { id, height ->
                        lifecycleScope.launch {
                            settingsRepository.setWidgetHeight(id, height)
                        }
                    },
                    packageManager = packageManager
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(sliderEventReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun addAppWidget(appWidgetProviderInfo: AppWidgetProviderInfo) {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val isAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            appWidgetProviderInfo.provider,
            null
        )

        if (isAllowed) {
            handleAppWidgetAddition(appWidgetId)
        } else {
            showAppWidgetBindDialog(
                appWidgetId,
                appWidgetProviderInfo.provider,
                appWidgetProviderInfo.profile
            )
        }
    }

    private fun completeAddAppWidgetView(appWidgetId: Int) {
        addedWidgetIds.add(appWidgetId)
    }

    private fun showAppWidgetBindDialog(
        appWidgetId: Int,
        provider: ComponentName,
        profile: UserHandle
    ) {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, profile)
        startActivityForResult(intent, APPWIDGET_BIND_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            .takeIf { it != null && it != -1 } ?: pendingWidgetId

        if (resultCode == RESULT_CANCELED) {
            if (appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
            pendingWidgetId = -1
            return
        }

        if (requestCode == APPWIDGET_BIND_REQUEST_CODE &&
            resultCode == RESULT_OK
        ) {
            handleAppWidgetAddition(appWidgetId)
        }

        if (requestCode == APPWIDGET_CONFIGURE_REQUEST_CODE &&
            resultCode == RESULT_OK
        ) {
            completeAddAppWidgetView(appWidgetId)
        }
        pendingWidgetId = -1
    }

    private fun handleAppWidgetAddition(appWidgetId: Int) {
        val appWidget = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (appWidget == null) {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
            return
        }
        val widgetFeatures =
            appWidget.widgetFeatures

        val configurationOptional =
            (widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0) &&
                    (widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0)

        if (appWidget.configure != null && !configurationOptional) {
            pendingWidgetId = appWidgetId
            try {
                appWidgetHost.startAppWidgetConfigureActivityForResult(
                    this,
                    appWidgetId,
                    0,
                    APPWIDGET_CONFIGURE_REQUEST_CODE,
                    null
                )
            } catch (e: Exception) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                pendingWidgetId = -1
            }
        } else {
            completeAddAppWidgetView(appWidgetId)
        }
    }
}
