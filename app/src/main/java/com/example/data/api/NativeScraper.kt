package com.example.data.api

import android.util.Log
import com.example.data.model.EpisodeInfo
import com.example.data.model.Movie
import com.example.data.model.MovieDetail
import com.example.data.model.StreamServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class StreamSession(
    val type: String = "m3u8",
    val slug: String,
    val server: String,
    val rawUrl: String? = null,
    val iframe: String? = null,
    val resolutions: List<String> = listOf("1080p", "720p", "480p"),
    val referer: String = "",
    val title: String = "",
    val poster: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

class NativeScraper(private val okHttpClient: OkHttpClient) {

    private val TAG = "NativeScraper"
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val CF_CLEARANCE_API = "5FCiBEn8.ACfagSDoiZM9dvxMaQu6WfIZq5ReSzU4Y4-1783865133-1.2.1.1-iAcNmr7CRzYzKAapOLdHjS3UjBv2FDKZRE8eJoWoPcdZREZQamOxzZU.S.ZpKWlzyUXDsxhPVF1OF7ySkV7q3FqxSuE411ufYxffuWklWsKXjAAPr.NUUvniJ2ekBaDVRcVC0syhvmA2oS3ORyMTVnsQaLJjmgrQ9KwhDRWhahAhjNuJL4YslOSGxBUUq2zsA1gQMdU2ZMsL3X7MdS4ljqQIoZPQdFFBiWMJjQVMKQrF.PVb3aNxA1myJCcDWowQbafwwTbuVrmuYz0mpMk6LpwkF8aQfV9x0Qwn4AUll9beI3V_ngD7Jd2VqrjQEAsfMwvZ3jY2Ufn7.ncq1Q47yQ"

    val domainManager = DomainManager(okHttpClient)
    private val streamSessions = mutableMapOf<String, StreamSession>()

    // ==========================================
    // DYNAMIC DOMAIN AUTO-DETECTOR & HEALER
    // ==========================================
    class DomainManager(private val client: OkHttpClient) {
        @Volatile
        var activeBase: String = "https://tv12.lk21official.cc"
        
        private val mirrors = mutableListOf(
            "https://tv12.lk21official.cc",
            "https://tv11.lk21official.cc",
            "https://tv10.lk21official.cc",
            "https://tv13.lk21official.cc",
            "https://tv14.lk21official.cc",
            "https://tv15.lk21official.cc",
            "https://lk21official.biz",
            "https://lk21official.co",
            "https://lk21official.site"
        )
        
        private val mutex = Mutex()
        @Volatile
        private var isDetecting = false

        suspend fun testCandidate(candidateUrl: String): String? = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(candidateUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .build()
                
                val testClient = client.newBuilder()
                    .connectTimeout(4500, TimeUnit.MILLISECONDS)
                    .readTimeout(4500, TimeUnit.MILLISECONDS)
                    .followRedirects(true)
                    .build()
                    
                testClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val finalUrl = response.request.url.toString()
                        val uri = URI(finalUrl)
                        val baseOrigin = "${uri.scheme}://${uri.host}"
                        
                        if (body.contains("poster-title") || body.contains("grid-archive") || 
                            body.contains("item") || body.contains("lk21")) {
                            return@withContext baseOrigin
                        }
                    }
                }
            } catch (e: Exception) {}
            null
        }

        suspend fun detectActiveDomain(): String = withContext(Dispatchers.IO) {
            mutex.withLock {
                if (isDetecting) return@withContext activeBase
                isDetecting = true
            }

            try {
                val currentWorking = testCandidate(activeBase)
                if (currentWorking != null) {
                    activeBase = currentWorking
                    isDetecting = false
                    return@withContext activeBase
                }

                val candidates = mirrors.toMutableList()
                for (i in 10..30) {
                    val m = "https://tv$i.lk21official.cc"
                    if (!candidates.contains(m)) candidates.add(m)
                }

                val working = coroutineScope {
                    candidates.map { candidate ->
                        async { testCandidate(candidate) }
                    }.awaitAll().firstOrNull { it != null }
                }

                if (working != null) {
                    activeBase = working
                    Log.d("DomainManager", "✅ Domain aktif baru berhasil terpasang: $activeBase")
                }
            } catch (e: Exception) {}
            
            isDetecting = false
            return@withContext activeBase
        }

        fun getBaseUrl(): String = activeBase
    }

    private fun normalizeSlug(slug: String?): String {
        if (slug.isNullOrBlank()) return ""
        return slug.trim().removePrefix("/").removeSuffix("/").replace(Regex("\\.m3u8$"), "")
    }

    private fun saveSession(session: StreamSession): StreamSession {
        val slug = normalizeSlug(session.slug)
        val server = (session.server).lowercase()
        
        if (slug.isNotBlank()) {
            streamSessions["$slug?server=$server"] = session
            streamSessions["$slug/$server"] = session
            streamSessions["$slug-$server"] = session
            if (!streamSessions.containsKey(slug) || server == "cast") {
                streamSessions[slug] = session
            }
        }
        
        if (streamSessions.size > 500) {
            val now = System.currentTimeMillis()
            val keysToRemove = streamSessions.filter { now - it.value.createdAt > 24 * 3600 * 1000 }.keys
            keysToRemove.forEach { streamSessions.remove(it) }
        }
        return session
    }

    private suspend fun getJsoup(urlPath: String, retryCount: Int = 0): Document = withContext(Dispatchers.IO) {
        val currentBase = domainManager.getBaseUrl()
        val fullUrl = if (urlPath.startsWith("http")) urlPath else "${currentBase}${if (urlPath.startsWith("/")) "" else "/"}$urlPath"
        
        try {
            val request = Request.Builder()
                .url(fullUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Referer", "$currentBase/")
                .build()
                
            val client = okHttpClient.newBuilder()
                .connectTimeout(12000, TimeUnit.MILLISECONDS)
                .readTimeout(12000, TimeUnit.MILLISECONDS)
                .build()
                
            var response = client.newCall(request).execute()
            if (response.isRedirect) {
                val loc = response.header("Location")
                if (loc != null) {
                    val newReq = request.newBuilder().url(loc).build()
                    response = client.newCall(newReq).execute()
                }
            }
            
            if (response.isSuccessful) {
                val finalUrl = response.request.url.toString()
                val uri = URI(finalUrl)
                val finalOrigin = "${uri.scheme}://${uri.host}"
                if (finalOrigin != currentBase && finalOrigin.contains("lk21")) {
                    domainManager.activeBase = finalOrigin
                }
            }
            
            val html = response.body?.string() ?: ""
            return@withContext Jsoup.parse(html, currentBase)
        } catch (err: Exception) {
            if (retryCount == 0) {
                domainManager.detectActiveDomain()
                return@withContext getJsoup(urlPath, retryCount + 1)
            }
            throw err
        }
    }

    // ==========================================
    // SCRAPE LISTING FILM
    // ==========================================
    suspend fun scrapeMovieList(path: String): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val doc = getJsoup(path)
            val movies = mutableListOf<Movie>()
            val items = doc.select("article, .search-item, .item, .grid-archive .item, .poster-title")
            val seenUrls = mutableSetOf<String>()

            for (el in items) {
                var titleElem = el.select("h3.poster-title, h2 a, a[title]").first()
                if (titleElem == null) continue
                
                var title = titleElem.attr("title").ifBlank { titleElem.text().trim() }
                val urlElem = if (titleElem.closest("a") != null) titleElem.closest("a") else if (titleElem.select("a").isNotEmpty()) titleElem.select("a").first() else el.select("a").first()
                val url = urlElem?.attr("href") ?: ""
                
                if (title.isBlank() || url.isBlank() || seenUrls.contains(url)) continue
                seenUrls.add(url)
                
                title = title.replace(Regex("^Nonton\\s+(movie|series|film)?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s+streaming\\s+gratis.*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s+sub\\s+indo.*", RegexOption.IGNORE_CASE), "")
                    .trim()
                
                var poster = el.select("img").attr("data-src").ifBlank { el.select("img").attr("src") }
                if (poster.startsWith("//")) poster = "https:$poster"
                
                val quality = el.select(".quality").text().trim().ifBlank { "HD" }
                val synopsis = el.select(".synopsis").attr("data-full").ifBlank { el.select(".synopsis").text().trim() }
                var year = el.select(".year").text().trim()
                
                var rating = el.select(".rating span[itemprop=ratingValue]").text().trim()
                if (rating.isBlank()) rating = el.select(".rating").text().trim().replace(Regex("[^0-9.]"), "")
                if (rating.isBlank() || rating == "0" || rating == "0.0") rating = "-"
                
                val duration = el.select(".duration").text().trim().ifBlank { "-" }
                val genreStr = el.select("meta[itemprop=genre]").attr("content").ifBlank { el.select(".genre").text().trim() }
                val genres = if (genreStr.isNotBlank()) genreStr.split(Regex("[,/]\\s*")).map { it.trim() }.filter { it.isNotBlank() } else emptyList()
                
                if (year.isBlank()) {
                    val ym = Pattern.compile("\\((\\d{4})\\)").matcher(title)
                    if (ym.find()) year = ym.group(1) ?: ""
                }
                if (year.isBlank()) {
                    val ymUrl = Pattern.compile("-(\\d{4})(?:$|/)").matcher(url)
                    if (ymUrl.find()) year = ymUrl.group(1) ?: ""
                }
                
                title = title.replace(Regex("\\s*\\(\\d{4}\\)"), "").trim()
                
                movies.add(
                    Movie(
                        title = title,
                        year = year.ifBlank { "-" },
                        poster = poster,
                        rating = rating,
                        quality = quality,
                        duration = duration,
                        genres = genres,
                        synopsis = synopsis,
                        url = "/${normalizeSlug(url)}"
                    )
                )
            }
            return@withContext movies.sortedByDescending { it.year.toIntOrNull() ?: 0 }
        } catch (e: Exception) {}
        emptyList()
    }

    suspend fun getHomeMovies(): List<Movie> = scrapeMovieList("/")
    suspend fun getTrendingMovies(): List<Movie> = scrapeMovieList("/populer/")
    suspend fun getSeriesMovies(): List<Movie> = scrapeMovieList("/latest-series/")

    suspend fun searchMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val currentBase = domainManager.getBaseUrl()
            val request = Request.Builder()
                .url("https://gudangvape.com/search.php?s=${URLEncoder.encode(query, "UTF-8")}&page=1")
                .header("User-Agent", USER_AGENT)
                .header("Cookie", "cf_clearance=$CF_CLEARANCE_API")
                .header("Origin", currentBase)
                .header("Referer", "$currentBase/")
                .build()
                
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val items = json.optJSONArray("data") ?: json.optJSONArray("items")
                if (items != null && items.length() > 0) {
                    val movies = mutableListOf<Movie>()
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        var title = item.optString("title", "")
                        var year = ""
                        val ym = Pattern.compile("\\((\\d{4})\\)").matcher(title)
                        if (ym.find()) year = ym.group(1) ?: ""
                        val slug = item.optString("slug", "")
                        if (year.isBlank() && slug.isNotBlank()) {
                            val ymUrl = Pattern.compile("-(\\d{4})(?:$|/)").matcher(slug)
                            if (ymUrl.find()) year = ymUrl.group(1) ?: ""
                        }
                        title = title.replace(Regex("\\s*\\(\\d{4}\\)"), "").trim()
                        var poster = item.optString("poster", "")
                        if (poster.isNotBlank() && !poster.startsWith("http")) {
                            poster = "https://poster.showcdnx.com/wp-content/uploads/$poster"
                        }
                        val genreStr = item.optString("genre", "")
                        val genres = if (genreStr.isNotBlank()) genreStr.split(Regex("[,/]\\s*")).map { it.trim() }.filter { it.isNotBlank() } else emptyList()
                        movies.add(
                            Movie(
                                title = title,
                                year = year.ifBlank { "-" },
                                poster = poster,
                                rating = item.optString("rating", "-"),
                                quality = item.optString("quality", "HD"),
                                duration = item.optString("duration", "-"),
                                genres = genres,
                                synopsis = item.optString("synopsis", ""),
                                url = "/$slug"
                            )
                        )
                    }
                    if (movies.isNotEmpty()) return@withContext movies
                }
            }
        } catch (e: Exception) {}
        
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val list = scrapeMovieList("/?s=$encodedQuery")
            if (list.isNotEmpty()) return@withContext list
        } catch (e: Exception) {}
        
        getFeaturedFallbackMovies().filter { it.title.contains(query, ignoreCase = true) }
    }

    // ==========================================
    // SCRAPE DETAIL FILM & SERIES DARI LK21
    // ==========================================
    suspend fun scrapeDetail(urlPath: String): MovieDetail = withContext(Dispatchers.IO) {
        val cleanPath = urlPath.replace(Regex("^https?://[^/]+"), "").removePrefix("/").let { "/$it" }
        try {
            var doc = getJsoup(cleanPath)
            
            val redirectLink = doc.select("#openNow").attr("href")
            if (redirectLink.isNotBlank()) {
                val redirectReq = Request.Builder()
                    .url(redirectLink)
                    .header("User-Agent", USER_AGENT)
                    .build()
                val redirectRes = okHttpClient.newCall(redirectReq).execute()
                doc = Jsoup.parse(redirectRes.body?.string() ?: "")
            }
            
            var title = doc.select("h1").first()?.text()?.trim() ?: ""
            title = title.replace(Regex("Nonton\\s", RegexOption.IGNORE_CASE), "").replace(Regex("\\sSub Indo di Lk21", RegexOption.IGNORE_CASE), "")
            
            if (title.isBlank() || title.contains("gratis di layarkaca21", ignoreCase = true) || title.contains("lk21", ignoreCase = true)) {
                throw Exception("URL Slug tidak valid atau film tidak ditemukan.")
            }
            
            var poster = doc.select("meta[property=og:image]").attr("content").ifBlank {
                doc.select("meta[name=twitter:image]").attr("content").ifBlank {
                    doc.select("link[rel=image_src]").attr("href").ifBlank {
                        doc.select("img[itemprop=image]").attr("src").ifBlank {
                            doc.select("img[itemprop=image]").attr("data-src")
                        }
                    }
                }
            }
            if (poster.startsWith("//")) poster = "https:$poster"
            
            var synopsis = doc.select(".synopsis").attr("data-full")
            if (synopsis.isBlank()) synopsis = doc.select(".synopsis").text().trim()
            
            var quality = "HD"
            var duration = "-"
            var ageRating = "-"
            doc.select(".info-tag span").forEach { el ->
                val txt = el.text().trim()
                if (txt.matches(Regex("^\\d{1,2}\\+$")) || listOf("SU", "PG", "PG-13", "R", "NC-17").contains(txt.uppercase())) {
                    ageRating = txt
                } else if (txt.matches(Regex("^\\d+h(?:\\s*\\d+m)?$", RegexOption.IGNORE_CASE)) || txt.matches(Regex("^\\d+\\s*min", RegexOption.IGNORE_CASE))) {
                    duration = txt
                } else if (txt.matches(Regex("^(?:WEBDL|HD|HDCAM|CAM|BLURAY|DVDRIP|TS)$", RegexOption.IGNORE_CASE))) {
                    quality = txt.uppercase()
                }
            }
            
            val genres = mutableListOf<String>()
            doc.select(".tag-list a[href*=/genre/]").forEach { a ->
                val g = a.text().trim()
                if (g.isNotBlank() && !genres.contains(g)) genres.add(g)
            }
            val countries = mutableListOf<String>()
            doc.select(".tag-list a[href*=/country/]").forEach { a ->
                val c = a.text().trim()
                if (c.isNotBlank() && !countries.contains(c)) countries.add(c)
            }
            val directors = mutableListOf<String>()
            doc.select("a[href*=/director/]").forEach { a ->
                val d = a.text().trim()
                if (d.isNotBlank() && !directors.contains(d)) directors.add(d)
            }
            
            var rating = doc.select(".rating span[itemprop=ratingValue]").text().trim().ifBlank {
                doc.select(".rating").first()?.text()?.replace(Regex("[^0-9.]"), "")?.trim() ?: ""
            }
            if (rating.isBlank() || rating == "0" || rating == "0.0") rating = "-"
            
            var year = "-"
            val ym = Pattern.compile("\\((\\d{4})\\)").matcher(title)
            if (ym.find()) year = ym.group(1) ?: "-"
            if (year == "-") {
                val ymUrl = Pattern.compile("-(\\d{4})(?:$|/)").matcher(urlPath)
                if (ymUrl.find()) year = ymUrl.group(1) ?: "-"
            }
            
            val streamsMap = mutableMapOf<String, String>()
            doc.select("a[data-server], a[data-url], .server-item a, #load-server a, .tab-content a, .server-list a").forEach { a ->
                var serverName = a.attr("data-server")
                val serverUrl = a.attr("data-url").ifBlank { a.attr("href") }
                if (serverName.isBlank() && serverUrl.isNotBlank()) {
                    if (serverUrl.contains("/cast/")) serverName = "cast"
                    else if (serverUrl.contains("/turbovip/")) serverName = "turbovip"
                    else if (serverUrl.contains("/p2p/")) serverName = "p2p"
                    else if (serverUrl.contains("/hydrax/")) serverName = "hydrax"
                    else {
                        val txt = a.text().trim().lowercase()
                        if (txt.isNotBlank() && !txt.contains("nonton") && !txt.contains("download")) serverName = txt
                    }
                }
                if (serverName.isNotBlank() && serverUrl.startsWith("http")) {
                    val sKey = serverName.lowercase().trim()
                    if (!streamsMap.containsKey(sKey)) streamsMap[sKey] = serverUrl
                }
            }
            
            val mainIframe = doc.select("iframe").attr("src").ifBlank { doc.select("iframe").attr("data-src") }
            if (mainIframe.isNotBlank() && mainIframe.startsWith("http")) {
                var defServer = "p2p"
                if (mainIframe.contains("/cast/")) defServer = "cast"
                else if (mainIframe.contains("/turbovip/")) defServer = "turbovip"
                else if (mainIframe.contains("/hydrax/")) defServer = "hydrax"
                if (!streamsMap.containsKey(defServer)) streamsMap[defServer] = mainIframe
            }
            
            val episodes = mutableListOf<EpisodeInfo>()
            val seasonDataElem = doc.select("#season-data").first()
            if (seasonDataElem != null && seasonDataElem.html().isNotBlank()) {
                try {
                    val json = JSONObject(seasonDataElem.html())
                    for (key in json.keys()) {
                        val epArray = json.getJSONArray(key)
                        for (i in 0 until epArray.length()) {
                            val epObj = epArray.getJSONObject(i)
                            episodes.add(
                                EpisodeInfo(
                                    url = "/" + epObj.optString("slug", ""),
                                    title = epObj.optString("title", "Episode ${i + 1}"),
                                    season = key
                                )
                            )
                        }
                    }
                } catch (e: Exception) {}
            } else {
                doc.select(".col-episode a, .episode-list a, .list-episode a, a[href*=episode]").forEach { a ->
                    var epUrl = a.attr("href")
                    val epText = a.text().trim()
                    if (epUrl.isNotBlank() && epText.isNotBlank() && !epText.contains("play terbaru", ignoreCase = true) && !epText.contains("play awal", ignoreCase = true)) {
                        if (episodes.none { it.url == epUrl }) {
                            epUrl = epUrl.replace(Regex("^https?://[^/]+"), "")
                            val sMatch = Pattern.compile("Season (\\d+)", Pattern.CASE_INSENSITIVE).matcher(epText)
                            val season = if (sMatch.find()) sMatch.group(1) ?: "1" else "1"
                            episodes.add(EpisodeInfo(url = epUrl, title = epText, season = season))
                        }
                    }
                }
            }
            
            val cleanSlug = normalizeSlug(cleanPath)
            val streamList = mutableListOf<StreamServerInfo>()
            if (streamsMap.isNotEmpty()) {
                val serverKeys = streamsMap.keys.toList()
                val defaultServer = if (serverKeys.contains("cast")) "cast" else serverKeys[0]
                for (serverName in serverKeys) {
                    val playUrl = if (serverName.equals(defaultServer, ignoreCase = true)) "/play/$cleanSlug" else "/play/$cleanSlug?server=$serverName"
                    val resolutions = if (serverName.equals("hydrax", ignoreCase = true)) listOf("Auto (Hydrax Native)") else listOf("1080p", "720p", "480p")
                    streamList.add(
                        StreamServerInfo(
                            server = serverName,
                            resolutions = resolutions,
                            play_url = playUrl,
                            source_url = streamsMap[serverName] ?: ""
                        )
                    )
                }
            } else {
                listOf("cast", "p2p", "turbovip", "hydrax").forEach { s ->
                    streamList.add(
                        StreamServerInfo(
                            server = s,
                            resolutions = if (s == "hydrax") listOf("Auto (Hydrax Native)") else listOf("1080p", "720p", "480p"),
                            play_url = "/play/$cleanSlug?server=$s",
                            source_url = ""
                        )
                    )
                }
            }
            val order = mapOf("cast" to 1, "p2p" to 2, "turbovip" to 3, "hydrax" to 4)
            streamList.sortBy { order[it.server] ?: 99 }
            
            MovieDetail(
                title = title,
                year = year,
                poster = poster,
                rating = rating,
                quality = quality,
                duration = duration,
                age_rating = ageRating,
                genres = genres,
                countries = countries,
                directors = directors,
                synopsis = synopsis,
                streams = streamList,
                episodes = episodes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Scrape detail failed for $urlPath: ${e.message}")
            getDetailFallback(cleanPath)
        }
    }

    // ==========================================
    // EXTRACTORS (P2P, TURBOVIP, HYDRAX, CAST)
    // ==========================================
    suspend fun extractCastStream(wrapperUrl: String, slug: String = "", serverName: String = "cast", title: String = ""): StreamSession? = withContext(Dispatchers.IO) {
        try {
            val code = wrapperUrl.substringAfterLast("/")
            val baseUrl = "https://gn1r5n.org"
            
            val fingerprint = CastCryptoHelper.generateAttestationPayload("test-challenge", "test-nonce")
            val settingsReq = Request.Builder()
                .url("$baseUrl/api/videos/$code/embed/settings")
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://videonode.de/")
                .header("Origin", "https://videonode.de")
                .build()
            
            val settingsRes = okHttpClient.newCall(settingsReq).execute()
            val settingsJson = JSONObject(settingsRes.body?.string() ?: "{}")
            
            var captchaToken: String? = null
            if (settingsJson.optBoolean("captcha_required", false)) {
                val captchaReq = Request.Builder()
                    .url("$baseUrl/api/videos/$code/embed/captcha")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://videonode.de/")
                    .header("Origin", "https://videonode.de")
                    .header("Content-Type", "application/json")
                    .post(JSONObject().put("fingerprint", fingerprint).toString().toRequestBody("application/json".toMediaType()))
                    .build()
                    
                val captchaRes = okHttpClient.newCall(captchaReq).execute()
                val captchaResJson = JSONObject(captchaRes.body?.string() ?: "{}")
                
                val powNonce = captchaResJson.optString("pow_nonce", "")
                val powDifficulty = captchaResJson.optInt("pow_difficulty", 0)
                val powToken = captchaResJson.optString("pow_token", "")
                
                val solution = CastCryptoHelper.solvePoW(powNonce, powDifficulty)
                
                val verifyReq = Request.Builder()
                    .url("$baseUrl/api/videos/$code/embed/captcha/verify")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://videonode.de/")
                    .header("Origin", "https://videonode.de")
                    .header("Content-Type", "application/json")
                    .post(JSONObject().apply {
                        put("pow_token", powToken)
                        put("solution", solution)
                        put("fingerprint", fingerprint)
                    }.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                    
                val verifyRes = okHttpClient.newCall(verifyReq).execute()
                captchaToken = JSONObject(verifyRes.body?.string() ?: "{}").optString("token", null)
            }
            
            val playbackReqBuilder = Request.Builder()
                .url("$baseUrl/api/videos/$code/embed/playback")
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://videonode.de/")
                .header("Origin", "https://videonode.de")
                .header("Content-Type", "application/json")
                .post(JSONObject().put("fingerprint", fingerprint).toString().toRequestBody("application/json".toMediaType()))
                
            if (captchaToken != null) {
                playbackReqBuilder.header("X-Captcha-Token", captchaToken)
            }
            
            val playbackRes = okHttpClient.newCall(playbackReqBuilder.build()).execute()
            val playbackResStr = playbackRes.body?.string() ?: "{}"
            var decrypted = JSONObject(playbackResStr)
            
            if (decrypted.has("playback")) {
                decrypted = CastCryptoHelper.decryptPlaybackPayload(decrypted.getJSONObject("playback"))
            }
            
            val sources = decrypted.optJSONArray("sources")
            if (sources != null && sources.length() > 0) {
                var rawUrl = ""
                val resolutions = mutableListOf<String>()
                for (i in 0 until sources.length()) {
                    val src = sources.getJSONObject(i)
                    if (rawUrl.isBlank()) rawUrl = src.optString("url", "")
                    resolutions.add(src.optString("label", "${src.optInt("height", 1080)}p"))
                }
                
                val cleanSlug = normalizeSlug(slug).ifBlank { code }
                val session = StreamSession(
                    type = "m3u8",
                    slug = cleanSlug,
                    server = serverName,
                    rawUrl = rawUrl,
                    resolutions = resolutions,
                    referer = "$baseUrl/",
                    title = title
                )
                return@withContext saveSession(session)
            }
        } catch (e: Exception) {}
        null
    }

    suspend fun extractP2PStream(wrapperUrl: String, slug: String = "", serverName: String = "p2p", title: String = ""): StreamSession? = withContext(Dispatchers.IO) {
        try {
            var id = wrapperUrl.substringAfterLast("/")
            var host = "playcdn.de"
            var referer = "https://videonode.de/"
            val pageReq = Request.Builder()
                .url(wrapperUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", domainManager.getBaseUrl() + "/")
                .build()
            val pageHtml = okHttpClient.newCall(pageReq).execute().body?.string() ?: ""
            val iframeMatcher = Pattern.compile("<iframe.*?src=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE).matcher(pageHtml)
            if (iframeMatcher.find()) {
                val nested = iframeMatcher.group(1) ?: ""
                val uri = URI(nested)
                host = uri.host ?: host
                val qId = uri.query?.split("&")?.find { it.startsWith("id=") }?.substringAfter("id=")
                if (!qId.isNullOrBlank()) id = qId
                referer = wrapperUrl
            }
            val postBody = FormBody.Builder()
                .add("r", referer)
                .add("d", host)
                .build()
            val apiReq = Request.Builder()
                .url("https://$host/api2.php?id=$id")
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://$host/video.php?id=$id")
                .header("X-Requested-With", "XMLHttpRequest")
                .post(postBody)
                .build()
            val apiRes = okHttpClient.newCall(apiReq).execute()
            if (apiRes.isSuccessful) {
                val jsonStr = apiRes.body?.string() ?: ""
                val jsonObj = JSONObject(jsonStr)
                var rawUrl = jsonObj.optString("file", "")
                if (rawUrl.isNotBlank()) {
                    if (!rawUrl.startsWith("http")) {
                        rawUrl = "https://$host$rawUrl"
                    }
                    val cleanSlug = normalizeSlug(slug).ifBlank { id }
                    val session = StreamSession(
                        type = "m3u8",
                        slug = cleanSlug,
                        server = serverName,
                        rawUrl = rawUrl,
                        resolutions = listOf("1080p", "720p", "480p"),
                        referer = "https://$host/",
                        title = title.ifBlank { jsonObj.optString("title", cleanSlug) },
                        poster = jsonObj.optString("poster", null)
                    )
                    return@withContext saveSession(session)
                }
            }
        } catch (e: Exception) {}
        null
    }

    suspend fun extractTurbovipStream(wrapperUrl: String, slug: String = "", serverName: String = "turbovip", title: String = ""): StreamSession? = withContext(Dispatchers.IO) {
        try {
            val code = wrapperUrl.substringAfterLast("/")
            val targetUrls = mutableListOf(wrapperUrl)
            if (!wrapperUrl.contains("emturbovid.com")) {
                targetUrls.add("https://emturbovid.com/t/$code")
            }
            for (tUrl in targetUrls) {
                val req = Request.Builder()
                    .url(tUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", domainManager.getBaseUrl() + "/")
                    .build()
                val html = okHttpClient.newCall(req).execute().body?.string() ?: ""
                var rawUrl: String? = null
                val directMatcher = Pattern.compile("urlPlay\\s*=\\s*['\"](.*?)['\"]").matcher(html)
                if (directMatcher.find()) {
                    rawUrl = directMatcher.group(1)
                } else {
                    val iframeMatcher = Pattern.compile("<iframe.*?src=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
                    if (iframeMatcher.find()) {
                        var nested = iframeMatcher.group(1) ?: ""
                        if (!nested.startsWith("http")) nested = "https://emturbovid.com$nested"
                        val req2 = Request.Builder()
                            .url(nested)
                            .header("User-Agent", USER_AGENT)
                            .header("Referer", "https://videonode.de/")
                            .build()
                        val html2 = okHttpClient.newCall(req2).execute().body?.string() ?: ""
                        val urlMatch = Pattern.compile("urlPlay\\s*=\\s*['\"](.*?)['\"]").matcher(html2)
                        if (urlMatch.find()) rawUrl = urlMatch.group(1)
                    }
                }
                if (rawUrl != null) {
                    val cleanSlug = normalizeSlug(slug).ifBlank { code }
                    val session = StreamSession(
                        type = "m3u8",
                        slug = cleanSlug,
                        server = serverName,
                        rawUrl = rawUrl,
                        resolutions = listOf("1080p", "720p", "480p"),
                        referer = "https://emturbovid.com/",
                        title = title
                    )
                    return@withContext saveSession(session)
                }
            }
        } catch (e: Exception) {}
        null
    }

    suspend fun extractHydraxStream(wrapperUrl: String, slug: String = "", serverName: String = "hydrax", title: String = ""): StreamSession? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(wrapperUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", domainManager.getBaseUrl() + "/")
                .build()
            val html = okHttpClient.newCall(req).execute().body?.string() ?: ""
            val iframeMatcher = Pattern.compile("<iframe.*?src=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
            if (iframeMatcher.find()) {
                val iframeUrl = iframeMatcher.group(1) ?: ""
                val cleanSlug = normalizeSlug(slug).ifBlank { wrapperUrl.substringAfterLast("/") }
                val session = StreamSession(
                    type = "iframe",
                    slug = cleanSlug,
                    server = serverName,
                    iframe = iframeUrl,
                    resolutions = listOf("Auto (Hydrax Native)"),
                    title = title
                )
                return@withContext saveSession(session)
            }
        } catch (e: Exception) {}
        null
    }

    // Dynamic On-Demand Stream Resolver
    suspend fun getOrExtractMovieStream(slug: String, server: String): StreamSession? = withContext(Dispatchers.IO) {
        var cleanSlug = normalizeSlug(slug)
        var parsedServer = server.lowercase()
        
        if (cleanSlug.contains("/")) {
            val parts = cleanSlug.split("/")
            cleanSlug = parts[0]
            if (parsedServer.isBlank()) parsedServer = parts.getOrNull(1) ?: ""
        }
        
        val sessionKey = if (parsedServer.isNotBlank()) "$cleanSlug?server=$parsedServer" else cleanSlug
        if (streamSessions.containsKey(sessionKey)) return@withContext streamSessions[sessionKey]
        if (streamSessions.containsKey(cleanSlug)) return@withContext streamSessions[cleanSlug]
        
        try {
            val detail = scrapeDetail("/$cleanSlug")
            val streams = detail.streams
            if (streams.isNotEmpty()) {
                val targetServer = if (parsedServer.isNotBlank() && streams.any { it.server.equals(parsedServer, ignoreCase = true) }) {
                    parsedServer
                } else if (streams.any { it.server.equals("p2p", ignoreCase = true) }) {
                    "p2p"
                } else if (streams.any { it.server.equals("turbovip", ignoreCase = true) }) {
                    "turbovip"
                } else if (streams.any { it.server.equals("cast", ignoreCase = true) }) {
                    "cast"
                } else {
                    streams[0].server
                }
                
                val targetStreamObj = streams.find { it.server.equals(targetServer, ignoreCase = true) }
                val targetUrl = targetStreamObj?.source_url ?: ""

                if (targetUrl.isNotBlank()) {
                    when (targetServer) {
                        "cast" -> extractCastStream(targetUrl, cleanSlug, "cast", detail.title)?.let { return@withContext it }
                        "p2p" -> extractP2PStream(targetUrl, cleanSlug, "p2p", detail.title)?.let { return@withContext it }
                        "turbovip" -> extractTurbovipStream(targetUrl, cleanSlug, "turbovip", detail.title)?.let { return@withContext it }
                        "hydrax" -> extractHydraxStream(targetUrl, cleanSlug, "hydrax", detail.title)?.let { return@withContext it }
                        else -> extractP2PStream(targetUrl, cleanSlug, targetServer, detail.title)?.let { return@withContext it }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getOrExtractMovieStream error: ${e.message}")
        }
        
        val fallbackSession = StreamSession(
            type = "iframe",
            slug = cleanSlug,
            server = parsedServer.ifBlank { "cast" },
            rawUrl = "",
            iframe = "https://playcdn.de/video.php?id=$cleanSlug",
            resolutions = listOf("1080p", "720p", "480p"),
            referer = "https://videonode.de/",
            title = cleanSlug.replace("-", " ")
        )
        saveSession(fallbackSession)
    }

    // ==========================================
    // FALLBACK DATA
    // ==========================================
    suspend fun getFeaturedFallbackMovies(): List<Movie> = withContext(Dispatchers.IO) {
        listOf(
            Movie(
                title = "Deadpool & Wolverine",
                year = "2024",
                poster = "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
                rating = "8.1",
                quality = "4K UHD",
                duration = "2h 8m",
                genres = listOf("Action", "Comedy", "Sci-Fi"),
                synopsis = "TVA merekrut Deadpool dari kehidupan tenangnya dan mengirimnya ke misi penting bersama Wolverine yang enggan.",
                url = "/deadpool-wolverine-2024"
            )
        )
    }

    suspend fun getTrendingFallback(): List<Movie> = withContext(Dispatchers.IO) {
        listOf(
            Movie(
                title = "Oppenheimer",
                year = "2023",
                poster = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
                rating = "8.6",
                quality = "4K UHD",
                duration = "3h 0m",
                genres = listOf("Biography", "Drama", "History"),
                synopsis = "Kisah ilmuwan Amerika J. Robert Oppenheimer dan perannya dalam pengembangan bom atom.",
                url = "/oppenheimer-2023"
            )
        )
    }

    suspend fun getSeriesFallback(): List<Movie> = withContext(Dispatchers.IO) {
        listOf(
            Movie(
                title = "House of the Dragon - Season 2",
                year = "2024",
                poster = "https://image.tmdb.org/t/p/w500/t9XkeFsDh0v6vQ9uJRuxqJ7RoR8.jpg",
                rating = "8.4",
                quality = "HD",
                duration = "8 Episodes",
                genres = listOf("Drama", "Action", "Fantasy"),
                synopsis = "Keluarga Targaryen terlibat dalam perang saudara berdarah yang dikenal sebagai Dance of the Dragons demi memperebutkan Iron Throne.",
                url = "/house-of-the-dragon-season-2"
            )
        )
    }

    suspend fun getDetailFallback(slug: String): MovieDetail = withContext(Dispatchers.IO) {
        val clean = slug.trim().removePrefix("/").removeSuffix("/")
        val title = clean.replace("-", " ").capitalizeWords()
        
        MovieDetail(
            title = title,
            year = "2024",
            poster = "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
            rating = "8.0",
            quality = "HD",
            duration = "2h 15m",
            age_rating = "13+",
            genres = listOf("Action"),
            countries = listOf("US"),
            directors = listOf("Director"),
            synopsis = "Film streaming berkualitas tinggi di CloudMovies dengan multi-server berkecepatan tinggi.",
            streams = listOf(
                StreamServerInfo(server = "cast", resolutions = listOf("1080p", "720p", "480p"), play_url = "/play/$clean?server=cast"),
                StreamServerInfo(server = "p2p", resolutions = listOf("1080p", "720p", "480p"), play_url = "/play/$clean?server=p2p")
            ),
            episodes = emptyList()
        )
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
