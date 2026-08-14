package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryEntity
import com.example.data.model.Movie
import com.example.data.model.MovieDetail
import com.example.data.model.StreamServerInfo
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val slug: String = "",
    val movie: MovieDetail? = null,
    val isWatchlisted: Boolean = false,
    val selectedServer: String = "cast",
    val selectedSeason: String = "1",
    val historyProgress: HistoryEntity? = null,
    val similarMovies: List<Movie> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class DetailViewModel(
    private val repository: MovieRepository,
    private val slug: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState(slug = slug))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
        observeWatchlist()
        observeHistory()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val detail = repository.getMovieDetail(slug)
                val similar = repository.getTrendingMovies().filter { it.cleanSlug != slug }

                val defaultServer = if (detail.streams.isNotEmpty()) {
                    detail.streams.first().server
                } else "cast"

                _uiState.update {
                    it.copy(
                        movie = detail,
                        selectedServer = defaultServer,
                        similarMovies = similar,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat detail film: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            repository.isWatchlisted(slug).collectLatest { isWatchlisted ->
                _uiState.update { it.copy(isWatchlisted = isWatchlisted) }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.getHistoryFlow(slug).collectLatest { history ->
                _uiState.update { it.copy(historyProgress = history) }
            }
        }
    }

    fun selectServer(serverName: String) {
        _uiState.update { it.copy(selectedServer = serverName) }
    }

    fun selectSeason(season: String) {
        _uiState.update { it.copy(selectedSeason = season) }
    }

    fun toggleWatchlist() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            if (_uiState.value.isWatchlisted) {
                repository.removeFromWatchlist(slug)
            } else {
                repository.toggleWatchlist(movie, slug)
            }
        }
    }

    companion object {
        fun provideFactory(repository: MovieRepository, slug: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DetailViewModel(repository, slug) as T
                }
            }
    }
}
