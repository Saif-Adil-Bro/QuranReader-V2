package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    object Home : BottomNavItem("home", "হোম", Icons.Filled.Home, Icons.Outlined.Home)
    object Mushaf : BottomNavItem("mushaf", "মুসহাফ", Icons.Default.MenuBook, Icons.Outlined.MenuBook)
    object Search : BottomNavItem("search", "সার্চ", Icons.Filled.Search, Icons.Outlined.Search)
    object Menu : BottomNavItem("settings", "মেনু", Icons.Filled.Menu, Icons.Outlined.Menu)
}

@Composable
fun BottomNavBar(
    navController: NavController,
    currentRoute: String?,
    isSplashVisible: Boolean = false
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Mushaf,
        BottomNavItem.Search,
        BottomNavItem.Menu
    )

    // The bottom nav bar should only be visible on top-level screens
    val isBottomNavItem = currentRoute != null && (
        currentRoute == "home" || 
        currentRoute == "mushaf" || 
        currentRoute == "search" || 
        currentRoute.startsWith("settings")
    ) && !isSplashVisible

    if (isBottomNavItem) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = currentRoute != null && (
                            currentRoute == item.route || 
                            (item.route == "settings" && currentRoute.startsWith("settings"))
                        )
                        BottomNavIcon(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                if (item.route == "home") {
                                    navController.popBackStack("home", inclusive = false)
                                } else if (item.route == "settings") {
                                    if (currentRoute != "settings") {
                                        navController.navigate("settings") {
                                            popUpTo("home") {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }
                                } else {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo("home") {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavIcon(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emeraldGreen = Color(0xFF10B981)
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    val color = if (isSelected) emeraldGreen else unselectedColor
    val iconColor = if (isSelected) emeraldGreen else unselectedColor
    val icon = if (isSelected) item.activeIcon else item.inactiveIcon
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = item.title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            color = color,
            fontSize = 12.sp,
            fontWeight = fontWeight
        )
    }
}
