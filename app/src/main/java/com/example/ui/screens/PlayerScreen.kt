package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EpisodeInfo
import com.example.ui.components.QualityBadge
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
import com.example.ui.viewmodel.AspectRatioMode
import com.example.ui.viewmodel.PlayerViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Keep screen on during playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            viewModel.saveHistory()
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler {
        viewModel.saveHistory()
        onBackClick()
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        // Player Surface (Embedded Streaming Player)
        var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
        
        DisposableEffect(context) {
            val player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        viewModel.setPlaying(isPlaying)
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        viewModel.setBuffering(state == Player.STATE_BUFFERING)
                    }
                })
            }
            exoPlayer = player
            onDispose {
                player.release()
            }
        }
        
        LaunchedEffect(uiState.streamUrl, exoPlayer) {
            val targetUrl = if (uiState.streamUrl.isNotBlank()) uiState.streamUrl else ""
            if (targetUrl.isNotBlank() && !uiState.isIframeStream && exoPlayer != null) {
                exoPlayer?.setMediaItem(MediaItem.fromUri(targetUrl))
                exoPlayer?.prepare()
            }
        }

        if (uiState.isIframeStream) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) { viewModel.setBuffering(false) }
                        }
                        webChromeClient = object : WebChromeClient() {}
                        setBackgroundColor(android.graphics.Color.BLACK)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    val url = if (uiState.iframeUrl.isNotBlank()) uiState.iframeUrl else uiState.playWebUrl
                    if (webView.url != url && url.isNotBlank()) webView.loadUrl(url)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { view ->
                    view.resizeMode = when (uiState.aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gesture Overlay (Taps, Double Tap for seeking, Controls Toggle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val screenWidth = size.width
                    detectTapGestures(
                        onTap = {
                            viewModel.toggleControls()
                        },
                        onDoubleTap = { offset ->
                            if (offset.x < screenWidth * 0.4f) {
                                viewModel.triggerSeekFeedback(isForward = false)
                                exoPlayer?.let { it.seekTo(it.currentPosition - 10000) } ?: webViewRef?.evaluateJavascript("if(window.player) player.currentTime -= 10;", null)
                            } else if (offset.x > screenWidth * 0.6f) {
                                viewModel.triggerSeekFeedback(isForward = true)
                                exoPlayer?.let { it.seekTo(it.currentPosition + 10000) } ?: webViewRef?.evaluateJavascript("if(window.player) player.currentTime += 10;", null)
                            } else {
                                viewModel.togglePlayPause()
                            }
                        }
                    )
                }
        )

        // Double Tap Left Feedback Ripple (-10s)
        AnimatedVisibility(
            visible = uiState.isDoubleTapLeftActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0x99141218)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "-10s",
                        tint = EditorialPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("-10s", color = EditorialPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Double Tap Right Feedback Ripple (+10s)
        AnimatedVisibility(
            visible = uiState.isDoubleTapRightActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0x99141218)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "+10s",
                        tint = EditorialPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("+10s", color = EditorialPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Buffering Indicator
        if (uiState.isBuffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x99141218)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EditorialPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = uiState.areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xCC141218),
                            0.2f to Color(0x44141218),
                            0.8f to Color(0x44141218),
                            1f to Color(0xEE141218)
                        )
                    )
            ) {
                // 1. Top Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.saveHistory()
                                onBackClick()
                            },
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = uiState.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Server: ${uiState.currentServer.uppercase()}",
                                color = EditorialPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!uiState.isScreenLocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Server Switch Button
                            IconButton(onClick = { viewModel.toggleServerDialog(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Switch Server",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Speed Button
                            IconButton(onClick = { viewModel.toggleSpeedDialog(true) }) {
                                Icon(
                                    imageVector = Icons.Default.SlowMotionVideo,
                                    contentDescription = "Speed",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Quality Button
                            IconButton(onClick = { viewModel.toggleQualityDialog(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Hd,
                                    contentDescription = "Quality",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Episodes Dialog (if episodes available)
                            if (uiState.availableEpisodes.isNotEmpty()) {
                                IconButton(onClick = { viewModel.toggleEpisodeDialog(true) }) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = "Episodes",
                                        tint = EditorialPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Center Play / Pause & Skip 10s Controls
                if (!uiState.isScreenLocked) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x55141218))
                                .clickable {
                                    viewModel.triggerSeekFeedback(false)
                                    exoPlayer?.let { it.seekTo(it.currentPosition - 10000) } ?: webViewRef?.evaluateJavascript("if(window.player) player.currentTime -= 10;", null)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "-10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Play/Pause Main Lilac Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(EditorialPrimary)
                                .clickable {
                                    viewModel.togglePlayPause()
                                    if (uiState.isPlaying) {
                                        exoPlayer?.pause() ?: webViewRef?.evaluateJavascript("if(window.player) player.pause();", null)
                                    } else {
                                        exoPlayer?.play() ?: webViewRef?.evaluateJavascript("if(window.player) player.play();", null)
                                    }
                                }
                                .testTag("player_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = EditorialOnPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Forward 10s
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x55141218))
                                .clickable {
                                    viewModel.triggerSeekFeedback(true)
                                    exoPlayer?.let { it.seekTo(it.currentPosition + 10000) } ?: webViewRef?.evaluateJavascript("if(window.player) player.currentTime += 10;", null)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "+10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // 3. Bottom Controls & Progress Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (!uiState.isScreenLocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QualityBadge(quality = uiState.selectedResolution)
                                Text(
                                    text = "CloudMovies Stream Engine",
                                    color = EditorialTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Aspect Ratio toggle
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x44FFFFFF))
                                        .clickable { viewModel.cycleAspectRatio() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = uiState.aspectRatioMode.label,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Screen lock toggle
                                IconButton(onClick = { viewModel.toggleScreenLock() }) {
                                    Icon(
                                        imageVector = if (uiState.isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Lock",
                                        tint = if (uiState.isScreenLocked) EditorialPrimary else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Screen Locked banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xCC141218))
                                    .border(1.dp, EditorialBorderSubtle, RoundedCornerShape(20.dp))
                                    .clickable { viewModel.toggleScreenLock() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Unlock",
                                        tint = EditorialPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Layar Terkunci (Ketuk untuk Buka)",
                                        color = EditorialPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Server Switcher Dialog
        if (uiState.showServerDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleServerDialog(false) },
                title = {
                    Text("PILIH SERVER STREAM", color = EditorialTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.availableServers.forEach { s ->
                            val isSelected = uiState.currentServer.equals(s, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EditorialPrimary else EditorialCard)
                                    .clickable { viewModel.switchServer(s) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Server ${s.uppercase()}",
                                    color = if (isSelected) EditorialOnPrimary else EditorialTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = EditorialOnPrimary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleServerDialog(false) }) {
                        Text("Tutup", color = EditorialPrimary)
                    }
                },
                containerColor = EditorialSurface
            )
        }

        // Speed Dialog
        if (uiState.showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleSpeedDialog(false) },
                title = { Text("KECEPATAN PUTAR", color = EditorialTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val isSelected = uiState.playbackSpeed == speed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EditorialPrimary else EditorialCard)
                                    .clickable {
                                        viewModel.setPlaybackSpeed(speed)
                                        exoPlayer?.setPlaybackSpeed(speed) ?: webViewRef?.evaluateJavascript("if(window.player) player.playbackRate = $speed;", null)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${speed}x ${if (speed == 1.0f) "(Normal)" else ""}",
                                    color = if (isSelected) EditorialOnPrimary else EditorialTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EditorialOnPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleSpeedDialog(false) }) {
                        Text("Tutup", color = EditorialPrimary)
                    }
                },
                containerColor = EditorialSurface
            )
        }

        // Quality Dialog
        if (uiState.showQualityDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleQualityDialog(false) },
                title = { Text("KUALITAS VIDEO", color = EditorialTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.availableResolutions.forEach { res ->
                            val isSelected = uiState.selectedResolution == res
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EditorialPrimary else EditorialCard)
                                    .clickable { viewModel.setResolution(res) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = res,
                                    color = if (isSelected) EditorialOnPrimary else EditorialTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EditorialOnPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleQualityDialog(false) }) {
                        Text("Tutup", color = EditorialPrimary)
                    }
                },
                containerColor = EditorialSurface
            )
        }

        // Episode List Dialog
        if (uiState.showEpisodeDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleEpisodeDialog(false) },
                title = { Text("PILIH EPISODE", color = EditorialTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.availableEpisodes) { ep ->
                            val isSelected = uiState.currentEpisode?.cleanSlug == ep.cleanSlug
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EditorialPrimary else EditorialCard)
                                    .clickable { viewModel.selectEpisode(ep) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ep.title,
                                    color = if (isSelected) EditorialOnPrimary else EditorialTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSelected) Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EditorialOnPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleEpisodeDialog(false) }) {
                        Text("Tutup", color = EditorialPrimary)
                    }
                },
                containerColor = EditorialSurface
            )
        }
    }
}
