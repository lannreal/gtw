package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Movie(
    @Json(name = "title") val title: String = "",
    @Json(name = "year") val year: String = "-",
    @Json(name = "poster") val poster: String = "",
    @Json(name = "rating") val rating: String = "-",
    @Json(name = "quality") val quality: String = "HD",
    @Json(name = "duration") val duration: String = "-",
    @Json(name = "genres") val genres: List<String> = emptyList(),
    @Json(name = "synopsis") val synopsis: String = "",
    @Json(name = "url") val url: String = ""
) {
    val cleanSlug: String
        get() = url.trim().removePrefix("/").removeSuffix("/").removeSuffix(".m3u8")
}

@JsonClass(generateAdapter = true)
data class MovieListResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "total") val total: Int? = 0,
    @Json(name = "movies") val movies: List<Movie> = emptyList(),
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamServerInfo(
    @Json(name = "server") val server: String = "",
    @Json(name = "resolutions") val resolutions: List<String> = emptyList(),
    @Json(name = "play_url") val play_url: String = "",
    @Json(name = "source_url") val source_url: String = ""
)

@JsonClass(generateAdapter = true)
data class EpisodeInfo(
    @Json(name = "url") val url: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "season") val season: String = "1"
) {
    val cleanSlug: String
        get() = url.trim().removePrefix("/").removeSuffix("/")
}

@JsonClass(generateAdapter = true)
data class MovieDetail(
    @Json(name = "title") val title: String = "",
    @Json(name = "year") val year: String = "-",
    @Json(name = "poster") val poster: String = "",
    @Json(name = "rating") val rating: String = "-",
    @Json(name = "quality") val quality: String = "HD",
    @Json(name = "duration") val duration: String = "-",
    @Json(name = "age_rating") val age_rating: String = "-",
    @Json(name = "genres") val genres: List<String> = emptyList(),
    @Json(name = "countries") val countries: List<String> = emptyList(),
    @Json(name = "directors") val directors: List<String> = emptyList(),
    @Json(name = "actors") val actors: List<String> = emptyList(),
    @Json(name = "synopsis") val synopsis: String = "",
    @Json(name = "streams") val streams: List<StreamServerInfo> = emptyList(),
    @Json(name = "episodes") val episodes: List<EpisodeInfo> = emptyList()
) {
    val isSeries: Boolean
        get() = episodes.isNotEmpty()
}

@JsonClass(generateAdapter = true)
data class MovieDetailResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: MovieDetail? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SessionResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "server") val server: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "iframe") val iframe: String? = null,
    @Json(name = "resolutions") val resolutions: List<String> = emptyList(),
    @Json(name = "title") val title: String? = null,
    @Json(name = "play_url") val play_url: String? = null,
    @Json(name = "stream_url") val stream_url: String? = null,
    @Json(name = "raw_url") val raw_url: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ServerStatus(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "status") val status: String = "OFFLINE",
    @Json(name = "engine") val engine: String = "",
    @Json(name = "active_target_domain") val active_target_domain: String = "",
    @Json(name = "uptime_seconds") val uptime_seconds: Long = 0,
    @Json(name = "memory_usage") val memory_usage: Long = 0
)
