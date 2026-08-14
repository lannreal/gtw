package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WatchlistTab(val title: String) {
    WATCHLIST("Daftar Tontonan"),
    HISTORY("Riwayat Menonton")
}

data class WatchlistUiState(
    val activeTab: WatchlistTab = WatchlistTab.WATCHLIST,
    val watchlistItems: List<WatchlistEntity> = emptyList(),
    val historyItems: List<HistoryEntity> = emptyList(),
    val isLoading: Boolean = false
)

class WatchlistViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.getAllWatchlist().collectLatest { list ->
                _uiState.update { it.copy(watchlistItems = list) }
            }
        }
        viewModelScope.launch {
            repository.getAllHistory().collectLatest { list ->
                _uiState.update { it.copy(historyItems = list) }
            }
        }
    }

    fun selectTab(tab: WatchlistTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun removeWatchlist(slug: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(slug)
        }
    }

    fun removeHistory(slug: String) {
        viewModelScope.launch {
            repository.deleteHistory(slug)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    companion object {
        fun provideFactory(repository: MovieRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WatchlistViewModel(repository) as T
                }
            }
    }
}
