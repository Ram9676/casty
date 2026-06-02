package com.casty.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.PlaylistPreview
import com.casty.music.data.PlaylistSortBy
import com.casty.music.data.PlayerServiceConnection
import com.casty.music.data.Song
import com.casty.music.data.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackActionsViewModel @Inject constructor(
    private val playerConnection: PlayerServiceConnection,
    private val repository: DatabaseRepository
) : ViewModel() {

    fun playNow(song: Song) {
        repository.recordPlayback(song)
        playerConnection.playSong(song)
    }

    fun playNext(song: Song) {
        repository.rememberSong(song)
        playerConnection.playNext(song)
    }

    fun addToQueue(song: Song) {
        repository.rememberSong(song)
        playerConnection.addToQueue(song)
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song)
        }
    }

    fun downloadForOffline(song: Song) {
        repository.markDownloaded(listOf(song))
        playerConnection.cacheForOffline(listOf(song), limit = 1)
    }

    fun getPlaylists(): Flow<List<PlaylistPreview>> =
        repository.getPlaylists(PlaylistSortBy.Name, SortOrder.Ascending)

    fun addToPlaylist(song: Song, playlistId: String) {
        repository.addSongToPlaylist(playlistId, song)
    }
}
