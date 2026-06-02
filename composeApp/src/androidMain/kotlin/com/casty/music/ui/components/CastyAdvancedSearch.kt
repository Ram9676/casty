package com.casty.music.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casty.music.ui.theme.CastyTheme
import kotlinx.coroutines.delay

/**
 * God-tier Search Bar with animated transitions, glass morphism, and intelligent UX.
 * Production-ready with haptics, focus management, and smooth state changes.
 */
@Composable
fun CastyAdvancedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "What do you want to listen to?",
    showSuggestions: Boolean = false,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = rememberCastyHaptics()
    
    var isFocused by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(modifier = modifier) {
        // Search input container with glass morphism
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .shadow(
                    elevation = if (isFocused) 12.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = CastyTheme.colors.accentPink.copy(alpha = 0.15f),
                    spotColor = CastyTheme.colors.accentPink.copy(alpha = 0.15f)
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CastyTheme.colors.elevation2,
                            CastyTheme.colors.elevation2.copy(alpha = 0.9f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isFocused) CastyTheme.colors.accentPink.copy(alpha = 0.5f) else Color.Transparent,
                            if (isFocused) CastyTheme.colors.accentPink.copy(alpha = 0.3f) else Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated search icon
                AnimatedContent(
                    targetState = isFocused || query.isNotEmpty(),
                    transitionSpec = {
                        scaleIn(initialScale = 0.8f) + fadeIn() togetherWith
                        scaleOut(targetScale = 0.8f) + fadeOut()
                    }
                ) { isActive ->
                    Icon(
                        imageVector = if (isActive) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Search,
                        contentDescription = if (isActive) "Search" else "Search",
                        tint = if (isActive) CastyTheme.colors.accentPink else CastyTheme.colors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Text input field
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty() && !isFocused) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                color = CastyTheme.colors.textSecondary.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                fontFamily = CastyTheme.typography.bodyLarge.fontFamily
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    BasicTextField(
                        value = query,
                        onValueChange = { 
                            haptics.tick()
                            onQueryChange(it)
                        },
                        textStyle = TextStyle(
                            color = CastyTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = CastyTheme.typography.bodyLarge.fontFamily
                        ),
                        cursorBrush = SolidColor(CastyTheme.colors.accentPink),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { 
                                onSearch(query)
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                
                // Clear button with scale animation
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = scaleIn(initialScale = 0.7f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.7f) + fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            haptics.confirm()
                            onClear()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = CastyTheme.colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // Suggestions dropdown with staggered animation
        AnimatedVisibility(
            visible = showSuggestions && suggestions.isNotEmpty() && query.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { -20 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -20 }) + fadeOut()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CastyTheme.colors.elevation2)
                    .border(
                        width = 1.dp,
                        color = CastyTheme.colors.borderInactive,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(suggestions.take(5)) { suggestion ->
                    SuggestionItem(
                        text = suggestion,
                        onClick = {
                            haptics.confirm()
                            onSuggestionClick(suggestion)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    text: String,
    onClick: () -> Unit
) {
    val haptics = rememberCastyHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "SuggestionScale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.tick()
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = CastyTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = CastyTheme.typography.bodyLarge.copy(
                color = CastyTheme.colors.textPrimary,
                fontSize = 15.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Intelligent search history chip with premium styling
 */
@Composable
fun CastySearchHistoryChip(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberCastyHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "ChipScale"
    )
    
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(CastyTheme.colors.elevation3)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.tick()
                    onClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = com.casty.music.R.drawable.history),
            contentDescription = null,
            tint = CastyTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = query,
            style = CastyTheme.typography.bodyMedium.copy(
                color = CastyTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        
        IconButton(
            onClick = {
                haptics.tick()
                onDelete()
            },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Remove",
                tint = CastyTheme.colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Premium filter chip for search results
 */
@Composable
fun CastyFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconId: Int? = null
) {
    val haptics = rememberCastyHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) CastyTheme.colors.accentPink else CastyTheme.colors.elevation2,
        label = "ChipBg"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else CastyTheme.colors.textPrimary,
        label = "ChipText"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "FilterScale"
    )
    
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.confirm()
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        iconId?.let { id ->
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = id),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Text(
            text = label,
            style = CastyTheme.typography.bodyMedium.copy(
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}
