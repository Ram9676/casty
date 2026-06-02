package com.casty.music.backend.innertube.models.body

import com.casty.music.backend.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
