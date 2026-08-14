package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.EpisodeInfo
import com.example.data.model.Movie
import com.example.ui.components.MovieRow
import com.example.ui.components.QualityBadge
import com.example.ui.components.RatingBadge
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorderSubtle
import com.example.ui.theme.EditorialCard
import com.example.ui.theme.EditorialGold
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSecondary
import com.example.ui.theme.EditorialSurface
import com.example.ui.theme.EditorialTextMuted
import com.example.ui.theme.EditorialTextPrimary
import com.example.ui.theme.EditorialTextSecondary
import com.example.ui.viewmodel.DetailViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (String, String?, String) -> Unit, // slug, server, episodeSlug
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EditorialBackground
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
                        text = "MEMUAT DETAIL FILM...",
                        color = EditorialTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else if (uiState.movie == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Film Tidak Ditemukan",
                        color = EditorialTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.errorMessage ?: "Terjadi kesalahan saat memuat data.",
                        color = EditorialTextMuted,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EditorialPrimary,
                            contentColor = EditorialOnPrimary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Kembali", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val movie = uiState.movie!!
            val isSeries = movie.isSeries || movie.episodes.isNotEmpty()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("detail_screen_scroll"),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                // 1. Hero Backdrop Section
                item(key = "detail_hero") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(movie.poster)
                                .crossfade(true)
                                .build(),
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Vignette & gradient overlays
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.5f to Color(0x66141218),
                                        0.85f to Color(0xEE141218),
                                        1f to EditorialBackground
                                    )
                                )
                        )

                        // Top Back Button
                        Box(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(16.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x66141218))
                                .border(1.dp, EditorialBorderSubtle, CircleShape)
                                .clickable { onBackClick() }
                                .testTag("detail_back_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = EditorialTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Floating Title and Quick Badges on Backdrop Bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QualityBadge(quality = movie.quality)
                                RatingBadge(rating = movie.rating)

                                if (movie.year.isNotBlank()) {
                                    Text(
                                        text = movie.year,
                                        color = EditorialTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (movie.duration.isNotBlank()) {
                                    Text(
                                        text = "• ${movie.duration}",
                                        color = EditorialTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = movie.title.uppercase(),
                                color = EditorialTextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                letterSpacing = (-0.5).sp,
                                lineHeight = 32.sp
                            )
                        }
                    }
                }

                // 2. Play & Watchlist Action Controls
                item(key = "detail_actions") {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play Button (Lilac Pill)
                            Button(
                                onClick = {
                                    val epSlug = if (isSeries) movie.episodes.firstOrNull()?.cleanSlug else null
                                    onPlayClick(uiState.slug, uiState.selectedServer, epSlug ?: uiState.slug)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EditorialPrimary,
                                    contentColor = EditorialOnPrimary
                                ),
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("detail_play_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = EditorialOnPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSeries) "PUTAR EPISODE 1" else "PUTAR FILM SEKARANG",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Watchlist Circle Button
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(EditorialCard)
                                    .border(1.dp, EditorialBorderSubtle, CircleShape)
                                    .clickable { viewModel.toggleWatchlist() }
                                    .testTag("detail_watchlist_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isWatchlisted) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = "Watchlist",
                                    tint = if (uiState.isWatchlisted) EditorialPrimary else EditorialTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Continue progress banner if already watched before
                        uiState.historyProgress?.let { history ->
                            if (history.currentPositionMs > 1000) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(EditorialCard)
                                        .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(12.dp))
                                        .clickable {
                                            onPlayClick(uiState.slug, history.server, history.slug)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Lanjutkan dari menit ${history.currentPositionMs / 60000}",
                                            color = EditorialPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { history.progressPercentage },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp),
                                            color = EditorialPrimary,
                                            trackColor = Color(0x3349454F)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = EditorialPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Server Selector Section
                item(key = "server_selector") {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(
                            text = "PILIH SERVER STREAMING",
                            color = EditorialTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val servers = if (movie.streams.isNotEmpty()) movie.streams.map { it.server } else listOf("cast", "p2p", "turbovip", "hydrax")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            servers.forEach { server ->
                                val isSelected = uiState.selectedServer.equals(server, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) EditorialPrimary else EditorialCard)
                                        .border(1.dp, if (isSelected) EditorialPrimary else EditorialBorderSubtle, RoundedCornerShape(20.dp))
                                        .clickable { viewModel.selectServer(server) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("server_chip_$server"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = server.uppercase(),
                                        color = if (isSelected) EditorialOnPrimary else EditorialTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Series Episodes Section (if TV Series)
                if (isSeries && movie.episodes.isNotEmpty()) {
                    item(key = "episodes_section") {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            Text(
                                text = "DAFTAR EPISODE (${movie.episodes.size} Episode)",
                                color = EditorialTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            movie.episodes.forEach { ep ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(EditorialCard)
                                        .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(14.dp))
                                        .clickable {
                                            onPlayClick(uiState.slug, uiState.selectedServer, ep.cleanSlug)
                                        }
                                        .padding(14.dp)
                                        .testTag("episode_item_${ep.cleanSlug}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(EditorialPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = EditorialOnPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = ep.title,
                                                color = EditorialTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (ep.season.isNotBlank()) {
                                                Text(
                                                    text = "Season ${ep.season}",
                                                    color = EditorialTextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    QualityBadge(quality = "HD")
                                }
                            }
                        }
                    }
                }

                // 5. Synopsis & Information
                item(key = "detail_synopsis") {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(
                            text = "SINOPSIS",
                            color = EditorialTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = movie.synopsis.ifBlank { "Belum ada sinopsis untuk film ini." },
                            color = EditorialTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )

                        // Genres tags
                        if (movie.genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                movie.genres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EditorialCard)
                                            .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = EditorialTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Cast & Directors info
                        if (movie.directors.isNotEmpty() || movie.actors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            if (movie.directors.isNotEmpty()) {
                                Text(
                                    text = "Sutradara: ${movie.directors.joinToString(", ")}",
                                    color = EditorialTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            if (movie.actors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pemeran: ${movie.actors.take(5).joinToString(", ")}",
                                    color = EditorialTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 6. Similar Movies
                if (uiState.similarMovies.isNotEmpty()) {
                    item(key = "similar_movies") {
                        Spacer(modifier = Modifier.height(10.dp))
                        MovieRow(
                            title = "Film Serupa Rekomendasi",
                            subtitle = "Pilihan terbaik untuk ditonton selanjutnya",
                            movies = uiState.similarMovies,
                            onMovieClick = onMovieClick
                        )
                    }
                }
            }
        }
    }
}
