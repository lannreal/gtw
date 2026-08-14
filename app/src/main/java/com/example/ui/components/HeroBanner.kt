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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Movie
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialTextPrimary
import com.example.ui.theme.EditorialTextSecondary

@Composable
fun HeroBanner(
    movie: Movie,
    isWatchlisted: Boolean,
    onPlayClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(390.dp)
            .clip(RoundedCornerShape(32.dp))
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp))
            .clickable { onDetailsClick() }
            .testTag("hero_banner")
    ) {
        // High-res backdrop image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(movie.poster)
                .crossfade(true)
                .build(),
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Editorial Vignette & Gradient Shadows
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color(0x66141218),
                        0.75f to Color(0xEE141218),
                        1f to EditorialBackground
                    )
                )
        )

        // Side glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0x99141218),
                        0.6f to Color.Transparent,
                        1f to Color(0x66141218)
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // Genre Pills & Trending badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Editorial Lilac Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EditorialPrimary)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TRENDING",
                        color = EditorialOnPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (movie.genres.isNotEmpty()) {
                    Text(
                        text = "• ${movie.genres.take(2).joinToString(" & ")}",
                        color = EditorialTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Movie Title (Editorial bold italic uppercase)
            Text(
                text = movie.title.uppercase(),
                color = EditorialTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-0.5).sp,
                lineHeight = 32.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (movie.synopsis.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movie.synopsis,
                    color = EditorialTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Now Button (Editorial Lilac Pill)
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EditorialPrimary,
                        contentColor = EditorialOnPrimary
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = EditorialOnPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAY NOW",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Frosted Glass Circle Watchlist Toggle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable { onWatchlistClick() }
                        .testTag("hero_watchlist_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWatchlisted) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Watchlist",
                        tint = if (isWatchlisted) EditorialPrimary else EditorialTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
