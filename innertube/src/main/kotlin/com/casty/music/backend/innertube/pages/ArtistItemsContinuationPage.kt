package com.casty.music.backend.innertube.pages

import com.casty.music.backend.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
