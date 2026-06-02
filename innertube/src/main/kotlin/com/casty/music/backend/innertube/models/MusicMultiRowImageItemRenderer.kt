package com.casty.music.backend.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicMultiRowImageItemRenderer(
    val title: Runs,
    val subtitle: Runs,
    val thumbnail: ThumbnailRenderer,
    val onTap: NavigationEndpoint,
)
