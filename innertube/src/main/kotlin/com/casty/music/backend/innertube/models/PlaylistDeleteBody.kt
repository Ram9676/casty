package com.casty.music.backend.innertube.models.body

import com.casty.music.backend.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)
