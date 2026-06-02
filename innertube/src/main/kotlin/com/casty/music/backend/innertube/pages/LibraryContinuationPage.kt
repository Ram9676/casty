package com.casty.music.backend.innertube.pages

import com.casty.music.backend.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
