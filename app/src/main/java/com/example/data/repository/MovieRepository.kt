package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.NativeScraper
import com.example.data.local.HistoryEntity
import com.example.data.local.MovieDao
import com.example.data.local.WatchlistEntity
import com.example.data.model.Movie
import com.example.data.model.MovieDetail
import com.example.data.model.ServerStatus
import com.example.data.model.SessionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MovieRepository(
    private val apiClient: ApiClient,
    private val movieDao: MovieDao,
    private val nativeScraper: NativeScraper
) {

    suspend fun getServerStatus(): ServerStatus = withContext(Dispatchers.IO) {
        try {
            apiClient.getApi().getStatus()
        } catch (e: Exception) {
            val activeTarget = nativeScraper.domainManager.getBaseUrl()
            ServerStatus(
                success = true,
                status = "ONLINE",
                engine = "100% Zero-Headless / Native Attestation Engine",
                active_target_domain = activeTarget
            )
        }
    }

    suspend fun getHomeMovies(): List<Movie> = withContext(Dispatchers.IO) {
        val native = nativeScraper.getHomeMovies()
        if (native.isNotEmpty()) return@withContext native
        try {
            apiClient.getApi().getHome().movies
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTrendingMovies(): List<Movie> = withContext(Dispatchers.IO) {
        val native = nativeScraper.getTrendingMovies()
        if (native.isNotEmpty()) return@withContext native
        try {
            apiClient.getApi().getTrending().movies
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesMovies(): List<Movie> = withContext(Dispatchers.IO) {
        val native = nativeScraper.getSeriesMovies()
        if (native.isNotEmpty()) return@withContext native
        try {
            apiClient.getApi().getSeries().movies
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val native = nativeScraper.searchMovies(query)
        if (native.isNotEmpty()) return@withContext native
        try {
            apiClient.getApi().search(query).movies
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovieDetail(slug: String): MovieDetail = withContext(Dispatchers.IO) {
        val cleanUrl = if (slug.startsWith("/")) slug else "/$slug"
        val native = nativeScraper.scrapeDetail(cleanUrl)
        if (native.title.isNotBlank()) return@withContext native
        try {
            apiClient.getApi().getDetail(cleanUrl).data ?: throw Exception("Not found")
        } catch (e: Exception) {
            nativeScraper.scrapeDetail(cleanUrl)
        }
    }

    suspend fun getSession(slug: String, server: String? = null): SessionResponse = withContext(Dispatchers.IO) {
        // 100% Native on-device extraction matching app.js (PRIMARY)
        val nativeSession = nativeScraper.getOrExtractMovieStream(slug, server ?: "")
        if (nativeSession != null && nativeSession.rawUrl != null) {
            return@withContext SessionResponse(
                success = true,
                slug = nativeSession.slug,
                server = nativeSession.server,
                type = nativeSession.type,
                iframe = nativeSession.iframe,
                resolutions = nativeSession.resolutions,
                title = nativeSession.title,
                stream_url = nativeSession.rawUrl,
                play_url = apiClient.getPlayUrl(slug, server)
            )
        }
        
        try {
            val res = apiClient.getApi().getSession(slug, server)
            if (res.success) return@withContext res
        } catch (e: Exception) {}

        SessionResponse(
            success = false,
                slug = slug,
                server = server ?: "cast",
                stream_url = apiClient.getFullStreamUrl(slug, server),
                play_url = apiClient.getPlayUrl(slug, server)
            )
    }

    // --- Watchlist DAO ---
    fun getAllWatchlist(): Flow<List<WatchlistEntity>> = movieDao.getAllWatchlist()

    fun isWatchlisted(slug: String): Flow<Boolean> = movieDao.isWatchlisted(slug)

    suspend fun toggleWatchlist(movie: MovieDetail, slug: String) = withContext(Dispatchers.IO) {
        val clean = slug.trim().removePrefix("/")
        val item = WatchlistEntity(
            slug = clean,
            title = movie.title,
            poster = movie.poster,
            year = movie.year,
            rating = movie.rating,
            quality = movie.quality
        )
        movieDao.insertWatchlist(item)
    }

    suspend fun removeFromWatchlist(slug: String) = withContext(Dispatchers.IO) {
        movieDao.deleteWatchlist(slug.trim().removePrefix("/"))
    }

    // --- History / Continue Watching DAO ---
    fun getAllHistory(): Flow<List<HistoryEntity>> = movieDao.getAllHistory()

    fun getHistoryFlow(slug: String): Flow<HistoryEntity?> = movieDao.getHistoryFlow(slug.trim().removePrefix("/"))

    suspend fun savePlaybackProgress(
        slug: String,
        title: String,
        poster: String,
        positionMs: Long,
        durationMs: Long,
        server: String = "cast",
        episodeTitle: String? = null,
        season: String? = null
    ) = withContext(Dispatchers.IO) {
        val clean = slug.trim().removePrefix("/")
        val entity = HistoryEntity(
            slug = clean,
            title = title,
            poster = poster,
            currentPositionMs = positionMs,
            durationMs = durationMs,
            server = server,
            episodeTitle = episodeTitle,
            season = season,
            lastWatchedAt = System.currentTimeMillis()
        )
        movieDao.saveHistory(entity)
    }

    suspend fun deleteHistory(slug: String) = withContext(Dispatchers.IO) {
        movieDao.deleteHistory(slug.trim().removePrefix("/"))
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        movieDao.clearAllHistory()
    }
}
