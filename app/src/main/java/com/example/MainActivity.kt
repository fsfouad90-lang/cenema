package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.details.MovieDetailsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.watchlist.WatchlistScreen
import androidx.compose.ui.platform.LocalContext
import com.example.ui.player.PlayerScreen
import com.example.ui.upgrade.UpgradeScreen
import com.example.ui.auth.AuthScreen
import com.example.data.model.UserSession
import com.example.ui.theme.CinemaFlowTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CinemaFlowTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val app = context.applicationContext as CinemaApplication
                val movieRepository = app.movieRepository
                val authRepository = app.authRepository
                val subscriptionRepository = app.subscriptionRepository

                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(movieRepository))
                val userSessionState by authRepository.currentUserSession.collectAsState()
                var hasSkippedAuth by rememberSaveable { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    if (userSessionState is UserSession.LoggedOut && !hasSkippedAuth) {
                        AuthScreen(
                            onSkip = {
                                hasSkippedAuth = true
                            }
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onMovieClick = { movieId ->
                                        navController.navigate("details/$movieId")
                                    },
                                    onWatchlistClick = {
                                        navController.navigate("watchlist")
                                    },
                                    onSignOutClick = {
                                        coroutineScope.launch {
                                            authRepository.logout()
                                            hasSkippedAuth = false // Return to onboarding Auth screen
                                        }
                                    }
                                )
                            }

                            composable("watchlist") {
                                WatchlistScreen(
                                    onMovieClick = { movieId ->
                                        navController.navigate("details/$movieId")
                                    },
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(
                                route = "details/{movieId}",
                                arguments = listOf(
                                    navArgument("movieId") { type = NavType.IntType }
                                )
                            ) { backStackEntry ->
                                val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0

                                MovieDetailsScreen(
                                    movieId = movieId,
                                    onBack = {
                                        navController.popBackStack()
                                    },
                                    onNavigateToPlayer = { id ->
                                        navController.navigate("player/$id")
                                    },
                                    onNavigateToUpgrade = {
                                        navController.navigate("upgrade")
                                    }
                                )
                            }

                            composable(
                                route = "player/{movieId}",
                                arguments = listOf(
                                    navArgument("movieId") { type = NavType.IntType }
                                )
                            ) { backStackEntry ->
                                val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                                PlayerScreen(
                                    movieId = movieId,
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(route = "upgrade") {
                                UpgradeScreen(
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
