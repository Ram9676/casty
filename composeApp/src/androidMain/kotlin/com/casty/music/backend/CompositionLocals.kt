package com.casty.music.backend

import androidx.compose.runtime.staticCompositionLocalOf
import com.casty.music.backend.playback.PlayerConnection

val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { null }
