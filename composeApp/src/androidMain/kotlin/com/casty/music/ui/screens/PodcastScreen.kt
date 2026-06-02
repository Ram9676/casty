package com.casty.music.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.casty.music.data.durationSecondsToText
import com.casty.music.ui.components.CastyArtwork
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.PodcastViewModel

@Composable
fun PodcastScreen(
    navController: NavController,
    showId: String,
    modifier: Modifier = Modifier,
    viewModel: PodcastViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(showId) {
        viewModel.loadPodcast(showId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = CastyTheme.colors.accentPink,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            uiState.error != null -> {
                PodcastStatus(
                    title = "Podcast could not load",
                    message = uiState.error.orEmpty(),
                    onBack = { navController.popBackStack() },
                    onRetry = { viewModel.loadPodcast(showId) },
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = CastyTheme.colors.textPrimary,
                                )
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, uiState.shareLink)
                                    context.startActivity(Intent.createChooser(shareIntent, null))
                                },
                                enabled = uiState.shareLink.isNotBlank(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = CastyTheme.colors.textPrimary,
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            CastyArtwork(
                                model = uiState.thumbnailUrl,
                                contentDescription = uiState.title,
                                contentScale = ContentScale.Crop,
                                artworkSizePx = 720,
                                modifier = Modifier.size(118.dp),
                                shape = RoundedCornerShape(8.dp),
                                fallbackIconRes = com.casty.music.R.drawable.podcast,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = uiState.title.ifBlank { "Podcast" },
                                    style = CastyTheme.typography.titleLarge.copy(
                                        color = CastyTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = uiState.author.ifBlank { "YouTube Music" },
                                    style = CastyTheme.typography.bodyMedium.copy(
                                        color = CastyTheme.colors.accentPink,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                uiState.episodeCountText?.takeIf { it.isNotBlank() }?.let { count ->
                                    Text(
                                        text = count,
                                        style = CastyTheme.typography.bodyMedium.copy(
                                            color = CastyTheme.colors.textSecondary,
                                        ),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val hasEpisodes = uiState.episodes.isNotEmpty()
                            Button(
                                onClick = { viewModel.playEpisode(0) },
                                enabled = hasEpisodes,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CastyTheme.colors.accentPink,
                                    contentColor = Color.Black,
                                ),
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play")
                            }

                            OutlinedButton(
                                onClick = viewModel::toggleSaved,
                                enabled = !uiState.isActionLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CastyTheme.colors.textPrimary,
                                ),
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                Text(if (uiState.isSaved) "Saved" else "Save")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Episodes",
                            style = CastyTheme.typography.titleMedium.copy(
                                color = CastyTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                        )
                    }

                    if (uiState.episodes.isEmpty()) {
                        item {
                            Text(
                                text = "No episodes were returned by YouTube Music for this show.",
                                style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.episodes,
                            key = { _, episode -> episode.id },
                        ) { index, episode ->
                            PodcastEpisodeRow(
                                title = episode.title,
                                subtitle = listOfNotNull(
                                    episode.publishDateText,
                                    durationSecondsToText(episode.duration),
                                ).joinToString(" - "),
                                imageUrl = episode.thumbnail,
                                onClick = { viewModel.playEpisode(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastEpisodeRow(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CastyArtwork(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            artworkSizePx = 512,
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(8.dp),
            fallbackIconRes = com.casty.music.R.drawable.podcast,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CastyTheme.typography.bodyLarge.copy(
                    color = CastyTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play episode",
                tint = CastyTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun PodcastStatus(
    title: String,
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = CastyTheme.colors.textPrimary,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CastyTheme.colors.elevation2)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = CastyTheme.typography.titleMedium.copy(
                    color = CastyTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = message,
                style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CastyTheme.colors.accentPink,
                    contentColor = Color.Black,
                ),
            ) {
                Text("Retry")
            }
        }
    }
}
