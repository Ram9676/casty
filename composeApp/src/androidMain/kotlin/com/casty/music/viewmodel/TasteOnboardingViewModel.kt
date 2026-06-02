package com.casty.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.TasteArtist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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

data class TasteOnboardingUiState(
    val artistQuery: String = "",
    val suggestedArtists: List<TasteArtist> = emptyList(),
    val relatedArtists: List<TasteArtist> = emptyList(),
    val selectedArtists: List<TasteArtist> = emptyList(),
    val isLoadingArtists: Boolean = true,
)

@HiltViewModel
class TasteOnboardingViewModel @Inject constructor(
    private val repository: DatabaseRepository,
) : ViewModel() {
    private val artistQuery = MutableStateFlow("")
    private val suggestedArtists = MutableStateFlow<List<TasteArtist>>(emptyList())
    private val relatedArtists = MutableStateFlow<List<TasteArtist>>(emptyList())
    private val selectedArtists = MutableStateFlow<List<TasteArtist>>(emptyList())
    private val isLoadingArtists = MutableStateFlow(true)
    private var relatedJob: Job? = null

    val uiState: StateFlow<TasteOnboardingUiState> = combine(
        artistQuery,
        suggestedArtists,
        relatedArtists,
        selectedArtists,
        isLoadingArtists,
    ) { query, suggestions, related, selected, loading ->
        TasteOnboardingUiState(
            artistQuery = query,
            suggestedArtists = suggestions,
            relatedArtists = related.filterNot { artist -> selected.any { it.id == artist.id } },
            selectedArtists = selected,
            isLoadingArtists = loading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasteOnboardingUiState())

    init {
        loadDefaultArtists()
        observeArtistSearch()
    }

    fun onArtistQueryChanged(query: String) {
        artistQuery.value = query
        isLoadingArtists.value = true
    }

    fun toggleArtist(artist: TasteArtist) {
        val current = selectedArtists.value
        selectedArtists.value = if (current.any { it.id == artist.id }) {
            current.filterNot { it.id == artist.id }
        } else {
            (current + artist).distinctBy { it.id }.take(12)
        }
        refreshRelatedArtists()
    }

    private fun loadDefaultArtists() {
        viewModelScope.launch(Dispatchers.IO) {
            isLoadingArtists.value = true
            suggestedArtists.value = repository.loadSuggestedTasteArtists()
            isLoadingArtists.value = false
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeArtistSearch() {
        viewModelScope.launch {
            artistQuery
                .debounce(180)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        if (suggestedArtists.value.isEmpty()) loadDefaultArtists() else isLoadingArtists.value = false
                        return@collect
                    }
                    isLoadingArtists.value = true
                    suggestedArtists.value = repository.searchTasteArtists(query, limit = 24)
                    isLoadingArtists.value = false
                }
        }
    }

    private fun refreshRelatedArtists() {
        relatedJob?.cancel()
        val selected = selectedArtists.value
        if (selected.isEmpty()) {
            relatedArtists.value = emptyList()
            return
        }
        relatedJob = viewModelScope.launch(Dispatchers.IO) {
            val related = selected
                .takeLast(3)
                .flatMap { repository.relatedTasteArtists(it) }
                .filterNot { artist -> selected.any { it.id == artist.id } }
                .distinctBy { it.id }
                .take(24)
            relatedArtists.value = related
        }
    }
}
