package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.HistoryEntity
import com.example.data.model.Movie
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorderSubtle
import com.example.ui.theme.EditorialCard
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSecondary
import com.example.ui.theme.EditorialSurfaceContainer
import com.example.ui.theme.EditorialTextMuted
import com.example.ui.theme.EditorialTextPrimary
import com.example.ui.theme.EditorialTextSecondary

@Composable
fun MovieRow(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    if (movies.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = EditorialTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.3).sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = EditorialTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 6.dp)
        ) {
            items(movies, key = { it.cleanSlug + it.title }) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    history: HistoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(210.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(EditorialCard)
            .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("continue_watching_${history.slug}")
    ) {
        // Thumbnail Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
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

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66141218))
            )

            // Center Play Icon Circle (Editorial Lilac)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EditorialPrimary)
                    .align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = EditorialOnPrimary,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center)
                )
            }

            // Server Tag at top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC1D1B20))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = history.server.uppercase(),
                    color = EditorialPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Progress Bar at Bottom (Editorial Lilac)
            LinearProgressIndicator(
                progress = { history.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = EditorialPrimary,
                trackColor = Color(0x3349454F),
            )
        }

        // Info
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = history.title,
                color = EditorialTextPrimary,
                fontSize = 13.sp,
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
            } else {
                val currentMins = history.currentPositionMs / 60000
                val totalMins = history.durationMs / 60000
                Text(
                    text = if (totalMins > 0) "$currentMins / $totalMins mnt" else "Lanjutkan menonton",
                    color = EditorialTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
