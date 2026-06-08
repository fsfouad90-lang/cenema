package com.example.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Movie
import com.example.data.model.UserSession
import com.example.data.repository.MovieRepository
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.SubscriptionRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface MovieDetailsUiState {
    object Loading : MovieDetailsUiState
    data class Success(val movie: Movie, val userStatus: String) : MovieDetailsUiState
    data class Error(val message: String) : MovieDetailsUiState
}

data class MovieDetailsScreenState(
    val uiState: MovieDetailsUiState = MovieDetailsUiState.Loading,
    val showPaywallDialog: Boolean = false,
    val navigateToPlayer: Int? = null,
    val navigateToUpgrade: Boolean = false,
    val isFavorite: Boolean = false
)

class MovieDetailsViewModel(
    private val movieId: Int,
    private val movieRepository: MovieRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _screenState = MutableStateFlow(MovieDetailsScreenState())
    val screenState = _screenState.asStateFlow()

    init {
        loadMovieDetails()
        observeFavoriteStatus()
        observeSubscriptionStatus()
    }

    private fun getActiveUserId(): String {
        return when (val session = authRepository.currentUserSession.value) {
            is UserSession.LoggedIn -> session.userId
            else -> "guest_user"
        }
    }

    private fun observeFavoriteStatus() {
        viewModelScope.launch {
            movieRepository.isFavoriteExistsFlow(movieId).collect { isFav ->
                _screenState.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    private fun observeSubscriptionStatus() {
        viewModelScope.launch {
            subscriptionRepository.userStatus.collect { status ->
                val currentState = _screenState.value.uiState
                if (currentState is MovieDetailsUiState.Success) {
                    _screenState.update {
                        it.copy(uiState = MovieDetailsUiState.Success(currentState.movie, status))
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _screenState.value.uiState
        if (currentState is MovieDetailsUiState.Success) {
            viewModelScope.launch {
                val isFav = _screenState.value.isFavorite
                if (isFav) {
                    movieRepository.removeFavorite(movieId)
                } else {
                    movieRepository.addFavorite(currentState.movie)
                }
            }
        }
    }

    fun loadMovieDetails() {
        val currentLang = if (Locale.getDefault().language.startsWith("ar")) "ar-SA" else "en-US"
        _screenState.update { it.copy(uiState = MovieDetailsUiState.Loading) }

        viewModelScope.launch {
            try {
                val movie = movieRepository.getMovieDetails(movieId, currentLang)
                val status = subscriptionRepository.fetchSubscriptionStatus(getActiveUserId())

                _screenState.update {
                    it.copy(
                        uiState = MovieDetailsUiState.Success(movie, status)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _screenState.update {
                    it.copy(
                        uiState = MovieDetailsUiState.Error(
                            e.localizedMessage ?: "Failed to fetch movie details"
                        )
                    )
                }
            }
        }
    }

    fun onPlayClicked() {
        val currentState = _screenState.value.uiState
        if (currentState is MovieDetailsUiState.Success) {
            viewModelScope.launch {
                val status = subscriptionRepository.fetchSubscriptionStatus(getActiveUserId())
                if (status == "Premium") {
                    _screenState.update { it.copy(navigateToPlayer = movieId) }
                } else {
                    _screenState.update { it.copy(showPaywallDialog = true) }
                }
            }
        }
    }

    fun dismissPaywall() {
        _screenState.update { it.copy(showPaywallDialog = false) }
    }

    fun onUpgradeClicked() {
        _screenState.update {
            it.copy(
                showPaywallDialog = false,
                navigateToUpgrade = true
            )
        }
    }

    fun clearNavigation() {
        _screenState.update {
            it.copy(
                navigateToPlayer = null,
                navigateToUpgrade = false
            )
        }
    }

    class Factory(
        private val movieId: Int,
        private val movieRepo: MovieRepository,
        private val subRepo: SubscriptionRepository,
        private val authRepo: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MovieDetailsViewModel::class.java)) {
                return MovieDetailsViewModel(movieId, movieRepo, subRepo, authRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
