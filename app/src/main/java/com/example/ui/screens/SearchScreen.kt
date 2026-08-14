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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Movie
import com.example.ui.components.MovieCard
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorderSubtle
import com.example.ui.theme.EditorialCard
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSurface
import com.example.ui.theme.EditorialTextMuted
import com.example.ui.theme.EditorialTextPrimary
import com.example.ui.theme.EditorialTextSecondary
import com.example.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onMovieClick: (Movie) -> Unit,
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
            // Search Input Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Cari judul film, anime, atau aktor...",
                            color = EditorialTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = if (uiState.query.isNotBlank()) EditorialPrimary else EditorialTextMuted
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearQuery() }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = EditorialTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EditorialSurface,
                        unfocusedContainerColor = EditorialSurface,
                        focusedBorderColor = EditorialPrimary,
                        unfocusedBorderColor = EditorialBorderSubtle,
                        focusedTextColor = EditorialTextPrimary,
                        unfocusedTextColor = EditorialTextPrimary,
                        cursorColor = EditorialPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input")
                )
            }

            // Quick Trending Suggestions Pills (when query is empty)
            if (uiState.query.isBlank()) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    Text(
                        text = "PENCARIAN POPULER 🔥",
                        color = EditorialTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.trendingSearches.forEach { keyword ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(EditorialCard)
                                    .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(20.dp))
                                    .clickable { viewModel.onSearchSuggestionClick(keyword) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                    .testTag("search_tag_$keyword")
                            ) {
                                Text(
                                    text = keyword,
                                    color = EditorialTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Rekomendasi Untuk Anda",
                        color = EditorialTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Results count or Loading indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.query.isNotBlank()) {
                    Text(
                        text = "Hasil untuk \"${uiState.query}\" (${uiState.results.size} film)",
                        color = EditorialTextMuted,
                        fontSize = 12.sp
                    )
                }

                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        color = EditorialPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Movies Grid
            if (uiState.results.isEmpty() && !uiState.isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = EditorialTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Tidak Ada Film Ditemukan",
                            color = EditorialTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Coba gunakan kata kunci lain seperti judul bahasa Inggris atau aktor.",
                            color = EditorialTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 115.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search_results_grid"),
                    contentPadding = PaddingValues(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.results, key = { it.cleanSlug + it.title }) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) },
                            width = 115
                        )
                    }
                }
            }
        }
    }
}
