package com.casty.music.backend.innertube

import com.casty.music.backend.innertube.models.Context
import com.casty.music.backend.innertube.models.MediaInfo
import com.casty.music.backend.innertube.models.ReturnYouTubeDislikeResponse
import com.casty.music.backend.innertube.models.YouTubeClient
import com.casty.music.backend.innertube.models.YouTubeLocale
import com.casty.music.backend.innertube.models.body.*
import com.casty.music.backend.innertube.models.response.NextResponse
import com.casty.music.backend.innertube.utils.parseCookieString
import com.casty.music.backend.innertube.utils.sha1
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.net.Proxy
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.*
import kotlin.io.encoding.Base64
import timber.log.Timber
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlin.random.Random

/**
 * Provide access to InnerTube endpoints.
 * For making HTTP requests, not parsing response.
 */
@OptIn(ExperimentalEncodingApi::class)
class InnerTube {
    private var httpClient = createClient()
    private val circuitBreaker = ApiCircuitBreaker()

    var locale = YouTubeLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag()
    )
    var visitorData: String? = null
    var dataSyncId: String? = null
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    var proxy: Proxy? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }
    
    var proxyAuth: String? = null

    var useLoginForBrowse: Boolean = false

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }

        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }

        // Enhanced network configuration for better performance
        engine {
            config {
                // Connection pool settings for better connection reuse
                connectionPool(
                    okhttp3.ConnectionPool(
                        10, // maxIdleConnections
                        5, // keepAliveDuration
                        java.util.concurrent.TimeUnit.MINUTES
                    )
                )
                
                // Timeout configurations
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                
                // Enable HTTP/2 for better performance
                protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
                
                // Retry on connection failure
                retryOnConnectionFailure(true)
                
                // Cache configuration for better performance
                cache(
                    okhttp3.Cache(
                        directory = java.io.File(System.getProperty("java.io.tmpdir"), "http_cache"),
                        maxSize = 24L * 1024L * 1024L
                    )
                )
                
                // Apply proxy configuration
                this@InnerTube.proxy?.let { proxyConfig ->
                    proxy(proxyConfig)
                }
                
                // Apply proxy authentication
                this@InnerTube.proxyAuth?.let { auth ->
                    proxyAuthenticator { _, response ->
                        response.request.newBuilder()
                            .header("Proxy-Authorization", auth)
                            .build()
                    }
                }
            }
        }

        // Request timeout configuration
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }

        defaultRequest {
            url(YouTubeClient.API_URL_YOUTUBE_MUSIC)
            // Add common headers for better compatibility
            header("Accept", "application/json")
            header("Accept-Language", "en-US,en;q=0.9")
            header("Cache-Control", "no-cache")
        }
    }

    private fun HttpRequestBuilder.ytClient(client: YouTubeClient, setLogin: Boolean = false) {
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId /* Not a typo. The Client-Name header does contain the client id. */)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
            append("X-Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
            append("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
            append("Accept-Language", locale.hl)
            append("X-Goog-AuthUser", "0")
            visitorData?.let { append("X-Goog-Visitor-Id", it) }
            if (setLogin && client.loginSupported) {
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
        }
        userAgent(client.userAgent)
        parameter("prettyPrint", false)
    }

    private suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 500L,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        var attempt = 0
        while (true) {
            try {
                circuitBreaker.beforeRequest()
                return block().also { circuitBreaker.recordSuccess() }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                attempt++
                val retryable = throwable.isRetryableNetworkFailure()
                circuitBreaker.recordFailure(retryable)
                if (!retryable || attempt >= maxAttempts) throw throwable

                delay(throwable.retryAfterMillis() ?: currentDelay.withJitter())
                currentDelay = min((currentDelay * factor).toLong(), MAX_RETRY_DELAY_MS)
            }
        }
    }

    private fun Throwable.isRetryableNetworkFailure(): Boolean {
        if (this is IOException) return true
        val status = (this as? ResponseException)?.response?.status?.value ?: return false
        return status == HttpStatusCode.TooManyRequests.value || status in 500..599
    }

    private fun Throwable.retryAfterMillis(): Long? =
        (this as? ResponseException)
            ?.response
            ?.headers
            ?.get(HttpHeaders.RetryAfter)
            ?.toLongOrNull()
            ?.times(1000L)
            ?.coerceIn(MIN_RETRY_DELAY_MS, MAX_RETRY_AFTER_MS)

    private fun Long.withJitter(): Long {
        val jitter = Random.nextLong(0L, (this / 2L).coerceAtLeast(1L))
        return (this + jitter).coerceAtMost(MAX_RETRY_DELAY_MS)
    }

    private class ApiCircuitBreaker {
        private var consecutiveFailures = 0
        private var openedAtMs = 0L

        @Synchronized
        fun beforeRequest() {
            if (openedAtMs == 0L) return
            val elapsed = System.currentTimeMillis() - openedAtMs
            if (elapsed < CIRCUIT_BREAKER_RESET_MS) {
                throw IOException("YouTube Music API circuit is temporarily open")
            }
            openedAtMs = 0L
            consecutiveFailures = 0
        }

        @Synchronized
        fun recordSuccess() {
            consecutiveFailures = 0
            openedAtMs = 0L
        }

        @Synchronized
        fun recordFailure(retryable: Boolean) {
            if (!retryable) return
            consecutiveFailures += 1
            if (consecutiveFailures >= CIRCUIT_BREAKER_FAILURES) {
                openedAtMs = System.currentTimeMillis()
            }
        }
    }

    private companion object {
        private const val CIRCUIT_BREAKER_FAILURES = 5
        private const val CIRCUIT_BREAKER_RESET_MS = 30_000L
        private const val MIN_RETRY_DELAY_MS = 250L
        private const val MAX_RETRY_DELAY_MS = 8_000L
        private const val MAX_RETRY_AFTER_MS = 30_000L
    }

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ) = withRetry {
        httpClient.post("search") {
            ytClient(client, setLogin = useLoginForBrowse)
            setBody(
                SearchBody(
                    context = client.toContext(
                        locale,
                        visitorData,
                        if (useLoginForBrowse) dataSyncId else null
                    ),
                    query = query,
                    params = params
                )
            )
            parameter("continuation", continuation)
            parameter("ctoken", continuation)
        }
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        signatureTimestamp: Int?,
        poToken: String? = null,
    ) = withRetry {
        httpClient.post("player") {
            ytClient(client, setLogin = true)
            setBody(
                PlayerBody(
                    context = client.toContext(locale, visitorData, dataSyncId).let {
                        if (client.isEmbedded) {
                            it.copy(
                                thirdParty = Context.ThirdParty(
                                    embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                                )
                            )
                        } else it
                    },
                    videoId = videoId,
                    playlistId = playlistId,
                    playbackContext = if (client.useSignatureTimestamp && signatureTimestamp != null) {
                        PlayerBody.PlaybackContext(
                            PlayerBody.PlaybackContext.ContentPlaybackContext(
                                signatureTimestamp
                            )
                        )
                    } else null,
                    serviceIntegrityDimensions = if (client.useWebPoTokens && poToken != null) {
                        PlayerBody.ServiceIntegrityDimensions(poToken)
                    } else null,
                )
            )
        }
    }

    suspend fun registerPlayback(
        url: String,
        cpn: String,
        playlistId: String?,
        client: YouTubeClient = YouTubeClient.WEB_REMIX,
    ) = withRetry {
        httpClient.get(url) {
            ytClient(client, true)
            parameter("ver", "2")
            parameter("c", client.clientName)
            parameter("cpn", cpn)

            if (playlistId != null) {
                parameter("list", playlistId)
                parameter("referrer", "https://music.youtube.com/playlist?list=$playlistId")
            }
        }
    }

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        setLogin: Boolean = false,
    ) = withRetry {
        httpClient.post("browse") {
            ytClient(client, setLogin = setLogin || useLoginForBrowse)
            setBody(
                BrowseBody(
                    context = client.toContext(
                        locale,
                        visitorData,
                        if (setLogin || useLoginForBrowse) dataSyncId else null
                    ),
                    browseId = browseId,
                    params = params,
                    continuation = continuation
                )
            )
        }
    }

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = withRetry {
        httpClient.post("next") {
            ytClient(client, setLogin = true)
            setBody(
                NextBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    videoId = videoId,
                    playlistId = playlistId,
                    playlistSetVideoId = playlistSetVideoId,
                    index = index,
                    params = params,
                    continuation = continuation
                )
            )
        }
    }

    suspend fun feedback(
        client: YouTubeClient,
        tokens: List<String>
    ) = withRetry {
        httpClient.post("feedback") {
            ytClient(client, setLogin = true)
            setBody(
                FeedbackBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    feedbackTokens = tokens
                )
            )
        }
    }

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = withRetry {
        httpClient.post("music/get_search_suggestions") {
            ytClient(client)
            setBody(
                GetSearchSuggestionsBody(
                    context = client.toContext(locale, visitorData, null),
                    input = input
                )
            )
        }
    }

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = withRetry {
        httpClient.post("music/get_queue") {
            ytClient(client)
            setBody(
                GetQueueBody(
                    context = client.toContext(locale, visitorData, null),
                    videoIds = videoIds,
                    playlistId = playlistId
                )
            )
        }
    }

    suspend fun getTranscript(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("https://music.youtube.com/youtubei/v1/get_transcript") {
            parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
            headers {
                append("Content-Type", "application/json")
            }
            setBody(
                GetTranscriptBody(
                    context = client.toContext(locale, null, null),
                    params = Base64.Default.encode(
                        "\n${11.toChar()}$videoId".encodeToByteArray()
                    )
                )
            )
        }
    }

    suspend fun getSwJsData() = withRetry { httpClient.get("https://music.youtube.com/sw.js_data") }

    suspend fun accountMenu(client: YouTubeClient) = withRetry {
        httpClient.post("account/account_menu") {
            ytClient(client, setLogin = true)
            setBody(AccountMenuBody(client.toContext(locale, visitorData, dataSyncId)))
        }
    }

    suspend fun likeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.video(videoId)
                )
            )
        }
    }

    suspend fun unlikeVideo(
        client: YouTubeClient,
        videoId: String,
    ) = withRetry {
        httpClient.post("like/removelike") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.video(videoId)
                )
            )
        }
    }

    suspend fun subscribeChannel(
        client: YouTubeClient,
        channelId: String,
        params: String? = null,
    ) = withRetry {
        httpClient.post("subscription/subscribe") {
            ytClient(client, setLogin = true)
            setBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId),
                    params = params
                )
            )
        }
    }

    suspend fun unsubscribeChannel(
        client: YouTubeClient,
        channelId: String,
        params: String? = null,
    ) = withRetry {
        httpClient.post("subscription/unsubscribe") {
            ytClient(client, setLogin = true)
            setBody(
                SubscribeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    channelIds = listOf(channelId),
                    params = params
                )
            )
        }
    }

    suspend fun likePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        httpClient.post("like/like") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.playlist(playlistId)
                )
            )
        }
    }

    suspend fun unlikePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        httpClient.post("like/removelike") {
            ytClient(client, setLogin = true)
            setBody(
                LikeBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    target = LikeBody.Target.playlist(playlistId)
                )
            )
        }
    }

    suspend fun addToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.AddVideoAction(addedVideoId = videoId)
                    )
                )
            )
        }
    }

    suspend fun addPlaylistToPlaylist(
        client: YouTubeClient,
        playlistId: String,
        addPlaylistId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.AddPlaylistAction(addedFullListId = addPlaylistId)
                    )
                )
            )
        }
    }

    suspend fun removeFromPlaylist(
        client: YouTubeClient,
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        Action.RemoveVideoAction(
                            removedVideoId = videoId,
                            setVideoId = setVideoId,
                        )
                    )
                )
            )
        }
    }

    suspend fun moveSongPlaylist(
        client: YouTubeClient,
        playlistId: String,
        setVideoId: String,
        successorSetVideoId: String?,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.MoveVideoAction(
                            movedSetVideoIdSuccessor = successorSetVideoId,
                            setVideoId = setVideoId,
                        )
                    )
                )
            )
        }
    }

    suspend fun createPlaylist(
        client: YouTubeClient,
        title: String,
    ) = withRetry {
        httpClient.post("playlist/create") {
            ytClient(client, true)
            setBody(
                CreatePlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    title = title
                )
            )
        }
    }

    suspend fun renamePlaylist(
        client: YouTubeClient,
        playlistId: String,
        name: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.RenamePlaylistAction(
                            playlistName = name
                        )
                    )
                )
            )
        }
    }
    
    suspend fun getUploadCustomThumbnailLink(
        client: YouTubeClient,
        contentLength: Int
    ) = withRetry {
        httpClient.post("https://music.youtube.com/playlist_image_upload/playlist_custom_thumbnail") {
            ytClient(client, setLogin = true)
            headers {
                append("X-Goog-Upload-Command", "start")
                append("X-Goog-Upload-Protocol", "resumable")
                append("X-Goog-Upload-Header-Content-Length", contentLength.toString())
            }
        }
    }

    suspend fun uploadCustomThumbnail(
        client: YouTubeClient,
        uploadId: String,
        image: ByteArray,
    ) = withRetry {
        httpClient.post("https://music.youtube.com/playlist_image_upload/playlist_custom_thumbnail") {
            ytClient(client, setLogin = true)
            parameter("upload_id", uploadId)
            parameter("upload_protocol", "resumable")
            headers {
                append("X-Goog-Upload-Command", "upload, finalize")
                append("X-Goog-Upload-Offset", "0")
            }
            setBody(image)
        }
    }

    suspend fun setThumbnailPlaylist(
        client: YouTubeClient,
        playlistId: String,
        blobId: String,
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.SetCustomThumbnailAction(
                            addedCustomThumbnail = Action.SetCustomThumbnailAction.AddedCustomThumbnail(
                                playlistScottyEncryptedBlobId = blobId
                            )
                        )
                    )
                )
            )
        }
    }

    suspend fun removeThumbnailPlaylist(
        client: YouTubeClient,
        playlistId: String
    ) = withRetry {
        httpClient.post("browse/edit_playlist") {
            ytClient(client, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId,
                    actions = listOf(
                        Action.RemoveCustomThumbnailAction()
                    )
                )
            )
        }
    }

    suspend fun deletePlaylist(
        client: YouTubeClient,
        playlistId: String,
    ) = withRetry {
        httpClient.post("playlist/delete") {
            Timber.tag("InnerTube").d("Deleting playlist %s", playlistId)
            ytClient(client, setLogin = true)
            setBody(
                PlaylistDeleteBody(
                    context = client.toContext(locale, visitorData, dataSyncId),
                    playlistId = playlistId
                )
            )
        }
    }

    private suspend fun returnYouTubeDislike(videoId: String) = withRetry {
        httpClient.get("https://returnyoutubedislikeapi.com/Votes?videoId=$videoId") {
            contentType(ContentType.Application.Json)
        }
    }


    /**
     * Initialize a song upload to YouTube Music.
     * Returns the upload URL in the X-Goog-Upload-URL header.
     */
    suspend fun initSongUpload(
        filename: String,
        contentLength: Long
    ) = withRetry {
        val authUser = "0"
        httpClient.post("https://upload.youtube.com/upload/usermusic/http?authuser=$authUser") {
            headers {
                append("X-Goog-Upload-Command", "start")
                append("X-Goog-Upload-Protocol", "resumable")
                append("X-Goog-Upload-Header-Content-Length", contentLength.toString())
                append("X-Goog-AuthUser", authUser)
                append("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("filename=$filename")
        }
    }

    /**
     * Upload song data to the provided upload URL.
     */
    suspend fun uploadSongData(
        uploadUrl: String,
        data: ByteArray,
        onProgress: ((Float) -> Unit)? = null
    ) = withRetry {
        httpClient.post(uploadUrl) {
            headers {
                append("X-Goog-Upload-Command", "upload, finalize")
                append("X-Goog-Upload-Offset", "0")
                append("X-Goog-AuthUser", "0")
                append("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
            contentType(ContentType.Application.OctetStream)
            setBody(data)
            onUpload { bytesSentTotal, contentLength ->
                contentLength?.let {
                    onProgress?.invoke(bytesSentTotal.toFloat() / it.toFloat())
                }
            }
        }
    }

    /**
     * Delete a privately owned (uploaded) song from YouTube Music.
     */
    suspend fun deletePrivatelyOwnedEntity(entityId: String) = withRetry {
        val context = YouTubeClient.WEB_REMIX.toContext(locale, visitorData, null)
        val requestBody = """{"context":${Json.encodeToString(context)},"entityId":"$entityId"}"""
        httpClient.post("https://music.youtube.com/youtubei/v1/music/delete_privately_owned_entity") {
            contentType(ContentType.Application.Json)
            headers {
                append("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
                append("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash = sha1("$currentTime ${cookieMap["SAPISID"]} ${YouTubeClient.ORIGIN_YOUTUBE_MUSIC}")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
            parameter("key", "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3")
            parameter("prettyPrint", false)
            setBody(requestBody)
        }
    }

    suspend fun getMediaInfo(videoId: String): Result<MediaInfo> =
        runCatching {
            val response = next(client = YouTubeClient.WEB, videoId, null, null, null, null, null).body<NextResponse>()

            val baseForInfo =
                response.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoSecondaryInfoRenderer != null
                    }?.videoSecondaryInfoRenderer

            val baseForTitle =
                response.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoPrimaryInfoRenderer != null
                    }?.videoPrimaryInfoRenderer

            val returnYouTubeDislikeResponse =
                returnYouTubeDislike(videoId).body<ReturnYouTubeDislikeResponse>()

            return@runCatching MediaInfo(
                videoId = videoId,
                title = baseForTitle
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text,
                author = baseForInfo
                    ?.owner
                    ?.videoOwnerRenderer
                    ?.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text,
                authorId =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId,
                authorThumbnail =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.find {
                            it.height == 48
                        }?.url
                        ?.replace("s48", "s960"),
                description = baseForInfo?.attributedDescription?.content,
                subscribers =
                    baseForInfo
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.subscriberCountText
                        ?.simpleText?.split(" ")?.firstOrNull(),
                uploadDate = baseForTitle?.dateText?.simpleText,
                viewCount = returnYouTubeDislikeResponse.viewCount,
                like = returnYouTubeDislikeResponse.likes,
                dislike = returnYouTubeDislikeResponse.dislikes,
            )

        }


}
