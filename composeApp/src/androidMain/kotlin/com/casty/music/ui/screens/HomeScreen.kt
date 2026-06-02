package com.casty.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.casty.music.data.cleanTitle
import com.casty.music.ui.components.CastyArtwork
import com.casty.music.ui.components.ContentCard
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.HomeViewModel
import com.casty.music.viewmodel.TrackActionsViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    trackActionsViewModel: TrackActionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas)
            .pullRefresh(pullRefreshState)
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CastyTheme.colors.accentPink)
            }
        } else {
            LazyColumnHome(
                uiState = uiState,
                navController = navController,
                onRefresh = viewModel::refresh,
                onPlaySong = trackActionsViewModel::playNow,
            )
        }

        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = CastyTheme.colors.elevation2,
            contentColor = CastyTheme.colors.accentPink,
        )
    }
}

@Composable
private fun LazyColumnHome(
    uiState: com.casty.music.viewmodel.HomeUiState,
    navController: NavController,
    onRefresh: () -> Unit,
    onPlaySong: (com.casty.music.data.Song) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.greeting,
                    style = CastyTheme.typography.displayMedium.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = { navController.navigate("settingsScreenRoute") }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = CastyTheme.colors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (uiState.tasteProfile.hasSelections) {
            item {
                TasteHighlightsRow(chips = uiState.tasteProfile.highlightChips())
            }
        }

        if (uiState.error != null) {
            item {
                StatusBlock(
                    title = "Casty is warming up",
                    message = uiState.error,
                    actionText = "Retry",
                    onAction = onRefresh
                )
            }
        }

        if (
            uiState.quickAccessItems.isEmpty() &&
            uiState.recommendedPlaylists.isEmpty() &&
            uiState.recentlyPlayedAlbums.isEmpty()
        ) {
            item {
                StatusBlock(
                    title = "Finding music for you",
                    message = "Home will fill from Casty recommendations, search, and your signed-in YouTube Music library.",
                    actionText = "Refresh",
                    onAction = onRefresh
                )
            }
        }

        if (uiState.quickAccessItems.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.quickAccessItems.chunked(2).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { item ->
                                QuickAccessItem(
                                    title = item.song.cleanTitle(),
                                    imageUrl = item.song.thumbnailUrl,
                                    onClick = { onPlaySong(item.song) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        if (uiState.recommendedPlaylists.isNotEmpty()) {
            item {
                ShelfHeader("Recommended Playlists")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.recommendedPlaylists, key = { it.playlist.id }) { playlist ->
                        ContentCard(
                            title = playlist.playlist.name,
                            subtitle = playlist.subtitle ?: playlist.playlist.authorText ?: "Playlist",
                            imageUrl = playlist.playlist.thumbnailUrl,
                            onClick = {
                                navController.navigate("playlistScreenRoute/playlist/${playlist.playlist.id}")
                            }
                        )
                    }
                }
            }
        }

        if (uiState.recentlyPlayedAlbums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ShelfHeader("Recently Played Albums")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.recentlyPlayedAlbums, key = { it.id }) { album ->
                        ContentCard(
                            title = album.title.orEmpty(),
                            subtitle = album.authorsText ?: "Album",
                            imageUrl = album.thumbnailUrl,
                            onClick = {
                                navController.navigate("playlistScreenRoute/album/${album.id}")
                            }
                        )
                    }
                }
            }
        }

        if (uiState.featuredArtists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ShelfHeader(if (uiState.tasteProfile.hasSelections) "Artists for your taste" else "Featured Artists")
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.featuredArtists, key = { it.id }) { artist ->
                        ContentCard(
                            title = artist.name,
                            subtitle = "Artist",
                            imageUrl = artist.thumbnailUrl,
                            onClick = {
                                navController.navigate("artistScreenRoute/${artist.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasteHighlightsRow(chips: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Tuned for you",
            style = CastyTheme.typography.titleMedium.copy(
                color = CastyTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            chips.forEach { chip ->
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = {
                        Text(
                            text = chip,
                            style = CastyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CastyTheme.colors.elevation2,
                        selectedLabelColor = CastyTheme.colors.textPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun QuickAccessItem(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(CastyTheme.colors.elevation2)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CastyArtwork(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(4.dp),
            fallbackIconRes = com.casty.music.R.drawable.playlist
        )
        Text(
            text = title,
            style = CastyTheme.typography.bodyMedium.copy(
                color = CastyTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f),
            maxLines = 2
        )
    }
}

@Composable
private fun ShelfHeader(title: String) {
    Text(
        text = title,
        style = CastyTheme.typography.titleLarge.copy(
            color = CastyTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun StatusBlock(
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CastyTheme.colors.elevation2)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = CastyTheme.typography.titleMedium.copy(
                color = CastyTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = message,
            style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary)
        )
        Text(
            text = actionText,
            style = CastyTheme.typography.bodyMedium.copy(
                color = CastyTheme.colors.accentPink,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}
