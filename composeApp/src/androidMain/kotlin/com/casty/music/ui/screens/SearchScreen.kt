package com.casty.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.casty.music.ui.components.CastyArtwork
import com.casty.music.ui.components.ContentCard
import com.casty.music.ui.components.TrackOptionsSheet
import com.casty.music.ui.components.TrackListItem
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.SearchViewModel
import com.casty.music.data.SearchQuery
import com.casty.music.data.Song
import com.casty.music.data.asSong
import com.casty.music.backend.innertube.models.AlbumItem
import com.casty.music.backend.innertube.models.ArtistItem
import com.casty.music.backend.innertube.models.EpisodeItem
import com.casty.music.backend.innertube.models.PlaylistItem
import com.casty.music.backend.innertube.models.PodcastItem
import com.casty.music.backend.innertube.models.SongItem
import com.casty.music.backend.innertube.models.YTItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    navController: NavController,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var selectedTrack by remember { mutableStateOf<Song?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Title
            Text(
                text = "Search",
                style = CastyTheme.typography.displayMedium.copy(
                    color = CastyTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
            )

            // Search Bar
            SearchBar(
                query = uiState.query,
                onQueryChange = { viewModel.onQueryChanged(it) },
                onSearch = { 
                    viewModel.performSearch(it)
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                onClear = { viewModel.onQueryChanged("") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.query.isBlank()) {
                // Default search state: Recent Searches & Categories
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (uiState.recentSearches.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent searches",
                                style = CastyTheme.typography.titleMedium.copy(
                                    color = CastyTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(uiState.recentSearches.take(5)) { search ->
                            RecentSearchRow(
                                searchQuery = search,
                                onClick = { viewModel.onQueryChanged(search.query) },
                                onDelete = { viewModel.deleteRecentSearch(search) }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    item {
                        Text(
                            text = "Browse all",
                            style = CastyTheme.typography.titleMedium.copy(
                                color = CastyTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    item {
                        CategoryGrid(onCategoryClick = { category ->
                            viewModel.onQueryChanged(category)
                        })
                    }
                }
            } else {
                // Search result state: Filter Chips & Results List
                FilterTabs(
                    selectedFilterIndex = uiState.selectedFilterIndex,
                    onFilterSelected = { viewModel.setFilterIndex(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CastyTheme.colors.accentPink)
                    }
                } else {
                    SearchResultsContent(
                        selectedFilterIndex = uiState.selectedFilterIndex,
                        uiState = uiState,
                        onSongClick = onPlaySong,
                        onSongMenuClick = { selectedTrack = it },
                        onAlbumClick = { id -> navController.navigate("playlistScreenRoute/album/$id") },
                        onArtistClick = { id -> navController.navigate("artistScreenRoute/$id") },
                        onPlaylistClick = { id -> navController.navigate("playlistScreenRoute/playlist/$id") },
                        onPodcastClick = { id -> navController.navigate("podcastScreenRoute/$id") },
                        onLoadMore = viewModel::loadMore
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CastyTheme.colors.elevation2)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search Icon",
            tint = CastyTheme.colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "What do you want to listen to?",
                    style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = CastyTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = CastyTheme.typography.bodyLarge.fontFamily
                ),
                cursorBrush = SolidColor(CastyTheme.colors.accentPink),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch(query) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear Search",
                    tint = CastyTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    searchQuery: SearchQuery,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = com.casty.music.R.drawable.history),
                contentDescription = null,
                tint = CastyTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = searchQuery.query,
                style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textPrimary)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Remove from history",
                tint = CastyTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FilterTabs(
    selectedFilterIndex: Int,
    onFilterSelected: (Int) -> Unit
) {
    val filters = listOf("All", "Songs", "Albums", "Artists", "Playlists", "Podcasts")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEachIndexed { index, title ->
            val isSelected = selectedFilterIndex == index
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) CastyTheme.colors.accentPink else CastyTheme.colors.elevation2
                    )
                    .clickable { onFilterSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (title == "All") {
                    Icon(
                        painter = painterResource(id = com.casty.music.R.drawable.search_circle),
                        contentDescription = null,
                        tint = if (isSelected) Color.Black else CastyTheme.colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
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
private fun SearchResultsContent(
    selectedFilterIndex: Int,
    uiState: com.casty.music.viewmodel.SearchUiState,
    onSongClick: (Song) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onPodcastClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val selectedResultsEmpty = when (selectedFilterIndex) {
        0 -> uiState.searchResultsAll.isEmpty()
        1 -> uiState.searchResultsSongs.isEmpty()
        2 -> uiState.searchResultsAlbums.isEmpty()
        3 -> uiState.searchResultsArtists.isEmpty()
        4 -> uiState.searchResultsPlaylists.isEmpty()
        else -> uiState.searchResultsPodcasts.isEmpty()
    }

    if (uiState.error != null) {
        SearchStatusBlock(
            title = "Search could not load",
            message = uiState.error,
        )
        return
    }

    if (selectedResultsEmpty) {
        if (uiState.hasPerformedSearch) {
            SearchStatusBlock(
                title = "No results found",
                message = "Try a song, artist, album, or playlist name. Casty searches YouTube Music directly.",
            )
        } else {
            ShimmerLoadingSkeleton()
        }
        return
    }

    when (selectedFilterIndex) {
        0 -> { // All
            val listState = rememberLazyListState()
            LoadMoreListEffect(
                listState = listState,
                hasMore = uiState.hasMore,
                isLoadingMore = uiState.isLoadingMore,
                onLoadMore = onLoadMore
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(uiState.searchResultsAll, key = { "${it::class.java.name}:${it.id}" }) { item ->
                    MixedSearchRow(
                        item = item,
                        onSongClick = onSongClick,
                        onSongMenuClick = onSongMenuClick,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                        onPodcastClick = onPodcastClick
                    )
                }
                loadingMoreFooter(uiState.isLoadingMore)
            }
        }
        1 -> { // Songs
            val listState = rememberLazyListState()
            LoadMoreListEffect(
                listState = listState,
                hasMore = uiState.hasMore,
                isLoadingMore = uiState.isLoadingMore,
                onLoadMore = onLoadMore
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(uiState.searchResultsSongs, key = { it.id }) { songItem ->
                    val song = songItem.asSong
                    TrackListItem(
                        song = song,
                        onClick = { onSongClick(song) },
                        onMenuClick = { onSongMenuClick(song) }
                    )
                }
                loadingMoreFooter(uiState.isLoadingMore)
            }
        }
        2 -> { // Albums
            val gridState = rememberLazyGridState()
            LoadMoreGridEffect(
                gridState = gridState,
                hasMore = uiState.hasMore,
                isLoadingMore = uiState.isLoadingMore,
                onLoadMore = onLoadMore
            )
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.searchResultsAlbums, key = { it.id }) { albumItem ->
                    ContentCard(
                        title = albumItem.title,
                        subtitle = albumItem.artists?.firstOrNull()?.name ?: "Album",
                        imageUrl = albumItem.thumbnail,
                        onClick = { onAlbumClick(albumItem.id) },
                        fillParentWidth = true
                    )
                }
                loadingMoreGridFooter(uiState.isLoadingMore)
            }
        }
        3 -> { // Artists
            val listState = rememberLazyListState()
            LoadMoreListEffect(
                listState = listState,
                hasMore = uiState.hasMore,
                isLoadingMore = uiState.isLoadingMore,
                onLoadMore = onLoadMore
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(uiState.searchResultsArtists, key = { it.id }) { artistItem ->
                    ArtistSearchRow(
                        name = artistItem.title,
                        imageUrl = artistItem.thumbnail,
                        onClick = { onArtistClick(artistItem.id) }
                    )
                }
                loadingMoreFooter(uiState.isLoadingMore)
            }
        }
        4 -> { // Playlists
            val gridState = rememberLazyGridState()
            LoadMoreGridEffect(
                gridState = gridState,
                hasMore = uiState.hasMore,
                isLoadingMore = uiState.isLoadingMore,
                onLoadMore = onLoadMore
            )
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.searchResultsPlaylists, key = { it.id }) { playlistItem ->
                    ContentCard(
                        title = playlistItem.title,
                        subtitle = "Playlist",
                        imageUrl = playlistItem.thumbnail,
                        onClick = { onPlaylistClick(playlistItem.id) },
                        fillParentWidth = true
                    )
                }
                loadingMoreGridFooter(uiState.isLoadingMore)
            }
        }
        5 -> { // Podcasts
            val listState = rememberLazyListState()
            LoadMoreListEffect(
                listState = listState,
                hasMore = uiState.hasMore,
                isLoadingMore = uiState.isLoadingMore,
                onLoadMore = onLoadMore
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(uiState.searchResultsPodcasts, key = { it.id }) { podcastItem ->
                    SearchResultRow(
                        title = podcastItem.title,
                        subtitle = podcastItem.author?.name ?: "Podcast",
                        imageUrl = podcastItem.thumbnail,
                        fallbackIconRes = com.casty.music.R.drawable.podcast,
                        onClick = { onPodcastClick(podcastItem.id) }
                    )
                }
                loadingMoreFooter(uiState.isLoadingMore)
            }
        }
    }
}

@Composable
private fun LoadMoreListEffect(
    listState: LazyListState,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore by remember(listState, hasMore, isLoadingMore) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val total = listState.layoutInfo.totalItemsCount
            hasMore && !isLoadingMore && total > 0 && lastVisible >= total - 5
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }
}

@Composable
private fun LoadMoreGridEffect(
    gridState: LazyGridState,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore by remember(gridState, hasMore, isLoadingMore) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val total = gridState.layoutInfo.totalItemsCount
            hasMore && !isLoadingMore && total > 0 && lastVisible >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.loadingMoreFooter(isLoadingMore: Boolean) {
    if (!isLoadingMore) return
    item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CastyTheme.colors.accentPink, modifier = Modifier.size(24.dp))
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.loadingMoreGridFooter(isLoadingMore: Boolean) {
    if (!isLoadingMore) return
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CastyTheme.colors.accentPink, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun MixedSearchRow(
    item: YTItem,
    onSongClick: (Song) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onPodcastClick: (String) -> Unit
) {
    when (item) {
        is SongItem -> {
            val song = item.asSong
            TrackListItem(
                song = song,
                onClick = { onSongClick(song) },
                onMenuClick = { onSongMenuClick(song) }
            )
        }
        is EpisodeItem -> {
            val song = item.asSongItem().asSong
            TrackListItem(
                song = song,
                onClick = { onSongClick(song) },
                onMenuClick = { onSongMenuClick(song) }
            )
        }
        is AlbumItem -> SearchResultRow(
            title = item.title,
            subtitle = item.artists?.joinToString(", ") { it.name } ?: "Album",
            imageUrl = item.thumbnail,
            fallbackIconRes = com.casty.music.R.drawable.album,
            onClick = { onAlbumClick(item.id) }
        )
        is ArtistItem -> SearchResultRow(
            title = item.title,
            subtitle = if (item.isProfile) "Profile" else "Artist",
            imageUrl = item.thumbnail,
            shape = CircleShape,
            fallbackIconRes = com.casty.music.R.drawable.artist,
            onClick = { onArtistClick(item.id) }
        )
        is PlaylistItem -> SearchResultRow(
            title = item.title,
            subtitle = listOfNotNull("Playlist", item.songCountText, item.author?.name).joinToString(" - "),
            imageUrl = item.thumbnail,
            fallbackIconRes = com.casty.music.R.drawable.playlist,
            onClick = { onPlaylistClick(item.id) }
        )
        is PodcastItem -> SearchResultRow(
            title = item.title,
            subtitle = item.author?.name ?: "Podcast",
            imageUrl = item.thumbnail,
            fallbackIconRes = com.casty.music.R.drawable.podcast,
            onClick = { onPodcastClick(item.id) }
        )
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    imageUrl: String?,
    fallbackIconRes: Int,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(6.dp),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CastyArtwork(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp),
            shape = shape,
            fallbackIconRes = fallbackIconRes
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CastyTheme.typography.bodyLarge.copy(
                    color = CastyTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = subtitle.ifBlank { "YouTube Music" },
                style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchStatusBlock(
    title: String,
    message: String,
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
    }
}

@Composable
private fun ArtistSearchRow(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CastyArtwork(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp),
            shape = CircleShape,
            fallbackIconRes = com.casty.music.R.drawable.artist
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name,
            style = CastyTheme.typography.bodyLarge.copy(
                color = CastyTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun CategoryGrid(
    onCategoryClick: (String) -> Unit
) {
    val categories = listOf(
        CategoryData("Podcasts", Color(0xFFE13300), Color(0xFFB82600)),
        CategoryData("New Releases", Color(0xFF148A08), Color(0xFF0F6606)),
        CategoryData("Charts", Color(0xFF8D67AB), Color(0xFF6B4E82)),
        CategoryData("Pop", Color(0xFF1E3264), Color(0xFF16254A)),
        CategoryData("Hip-Hop", Color(0xFFBA5D07), Color(0xFF8F4705)),
        CategoryData("Rock", Color(0xFFE91429), Color(0xFFB50F20)),
        CategoryData("Discover", Color(0xFFE8115B), Color(0xFFB50D47))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val pairs = categories.chunked(2)
        pairs.forEach { rowCategories ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCategories.forEach { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category.title) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class CategoryData(
    val title: String,
    val colorStart: Color,
    val colorEnd: Color
)

@Composable
private fun CategoryCard(
    category: CategoryData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(category.colorStart, category.colorEnd)))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = category.title,
            style = CastyTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
private fun ShimmerLoadingSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CastyTheme.colors.elevation2.copy(alpha = shimmerAlpha))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CastyTheme.colors.elevation2.copy(alpha = shimmerAlpha))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CastyTheme.colors.elevation2.copy(alpha = shimmerAlpha))
                    )
                }
            }
        }
    }
}
