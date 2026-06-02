package com.casty.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casty.music.data.Album
import com.casty.music.data.AlbumSortBy
import com.casty.music.data.Artist
import com.casty.music.data.ArtistSortBy
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.Playlist
import com.casty.music.data.PlaylistPreview
import com.casty.music.data.PlaylistSortBy
import com.casty.music.data.PlayerServiceConnection
import com.casty.music.data.Song
import com.casty.music.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LibraryItem {
    data class TrackItem(val song: Song) : LibraryItem()
    data class PlaylistItem(val playlist: PlaylistPreview) : LibraryItem()
    data class AlbumItem(val album: Album) : LibraryItem()
    data class ArtistItem(val artist: Artist) : LibraryItem()
}

enum class LibraryFilter {
    History, LikedSongs, WatchLater, Downloads, Playlists, Albums, Artists
}

data class LibraryUiState(
    val selectedFilter: LibraryFilter = LibraryFilter.Playlists,
    val items: List<LibraryItem> = emptyList(),
    val isGridView: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val librarySearchQuery: String = "",
)

private data class LibraryCollections(
    val historySongs: List<Song>,
    val recentPlaylists: List<PlaylistPreview>,
    val likedSongs: List<Song>,
    val watchLaterSongs: List<Song>,
    val downloadedSongs: List<Song>,
    val playlists: List<PlaylistPreview>,
    val albums: List<Album>,
    val artists: List<Artist>,
)

private data class LibrarySongCollections(
    val historySongs: List<Song>,
    val likedSongs: List<Song>,
    val watchLaterSongs: List<Song>,
    val downloadedSongs: List<Song>,
)

private data class LibraryBrowseCollections(
    val recentPlaylists: List<PlaylistPreview>,
    val playlists: List<PlaylistPreview>,
    val albums: List<Album>,
    val artists: List<Artist>,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: DatabaseRepository,
    private val playerConnection: PlayerServiceConnection,
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(LibraryFilter.Playlists)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery: StateFlow<String> = _librarySearchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _isRefreshing = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val playlistsFlow = repository.getPlaylists(PlaylistSortBy.Name, SortOrder.Ascending)
    private val albumsFlow = repository.getAlbums(AlbumSortBy.Title, SortOrder.Ascending)
    private val artistsFlow = repository.getArtists(ArtistSortBy.Name, SortOrder.Ascending)
    private val likedSongsFlow = repository.getLikedSongs()
    private val watchLaterSongsFlow = repository.getWatchLaterSongs()
    private val recentPlaylistsFlow = repository.getRecentPlaylists()
    private val historySongsFlow = repository.getRecentTracks().map { tracks -> tracks.map { it.song } }
    private val downloadedSongsFlow = repository.getDownloadedSongs()

    private val songCollectionsFlow = combine(
        historySongsFlow,
        likedSongsFlow,
        watchLaterSongsFlow,
        downloadedSongsFlow,
    ) { historySongs, likedSongs, watchLaterSongs, downloadedSongs ->
        LibrarySongCollections(
            historySongs = historySongs,
            likedSongs = likedSongs,
            watchLaterSongs = watchLaterSongs,
            downloadedSongs = downloadedSongs,
        )
    }

    private val browseCollectionsFlow = combine(
        recentPlaylistsFlow,
        playlistsFlow,
        albumsFlow,
        artistsFlow,
    ) { recentPlaylists, playlists, albums, artists ->
        LibraryBrowseCollections(
            recentPlaylists = recentPlaylists,
            playlists = playlists,
            albums = albums,
            artists = artists,
        )
    }

    private val collectionsFlow = combine(
        songCollectionsFlow,
        browseCollectionsFlow,
    ) { songs, browse ->
        LibraryCollections(
            historySongs = songs.historySongs,
            recentPlaylists = browse.recentPlaylists,
            likedSongs = songs.likedSongs,
            watchLaterSongs = songs.watchLaterSongs,
            downloadedSongs = songs.downloadedSongs,
            playlists = browse.playlists,
            albums = browse.albums,
            artists = browse.artists,
        )
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        _selectedFilter,
        _isGridView,
        collectionsFlow,
        _isRefreshing,
        _errorMessage,
        _librarySearchQuery,
    ) { values ->
        val filter = values[0] as LibraryFilter
        val isGrid = values[1] as Boolean
        val collections = values[2] as LibraryCollections
        val isRefreshing = values[3] as Boolean
        val error = values[4] as String?
        val searchQuery = values[5] as String

        val allItems = when (filter) {
            LibraryFilter.History -> (
                collections.historySongs.map { LibraryItem.TrackItem(it) } +
                    collections.recentPlaylists.map { LibraryItem.PlaylistItem(it) }
                )
            LibraryFilter.LikedSongs -> collections.likedSongs.map { LibraryItem.TrackItem(it) }
            LibraryFilter.WatchLater -> collections.watchLaterSongs.map { LibraryItem.TrackItem(it) }
            LibraryFilter.Downloads -> collections.downloadedSongs.map { LibraryItem.TrackItem(it) }
            LibraryFilter.Playlists -> collections.playlists.map { LibraryItem.PlaylistItem(it) }
            LibraryFilter.Albums -> collections.albums.map { LibraryItem.AlbumItem(it) }
            LibraryFilter.Artists -> collections.artists.map { LibraryItem.ArtistItem(it) }
        }

        val items = if (searchQuery.isBlank()) allItems else allItems.filter { item ->
            val name = when (item) {
                is LibraryItem.TrackItem -> item.song.title
                is LibraryItem.PlaylistItem -> item.playlist.playlist.name
                is LibraryItem.AlbumItem -> item.album.title
                is LibraryItem.ArtistItem -> item.artist.name
            }
            name?.contains(searchQuery, ignoreCase = true) == true
        }

        LibraryUiState(
            selectedFilter = filter,
            items = items,
            isGridView = isGrid,
            isLoading = isRefreshing && items.isEmpty(),
            error = error,
            librarySearchQuery = searchQuery,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(isLoading = true)
    )

    init {
        refresh()
    }

    fun setFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
    }

    fun setLibrarySearchQuery(query: String) {
        _librarySearchQuery.value = query
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun playSong(song: Song) {
        repository.recordPlayback(song)
        playerConnection.playSong(song)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newPlaylist = Playlist(
                name = name,
                isYoutubePlaylist = false
            )
            repository.insertPlaylist(newPlaylist)
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            _errorMessage.value = null
            runCatching { repository.refreshLibraryContent() }
                .onFailure { _errorMessage.value = it.message ?: "Casty could not load your library." }
            _isRefreshing.value = false
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlist)
        }
    }
}
