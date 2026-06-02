package com.casty.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.PlayerServiceConnection
import com.casty.music.data.asSong
import com.casty.music.backend.innertube.YouTube
import com.casty.music.backend.innertube.models.EpisodeItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PodcastUiState(
    val title: String = "",
    val author: String = "",
    val episodeCountText: String? = null,
    val thumbnailUrl: String? = null,
    val shareLink: String = "",
    val episodes: List<EpisodeItem> = emptyList(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val repository: DatabaseRepository,
    private val playerConnection: PlayerServiceConnection,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastUiState(isLoading = true))
    val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

    private var currentPodcastId: String = ""

    fun loadPodcast(showId: String) {
        if (showId.isBlank()) {
            _uiState.value = PodcastUiState(
                isLoading = false,
                error = "Missing podcast ID.",
            )
            return
        }

        currentPodcastId = showId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val page = YouTube.podcast(showId).getOrThrow()
                val episodeSongs = page.episodes.map { it.asSongItem().asSong }
                episodeSongs.forEach(repository::rememberSong)
                playerConnection.prefetchSongs(episodeSongs.take(4), limit = 4)
                _uiState.value = PodcastUiState(
                    title = page.podcast.title,
                    author = page.podcast.author?.name.orEmpty(),
                    episodeCountText = page.podcast.episodeCountText,
                    thumbnailUrl = page.podcast.thumbnail,
                    shareLink = page.podcast.shareLink,
                    episodes = page.episodes,
                    isSaved = page.podcast.libraryRemoveToken != null || page.isChannelSubscribed,
                    isLoading = false,
                )
            }.onFailure { throwable ->
                _uiState.value = PodcastUiState(
                    isLoading = false,
                    error = throwable.message ?: "Casty could not load this podcast.",
                )
            }
        }
    }

    fun playEpisode(index: Int) {
        val episodes = _uiState.value.episodes
        if (episodes.isEmpty()) return
        val songs = episodes.map { it.asSongItem().asSong }
        songs.forEach(repository::rememberSong)
        val startIndex = index.coerceIn(songs.indices)
        repository.recordPlayback(songs[startIndex])
        playerConnection.playQueue(songs, startIndex)
    }

    fun toggleSaved() {
        val podcastId = currentPodcastId.takeIf { it.isNotBlank() } ?: return
        val shouldSave = !_uiState.value.isSaved
        _uiState.value = _uiState.value.copy(isActionLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { YouTube.savePodcast(podcastId, shouldSave).getOrThrow() }
            _uiState.value = _uiState.value.copy(
                isSaved = shouldSave,
                isActionLoading = false,
            )
            if (shouldSave) {
                runCatching { repository.refreshLibraryContent() }
            }
        }
    }
}
