package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClient
import com.example.data.model.EpisodeInfo
import com.example.data.model.MovieDetail
import com.example.data.model.SessionResponse
import com.example.data.model.StreamServerInfo
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AspectRatioMode(val label: String) {
    FIT("Pas Layar (Fit)"),
    FILL("Penuhi Layar (Fill)"),
    ZOOM("Zoom 16:9")
}

data class PlayerUiState(
    val slug: String = "",
    val title: String = "",
    val poster: String = "",
    val currentServer: String = "cast",
    val availableServers: List<String> = listOf("cast", "p2p", "turbovip", "hydrax"),
    val streamUrl: String = "",
    val playWebUrl: String = "",
    val isIframeStream: Boolean = false,
    val iframeUrl: String = "",
    val availableResolutions: List<String> = listOf("Auto", "1080p", "720p", "480p"),
    val selectedResolution: String = "Auto",
    val availableEpisodes: List<EpisodeInfo> = emptyList(),
    val currentEpisode: EpisodeInfo? = null,
    val isPlaying: Boolean = true,
    val isBuffering: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val areControlsVisible: Boolean = true,
    val isScreenLocked: Boolean = false,
    val showSpeedDialog: Boolean = false,
    val showQualityDialog: Boolean = false,
    val showServerDialog: Boolean = false,
    val showEpisodeDialog: Boolean = false,
    val errorMessage: String? = null,
    val showCenterFeedback: String? = null, // "PLAY", "PAUSE", "+10s", "-10s"
    val isDoubleTapLeftActive: Boolean = false,
    val isDoubleTapRightActive: Boolean = false
)

