package com.casty.music.backend.innertube.pages

import com.casty.music.backend.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
