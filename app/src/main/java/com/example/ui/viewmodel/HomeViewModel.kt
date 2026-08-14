package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryEntity
import com.example.data.model.Movie
import com.example.data.model.ServerStatus
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val serverStatus: ServerStatus = ServerStatus(),
    val heroMovie: Movie? = null,
    val isHeroWatchlisted: Boolean = false,
    val homeMovies: List<Movie> = emptyList(),
    val trendingMovies: List<Movie> = emptyList(),
    val seriesMovies: List<Movie> = emptyList(),
    val historyList: List<HistoryEntity> = emptyList(),
    val selectedGenre: String = "Semua",
    val genresList: List<String> = listOf("Semua", "Action", "Adventure", "Sci-Fi", "Drama", "Animation", "Comedy", "Horror", "Fantasy"),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.getAllHistory().collectLatest { history ->
                _uiState.update { it.copy(historyList = history) }
            }
        }
    }

    fun loadAllData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, errorMessage = null) }
            try {
                val status = repository.getServerStatus()
                val home = repository.getHomeMovies()
                val trending = repository.getTrendingMovies()
                val series = repository.getSeriesMovies()

                val hero = trending.firstOrNull() ?: home.firstOrNull()

                _uiState.update {
                    it.copy(
                        serverStatus = status,
                        heroMovie = hero,
                        homeMovies = home,
                        trendingMovies = trending,
                        seriesMovies = series,
                        isLoading = false,
                        isRefreshing = false
                    )
                }

                if (hero != null) {
                    repository.isWatchlisted(hero.cleanSlug).collectLatest { isWatchlisted ->
                        _uiState.update { it.copy(isHeroWatchlisted = isWatchlisted) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Gagal memuat tayangan."
                    )
                }
            }
        }
    }

    fun selectGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
    }

    fun toggleHeroWatchlist() {
        val hero = _uiState.value.heroMovie ?: return
        viewModelScope.launch {
            val isWatchlisted = _uiState.value.isHeroWatchlisted
            if (isWatchlisted) {
                repository.removeFromWatchlist(hero.cleanSlug)
            } else {
                repository.getMovieDetail(hero.cleanSlug).let { detail ->
                    repository.toggleWatchlist(detail, hero.cleanSlug)
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: MovieRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
