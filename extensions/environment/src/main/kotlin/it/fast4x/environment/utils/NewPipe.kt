package it.fast4x.environment.utils

import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import it.fast4x.environment.Environment
import it.fast4x.environment.models.Context
import it.fast4x.environment.models.PlayerResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.io.IOException
import java.net.Proxy

private class NewPipeDownloaderImpl(proxy: Proxy?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", Context.USER_AGENT)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("NewPipe in Environment reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body?.string()

        val latestUrl = response.request.url.toString()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBodyToReturn,
            responseBodyToReturn?.toByteArray(),
            latestUrl
        )
    }

    override fun executeAsync(request: Request, callback: AsyncCallback?): CancellableCall {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", Context.USER_AGENT)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val call = client.newCall(requestBuilder.build())
        val cancellableCall = CancellableCall(call)
        call.enqueue(
            object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    cancellableCall.setFinished()
                    callback?.onError(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use { httpResponse ->
                        try {
                            if (httpResponse.code == 429) {
                                callback?.onError(
                                    ReCaptchaException("NewPipe in Environment reCaptcha Challenge requested", url),
                                )
                                return
                            }

                            val responseBodyToReturn = httpResponse.body?.string()
                            val latestUrl = httpResponse.request.url.toString()
                            callback?.onSuccess(
                                Response(
                                    httpResponse.code,
                                    httpResponse.message,
                                    httpResponse.headers.toMultimap(),
                                    responseBodyToReturn,
                                    responseBodyToReturn?.toByteArray(),
                                    latestUrl,
                                ),
                            )
                        } catch (exception: Exception) {
                            callback?.onError(exception)
                        } finally {
                            cancellableCall.setFinished()
                        }
                    }
                }
            },
        )
        return cancellableCall
    }
}

object NewPipeUtils {

    init {
        NewPipe.init(NewPipeDownloaderImpl(Environment.proxy))
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            format.url?.let {
                return@runCatching it
            }
            format.signatureCipher.let {
                if (it == null) throw ParsingException("NewPipe in Environment Could not find format signatureCipher")
                return@runCatching decodeSignatureCipher(videoId, it)
            }
        }

    fun decodeSignatureCipher(
        videoId: String,
        signatureCipher: String,
    ): String =
        signatureCipher.let { signature ->
            val params = parseQueryString(signature)
            val obfuscatedSignature = params["s"]
                ?: throw ParsingException("NewPipe in Environment decodeSignatureCipher Could not parse cipher signature")
            val signatureParam = params["sp"]
                ?: throw ParsingException("NewPipe in Environment decodeSignatureCipher Could not parse cipher signature parameter")
            val url = params["url"]?.let { URLBuilder(it) }
                ?: throw ParsingException("NewPipe in Environment decodeSignatureCipher Could not parse cipher url")
            url.parameters[signatureParam] =
                YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                    videoId,
                    obfuscatedSignature
                )
            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url.toString()
            )
        }
}
