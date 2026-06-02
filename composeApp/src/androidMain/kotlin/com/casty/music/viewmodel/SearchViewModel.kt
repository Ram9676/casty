package com.casty.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.PlayerServiceConnection
import com.casty.music.data.SearchQuery
import com.casty.music.data.asSong
import com.casty.music.backend.innertube.YouTube
import com.casty.music.backend.innertube.models.AlbumItem
import com.casty.music.backend.innertube.models.ArtistItem
import com.casty.music.backend.innertube.models.EpisodeItem
import com.casty.music.backend.innertube.models.PlaylistItem
import com.casty.music.backend.innertube.models.PodcastItem
import com.casty.music.backend.innertube.models.SongItem
import com.casty.music.backend.innertube.models.YTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val searchResultsAll: List<YTItem> = emptyList(),
    val searchResultsSongs: List<SongItem> = emptyList(),
    val searchResultsAlbums: List<AlbumItem> = emptyList(),
    val searchResultsArtists: List<ArtistItem> = emptyList(),
    val searchResultsPlaylists: List<PlaylistItem> = emptyList(),
    val searchResultsPodcasts: List<PodcastItem> = emptyList(),
    val recentSearches: List<SearchQuery> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val selectedFilterIndex: Int = 0,
    val error: String? = null,
    val hasPerformedSearch: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: DatabaseRepository,
    private val playerConnection: PlayerServiceConnection,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedFilterIndex = MutableStateFlow(0)
    val selectedFilterIndex: StateFlow<Int> = _selectedFilterIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _hasMore = MutableStateFlow(false)
    private val _hasPerformedSearch = MutableStateFlow(false)

    private val _resultsAll = MutableStateFlow<List<YTItem>>(emptyList())
    private val _resultsSongs = MutableStateFlow<List<SongItem>>(emptyList())
    private val _resultsAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    private val _resultsArtists = MutableStateFlow<List<ArtistItem>>(emptyList())
    private val _resultsPlaylists = MutableStateFlow<List<PlaylistItem>>(emptyList())
    private val _resultsPodcasts = MutableStateFlow<List<PodcastItem>>(emptyList())
    private val continuationTokens = mutableMapOf<Int, String?>()
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    val recentSearches: StateFlow<List<SearchQuery>> = repository.getSearchQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _resultsAll,
        _resultsSongs,
        _resultsAlbums,
        _resultsArtists,
        _resultsPlaylists,
        _resultsPodcasts,
        recentSearches,
        _isLoading,
        _isLoadingMore,
        _hasMore,
        _selectedFilterIndex,
        _errorMessage,
        _hasPerformedSearch,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SearchUiState(
            query = values[0] as String,
            searchResultsAll = values[1] as List<YTItem>,
            searchResultsSongs = values[2] as List<SongItem>,
            searchResultsAlbums = values[3] as List<AlbumItem>,
            searchResultsArtists = values[4] as List<ArtistItem>,
            searchResultsPlaylists = values[5] as List<PlaylistItem>,
            searchResultsPodcasts = values[6] as List<PodcastItem>,
            recentSearches = values[7] as List<SearchQuery>,
            isLoading = values[8] as Boolean,
            isLoadingMore = values[9] as Boolean,
            hasMore = values[10] as Boolean,
            selectedFilterIndex = values[11] as Int,
            error = values[12] as? String,
            hasPerformedSearch = values[13] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    init {
        setupSearchDebounce()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _query
                .debounce(120)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isNotBlank()) performSearch(q) else clearResults()
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _hasPerformedSearch.value = false
        _query.value = newQuery
    }

    fun setFilterIndex(index: Int) {
        _selectedFilterIndex.value = index
        _query.value.takeIf { it.isNotBlank() }?.let(::performSearch)
    }

    private fun clearResults() {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        continuationTokens.clear()
        _resultsAll.value = emptyList()
        _resultsSongs.value = emptyList()
        _resultsAlbums.value = emptyList()
        _resultsArtists.value = emptyList()
        _resultsPlaylists.value = emptyList()
        _resultsPodcasts.value = emptyList()
        _isLoading.value = false
        _isLoadingMore.value = false
        _hasMore.value = false
        _errorMessage.value = null
    }

    fun performSearch(q: String) {
        if (q.isBlank()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        _isLoading.value = true
        _isLoadingMore.value = false
        _errorMessage.value = null
        _hasMore.value = false
        continuationTokens.clear()

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertSearchQuery(SearchQuery(query = q))
                val filterIndex = _selectedFilterIndex.value
                val result = fetchSearchPage(q, filterIndex)
                applyResults(filterIndex, result.items, append = false)
                result.continuations.forEach { (index, token) ->
                    continuationTokens[index] = token
                }
                _hasMore.value = hasMoreFor(filterIndex)
                repository.rememberSearchResults(result.items)
                prefetchPlayableItems(result.items)
            } catch (throwable: Throwable) {
                _errorMessage.value = throwable.message ?: "Casty search failed. Try again."
                applyResults(_selectedFilterIndex.value, emptyList(), append = false)
            } finally {
                _isLoading.value = false
                _hasPerformedSearch.value = true
            }
        }
    }

    fun loadMore() {
        val query = _query.value.takeIf { it.isNotBlank() } ?: return
        val filterIndex = _selectedFilterIndex.value
        if (_isLoading.value || _isLoadingMore.value || !hasMoreFor(filterIndex)) return

        _isLoadingMore.value = true
        _errorMessage.value = null
        loadMoreJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val newItems = if (filterIndex == FILTER_ALL) {
                    FILTERED_INDICES.flatMap { loadContinuationForIndex(it) }
                } else {
                    loadContinuationForIndex(filterIndex)
                }.distinctBy { it.uniqueSearchKey() }

                if (newItems.isEmpty() && hasMoreFor(filterIndex)) {
                    performSearch(query)
                    return@launch
                }
                applyResults(filterIndex, newItems, append = true)
                repository.rememberSearchResults(newItems)
                prefetchPlayableItems(newItems)
                _hasMore.value = hasMoreFor(filterIndex)
            } catch (throwable: Throwable) {
                _errorMessage.value = throwable.message ?: "More results could not load."
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun deleteRecentSearch(query: SearchQuery) {
        repository.deleteSearchQuery(query)
    }

    private suspend fun fetchSearchPage(query: String, filterIndex: Int): SearchPage {
        if (filterIndex != FILTER_ALL) {
            val result = YouTube.search(query, filterIndex.toSearchFilter()).getOrThrow()
            return SearchPage(
                items = result.items.distinctBy { it.uniqueSearchKey() },
                continuations = mapOf(filterIndex to result.continuation),
            )
        }

        val summaryItems = runCatching {
            YouTube.searchSummary(query)
                .getOrThrow()
                .summaries
                .flatMap { it.items }
        }.getOrElse { emptyList() }

        val typedPages = FILTERED_INDICES.mapNotNull { index ->
            runCatching {
                val result = YouTube.search(query, index.toSearchFilter()).getOrThrow()
                index to result
            }.getOrNull()
        }

        val items = (summaryItems + typedPages.flatMap { it.second.items })
            .distinctBy { it.uniqueSearchKey() }
        val continuations = typedPages.associate { (index, page) -> index to page.continuation }
        return SearchPage(items, continuations)
    }

    private suspend fun loadContinuationForIndex(index: Int): List<YTItem> {
        val continuation = continuationTokens[index] ?: return emptyList()
        val result = YouTube.searchContinuation(continuation).getOrThrow()
        continuationTokens[index] = result.continuation
        return result.items
    }

    private fun applyResults(filterIndex: Int, items: List<YTItem>, append: Boolean) {
        if (filterIndex == FILTER_ALL) {
            val nextAll = if (append) _resultsAll.value + items else items
            _resultsAll.value = nextAll.distinctBy { it.uniqueSearchKey() }
            _resultsSongs.value = _resultsAll.value.filterIsInstance<SongItem>()
            _resultsAlbums.value = _resultsAll.value.filterIsInstance<AlbumItem>()
            _resultsArtists.value = _resultsAll.value.filterIsInstance<ArtistItem>()
            _resultsPlaylists.value = _resultsAll.value.filterIsInstance<PlaylistItem>()
            _resultsPodcasts.value = _resultsAll.value.filterIsInstance<PodcastItem>()
            return
        }

        if (!append) _resultsAll.value = emptyList()
        when (filterIndex) {
            FILTER_SONGS -> _resultsSongs.value = merge(_resultsSongs.value, items.filterIsInstance<SongItem>(), append)
            FILTER_ALBUMS -> _resultsAlbums.value = merge(_resultsAlbums.value, items.filterIsInstance<AlbumItem>(), append)
            FILTER_ARTISTS -> _resultsArtists.value = merge(_resultsArtists.value, items.filterIsInstance<ArtistItem>(), append)
            FILTER_PLAYLISTS -> _resultsPlaylists.value = merge(_resultsPlaylists.value, items.filterIsInstance<PlaylistItem>(), append)
            FILTER_PODCASTS -> _resultsPodcasts.value = merge(_resultsPodcasts.value, items.filterIsInstance<PodcastItem>(), append)
        }
    }

    private fun <T : YTItem> merge(current: List<T>, newItems: List<T>, append: Boolean): List<T> =
        (if (append) current + newItems else newItems).distinctBy { it.uniqueSearchKey() }

    private fun hasMoreFor(filterIndex: Int): Boolean =
        if (filterIndex == FILTER_ALL) {
            FILTERED_INDICES.any { continuationTokens[it] != null }
        } else {
            continuationTokens[filterIndex] != null
        }

    private fun Int.toSearchFilter(): YouTube.SearchFilter = when (this) {
        FILTER_SONGS -> YouTube.SearchFilter.FILTER_SONG
        FILTER_ALBUMS -> YouTube.SearchFilter.FILTER_ALBUM
        FILTER_ARTISTS -> YouTube.SearchFilter.FILTER_ARTIST
        FILTER_PODCASTS -> YouTube.SearchFilter.FILTER_PODCAST
        FILTER_EPISODES -> YouTube.SearchFilter.FILTER_EPISODE
        else -> YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST
    }

    private fun YTItem.uniqueSearchKey(): String = "${this::class.java.name}:$id"

    private fun prefetchPlayableItems(items: List<YTItem>) {
        val songs = items.mapNotNull { item ->
            when (item) {
                is SongItem -> item.asSong
                is EpisodeItem -> item.asSongItem().asSong
                else -> null
            }
        }
        playerConnection.prefetchSongs(songs)
    }

    private data class SearchPage(
        val items: List<YTItem>,
        val continuations: Map<Int, String?>,
    )

    companion object {
        const val FILTER_ALL = 0
        const val FILTER_SONGS = 1
        const val FILTER_ALBUMS = 2
        const val FILTER_ARTISTS = 3
        const val FILTER_PLAYLISTS = 4
        const val FILTER_PODCASTS = 5
        private const val FILTER_EPISODES = 6
        private val FILTERED_INDICES = listOf(
            FILTER_SONGS,
            FILTER_ALBUMS,
            FILTER_ARTISTS,
            FILTER_PLAYLISTS,
            FILTER_PODCASTS,
            FILTER_EPISODES,
        )
    }
}
