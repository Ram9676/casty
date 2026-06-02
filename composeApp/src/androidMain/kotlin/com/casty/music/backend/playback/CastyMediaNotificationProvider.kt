package com.casty.music.backend.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.media3.common.util.UnstableApi
import com.casty.music.R

/**
 * Placeholder for future custom notification provider.
 * Currently we use the default reliable provider directly in MusicService
 * to guarantee a stable build and working background/lockscreen playback.
 *
 * Do not delete this file yet — we will make a proper custom one later.
 */
@UnstableApi
object CastyMediaNotificationProvider {
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "casty_now_playing",
                context.getString(R.string.now_playing_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Casty music playback controls"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}