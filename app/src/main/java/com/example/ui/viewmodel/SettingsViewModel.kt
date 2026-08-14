package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ServerStatus
import com.example.data.repository.MovieRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = "",
    val defaultServer: String = "cast",
    val isAutoplay: Boolean = true,
    val serverStatus: ServerStatus = ServerStatus(),
    val isTestingConnection: Boolean = false,
    val testResultMessage: String? = null,
    val isSuccess: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            baseUrl = settingsRepository.getBaseUrl(),
            defaultServer = settingsRepository.getDefaultServer(),
            isAutoplay = settingsRepository.isAutoplayEnabled()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkStatus()
    }

    fun onBaseUrlChange(newUrl: String) {
        _uiState.update { it.copy(baseUrl = newUrl, testResultMessage = null) }
    }

    fun saveBaseUrl() {
        settingsRepository.setBaseUrl(_uiState.value.baseUrl)
        checkStatus()
    }

    fun selectPresetUrl(preset: String) {
        _uiState.update { it.copy(baseUrl = preset) }
        settingsRepository.setBaseUrl(preset)
        checkStatus()
    }

    fun setDefaultServer(server: String) {
        settingsRepository.setDefaultServer(server)
        _uiState.update { it.copy(defaultServer = server) }
    }

    fun setAutoplay(enabled: Boolean) {
        settingsRepository.setAutoplayEnabled(enabled)
        _uiState.update { it.copy(isAutoplay = enabled) }
    }

    fun checkStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testResultMessage = null) }
            val status = movieRepository.getServerStatus()
            val isOnline = status.status.equals("ONLINE", ignoreCase = true)
            _uiState.update {
                it.copy(
                    serverStatus = status,
                    isTestingConnection = false,
                    isSuccess = isOnline,
                    testResultMessage = if (isOnline) "Terhubung ke ${settingsRepository.getBaseUrl()} (${status.engine})" else "Server offline. Scraper direct fallback aktif."
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            movieRepository: MovieRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsRepository, movieRepository) as T
                }
            }
    }
}
