package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorialGold
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSurfaceVariant
import com.example.ui.theme.EditorialTextPrimary

@Composable
fun RatingBadge(
    rating: String,
    modifier: Modifier = Modifier
) {
    if (rating.isBlank() || rating == "-") return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC141218))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = EditorialGold,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = rating,
            color = EditorialTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QualityBadge(
    quality: String,
    modifier: Modifier = Modifier
) {
    val q = quality.ifBlank { "HD" }
    val is4K = q.contains("4K", ignoreCase = true)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (is4K) EditorialPrimary else EditorialSurfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = q.uppercase(),
            color = if (is4K) EditorialOnPrimary else EditorialTextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}
