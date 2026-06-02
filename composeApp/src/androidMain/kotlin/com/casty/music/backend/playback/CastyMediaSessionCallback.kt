@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.casty.music.backend.playback

import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * God-level MediaLibrarySession.Callback for Casty.
 * 
 * This is the brain of background / lock screen / Android Auto / Wear interaction.
 * Currently solid foundation. Future expansions (very ambitious):
 * - Heart / like song directly from lock screen / notification
 * - "Play radio from this song" from notification
 * - Browse library from Android Auto
 * - Voice commands integration
 */
class CastyMediaSessionCallback : MediaLibraryService.MediaLibrarySession.Callback {

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        // For now return empty root. Can be expanded to full library browsing later.
        return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
    }

    // You can add onCustomCommand here for premium actions (like, save playlist, etc.)
    // Example skeleton:
    // override fun onCustomCommand(...) { ... return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS)) }
}
