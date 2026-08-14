package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.HistoryEntity
import com.example.data.model.Movie
import com.example.ui.components.CloudMoviesTopBar
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.HeroBanner
import com.example.ui.components.MovieRow
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorderSubtle
import com.example.ui.theme.EditorialCard
import com.example.ui.theme.EditorialError
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSurface
import com.example.ui.theme.EditorialTextMuted
import com.example.ui.theme.EditorialTextPrimary
import com.example.ui.theme.EditorialTextSecondary
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (Movie) -> Unit,
    onPlayMovie: (String, String?) -> Unit,
    onContinueHistoryClick: (HistoryEntity) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EditorialBackground,
        topBar = {
            CloudMoviesTopBar(
                serverStatus = uiState.serverStatus,
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = EditorialPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "MEMUAT CLOUDMOVIES...",
                        color = EditorialTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else if (uiState.errorMessage != null && uiState.homeMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Gagal Terhubung ke Server",
                        color = EditorialError,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.errorMessage ?: "Terjadi kesalahan.",
                        color = EditorialTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.loadAllData(isRefresh = true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EditorialPrimary,
                            contentColor = EditorialOnPrimary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Coba Lagi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("home_movie_list"),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // 1. Hero Featured Movie
                uiState.heroMovie?.let { hero ->
                    item(key = "hero_banner") {
                        HeroBanner(
                            movie = hero,
                            isWatchlisted = uiState.isHeroWatchlisted,
                            onPlayClick = { onPlayMovie(hero.cleanSlug, null) },
                            onWatchlistClick = { viewModel.toggleHeroWatchlist() },
                            onDetailsClick = { onMovieClick(hero) }
                        )
                    }
                }

                // 2. Genre Chips Row
                item(key = "genres_row") {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp)
                    ) {
                        items(uiState.genresList) { genre ->
                            val isSelected = uiState.selectedGenre == genre
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectGenre(genre) },
                                label = {
                                    Text(
                                        text = genre,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EditorialPrimary,
                                    selectedLabelColor = EditorialOnPrimary,
                                    containerColor = EditorialCard,
                                    labelColor = EditorialTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) EditorialPrimary else EditorialBorderSubtle
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("genre_chip_$genre")
                            )
                        }
                    }
                }

                // 3. Continue Watching / History Row (If exists)
                if (uiState.historyList.isNotEmpty()) {
                    item(key = "continue_watching_section") {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Text(
                                text = "Lanjutkan Menonton",
                                color = EditorialTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp)
                            ) {
                                items(uiState.historyList, key = { it.slug }) { history ->
                                    ContinueWatchingCard(
                                        history = history,
                                        onClick = { onContinueHistoryClick(history) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Filtered or Categorized Rows
                val filter = uiState.selectedGenre
                val filteredHome = if (filter == "Semua") uiState.homeMovies else uiState.homeMovies.filter { it.genres.contains(filter) }
                val filteredTrending = if (filter == "Semua") uiState.trendingMovies else uiState.trendingMovies.filter { it.genres.contains(filter) }
                val filteredSeries = if (filter == "Semua") uiState.seriesMovies else uiState.seriesMovies.filter { it.genres.contains(filter) }

                // 4. Rilis Terbaru (Home)
                item(key = "home_movies_row") {
                    MovieRow(
                        title = if (filter == "Semua") "Rilis Terbaru di LK21" else "Rilis Terbaru ($filter)",
                        subtitle = "Koleksi film kualitas HD & 4K terlengkap",
                        movies = filteredHome,
                        onMovieClick = onMovieClick
                    )
                }

                // 5. Sedang Populer & Tren (Trending)
                item(key = "trending_movies_row") {
                    Spacer(modifier = Modifier.height(14.dp))
                    MovieRow(
                        title = "Sedang Populer & Trending 🔥",
                        subtitle = "Film paling banyak ditonton minggu ini",
                        movies = filteredTrending,
                        onMovieClick = onMovieClick
                    )
                }

                // 6. Serial TV & Drama
                item(key = "series_movies_row") {
                    Spacer(modifier = Modifier.height(14.dp))
                    MovieRow(
                        title = "Serial TV & Drama Series 📺",
                        subtitle = "Episode lengkap update tercepat",
                        movies = filteredSeries,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }
    }
}
