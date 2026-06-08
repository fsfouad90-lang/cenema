package com.example.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Movie
import com.example.data.repository.MovieRepository
import com.example.data.repository.MovieRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class HomeUiState(
    val isLoading: Boolean = false,
    val trending: List<Movie> = emptyList(),
    val action: List<Movie> = emptyList(),
    val comedy: List<Movie> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Movie> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
)

class HomeViewModel(private val repository: MovieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        loadMovies()
    }

    fun loadMovies() {
        val currentLang = if (Locale.getDefault().language.startsWith("ar")) "ar-SA" else "en-US"
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Fetch in parallel
                val trendingJob = repository.getTrendingMovies(currentLang)
                val actionJob = repository.getActionMovies(currentLang)
                val comedyJob = repository.getComedyMovies(currentLang)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        trending = trendingJob,
                        action = actionJob,
                        comedy = comedyJob,
                        error = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to fetch cinematic list"
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false,
                    searchError = null
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce for 300ms to preserve user bandwidth and endpoint limits
            kotlinx.coroutines.delay(300)
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            try {
                val currentLang = if (Locale.getDefault().language.startsWith("ar")) "ar-SA" else "en-US"
                val results = repository.searchMovies(query, currentLang)
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false,
                        searchError = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = e.localizedMessage ?: "Failed to perform search"
                    )
                }
            }
        }
    }

    // Helper function to resolve single movie details by ID sequentially
    fun findMovieById(id: Int): Movie? {
        val state = _uiState.value
        return state.trending.find { it.id == id }
            ?: state.action.find { it.id == id }
            ?: state.comedy.find { it.id == id }
            ?: state.searchResults.find { it.id == id }
    }

    class Factory(
        private val repository: MovieRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
