package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerStatus
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorderSubtle
import com.example.ui.theme.EditorialGreen
import com.example.ui.theme.EditorialGold
import com.example.ui.theme.EditorialOnPrimary
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSurface
import com.example.ui.theme.EditorialTabActive
import com.example.ui.theme.EditorialTabActiveIcon
import com.example.ui.theme.EditorialTextMuted
import com.example.ui.theme.EditorialTextPrimary
import com.example.ui.theme.EditorialTextSecondary

@Composable
fun CloudMoviesTopBar(
    serverStatus: ServerStatus,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EditorialBackground,
                        EditorialBackground.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Live status pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Editorial Lilac Icon Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EditorialPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "CloudMovies",
                        tint = EditorialOnPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // App Title (Editorial bold italic uppercase)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CLOUD",
                        color = EditorialTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "MOVIES",
                        color = EditorialPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Live Server indicator pill
                val isOnline = serverStatus.status.equals("ONLINE", ignoreCase = true)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isOnline) Color(0x2681C784) else Color(0x26FFD54F))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) EditorialGreen else EditorialGold)
                    )
                    Text(
                        text = if (isOnline) "ONLINE" else "STANDALONE",
                        color = if (isOnline) EditorialGreen else EditorialGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action Icons (Frosted Search & Settings Buttons)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, EditorialBorderSubtle, CircleShape)
                        .clickable { onSearchClick() }
                        .testTag("topbar_search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = EditorialTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, EditorialBorderSubtle, CircleShape)
                        .clickable { onSettingsClick() }
                        .testTag("topbar_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = EditorialTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

enum class NavTab(val title: String) {
    HOME("Beranda"),
    SEARCH("Pencarian"),
    WATCHLIST("Daftar"),
    SETTINGS("Pengaturan")
}

@Composable
fun CloudMoviesBottomNav(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, EditorialBorderSubtle)
            .shadow(elevation = 16.dp)
    ) {
        NavigationBar(
            modifier = Modifier.testTag("bottom_navigation_bar"),
            containerColor = EditorialSurface,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentTab == NavTab.HOME,
                onClick = { onTabSelected(NavTab.HOME) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == NavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Beranda"
                    )
                },
                label = {
                    Text(
                        "Beranda",
                        fontSize = 11.sp,
                        fontWeight = if (currentTab == NavTab.HOME) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EditorialTabActiveIcon,
                    selectedTextColor = EditorialPrimary,
                    unselectedIconColor = EditorialTextMuted,
                    unselectedTextColor = EditorialTextMuted,
                    indicatorColor = EditorialTabActive
                ),
                modifier = Modifier.testTag("nav_home")
            )

            NavigationBarItem(
                selected = currentTab == NavTab.SEARCH,
                onClick = { onTabSelected(NavTab.SEARCH) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == NavTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                        contentDescription = "Pencarian"
                    )
                },
                label = {
                    Text(
                        "Cari",
                        fontSize = 11.sp,
                        fontWeight = if (currentTab == NavTab.SEARCH) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EditorialTabActiveIcon,
                    selectedTextColor = EditorialPrimary,
                    unselectedIconColor = EditorialTextMuted,
                    unselectedTextColor = EditorialTextMuted,
                    indicatorColor = EditorialTabActive
                ),
                modifier = Modifier.testTag("nav_search")
            )

            NavigationBarItem(
                selected = currentTab == NavTab.WATCHLIST,
                onClick = { onTabSelected(NavTab.WATCHLIST) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == NavTab.WATCHLIST) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Daftar Tontonan"
                    )
                },
                label = {
                    Text(
                        "Daftar Saya",
                        fontSize = 11.sp,
                        fontWeight = if (currentTab == NavTab.WATCHLIST) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EditorialTabActiveIcon,
                    selectedTextColor = EditorialPrimary,
                    unselectedIconColor = EditorialTextMuted,
                    unselectedTextColor = EditorialTextMuted,
                    indicatorColor = EditorialTabActive
                ),
                modifier = Modifier.testTag("nav_watchlist")
            )

            NavigationBarItem(
                selected = currentTab == NavTab.SETTINGS,
                onClick = { onTabSelected(NavTab.SETTINGS) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == NavTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                        contentDescription = "Pengaturan"
                    )
                },
                label = {
                    Text(
                        "Pengaturan",
                        fontSize = 11.sp,
                        fontWeight = if (currentTab == NavTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EditorialTabActiveIcon,
                    selectedTextColor = EditorialPrimary,
                    unselectedIconColor = EditorialTextMuted,
                    unselectedTextColor = EditorialTextMuted,
                    indicatorColor = EditorialTabActive
                ),
                modifier = Modifier.testTag("nav_settings")
            )
        }
    }
}
