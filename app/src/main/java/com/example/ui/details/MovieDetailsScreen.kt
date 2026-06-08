package com.example.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.CinemaApplication
import com.example.data.repository.MovieRepository
import com.example.data.model.Movie
import com.example.ui.theme.*

@Composable
fun MovieDetailsScreen(
    movieId: Int,
    onBack: () -> Unit,
    onNavigateToPlayer: (Int) -> Unit,
    onNavigateToUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    app: CinemaApplication = LocalContext.current.applicationContext as CinemaApplication,
    viewModel: MovieDetailsViewModel = viewModel(
        factory = MovieDetailsViewModel.Factory(
            movieId = movieId,
            movieRepo = app.movieRepository,
            subRepo = app.subscriptionRepository,
            authRepo = app.authRepository
        )
    )
) {
    val state by viewModel.screenState.collectAsState()

    // Handle single-time navigation actions
    LaunchedEffect(state.navigateToPlayer) {
        state.navigateToPlayer?.let { id ->
            onNavigateToPlayer(id)
            viewModel.clearNavigation()
        }
    }

    LaunchedEffect(state.navigateToUpgrade) {
        if (state.navigateToUpgrade) {
            onNavigateToUpgrade()
            viewModel.clearNavigation()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBackground)
    ) {
        when (val uiState = state.uiState) {
            is MovieDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = IndigoPrimary,
                        modifier = Modifier.testTag("details_loading_indicator")
                    )
                }
            }
            is MovieDetailsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.message,
                        color = CinemaTextLight,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.loadMovieDetails() },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text(text = stringResource(id = R.string.retry_button))
                    }
                }
            }
            is MovieDetailsUiState.Success -> {
                MovieDetailsContent(
                    movie = uiState.movie,
                    userStatus = uiState.userStatus,
                    isFavorite = state.isFavorite,
                    onBack = onBack,
                    onPlayClick = { viewModel.onPlayClicked() },
                    onFavoriteToggle = { viewModel.toggleFavorite() }
                )
            }
        }

        // Paywall Dialog Sheet
        AnimatedVisibility(
            visible = state.showPaywallDialog,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PaywallDialog(
                onDismiss = { viewModel.dismissPaywall() },
                onUpgrade = { viewModel.onUpgradeClicked() }
            )
        }
    }
}

@Composable
private fun MovieDetailsContent(
    movie: Movie,
    userStatus: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onPlayClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Immersive Backdrop Image Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.backdropUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dramatic cinematic dark backdrop gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    CinemaBackground
                                )
                            )
                        )
                )

                // Quick Play Icon Overlay (Touch target size > 48dp)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.9f))
                        .clickable { onPlayClick() }
                        .testTag("play_trailer_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Film",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Movie Metadata Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurpleAccent.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = movie.category.uppercase(),
                            color = PurpleAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Rating Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CinemaGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.rating_label, movie.rating),
                            color = CinemaTextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // User status helper (Free / Premium Indicator)
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (userStatus == "Premium") CinemaGold.copy(alpha = 0.15f)
                                else CinemaTextMuted.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = userStatus.uppercase(),
                            color = if (userStatus == "Premium") CinemaGold else CinemaTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Headline Title text
                Text(
                    text = movie.title,
                    color = CinemaTextLight,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp,
                    modifier = Modifier.testTag("details_movie_title")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Release Date label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CinemaTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.release_date_label, movie.releaseDate),
                        color = CinemaTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Play and Favorite Controls in high fidelity Row layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("play_movie_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndigoPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.play_button_label),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Save to Favorites Button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isFavorite) CinemaGold.copy(alpha = 0.15f) else CinemaSurface)
                            .testTag("favorite_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Save to Favorites",
                            tint = if (isFavorite) CinemaGold else CinemaTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = CinemaSurface, thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                // Movie ID details label
                Text(
                    text = stringResource(id = R.string.movie_id_label, movie.id),
                    color = IndigoPrimaryLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("details_movie_id")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Overview Header Label
                Text(
                    text = stringResource(id = R.string.overview_label),
                    color = CinemaTextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Main overview description
                Text(
                    text = movie.overview,
                    color = CinemaTextMuted,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.padding(bottom = 60.dp)
                )
            }
        }

        // Floated Back navigation arrow
        Box(
            modifier = Modifier
                .padding(20.dp)
                .statusBarsPadding()
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onBack() }
                .testTag("details_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Navigate Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PaywallDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CinemaGold,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(id = R.string.paywall_title),
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = CinemaTextLight
                )
            }
        },
        text = {
            Text(
                text = stringResource(id = R.string.paywall_message),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = CinemaTextMuted
            )
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("paywall_upgrade_button")
            ) {
                Text(
                    text = stringResource(id = R.string.upgrade_button),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("paywall_dismiss_button")
            ) {
                Text(
                    text = stringResource(id = R.string.dismiss),
                    color = CinemaTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.testTag("paywall_dialog_container")
    )
}
