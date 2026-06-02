package com.casty.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.PlayerServiceConnection
import com.casty.music.data.Playlist
import com.casty.music.data.PlaylistPreview
import com.casty.music.data.Song
import com.casty.music.data.asSong
import com.casty.music.backend.innertube.YouTube
import com.casty.music.backend.innertube.models.AlbumItem
import com.casty.music.backend.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ContentType {
    PLAYLIST,
    ALBUM,
    ARTIST,
}

data class ContentUiState(
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String? = null,
    val tracks: List<Song> = emptyList(),
    val artistAlbums: List<AlbumItem> = emptyList(),
    val contentType: ContentType? = null,
    val contentId: String? = null,
    val playlistId: String? = null,
    val continuation: String? = null,
    val isLoading: Boolean = true,
    val isActionLoading: Boolean = false,
    val error: String? = null,
    val isFollowing: Boolean = false,
    val isDownloaded: Boolean = false,
)

@HiltViewModel
class ContentViewModel @Inject constructor(
    private val repository: DatabaseRepository,
    private val playerConnection: PlayerServiceConnection,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContentUiState())
    val uiState: StateFlow<ContentUiState> = _uiState.asStateFlow()

    fun loadContent(type: ContentType, id: String) {
        _uiState.value = ContentUiState(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (type) {
                    ContentType.PLAYLIST -> loadPlaylist(id)
                    ContentType.ALBUM -> loadAlbum(id)
                    ContentType.ARTIST -> loadArtist(id)
                }
            }.onFailure { throwable ->
                _uiState.value = ContentUiState(
                    isLoading = false,
                    error = throwable.message ?: "Casty could not load this content",
                )
            }
        }
    }

    private suspend fun loadPlaylist(id: String) {
        val page = YouTube.playlist(id).getOrThrow()
        val tracks = page.songs.map { it.asSong }
        tracks.forEach(repository::rememberSong)
        _uiState.value = ContentUiState(
            title = page.playlist.title,
            description = page.playlist.songCountText ?: "Playlist",
            thumbnailUrl = page.playlist.thumbnail,
            tracks = tracks,
            contentType = ContentType.PLAYLIST,
            contentId = id,
            playlistId = id,
            continuation = page.songsContinuation,
            isLoading = false,
        )
        playerConnection.prefetchSongs(tracks)
    }

    private suspend fun loadAlbum(id: String) {
        val page = YouTube.album(id).getOrThrow()
        val tracks = page.songs.map { it.asSong }
        tracks.forEach(repository::rememberSong)
        _uiState.value = ContentUiState(
            title = page.album.title,
            description = page.album.artists?.joinToString(", ") { it.name }.orEmpty(),
            thumbnailUrl = page.album.thumbnail,
            tracks = tracks,
            contentType = ContentType.ALBUM,
            contentId = id,
            isLoading = false,
        )
        playerConnection.prefetchSongs(tracks)
    }

    private suspend fun loadArtist(id: String) {
        val page = YouTube.artist(id).getOrThrow()
        val songs = page.sections
            .flatMap { it.items }
            .filterIsInstance<SongItem>()
            .distinctBy { it.id }
            .map { it.asSong }
        songs.forEach(repository::rememberSong)
        _uiState.value = ContentUiState(
            title = page.artist.title,
            description = page.monthlyListenerCount ?: page.subscriberCountText.orEmpty(),
            thumbnailUrl = page.artist.thumbnail,
            tracks = songs,
            artistAlbums = page.sections.flatMap { it.items }.filterIsInstance<AlbumItem>().distinctBy { it.id },
            contentType = ContentType.ARTIST,
            contentId = id,
            isLoading = false,
            isFollowing = page.isSubscribed,
        )
        playerConnection.prefetchSongs(songs)
    }

    fun playAll(songs: List<Song>) {
        songs.forEach(repository::rememberSong)
        songs.firstOrNull()?.let(repository::recordPlayback)
        val state = _uiState.value
        state.asHistoryPlaylist(songs.size)?.let(repository::recordPlaylistPlayback)
        if (state.playlistId != null) {
            playerConnection.playYouTubePlaylist(
                playlistId = state.playlistId,
                title = state.title,
                songs = songs,
                continuation = state.continuation,
            )
        } else {
            playerConnection.playQueue(songs)
        }
    }

    fun playFromIndex(index: Int) {
        val state = _uiState.value
        val songs = state.tracks
        if (songs.isEmpty()) return
        songs.forEach(repository::rememberSong)
        val startIndex = index.coerceIn(songs.indices)
        repository.recordPlayback(songs[startIndex])
        state.asHistoryPlaylist(songs.size)?.let(repository::recordPlaylistPlayback)
        if (state.playlistId != null) {
            playerConnection.playYouTubePlaylist(
                playlistId = state.playlistId,
                title = state.title,
                songs = songs,
                startIndex = startIndex,
                continuation = state.continuation,
            )
        } else {
            playerConnection.playQueue(songs, startIndex)
        }
    }

    fun toggleFollow() {
        val state = _uiState.value
        val id = state.contentId ?: return
        val shouldFollow = !state.isFollowing
        _uiState.value = state.copy(isActionLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                when (state.contentType) {
                    ContentType.PLAYLIST -> YouTube.likePlaylist(id, shouldFollow).getOrThrow()
                    ContentType.ARTIST -> YouTube.subscribeChannel(id, shouldFollow).getOrThrow()
                    ContentType.ALBUM, null -> Unit
                }
            }
            _uiState.value = _uiState.value.copy(
                isFollowing = shouldFollow,
                isActionLoading = false,
            )
            if (shouldFollow) repository.refreshLibraryContent()
        }
    }

    fun toggleDownload() {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        _uiState.value = _uiState.value.copy(isActionLoading = true, isDownloaded = true)
        repository.markDownloaded(tracks)
        playerConnection.cacheForOffline(tracks)
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isActionLoading = false)
        }
    }
}

private fun ContentUiState.asHistoryPlaylist(songCount: Int): PlaylistPreview? {
    val id = contentId?.takeIf { it.isNotBlank() } ?: playlistId?.takeIf { it.isNotBlank() } ?: return null
    if (contentType == ContentType.ARTIST) return null
    val typeLabel = when (contentType) {
        ContentType.ALBUM -> "Album"
        ContentType.PLAYLIST -> "Playlist"
        else -> "Collection"
    }
    return PlaylistPreview(
        playlist = Playlist(
            id = id,
            name = title.ifBlank { typeLabel },
            isYoutubePlaylist = contentType == ContentType.PLAYLIST,
            thumbnailUrl = thumbnailUrl,
            authorText = description.takeIf { it.isNotBlank() },
        ),
        songCount = songCount,
        subtitle = typeLabel,
    )
}
