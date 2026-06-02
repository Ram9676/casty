package com.casty.music.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casty.music.backend.constants.AudioQuality
import com.casty.music.backend.constants.AudioQualityKey
import com.casty.music.backend.constants.SkipSilenceKey
import com.casty.music.backend.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val audioQuality: String = AudioQuality.VERY_HIGH.toDisplayLabel(),
    val skipSilence: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data
                .map { preferences ->
                    SettingsUiState(
                        audioQuality = preferences[AudioQualityKey]
                            ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
                            ?.toDisplayLabel()
                            ?: AudioQuality.VERY_HIGH.toDisplayLabel(),
                        skipSilence = preferences[SkipSilenceKey] ?: false,
                    )
                }
                .distinctUntilChanged()
                .collect { _uiState.value = it }
        }
    }

    fun setAudioQuality(quality: String) {
        val audioQuality = quality.toAudioQuality()
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { preferences ->
                preferences[AudioQualityKey] = audioQuality.name
            }
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { preferences ->
                preferences[SkipSilenceKey] = enabled
            }
        }
    }

    companion object {
        val audioQualityOptions = listOf(
            AudioQuality.AUTO.toDisplayLabel(),
            AudioQuality.LOW.toDisplayLabel(),
            AudioQuality.HIGH.toDisplayLabel(),
            AudioQuality.VERY_HIGH.toDisplayLabel(),
        )
    }
}

private fun String.toAudioQuality(): AudioQuality = when (this) {
    AudioQuality.AUTO.toDisplayLabel() -> AudioQuality.AUTO
    AudioQuality.LOW.toDisplayLabel() -> AudioQuality.LOW
    AudioQuality.HIGH.toDisplayLabel() -> AudioQuality.HIGH
    AudioQuality.VERY_HIGH.toDisplayLabel() -> AudioQuality.VERY_HIGH
    else -> AudioQuality.VERY_HIGH
}

private fun AudioQuality.toDisplayLabel(): String = when (this) {
    AudioQuality.AUTO -> "Auto"
    AudioQuality.LOW -> "Low"
    AudioQuality.HIGH -> "High"
    AudioQuality.VERY_HIGH -> "Very High"
}
