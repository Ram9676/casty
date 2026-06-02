package com.casty.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casty.music.data.TasteArtist
import com.casty.music.data.TasteProfile
import com.casty.music.ui.components.CastyArtwork
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.TasteOnboardingViewModel

private val languageChoices = listOf(
    "Telugu",
    "Tamil",
    "Hindi",
    "Malayalam",
    "Kannada",
    "Punjabi",
    "English",
    "Bengali",
    "Marathi",
    "Gujarati",
    "Odia",
    "Assamese",
    "Urdu",
)

private val industryChoices = listOf(
    "Tollywood",
    "Kollywood",
    "Bollywood",
    "Mollywood",
    "Sandalwood",
    "Pollywood",
    "Independent",
    "International",
    "K-Pop",
    "J-Pop",
    "Lo-fi",
    "Classical",
)

@Composable
fun TasteOnboardingScreen(
    onContinue: (TasteProfile) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasteOnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedLanguages by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var selectedIndustries by rememberSaveable { mutableStateOf(emptySet<String>()) }

    val hasSelections = selectedLanguages.isNotEmpty() ||
        selectedIndustries.isNotEmpty() ||
        uiState.selectedArtists.isNotEmpty()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas),
        containerColor = CastyTheme.colors.systemCanvas,
        bottomBar = {
            Surface(
                color = CastyTheme.colors.systemCanvas,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Button(
                    onClick = {
                        onContinue(
                            TasteProfile(
                                selectedLanguages = selectedLanguages,
                                selectedIndustries = selectedIndustries,
                                favoriteArtists = uiState.selectedArtists.map { it.name }.toSet(),
                                isCompleted = true,
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CastyTheme.colors.accentPink),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text(
                        text = if (hasSelections) "Continue" else "Continue with popular picks",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose your music",
                        style = CastyTheme.typography.displayMedium.copy(
                            color = CastyTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "Pick languages, industries, and artists. Casty will tune Home around them.",
                        style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary),
                    )
                }
            }

            item {
                TasteChipSection(
                    title = "Languages",
                    choices = languageChoices,
                    selected = selectedLanguages,
                    onToggle = { choice -> selectedLanguages = selectedLanguages.toggle(choice) },
                )
            }

            item {
                TasteChipSection(
                    title = "Industries",
                    choices = industryChoices,
                    selected = selectedIndustries,
                    onToggle = { choice -> selectedIndustries = selectedIndustries.toggle(choice) },
                )
            }

            item {
                ArtistSearchBar(
                    query = uiState.artistQuery,
                    onQueryChange = viewModel::onArtistQueryChanged,
                )
            }

            if (uiState.selectedArtists.isNotEmpty()) {
                item {
                    ArtistRail(
                        title = "Selected artists",
                        artists = uiState.selectedArtists,
                        selectedArtists = uiState.selectedArtists,
                        onArtistClick = viewModel::toggleArtist,
                    )
                }
            }

            item {
                ArtistRail(
                    title = if (uiState.artistQuery.isBlank()) "Popular artists" else "Artist results",
                    artists = uiState.suggestedArtists,
                    selectedArtists = uiState.selectedArtists,
                    isLoading = uiState.isLoadingArtists,
                    onArtistClick = viewModel::toggleArtist,
                )
            }

            if (uiState.relatedArtists.isNotEmpty()) {
                item {
                    ArtistRail(
                        title = "More artists like these",
                        artists = uiState.relatedArtists,
                        selectedArtists = uiState.selectedArtists,
                        onArtistClick = viewModel::toggleArtist,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasteChipSection(
    title: String,
    choices: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = CastyTheme.typography.titleLarge.copy(
                color = CastyTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            choices.forEach { choice ->
                val isSelected = choice in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(choice) },
                    label = {
                        Text(
                            text = choice,
                            style = CastyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CastyTheme.colors.accentPink,
                        selectedLabelColor = Color.Black,
                        containerColor = CastyTheme.colors.elevation2,
                        labelColor = CastyTheme.colors.textPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ArtistSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CastyTheme.colors.elevation2)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = CastyTheme.colors.textSecondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isBlank()) {
                Text(
                    text = "Search artists",
                    style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(CastyTheme.colors.accentPink),
                textStyle = TextStyle(
                    color = CastyTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = CastyTheme.typography.bodyLarge.fontFamily,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ArtistRail(
    title: String,
    artists: List<TasteArtist>,
    selectedArtists: List<TasteArtist>,
    onArtistClick: (TasteArtist) -> Unit,
    isLoading: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = title,
            style = CastyTheme.typography.titleLarge.copy(
                color = CastyTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )

        when {
            isLoading && artists.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = CastyTheme.colors.accentPink)
                }
            }
            artists.isEmpty() -> {
                Text(
                    text = "No artists found yet.",
                    style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary),
                )
            }
            else -> {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(end = 16.dp),
                ) {
                    items(artists, key = { it.id }) { artist ->
                        ArtistBubble(
                            artist = artist,
                            selected = selectedArtists.any { it.id == artist.id },
                            onClick = { onArtistClick(artist) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistBubble(
    artist: TasteArtist,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            CastyArtwork(
                model = artist.thumbnailUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                artworkSizePx = 320,
                modifier = Modifier.size(104.dp),
                shape = CircleShape,
                fallbackIconRes = com.casty.music.R.drawable.artist,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(CastyTheme.colors.accentPink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Text(
            text = artist.name,
            style = CastyTheme.typography.bodySmall.copy(
                color = CastyTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value
