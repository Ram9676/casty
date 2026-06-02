package com.casty.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showQualityDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CastyTheme.colors.systemCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CastyTheme.colors.textPrimary
                    )
                }
                Text(
                    text = "Settings",
                    style = CastyTheme.typography.titleLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Section: Audio Quality
                item {
                    SettingsSectionHeader("Audio Quality")
                }
                item {
                    val isHighQuality = uiState.audioQuality == "High" || uiState.audioQuality == "Very High"
                    SettingsRow(
                        title = "Audio Quality Format",
                        subtitle = uiState.audioQuality,
                        onClick = { showQualityDialog = true },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isHighQuality) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CastyTheme.colors.accentPink.copy(0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "HQ",
                                            style = CastyTheme.typography.bodyMedium.copy(
                                                color = CastyTheme.colors.accentPink,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = CastyTheme.colors.textSecondary
                                )
                            }
                        }
                    )
                }

                // Section: Playback
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionHeader("Playback")
                }
                item {
                    SettingsSwitchRow(
                        title = "Skip Silence",
                        subtitle = "Automatically skip silent gaps in audio",
                        checked = uiState.skipSilence,
                        onCheckedChange = { viewModel.setSkipSilence(it) }
                    )
                }
            }
        }
    }

    // Audio Quality Dialog
    if (showQualityDialog) {
        val qualities = SettingsViewModel.audioQualityOptions
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = CastyTheme.colors.elevation2,
            title = {
                Text(
                    text = "Select Audio Quality",
                    style = CastyTheme.typography.titleLarge.copy(
                        color = CastyTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column {
                    qualities.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAudioQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = quality,
                                style = CastyTheme.typography.bodyLarge.copy(
                                    color = if (uiState.audioQuality == quality) CastyTheme.colors.accentPink else CastyTheme.colors.textPrimary,
                                    fontWeight = if (uiState.audioQuality == quality) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            if (uiState.audioQuality == quality) {
                                Icon(
                                    painter = painterResource(id = com.casty.music.R.drawable.sparkles),
                                    contentDescription = "Selected",
                                    tint = CastyTheme.colors.accentPink,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = CastyTheme.typography.titleMedium.copy(
            color = CastyTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CastyTheme.typography.bodyLarge.copy(
                    color = CastyTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary)
            )
        }
        trailingContent()
    }
    HorizontalDivider(color = CastyTheme.colors.elevation2, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CastyTheme.typography.bodyLarge.copy(
                    color = CastyTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = CastyTheme.typography.bodyMedium.copy(color = CastyTheme.colors.textSecondary)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CastyTheme.colors.accentPink,
                uncheckedThumbColor = CastyTheme.colors.textSecondary,
                uncheckedTrackColor = CastyTheme.colors.elevation3
            )
        )
    }
    HorizontalDivider(color = CastyTheme.colors.elevation2, modifier = Modifier.padding(horizontal = 16.dp))
}
