package com.example.data.api

import com.example.data.model.MovieDetailResponse
import com.example.data.model.MovieListResponse
import com.example.data.model.ServerStatus
import com.example.data.model.SessionResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CloudMoviesApi {

    @GET("api/status")
    suspend fun getStatus(): ServerStatus

    @GET("api/home")
    suspend fun getHome(): MovieListResponse

    @GET("api/trending")
    suspend fun getTrending(): MovieListResponse

    @GET("api/series")
    suspend fun getSeries(): MovieListResponse

    @GET("api/search")
    suspend fun search(@Query("q") query: String): MovieListResponse

    @GET("api/detail")
    suspend fun getDetail(@Query("url") url: String): MovieDetailResponse

    @GET("api/session/{slug}")
    suspend fun getSession(
        @Path("slug") slug: String,
        @Query("server") server: String? = null
    ): SessionResponse

    @GET("api/extract")
    suspend fun extractStream(
        @Query("url") url: String,
        @Query("slug") slug: String? = null,
        @Query("server") server: String? = null,
        @Query("title") title: String? = null
    ): SessionResponse
}
