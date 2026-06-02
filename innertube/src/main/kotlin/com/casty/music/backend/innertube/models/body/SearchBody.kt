package com.casty.music.backend.innertube.models.body

import com.casty.music.backend.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