class PlayerViewModel(
    private val repository: MovieRepository,
    private val apiClient: ApiClient,
    private val slug: String,
    private val initialServer: String = "cast",
    private val initialEpisodeSlug: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            slug = slug,
            currentServer = initialServer,
            title = slug.replace("-", " ").capitalizeWords()
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressTrackingJob: Job? = null
    private var controlsAutoTimerJob: Job? = null

    init {
        loadSessionAndDetail()
        startControlsTimer()
    }

    private fun loadSessionAndDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuffering = true, errorMessage = null) }
            val activeSlug = initialEpisodeSlug ?: slug

            try {
                val detail = repository.getMovieDetail(slug)
                val session = repository.getSession(activeSlug, _uiState.value.currentServer)

                val streamUrl = session.stream_url ?: ""
                val playUrl = session.play_url ?: apiClient.getPlayUrl(activeSlug, _uiState.value.currentServer)
                val isIframe = session.type == "iframe" || (streamUrl.isBlank() && !session.iframe.isNullOrBlank()) || streamUrl.contains("iframe")

                val servers = if (detail.streams.isNotEmpty()) {
                    detail.streams.map { it.server }
                } else listOf("cast", "p2p", "turbovip", "hydrax")

                val currentEp = detail.episodes.find { it.cleanSlug == activeSlug }

                _uiState.update {
                    it.copy(
                        title = currentEp?.title ?: detail.title.ifBlank { activeSlug.replace("-", " ") },
                        poster = detail.poster,
                        streamUrl = streamUrl,
                        playWebUrl = playUrl,
                        isIframeStream = isIframe,
                        iframeUrl = session.iframe ?: "",
                        availableServers = servers,
                        availableResolutions = if (session.resolutions.isNotEmpty()) listOf("Auto") + session.resolutions else listOf("Auto", "1080p", "720p", "480p"),
                        availableEpisodes = detail.episodes,
                        currentEpisode = currentEp,
                        isBuffering = false
                    )
                }

                // Check previous history progress to resume
                val history = repository.getHistoryFlow(activeSlug)
                // history will be handled by UI seeking if required
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        streamUrl = apiClient.getFullStreamUrl(activeSlug, _uiState.value.currentServer),
                        playWebUrl = apiClient.getPlayUrl(activeSlug, _uiState.value.currentServer),
                        isBuffering = false,
                        errorMessage = "Memuat pemutar: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun switchServer(serverName: String) {
        _uiState.update {
            it.copy(
                currentServer = serverName,
                showServerDialog = false,
                isBuffering = true
            )
        }
        loadSessionAndDetail()
    }

    fun selectEpisode(episode: EpisodeInfo) {
        _uiState.update {
            it.copy(
                currentEpisode = episode,
                title = episode.title,
                showEpisodeDialog = false,
                isBuffering = true
            )
        }
        loadSessionAndDetail()
    }

    fun setPlaybackPosition(posMs: Long, durMs: Long) {
        _uiState.update {
            it.copy(
                currentPositionMs = posMs,
                durationMs = durMs
            )
        }
    }

    fun setBuffering(isBuffering: Boolean) {
        _uiState.update { it.copy(isBuffering = isBuffering) }
    }

    fun setPlaying(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
        triggerCenterFeedback(if (isPlaying) "PLAY" else "PAUSE")
        if (isPlaying) {
            startControlsTimer()
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isScreenLocked) return
        val nextPlaying = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = nextPlaying) }
        triggerCenterFeedback(if (nextPlaying) "PLAY" else "PAUSE")
        if (nextPlaying) {
            startControlsTimer()
        }
    }

    fun triggerSeekFeedback(isForward: Boolean) {
        if (_uiState.value.isScreenLocked) return
        showControls()
        viewModelScope.launch {
            if (isForward) {
                _uiState.update { it.copy(isDoubleTapRightActive = true) }
                delay(400)
                _uiState.update { it.copy(isDoubleTapRightActive = false) }
            } else {
                _uiState.update { it.copy(isDoubleTapLeftActive = true) }
                delay(400)
                _uiState.update { it.copy(isDoubleTapLeftActive = false) }
            }
        }
    }

    private fun triggerCenterFeedback(action: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showCenterFeedback = action) }
            delay(350)
            _uiState.update { it.copy(showCenterFeedback = null) }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed, showSpeedDialog = false) }
    }

    fun setResolution(res: String) {
        _uiState.update { it.copy(selectedResolution = res, showQualityDialog = false) }
    }

    fun cycleAspectRatio() {
        if (_uiState.value.isScreenLocked) return
        val current = _uiState.value.aspectRatioMode
        val next = when (current) {
            AspectRatioMode.FIT -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.ZOOM
            AspectRatioMode.ZOOM -> AspectRatioMode.FIT
        }
        _uiState.update { it.copy(aspectRatioMode = next) }
    }

    fun toggleScreenLock() {
        val next = !_uiState.value.isScreenLocked
        _uiState.update { it.copy(isScreenLocked = next) }
        if (!next) {
            showControls()
        }
    }

    fun showControls() {
        _uiState.update { it.copy(areControlsVisible = true) }
        startControlsTimer()
    }

    fun toggleControls() {
        if (_uiState.value.isScreenLocked) {
            _uiState.update { it.copy(areControlsVisible = !it.areControlsVisible) }
            return
        }
        val next = !_uiState.value.areControlsVisible
        _uiState.update { it.copy(areControlsVisible = next) }
        if (next) {
            startControlsTimer()
        }
    }

    private fun startControlsTimer() {
        controlsAutoTimerJob?.cancel()
        controlsAutoTimerJob = viewModelScope.launch {
            delay(3500)
            if (_uiState.value.isPlaying &&
                !_uiState.value.showSpeedDialog &&
                !_uiState.value.showQualityDialog &&
                !_uiState.value.showServerDialog &&
                !_uiState.value.showEpisodeDialog
            ) {
                _uiState.update { it.copy(areControlsVisible = false) }
            }
        }
    }

    fun toggleSpeedDialog(show: Boolean) {
        _uiState.update { it.copy(showSpeedDialog = show) }
        if (!show) startControlsTimer()
    }

    fun toggleQualityDialog(show: Boolean) {
        _uiState.update { it.copy(showQualityDialog = show) }
        if (!show) startControlsTimer()
    }

    fun toggleServerDialog(show: Boolean) {
        _uiState.update { it.copy(showServerDialog = show) }
        if (!show) startControlsTimer()
    }

    fun toggleEpisodeDialog(show: Boolean) {
        _uiState.update { it.copy(showEpisodeDialog = show) }
        if (!show) startControlsTimer()
    }

    fun saveHistory() {
        val state = _uiState.value
        if (state.durationMs > 0 && state.currentPositionMs > 1000) {
            viewModelScope.launch {
                repository.savePlaybackProgress(
                    slug = state.slug,
                    title = state.title,
                    poster = state.poster,
                    positionMs = state.currentPositionMs,
                    durationMs = state.durationMs,
                    server = state.currentServer,
                    episodeTitle = state.currentEpisode?.title,
                    season = state.currentEpisode?.season
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveHistory()
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    companion object {
        fun provideFactory(
            repository: MovieRepository,
            apiClient: ApiClient,
            slug: String,
            server: String = "cast",
            episodeSlug: String? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayerViewModel(repository, apiClient, slug, server, episodeSlug) as T
                }
            }
    }
}
