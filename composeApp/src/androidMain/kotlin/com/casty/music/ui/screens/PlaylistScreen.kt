package com.casty.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.casty.music.data.Song
import com.casty.music.ui.components.CastyArtwork
import com.casty.music.ui.components.TrackOptionsSheet
import com.casty.music.ui.components.TrackListItem
import com.casty.music.ui.components.SkeletonTrackList
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.ContentType
import com.casty.music.viewmodel.ContentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    navController: NavController,
    contentType: ContentType,
    contentId: String,
    modifier: Modifier = Modifier,
    viewModel: ContentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTrack by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(contentType, contentId) {
        viewModel.loadContent(contentType, contentId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas)
    ) {
        if (uiState.isLoading) {
            // World-class shimmer loading (no janky spinner)
            Column(Modifier.fillMaxSize()) {
                // Fake hero header while loading
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(CastyTheme.colors.elevation2)
                )
                Spacer(Modifier.height(24.dp))
                SkeletonTrackList(count = 9, modifier = Modifier.padding(horizontal = 8.dp))
            }
        } else if (uiState.error != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Oops! Something went wrong",
                    style = CastyTheme.typography.titleLarge.copy(color = Color.Red, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error ?: "",
                    style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadContent(contentType, contentId) },
                    colors = ButtonDefaults.buttonColors(containerColor = CastyTheme.colors.accentPink)
                ) {
                    Text(text = "Retry", color = Color.Black)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Large Hero Header
                item {
                    HeroHeader(
                        title = uiState.title,
                        description = uiState.description,
                        imageUrl = uiState.thumbnailUrl,
                        isDownloaded = uiState.isDownloaded,
                        isFollowing = uiState.isFollowing,
                        contentType = contentType,
                        onBackClick = { navController.popBackStack() },
                        onDownloadClick = { viewModel.toggleDownload() },
                        onFollowClick = { viewModel.toggleFollow() },
                        onPlayAllClick = { viewModel.playAll(uiState.tracks) }
                    )
                }

                // Songs List
                itemsIndexed(
                    items = uiState.tracks,
                    key = { _, song -> song.id },
                ) { index, song ->
                    TrackListItem(
                        song = song,
                        onClick = { viewModel.playFromIndex(index) },
                        onMenuClick = { selectedTrack = song }
                    )
                }
            }
        }

        TrackOptionsSheet(
            visible = selectedTrack != null,
            song = selectedTrack,
            onDismiss = { selectedTrack = null }
        )
    }
}

@Composable
private fun HeroHeader(
    title: String,
    description: String,
    imageUrl: String?,
    isDownloaded: Boolean,
    isFollowing: Boolean,
    contentType: ContentType,
    onBackClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFollowClick: () -> Unit,
    onPlayAllClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3264).copy(alpha = 0.6f),
                        CastyTheme.colors.systemCanvas
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
        ) {
            // Navigation Back Arrow
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CastyTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CastyArtwork(
                    model = imageUrl,
                    contentDescription = "Playlist Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(170.dp),
                    shape = RoundedCornerShape(8.dp),
                    fallbackIconRes = com.casty.music.R.drawable.playlist
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info (Title, description, type, downloads)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = CastyTheme.typography.titleLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Actions Row: Follow, Download, Play FAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like/Follow Button
                        IconButton(onClick = onFollowClick) {
                            Icon(
                                painter = painterResource(
                                    id = if (isFollowing) com.casty.music.R.drawable.heart else com.casty.music.R.drawable.heart_outline
                                ),
                                contentDescription = "Like",
                                tint = if (isFollowing) CastyTheme.colors.accentPink else CastyTheme.colors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Download Button
                        IconButton(onClick = onDownloadClick) {
                            Icon(
                                painter = painterResource(
                                    id = if (isDownloaded) com.casty.music.R.drawable.downloaded else com.casty.music.R.drawable.download
                                ),
                                contentDescription = "Download",
                                tint = if (isDownloaded) CastyTheme.colors.accentPink else CastyTheme.colors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Green Circular Play Button FAB
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CastyTheme.colors.accentPink)
                            .clickable(onClick = onPlayAllClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play all",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
