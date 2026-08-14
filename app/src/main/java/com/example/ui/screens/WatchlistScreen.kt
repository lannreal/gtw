package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.local.HistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.Movie
import com.example.ui.components.QualityBadge
import com.example.ui.components.RatingBadge
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorderSubtle
import com.example.ui.theme.EditorialCard
import com.example.ui.theme.EditorialError
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSecondary
import com.example.ui.theme.EditorialSurface
import com.example.ui.theme.EditorialTextMuted
import com.example.ui.theme.EditorialTextPrimary
import com.example.ui.theme.EditorialTextSecondary
import com.example.ui.viewmodel.WatchlistTab
import com.example.ui.viewmodel.WatchlistViewModel

@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onMovieClick: (Movie) -> Unit,
    onPlayClick: (String, String?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EditorialBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Screen Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "KOLEKSI SAYA",
                    color = EditorialTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Daftar tontonan favorit & riwayat pemutaran film",
                    color = EditorialTextMuted,
                    fontSize = 12.sp
                )
            }

            // Tab Selector (Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Watchlist Tab
                val isWatchlistSelected = uiState.activeTab == WatchlistTab.WATCHLIST
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isWatchlistSelected) EditorialPrimary else EditorialCard)
                        .border(1.dp, if (isWatchlistSelected) EditorialPrimary else EditorialBorderSubtle, RoundedCornerShape(20.dp))
                        .clickable { viewModel.selectTab(WatchlistTab.WATCHLIST) }
                        .padding(vertical = 10.dp)
                        .testTag("tab_watchlist"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tersimpan (${uiState.watchlistItems.size})",
                        color = if (isWatchlistSelected) EditorialOnPrimary else EditorialTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // History Tab
                val isHistorySelected = uiState.activeTab == WatchlistTab.HISTORY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isHistorySelected) EditorialPrimary else EditorialCard)
                        .border(1.dp, if (isHistorySelected) EditorialPrimary else EditorialBorderSubtle, RoundedCornerShape(20.dp))
                        .clickable { viewModel.selectTab(WatchlistTab.HISTORY) }
                        .padding(vertical = 10.dp)
                        .testTag("tab_history"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Riwayat (${uiState.historyItems.size})",
                        color = if (isHistorySelected) EditorialOnPrimary else EditorialTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Content List
            if (uiState.activeTab == WatchlistTab.WATCHLIST) {
                if (uiState.watchlistItems.isEmpty()) {
                    EmptyCollectionState(
                        title = "Belum Ada Film Tersimpan",
                        subtitle = "Tekan tombol '+' pada film favorit Anda untuk menyimpannya di sini.",
                        icon = Icons.Default.BookmarkBorder
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 115.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("watchlist_grid"),
                        contentPadding = PaddingValues(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.watchlistItems, key = { it.slug }) { item ->
                            WatchlistCard(
                                item = item,
                                onClick = {
                                    onMovieClick(
                                        Movie(
                                            title = item.title,
                                            poster = item.poster,
                                            rating = item.rating,
                                            quality = item.quality,
                                            year = item.year,
                                            genres = emptyList(),
                                            url = "/${item.slug}"
                                        )
                                    )
                                },
                                onRemove = { viewModel.removeWatchlist(item.slug) }
                            )
                        }
                    }
                }
            } else {
                if (uiState.historyItems.isEmpty()) {
                    EmptyCollectionState(
                        title = "Belum Ada Riwayat Nonton",
                        subtitle = "Film yang Anda putar akan otomatis tercatat perkembangannya di sini.",
                        icon = Icons.Default.History
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("history_list"),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.historyItems, key = { it.slug }) { history ->
                            HistoryItemCard(
                                history = history,
                                onClick = {
                                    onPlayClick(history.slug, history.server, history.slug)
                                },
                                onRemove = { viewModel.removeHistory(history.slug) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistCard(
    item: WatchlistEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(EditorialCard)
                .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(20.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.poster)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1f to Color(0xCC141218)
                        )
                    )
            )

            // Rating
            if (item.rating.isNotBlank() && item.rating != "-") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    RatingBadge(rating = item.rating)
                }
            }

            // Remove button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC141218))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = EditorialError,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = EditorialTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (item.year.isNotBlank() && item.year != "-") {
            Text(
                text = item.year,
                color = EditorialTextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    history: HistoryEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EditorialCard)
            .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Poster Box
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(105.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(history.poster)
                    .crossfade(true)
                    .build(),
                contentDescription = history.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(EditorialPrimary)
                    .align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = EditorialOnPrimary,
                    modifier = Modifier.size(16.dp).align(Alignment.Center)
                )
            }
        }

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = history.title,
                color = EditorialTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!history.episodeTitle.isNullOrBlank()) {
                Text(
                    text = history.episodeTitle,
                    color = EditorialSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val currentMins = history.currentPositionMs / 60000
            val totalMins = history.durationMs / 60000
            Text(
                text = if (totalMins > 0) "Berhenti di $currentMins / $totalMins mnt" else "Terakhir ditonton",
                color = EditorialTextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { history.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = EditorialPrimary,
                trackColor = Color(0x3349454F)
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Hapus Riwayat",
                tint = EditorialTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyCollectionState(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EditorialCard)
                    .border(1.dp, EditorialBorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EditorialPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = title,
                color = EditorialTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = EditorialTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
