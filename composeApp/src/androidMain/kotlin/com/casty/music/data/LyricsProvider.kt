package com.casty.music.data

import it.fast4x.kugou.KuGou
import it.fast4x.lrclib.LrcLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

data class LyricLine(
    val timestampMs: Long,
    val text: String,
)

@Singleton
class LyricsProvider @Inject constructor() {
    private val memoryCache = ConcurrentHashMap<String, List<LyricLine>>()

    fun getLyrics(
        songId: String,
        title: String,
        artist: String,
        durationMs: Long,
    ): Flow<List<LyricLine>> = flow {
        memoryCache[songId]?.let {
            emit(it)
            return@flow
        }

        val cleanTitle = title.cleanLyricsTitle()
        val cleanArtist = artist.cleanLyricsArtist()

        val lrcLibLines = runCatching {
            LrcLib.lyrics(
                artist = cleanArtist,
                title = cleanTitle,
                duration = durationMs.coerceAtLeast(0L).milliseconds,
            )?.getOrNull()?.text.toLyricLines()
        }.getOrDefault(emptyList())

        if (lrcLibLines.isNotEmpty()) {
            memoryCache[songId] = lrcLibLines
            emit(lrcLibLines)
            return@flow
        }

        val kugouLines = runCatching {
            KuGou.lyrics(
                artist = cleanArtist,
                title = cleanTitle,
                duration = durationMs.coerceAtLeast(0L) / 1000L,
            )?.getOrNull()?.value.toLyricLines()
        }.getOrDefault(emptyList())

        if (kugouLines.isNotEmpty()) {
            memoryCache[songId] = kugouLines
        }

        // Casty policy (user request): Only English + Telugu. 
        // Everything else falls back to English/romanized to keep APK small and experience consistent.
        // No heavy Casty-style language conversion/translation.
        emit(kugouLines)
    }.flowOn(Dispatchers.IO)

    private fun String?.toLyricLines(): List<LyricLine> =
        orEmpty()
            .lineSequence()
            .flatMap { line ->
                val matches = lrcLineRegex.findAll(line).toList()
                if (matches.isEmpty()) return@flatMap emptySequence()

                val text = line.substring(matches.last().range.last + 1).trim()
                if (text.isBlank()) return@flatMap emptySequence()

                matches.mapNotNull { match ->
                    val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                    val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                    val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                    LyricLine(
                        timestampMs = minutes * 60_000L + seconds * 1000L + fraction,
                        text = text,
                    )
                }.asSequence()
            }
            .distinctBy { it.timestampMs to it.text }
            .sortedBy { it.timestampMs }
            .toList()

    private fun String.cleanLyricsTitle(): String =
        removePrefix("Song: ")
            .replace(Regex("\\s*\\([^)]*(official|lyrics?|audio|video|visualizer|remaster|remastered|radio edit)[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[[^]]*(official|lyrics?|audio|video|visualizer|remaster|remastered|radio edit)[^]]*]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(feat\\.|ft\\.|featuring)\\s+.+$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun String.cleanLyricsArtist(): String =
        substringBefore(",")
            .replace(Regex("\\s+(feat\\.|ft\\.|featuring)\\s+.+$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private companion object {
        val lrcLineRegex = Regex("""\[(\d{1,3}):(\d{2})[.:](\d{1,3})]""")
    }
}
