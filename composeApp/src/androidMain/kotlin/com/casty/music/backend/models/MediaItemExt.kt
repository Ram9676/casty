@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.casty.music.backend.models

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata as AndroidMediaMetadata
import com.casty.music.backend.innertube.models.SongItem
import java.util.Locale

fun MediaMetadata.toMediaItem(): MediaItem {
    val self = this
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(id.toUri())
                .setExtras(Bundle().apply { putSerializable("casty_metadata", self) })
                .build()
        )
        .setMediaMetadata(
            AndroidMediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artists.joinToString(", ") { it.name })
                .setDisplayTitle(title)
                .setSubtitle(artists.joinToString(", ") { it.name })
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    Bundle().apply {
                        durationSecondsToText(duration)?.let { putString("durationText", it) }
                        thumbnailUrl?.let { putString("artwork_uri", it) }
                    }
                )
                .setIsPlayable(true)
                .build()
        )
        .build()
}

fun SongItem.toMediaMetadata(): MediaMetadata =
    MediaMetadata(
        id = id,
        title = title,
        artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
        duration = duration ?: -1,
        thumbnailUrl = thumbnail,
        album = album?.let { MediaMetadata.Album(id = it.id, title = it.name) },
        setVideoId = setVideoId,
        musicVideoType = musicVideoType,
        explicit = explicit,
        likedDate = null,
        libraryAddToken = libraryAddToken,
        libraryRemoveToken = libraryRemoveToken,
        isEpisode = isEpisode,
        uploadEntityId = uploadEntityId,
    )

fun SongItem.toMediaItem(): MediaItem = toMediaMetadata().toMediaItem()

private fun durationSecondsToText(duration: Int?): String? {
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
