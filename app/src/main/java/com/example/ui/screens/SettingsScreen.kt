package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorderSubtle
import com.example.ui.theme.EditorialCard
import com.example.ui.theme.EditorialGreen
import com.example.ui.theme.EditorialGold
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSecondary
import com.example.ui.theme.EditorialSurface
import com.example.ui.theme.EditorialTextMuted
import com.example.ui.theme.EditorialTextPrimary
import com.example.ui.theme.EditorialTextSecondary
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EditorialBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .testTag("settings_screen"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item(key = "settings_header") {
                Column {
                    Text(
                        text = "PENGATURAN",
                        color = EditorialTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Konfigurasi server API, streaming engine, dan tampilan",
                        color = EditorialTextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Server Engine Status Card
            item(key = "server_status_card") {
                val isOnline = uiState.serverStatus.status.equals("ONLINE", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(EditorialCard)
                        .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) EditorialGreen else EditorialGold)
                                )
                                Text(
                                    text = if (isOnline) "SERVER ONLINE" else "OFFLINE / STANDALONE",
                                    color = if (isOnline) EditorialGreen else EditorialGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { viewModel.checkStatus() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EditorialPrimary,
                                    contentColor = EditorialOnPrimary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !uiState.isTestingConnection,
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (uiState.isTestingConnection) {
                                    CircularProgressIndicator(
                                        color = EditorialOnPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Test",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (!uiState.testResultMessage.isNullOrBlank()) {
                            Text(
                                text = uiState.testResultMessage!!,
                                color = EditorialTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Engine: ${uiState.serverStatus.engine.ifBlank { "CloudMovies Hybrid" }}",
                                color = EditorialTextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Target: ${uiState.serverStatus.active_target_domain.ifBlank { "lk21official.cc" }}",
                                color = EditorialTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // API Endpoint Settings
            item(key = "api_endpoint_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "URL SERVER API (app.js)",
                        color = EditorialTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = uiState.baseUrl,
                        onValueChange = { viewModel.onBaseUrlChange(it) },
                        placeholder = { Text("https://cloudmovies-api-ashy.vercel.app", color = EditorialTextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = EditorialSurface,
                            unfocusedContainerColor = EditorialSurface,
                            focusedBorderColor = EditorialPrimary,
                            unfocusedBorderColor = EditorialBorderSubtle,
                            focusedTextColor = EditorialTextPrimary,
                            unfocusedTextColor = EditorialTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("api_url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveBaseUrl() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EditorialPrimary,
                                contentColor = EditorialOnPrimary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simpan & Hubungkan", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Presets
                    Text(
                        text = "Preset Cepat:",
                        color = EditorialTextMuted,
                        fontSize = 11.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "Vercel Cloud" to "https://cloudmovies-api-ashy.vercel.app",
                            "Local Dev" to "http://10.0.2.2:3000"
                        ).forEach { (name, url) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EditorialCard)
                                    .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectPresetUrl(url) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = name, color = EditorialPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Default Stream Server Section
            item(key = "default_server_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SERVER STREAMING DEFAULT",
                        color = EditorialTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("cast", "p2p", "turbovip", "hydrax").forEach { server ->
                            val isSelected = uiState.defaultServer.equals(server, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) EditorialPrimary else EditorialCard)
                                    .border(1.dp, if (isSelected) EditorialPrimary else EditorialBorderSubtle, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setDefaultServer(server) }
                                    .padding(vertical = 10.dp),
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

            // Preferences (Autoplay)
            item(key = "preferences_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(EditorialCard)
                        .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Putar Otomatis (Autoplay)",
                                color = EditorialTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mulai streaming otomatis saat membuka player",
                                color = EditorialTextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = uiState.isAutoplay,
                            onCheckedChange = { viewModel.setAutoplay(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EditorialOnPrimary,
                                checkedTrackColor = EditorialPrimary,
                                uncheckedThumbColor = EditorialTextMuted,
                                uncheckedTrackColor = EditorialSurface
                            )
                        )
                    }
                }
            }

            // About App & Theme
            item(key = "about_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(EditorialCard)
                        .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TENTANG CLOUDMOVIES",
                        color = EditorialTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "CloudMovies v2.4.0 — Editorial Aesthetic",
                        color = EditorialTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aplikasi streaming film modern dengan scraper LK21, Idlix, multi-server cast, dan pemutar video high-performance.",
                        color = EditorialTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
