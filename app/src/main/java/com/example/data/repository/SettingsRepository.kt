package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cloudmovies_prefs", Context.MODE_PRIVATE)

    companion object {
        // GANTI URL DI BAWAH INI DENGAN URL HASIL DEPLOY RAILWAY (misal: "https://nama-project-kamu.up.railway.app/")
        const val DEFAULT_BASE_URL = "https://ganti-dengan-url-railway-kamu.up.railway.app/" 
        const val KEY_BASE_URL = "base_url"
        const val KEY_DEFAULT_SERVER = "default_server"
        const val KEY_AUTOPLAY = "autoplay"
        const val KEY_SUBTITLE_SIZE = "subtitle_size"
    }

    private val _baseUrlState = MutableStateFlow(getBaseUrl())
    val baseUrlState: StateFlow<String> = _baseUrlState.asStateFlow()

    private val _defaultServerState = MutableStateFlow(getDefaultServer())
    val defaultServerState: StateFlow<String> = _defaultServerState.asStateFlow()

    fun getBaseUrl(): String {
        var url = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        if (!url.endsWith("/")) {
            url += "/"
        }
        return url
    }

    fun setBaseUrl(url: String) {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized += "/"
        }
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        _baseUrlState.value = normalized
    }

    fun getDefaultServer(): String {
        return prefs.getString(KEY_DEFAULT_SERVER, "cast") ?: "cast"
    }

    fun setDefaultServer(server: String) {
        prefs.edit().putString(KEY_DEFAULT_SERVER, server).apply()
        _defaultServerState.value = server
    }

    fun isAutoplayEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTOPLAY, true)
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
    }
}
