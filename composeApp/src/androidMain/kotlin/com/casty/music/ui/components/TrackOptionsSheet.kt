package com.casty.music.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.casty.music.data.Song
import com.casty.music.data.cleanTitle
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.TrackActionsViewModel
import com.casty.music.R

@Composable
@OptIn(UnstableApi::class)
fun TrackOptionsSheet(
    visible: Boolean,
    song: Song?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackActionsViewModel = hiltViewModel()
) {
    val selectedSong = song ?: return
    val context = LocalContext.current
    var showPlaylistPicker by remember { mutableStateOf(false) }

    fun complete(message: String) {
        onDismiss()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    CastyBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CastyTheme.colors.elevation2, RoundedCornerShape(12.dp))
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CastyArtwork(
                    model = selectedSong.thumbnailUrl,
                    contentDescription = selectedSong.title,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedSong.cleanTitle(),
                        style = CastyTheme.typography.titleSmall.copy(
                            color = CastyTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedSong.artistsText.orEmpty(),
                        style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            TrackOptionRow(
                iconRes = R.drawable.play,
                label = "Play now",
                tint = CastyTheme.colors.accentPink
            ) {
                viewModel.playNow(selectedSong)
                complete("Playing ${selectedSong.cleanTitle()}")
            }

            TrackOptionRow(
                iconRes = R.drawable.play_skip_forward,
                label = "Play next"
            ) {
                viewModel.playNext(selectedSong)
                complete("Will play next")
            }

            TrackOptionRow(
                iconRes = R.drawable.addqueue,
                label = "Add to queue"
            ) {
                viewModel.addToQueue(selectedSong)
                complete("Added to queue")
            }

            TrackOptionRow(
                iconRes = R.drawable.heart_outline,
                label = "Like / unlike"
            ) {
                viewModel.toggleLike(selectedSong)
                complete("Updated liked songs")
            }

            TrackOptionRow(
                iconRes = R.drawable.playlist,
                label = "Add to playlist"
            ) {
                showPlaylistPicker = true
            }

            TrackOptionRow(
                iconRes = R.drawable.download,
                label = "Download for offline"
            ) {
                viewModel.downloadForOffline(selectedSong)
                complete("Caching for offline")
            }

            TrackOptionRow(
                iconRes = R.drawable.share_social,
                label = "Share"
            ) {
                val shareText = buildString {
                    append(selectedSong.cleanTitle())
                    if (!selectedSong.artistsText.isNullOrBlank()) {
                        append(" - ")
                        append(selectedSong.artistsText)
                    }
                    append("\nhttps://music.youtube.com/watch?v=")
                    append(selectedSong.id)
                }
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, shareText),
                        "Share track"
                    )
                )
                onDismiss()
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showPlaylistPicker) {
                val playlists by viewModel.getPlaylists().collectAsStateWithLifecycle(initialValue = emptyList())
                AlertDialog(
                    onDismissRequest = { showPlaylistPicker = false },
                    containerColor = CastyTheme.colors.elevation2,
                    title = {
                        Text(
                            text = "Add to playlist",
                            style = CastyTheme.typography.titleLarge.copy(
                                color = CastyTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    text = {
                        if (playlists.isEmpty()) {
                            Text(
                                text = "No playlists yet. Create one from Your Library.",
                                style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary)
                            )
                        } else {
                            Column {
                                playlists.forEach { playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.addToPlaylist(selectedSong, playlist.playlist.id)
                                                showPlaylistPicker = false
                                                complete("Added to ${playlist.playlist.name}")
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.playlist),
                                            contentDescription = null,
                                            tint = CastyTheme.colors.textSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = playlist.playlist.name,
                                                style = CastyTheme.typography.bodyLarge.copy(
                                                    color = CastyTheme.colors.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "${playlist.songCount} songs",
                                                style = CastyTheme.typography.bodySmall.copy(color = CastyTheme.colors.textSecondary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showPlaylistPicker = false }) {
                            Text(
                                text = "Cancel",
                                style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackOptionRow(
    iconRes: Int,
    label: String,
    tint: Color = CastyTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            style = CastyTheme.typography.titleSmall.copy(color = CastyTheme.colors.textPrimary)
        )
    }
}
