package com.casty.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casty.music.data.Album
import com.casty.music.data.Artist
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.PlaylistPreview
import com.casty.music.data.SongEntity
import com.casty.music.data.TasteProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private data class HomeCollections(
    val quickAccess: List<SongEntity>,
    val playlists: List<PlaylistPreview>,
    val albums: List<Album>,
    val artists: List<Artist>,
)

data class HomeUiState(
    val greeting: String = "",
    val tasteProfile: TasteProfile = TasteProfile(),
    val quickAccessItems: List<SongEntity> = emptyList(),
    val recommendedPlaylists: List<PlaylistPreview> = emptyList(),
    val recentlyPlayedAlbums: List<Album> = emptyList(),
    val featuredArtists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DatabaseRepository
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val greetingFlow = kotlinx.coroutines.flow.flow {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        emit(greeting)
    }

    private val quickAccessFlow = repository.getRecentTracks().map { tracks ->
        tracks.take(8)
    }

    private val tasteProfileFlow = repository.observeTasteProfile()

    private val playlistsFlow = repository.getPlaylists().map { playlists ->
        playlists.take(8)
    }

    private val albumsFlow = repository.getAlbums().map { albums ->
        albums.take(8)
    }

    private val artistsFlow = repository.getArtists().map { artists ->
        artists.take(8)
    }

    private val collectionsFlow = combine(
        quickAccessFlow,
        playlistsFlow,
        albumsFlow,
        artistsFlow,
    ) { quickAccess, playlists, albums, artists ->
        HomeCollections(
            quickAccess = quickAccess,
            playlists = playlists,
            albums = albums,
            artists = artists,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        greetingFlow,
        tasteProfileFlow,
        collectionsFlow,
        isRefreshing,
        errorMessage,
    ) { greeting, tasteProfile, collections, refreshing, error ->
        HomeUiState(
            greeting = greeting,
            tasteProfile = tasteProfile,
            quickAccessItems = collections.quickAccess,
            recommendedPlaylists = collections.playlists,
            recentlyPlayedAlbums = collections.albums,
            featuredArtists = collections.artists,
            isLoading = refreshing &&
                collections.quickAccess.isEmpty() &&
                collections.playlists.isEmpty() &&
                collections.albums.isEmpty() &&
                collections.artists.isEmpty(),
            isRefreshing = refreshing,
            error = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            errorMessage.value = null
            runCatching { repository.refreshHomeContent() }
                .onFailure { errorMessage.value = it.message ?: "Casty could not load home music." }
            isRefreshing.value = false
        }
    }
}
