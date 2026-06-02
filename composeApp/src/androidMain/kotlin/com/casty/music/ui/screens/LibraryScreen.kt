package com.casty.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.casty.music.ui.components.CastyArtwork
import com.casty.music.ui.components.ContentCard
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.LibraryFilter
import com.casty.music.viewmodel.LibraryItem
import com.casty.music.viewmodel.LibraryViewModel
import com.casty.music.data.Playlist

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: User Profile Avatar, Your Library Title, +, Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CastyTheme.colors.accentPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            style = CastyTheme.typography.titleMedium.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Your Library",
                        style = CastyTheme.typography.titleLarge.copy(
                            color = CastyTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Row {
                    IconButton(onClick = { navController.navigate("searchScreenRoute") }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Library",
                            tint = CastyTheme.colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Playlist",
                            tint = CastyTheme.colors.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Pinned Quick Access Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryQuickCard(
                    title = "Liked Songs",
                    iconRes = com.casty.music.R.drawable.heart,
                    gradientColors = listOf(Color(0xFFFF2D92), Color(0xFF1A7F3F)),
                    onClick = { viewModel.setFilter(LibraryFilter.LikedSongs) },
                    modifier = Modifier.weight(1f)
                )
                LibraryQuickCard(
                    title = "History",
                    iconRes = com.casty.music.R.drawable.history,
                    gradientColors = listOf(Color(0xFF535353), Color(0xFF333333)),
                    onClick = { viewModel.setFilter(LibraryFilter.History) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryQuickCard(
                    title = "Downloads",
                    iconRes = com.casty.music.R.drawable.download,
                    gradientColors = listOf(Color(0xFF4A90D9), Color(0xFF2C5F8A)),
                    onClick = { viewModel.setFilter(LibraryFilter.Downloads) },
                    modifier = Modifier.weight(1f)
                )
                LibraryQuickCard(
                    title = "Watch Later",
                    iconRes = com.casty.music.R.drawable.history,
                    gradientColors = listOf(Color(0xFFE8A317), Color(0xFFB07A0E)),
                    onClick = { viewModel.setFilter(LibraryFilter.WatchLater) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Chips
            FilterChipsRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            // Layout Control Row: Sorting and List/Grid toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sorting
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = CastyTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Most Recent",
                        style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textPrimary)
                    )
                }

                // Grid/List toggle
                IconButton(onClick = { viewModel.toggleViewMode() }) {
                    Icon(
                        painter = painterResource(
                        id = if (uiState.isGridView) com.casty.music.R.drawable.list_view else com.casty.music.R.drawable.grid_view
                        ),
                        contentDescription = "Toggle layout mode",
                        tint = CastyTheme.colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CastyTheme.colors.accentPink)
                }
            } else if (uiState.items.isEmpty()) {
                LibraryStatusBlock(
                    title = if (uiState.error == null) uiState.selectedFilter.emptyTitle else "Library could not load",
                    message = uiState.error
                        ?: uiState.selectedFilter.emptyMessage,
                    actionText = "Refresh",
                    onAction = viewModel::refresh
                )
            } else {
                if (uiState.isGridView) {
                    LibraryGrid(
                        items = uiState.items,
                        onItemClick = { item ->
                            when (item) {
                                is LibraryItem.TrackItem -> viewModel.playSong(item.song)
                                is LibraryItem.PlaylistItem -> navController.navigate("playlistScreenRoute/playlist/${item.playlist.playlist.id}")
                                is LibraryItem.AlbumItem -> navController.navigate("playlistScreenRoute/album/${item.album.id}")
                                is LibraryItem.ArtistItem -> navController.navigate("artistScreenRoute/${item.artist.id}")
                            }
                        },
                        onItemLongClick = { item ->
                            if (item is LibraryItem.PlaylistItem) {
                                playlistToDelete = item.playlist.playlist
                            }
                        }
                    )
                } else {
                    LibraryList(
                        items = uiState.items,
                        onItemClick = { item ->
                            when (item) {
                                is LibraryItem.TrackItem -> viewModel.playSong(item.song)
                                is LibraryItem.PlaylistItem -> navController.navigate("playlistScreenRoute/playlist/${item.playlist.playlist.id}")
                                is LibraryItem.AlbumItem -> navController.navigate("playlistScreenRoute/album/${item.album.id}")
                                is LibraryItem.ArtistItem -> navController.navigate("artistScreenRoute/${item.artist.id}")
                            }
                        },
                        onItemLongClick = { item ->
                            if (item is LibraryItem.PlaylistItem) {
                                playlistToDelete = item.playlist.playlist
                            }
                        }
                    )
                }
            }
        }
    }

    // Playlist Creation Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCreatePlaylistDialog = false
                playlistNameInput = ""
            },
            containerColor = CastyTheme.colors.elevation2,
            title = {
                Text(
                    text = "Create Playlist",
                    style = CastyTheme.typography.titleLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for your playlist.",
                        style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    BasicTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        textStyle = TextStyle(
                            color = CastyTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontFamily = CastyTheme.typography.bodyLarge.fontFamily
                        ),
                        cursorBrush = SolidColor(CastyTheme.colors.accentPink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CastyTheme.colors.elevation3, RoundedCornerShape(4.dp))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput)
                        }
                        showCreatePlaylistDialog = false
                        playlistNameInput = ""
                    }
                ) {
                    Text(
                        text = "Create",
                        style = CastyTheme.typography.bodyLarge.copy(
                            color = CastyTheme.colors.accentPink,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCreatePlaylistDialog = false
                        playlistNameInput = ""
                    }
                ) {
                    Text(
                        text = "Cancel",
                        style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary)
                    )
                }
            }
        )
    }

    // Delete Playlist Dialog
    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = CastyTheme.colors.elevation2,
            title = {
                Text(
                    text = "Delete Playlist",
                    style = CastyTheme.typography.titleLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${playlist.name}\"? This cannot be undone.",
                    style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist)
                        playlistToDelete = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        style = CastyTheme.typography.bodyLarge.copy(
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { playlistToDelete = null }
                ) {
                    Text(
                        text = "Cancel",
                        style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary)
                    )
                }
            }
        )
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit
) {
    val filters = LibraryFilter.values()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) CastyTheme.colors.accentPink else CastyTheme.colors.elevation2
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = filter.label,
                    style = CastyTheme.typography.bodyMedium.copy(
                        color = if (isSelected) Color.Black else CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun LibraryList(
    items: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onItemLongClick: (LibraryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(items, key = { it.libraryKey }) { item ->
            LibraryRowItem(
                item = item,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGrid(
    items: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    onItemLongClick: (LibraryItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items, key = { it.libraryKey }) { item ->
            val title = when (item) {
                is LibraryItem.TrackItem -> item.song.title
                is LibraryItem.PlaylistItem -> item.playlist.playlist.name
                is LibraryItem.AlbumItem -> item.album.title
                is LibraryItem.ArtistItem -> item.artist.name
            }
            val subtitle = when (item) {
                is LibraryItem.PlaylistItem -> "Playlist - ${item.playlist.songCount} songs"
                is LibraryItem.AlbumItem -> "Album - ${item.album.authorsText}"
                is LibraryItem.TrackItem -> item.song.artistsText ?: if (item.song.likedAt != null) "Liked song" else "Song"
                is LibraryItem.ArtistItem -> "Artist"
            }
            val imageUrl = when (item) {
                is LibraryItem.TrackItem -> item.song.thumbnailUrl
                is LibraryItem.PlaylistItem -> item.playlist.playlist.thumbnailUrl
                is LibraryItem.AlbumItem -> item.album.thumbnailUrl
                is LibraryItem.ArtistItem -> item.artist.thumbnailUrl
            }
            ContentCard(
                title = title.orEmpty(),
                subtitle = subtitle,
                imageUrl = imageUrl,
                onClick = { onItemClick(item) },
                fillParentWidth = true,
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryRowItem(
    item: LibraryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val title = when (item) {
        is LibraryItem.TrackItem -> item.song.title
        is LibraryItem.PlaylistItem -> item.playlist.playlist.name
        is LibraryItem.AlbumItem -> item.album.title
        is LibraryItem.ArtistItem -> item.artist.name
    }
    val subtitle = when (item) {
        is LibraryItem.PlaylistItem -> "Playlist - ${item.playlist.songCount} songs"
        is LibraryItem.AlbumItem -> "Album - ${item.album.authorsText}"
        is LibraryItem.TrackItem -> item.song.artistsText ?: if (item.song.likedAt != null) "Liked song" else "Song"
        is LibraryItem.ArtistItem -> "Artist"
    }
    val imageUrl = when (item) {
        is LibraryItem.TrackItem -> item.song.thumbnailUrl
        is LibraryItem.PlaylistItem -> item.playlist.playlist.thumbnailUrl
        is LibraryItem.AlbumItem -> item.album.thumbnailUrl
        is LibraryItem.ArtistItem -> item.artist.thumbnailUrl
    }
    val isArtist = item is LibraryItem.ArtistItem

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        CastyArtwork(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp),
            shape = if (isArtist) CircleShape else RoundedCornerShape(4.dp),
            fallbackIconRes = when (item) {
                is LibraryItem.ArtistItem -> com.casty.music.R.drawable.artist
                is LibraryItem.TrackItem -> com.casty.music.R.drawable.musical_notes
                else -> com.casty.music.R.drawable.playlist
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.orEmpty(),
                style = CastyTheme.typography.bodyLarge.copy(
                    color = CastyTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
                maxLines = 1
            )
        }
    }
}

private val LibraryFilter.label: String
    get() = when (this) {
        LibraryFilter.History -> "History"
        LibraryFilter.LikedSongs -> "Liked Songs"
        LibraryFilter.WatchLater -> "Watch Later"
        LibraryFilter.Downloads -> "Downloads"
        LibraryFilter.Playlists -> "Playlists"
        LibraryFilter.Albums -> "Albums"
        LibraryFilter.Artists -> "Artists"
    }

private val LibraryFilter.emptyTitle: String
    get() = when (this) {
        LibraryFilter.History -> "No playback history yet"
        LibraryFilter.LikedSongs -> "No liked songs yet"
        LibraryFilter.WatchLater -> "Watch Later is empty"
        LibraryFilter.Downloads -> "No downloads yet"
        LibraryFilter.Playlists -> "No playlists yet"
        LibraryFilter.Albums -> "No saved albums yet"
        LibraryFilter.Artists -> "No followed artists yet"
    }

private val LibraryFilter.emptyMessage: String
    get() = when (this) {
        LibraryFilter.History -> "Play songs, albums, or playlists and Casty will keep a short recent history here."
        LibraryFilter.LikedSongs -> "Tap the heart on a track to keep it in Liked Songs."
        LibraryFilter.WatchLater -> "Sign in and sync your YouTube Watch Later playlist to show it here."
        LibraryFilter.Downloads -> "Download playlists or tracks for offline playback."
        LibraryFilter.Playlists -> "Sign in or create a playlist to build your library."
        LibraryFilter.Albums -> "Saved albums from your account will appear here."
        LibraryFilter.Artists -> "Follow artists to keep them close."
    }

private val LibraryItem.libraryKey: String
    get() = when (this) {
        is LibraryItem.TrackItem -> "track:${song.id}"
        is LibraryItem.PlaylistItem -> "playlist:${playlist.playlist.id}"
        is LibraryItem.AlbumItem -> "album:${album.id}"
        is LibraryItem.ArtistItem -> "artist:${artist.id}"
    }

@Composable
private fun LibraryStatusBlock(
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
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

@Composable
private fun LibraryQuickCard(
    title: String,
    iconRes: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            style = CastyTheme.typography.bodyMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}
