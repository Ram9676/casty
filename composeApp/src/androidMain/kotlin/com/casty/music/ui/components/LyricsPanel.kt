package com.casty.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.viewmodel.NowPlayingViewModel

@Composable
fun LyricsPanel(
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.activeLyricIndex) {
        if (uiState.activeLyricIndex >= 0 && uiState.lyrics.isNotEmpty()) {
            listState.animateScrollToItem(uiState.activeLyricIndex)
        }
    }

    if (uiState.isLyricsLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = CastyTheme.colors.accentPink)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Searching lyrics...",
                    style = CastyTheme.typography.bodyLarge.copy(color = CastyTheme.colors.textSecondary)
                )
            }
        }
    } else if (uiState.lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.lyricsError ?: "Lyrics unavailable",
                style = CastyTheme.typography.bodyLarge.copy(
                    color = CastyTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
            
            itemsIndexed(
                items = uiState.lyrics,
                key = { _, line -> "${line.timestampMs}:${line.text}" },
            ) { index, line ->
                val isActive = index == uiState.activeLyricIndex
                
                // Color transition from #535353 (inactive) to #FFFFFF (active)
                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color.White else Color(0xFF535353),
                    label = "LyricColor"
                )

                // Scale transition from 0.85f to 1.05f
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.05f else 0.95f,
                    label = "LyricScale"
                )

                Text(
                    text = line.text,
                    color = textColor,
                    style = CastyTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    textAlign = TextAlign.Start
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
