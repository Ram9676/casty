package com.casty.music.backend.innertube.models.body

import com.casty.music.backend.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeBody(
    val channelIds: List<String>,
    val context: Context,
    val params: String? = null,
)
