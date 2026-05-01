package com.xsgrok2.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xsgrok2.app.data.preferences.AppPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val model: String = "grok-4.20-beta",
    val apiBaseUrl: String = "https://api.apiyi.com/v1",
    val writingStyle: String = "细腻生动",
    val fontSize: Int = 16,
    val nightMode: Boolean = false,
    val isSaved: Boolean = false
)

class SettingsViewModel(
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                apiKey = preferences.apiKey,
                model = preferences.model,
                apiBaseUrl = preferences.apiBaseUrl,
                writingStyle = preferences.writingStyle,
                fontSize = preferences.fontSize,
                nightMode = preferences.nightMode
            )
        }
    }

    fun updateApiKey(key: String) { _uiState.update { it.copy(apiKey = key, isSaved = false) } }
    fun updateModel(model: String) { _uiState.update { it.copy(model = model, isSaved = false) } }
    fun updateApiBaseUrl(url: String) { _uiState.update { it.copy(apiBaseUrl = url, isSaved = false) } }
    fun updateWritingStyle(style: String) { _uiState.update { it.copy(writingStyle = style, isSaved = false) } }
    fun updateFontSize(size: Int) { _uiState.update { it.copy(fontSize = size, isSaved = false) } }
    fun updateNightMode(enabled: Boolean) { _uiState.update { it.copy(nightMode = enabled, isSaved = false) } }

    fun saveSettings() {
        viewModelScope.launch {
            preferences.apiKey = _uiState.value.apiKey
            preferences.model = _uiState.value.model
            preferences.apiBaseUrl = _uiState.value.apiBaseUrl
            preferences.writingStyle = _uiState.value.writingStyle
            preferences.fontSize = _uiState.value.fontSize
            preferences.nightMode = _uiState.value.nightMode
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    class Factory(private val preferences: AppPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferences) as T
        }
    }
}
