package com.casty.music.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.casty.music.ui.components.HapticType
import com.casty.music.ui.components.castyClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casty.music.ui.theme.CastyTheme
import com.casty.music.R

enum class BottomTab {
    Home, Search, Library, Account
}

@Composable
fun BottomNavBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberCastyHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CastyTheme.colors.systemCanvas)
            .navigationBarsPadding()
    ) {
        // 1dp top border matching Spotify style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CastyTheme.colors.elevation4)
        ) {}

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTabItem(
                tab = BottomTab.Home,
                iconRes = R.drawable.musical_notes,
                label = "Home",
                isSelected = selectedTab == BottomTab.Home,
                onClick = {
                    onTabSelected(BottomTab.Home)
                }
            )

            BottomTabItem(
                tab = BottomTab.Search,
                iconRes = R.drawable.search,
                label = "Search",
                isSelected = selectedTab == BottomTab.Search,
                onClick = {
                    onTabSelected(BottomTab.Search)
                }
            )

            BottomTabItem(
                tab = BottomTab.Library,
                iconRes = R.drawable.library,
                label = "Library",
                isSelected = selectedTab == BottomTab.Library,
                onClick = {
                    onTabSelected(BottomTab.Library)
                }
            )

            BottomTabItem(
                tab = BottomTab.Account,
                iconRes = R.drawable.person,
                label = "Account",
                isSelected = selectedTab == BottomTab.Account,
                onClick = {
                    onTabSelected(BottomTab.Account)
                }
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: BottomTab,
    @DrawableRes iconRes: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) CastyTheme.colors.textPrimary else CastyTheme.colors.textSecondary

    Column(
        modifier = Modifier
            .castyClickable(
                onClick = onClick,
                hapticType = HapticType.Light
            )
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Row(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(CastyTheme.colors.accentPink, RoundedCornerShape(500.dp))
            ) {}
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        if (isSelected) {
            Text(
                text = label,
                style = CastyTheme.typography.labelSmall.copy(
                    color = contentColor,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
