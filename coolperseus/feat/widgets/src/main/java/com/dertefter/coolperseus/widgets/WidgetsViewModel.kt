package com.dertefter.coolperseus.widgets

import androidx.lifecycle.ViewModel
import com.dertefter.coolperseus.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class WidgetsUiState(
    val isEditMode: Boolean = false,
    val draggedItemId: Int? = null,
    val resizingItemId: Int? = null,
)

@HiltViewModel
class WidgetsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {


}