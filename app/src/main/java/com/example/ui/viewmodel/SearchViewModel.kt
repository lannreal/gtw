package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Movie
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Movie> = emptyList(),
    val isSearching: Boolean = false,
    val selectedFilter: String = "Semua",
    val trendingSearches: List<String> = listOf("Deadpool", "Dune", "Avatar", "Kingdom", "Inside Out", "Oppenheimer", "Spider-Man", "House of Dragon"),
    val errorMessage: String? = null
)

class SearchViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Pre-load trending movies as initial search view
        loadTrendingInitial()
    }

    private fun loadTrendingInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val trending = repository.getTrendingMovies()
            _uiState.update { it.copy(results = trending, isSearching = false) }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            loadTrendingInitial()
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce 400ms
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            try {
                val results = repository.searchMovies(newQuery)
                _uiState.update { it.copy(results = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "Gagal mencari: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onSearchSuggestionClick(keyword: String) {
        onQueryChanged(keyword)
    }

    fun clearQuery() {
        onQueryChanged("")
    }

    companion object {
        fun provideFactory(repository: MovieRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(repository) as T
                }
            }
    }
}
