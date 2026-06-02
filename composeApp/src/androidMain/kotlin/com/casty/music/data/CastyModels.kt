package com.casty.music.data

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata as AndroidMediaMetadata
import com.casty.music.backend.innertube.models.AlbumItem
import com.casty.music.backend.innertube.models.ArtistItem
import com.casty.music.backend.innertube.models.PlaylistItem
import com.casty.music.backend.innertube.models.SongItem
import com.casty.music.backend.models.MediaMetadata
import com.casty.music.backend.models.toMediaItem
import java.time.LocalDateTime
import java.util.Locale

data class Song(
    val id: String,
    val title: String,
    val artistsText: String? = null,
    val durationText: String? = null,
    val thumbnailUrl: String? = null,
    val likedAt: LocalDateTime? = null,
)

data class SongEntity(val song: Song)

data class SearchQuery(val query: String)

data class Playlist(
    val id: String = "local-${System.currentTimeMillis()}",
    val name: String,
    val isYoutubePlaylist: Boolean = false,
    val thumbnailUrl: String? = null,
    val authorText: String? = null,
)

data class PlaylistPreview(
    val playlist: Playlist,
    val songCount: Int = 0,
    val subtitle: String? = null,
)

data class Album(
    val id: String,
    val title: String? = null,
    val authorsText: String? = null,
    val thumbnailUrl: String? = null,
)

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
)

enum class SongSortBy { DatePlayed, Title }
enum class PlaylistSortBy { Name }
enum class PlaylistSongSortBy { Position }
enum class AlbumSortBy { Title }
enum class ArtistSortBy { Name }
enum class SortOrder { Ascending, Descending }

fun Song.cleanTitle(): String = title
    .removePrefix("Song: ")
    .replace(Regex("\\s+"), " ")
    .trim()

fun durationTextToMillis(duration: String): Long {
    val parts = duration.split(":").mapNotNull { it.toLongOrNull() }
    return when (parts.size) {
        3 -> ((parts[0] * 3600) + (parts[1] * 60) + parts[2]) * 1000
        2 -> ((parts[0] * 60) + parts[1]) * 1000
        1 -> parts[0] * 1000
        else -> 0L
    }
}

fun durationSecondsToText(duration: Int?): String? {
    val total = duration ?: return null
    if (total < 0) return null
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

val Song.asMediaMetadata: MediaMetadata
    get() = MediaMetadata(
        id = id,
        title = title,
        artists = artistsText
            ?.split(",")
            ?.mapNotNull { name -> name.trim().takeIf { it.isNotBlank() } }
            ?.map { MediaMetadata.Artist(id = null, name = it) }
            .orEmpty(),
        duration = (durationText?.let(::durationTextToMillis)?.div(1000L) ?: -1L).toInt(),
        thumbnailUrl = thumbnailUrl,
        liked = likedAt != null,
        likedDate = likedAt,
    )

val Song.asMediaItem: MediaItem
    get() = asMediaMetadata.toMediaItem()

fun MediaMetadata.toCastySong(): Song = Song(
    id = id,
    title = title,
    artistsText = artists.joinToString(", ") { it.name }.ifBlank { null },
    durationText = durationSecondsToText(duration),
    thumbnailUrl = thumbnailUrl,
    likedAt = likedDate,
)



val SongItem.asSong: Song
    get() = Song(
        id = id,
        title = title,
        artistsText = artists.joinToString(", ") { it.name }.ifBlank { null },
        durationText = durationSecondsToText(duration),
        thumbnailUrl = thumbnail,
    )

fun SongItem.toMediaMetadata(): MediaMetadata = MediaMetadata(
    id = id,
    title = title,
    artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
    duration = duration ?: -1,
    thumbnailUrl = thumbnail,
    album = album?.let { MediaMetadata.Album(id = it.id, title = it.name) },
    setVideoId = setVideoId,
    musicVideoType = musicVideoType,
    explicit = explicit,
    libraryAddToken = libraryAddToken,
    libraryRemoveToken = libraryRemoveToken,
    isEpisode = isEpisode,
    uploadEntityId = uploadEntityId,
)

fun PlaylistItem.toCastyPlaylistPreview(): PlaylistPreview = PlaylistPreview(
    playlist = Playlist(
        id = id,
        name = title,
        isYoutubePlaylist = true,
        thumbnailUrl = thumbnail,
        authorText = author?.name,
    ),
    songCount = songCountText?.firstNumberOrNull() ?: 0,
    subtitle = listOfNotNull("Playlist", songCountText, author?.name)
        .joinToString(" - ")
        .ifBlank { null },
)

fun AlbumItem.toCastyAlbum(): Album = Album(
    id = id,
    title = title,
    authorsText = artists?.joinToString(", ") { it.name },
    thumbnailUrl = thumbnail,
)

fun ArtistItem.toCastyArtist(): Artist = Artist(
    id = id,
    name = title,
    thumbnailUrl = thumbnail,
)

private fun String.firstNumberOrNull(): Int? =
    Regex("\\d+").find(replace(",", ""))?.value?.toIntOrNull()
