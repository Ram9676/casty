package com.casty.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.casty.music.data.Song
import com.casty.music.ui.components.CastyArtwork
import com.casty.music.ui.components.ContentCard
import com.casty.music.ui.components.TrackOptionsSheet
import com.casty.music.ui.components.TrackListItem
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.ContentType
import com.casty.music.viewmodel.ContentViewModel

@Composable
fun ArtistScreen(
    navController: NavController,
    artistId: String,
    modifier: Modifier = Modifier,
    viewModel: ContentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTrack by remember { mutableStateOf<Song?>(null) }
    var showAllSongs by remember { mutableStateOf(false) }
    var showAllAlbums by remember { mutableStateOf(false) }

    LaunchedEffect(artistId) {
        viewModel.loadContent(ContentType.ARTIST, artistId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas)
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CastyTheme.colors.accentPink)
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
                    onClick = { viewModel.loadContent(ContentType.ARTIST, artistId) },
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
                // Large Artist Hero
                item {
                    ArtistHero(
                        name = uiState.title,
                        subscriberCount = uiState.description,
                        imageUrl = uiState.thumbnailUrl,
                        isFollowing = uiState.isFollowing,
                        onBackClick = { navController.popBackStack() },
                        onFollowClick = { viewModel.toggleFollow() },
                        onPlayClick = { viewModel.playAll(uiState.tracks) }
                    )
                }

                // Popular Songs section header
                item {
                    Text(
                        text = "Popular",
                        style = CastyTheme.typography.titleLarge.copy(
                            color = CastyTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                }

                // Songs (top 5 or all)
                val displayedTracks = if (showAllSongs) uiState.tracks else uiState.tracks.take(5)
                itemsIndexed(
                    items = displayedTracks,
                    key = { _, song -> song.id },
                ) { index, song ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = CastyTheme.typography.bodyLarge.copy(
                                color = CastyTheme.colors.textSecondary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp)
                        )
                        TrackListItem(
                            song = song,
                            onClick = { viewModel.playFromIndex(index) },
                            onMenuClick = { selectedTrack = song },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (uiState.tracks.size > 5) {
                    item {
                        Text(
                            text = if (showAllSongs) "Show less" else "See all",
                            style = CastyTheme.typography.bodyMedium.copy(
                                color = CastyTheme.colors.accentPink,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable { showAllSongs = !showAllSongs }
                                .padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
                        )
                    }
                }

                // Discography Section (Albums)
                if (uiState.artistAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            style = CastyTheme.typography.titleLarge.copy(
                                color = CastyTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                        )
                    }
                    item {
                        val displayedAlbums = if (showAllAlbums) uiState.artistAlbums else uiState.artistAlbums.take(4)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayedAlbums, key = { it.id }) { album ->
                                ContentCard(
                                    title = album.title,
                                    subtitle = "Album - YouTube Music",
                                    imageUrl = album.thumbnail,
                                    onClick = { navController.navigate("playlistScreenRoute/album/${album.id}") }
                                )
                            }
                        }
                    }
                    if (uiState.artistAlbums.size > 4) {
                        item {
                            Text(
                                text = if (showAllAlbums) "Show less" else "See all",
                                style = CastyTheme.typography.bodyMedium.copy(
                                    color = CastyTheme.colors.accentPink,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clickable { showAllAlbums = !showAllAlbums }
                                    .padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
                            )
                        }
                    }
                }

                // About section (Bio)
                if (uiState.description.isNotBlank()) {
                    item {
                        Text(
                            text = "About",
                            style = CastyTheme.typography.titleLarge.copy(
                                color = CastyTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CastyTheme.colors.elevation2)
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = uiState.description,
                                    style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textPrimary),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
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
private fun ArtistHero(
    name: String,
    subscriberCount: String,
    imageUrl: String?,
    isFollowing: Boolean,
    onBackClick: () -> Unit,
    onFollowClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Large background artist image
        CastyArtwork(
            model = imageUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            fallbackIconRes = com.casty.music.R.drawable.artist
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(start = 8.dp, top = 24.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Name, listeners, verified, action buttons at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = com.casty.music.R.drawable.sparkles),
                    contentDescription = "Verified Artist",
                    tint = CastyTheme.colors.accentPink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Verified Artist",
                    style = CastyTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (subscriberCount.isNotBlank()) {
                Text(
                    text = subscriberCount,
                    style = CastyTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = name,
                style = CastyTheme.typography.displayLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Follow Button
                Button(
                    onClick = onFollowClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color.Transparent else Color.White,
                        contentColor = if (isFollowing) Color.White else Color.Black
                    ),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isFollowing) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .height(36.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Play FAB
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CastyTheme.colors.accentPink)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Artist",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
