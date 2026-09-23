package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.ui.component.AdMobBanner
import com.thirtytwo_cereernote.ui.navigation.CareerNoteNavGraph
import com.thirtytwo_cereernote.ui.navigation.Screen
import com.thirtytwo_cereernote.ui.navigation.bottomNavItems
import com.thirtytwo_cereernote.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    initialId: Long = -1L,
    initialType: String? = null,
    extraId: Long = -1L
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Handle initial navigation from intent
    LaunchedEffect(initialId, initialType, extraId) {
        if (initialId != -1L) {
            when (initialType) {
                NotificationHelper.TYPE_APPLICATION -> {
                    navController.navigate("application_detail/$initialId")
                }
                NotificationHelper.TYPE_INTERVIEW -> {
                    if (extraId != -1L) {
                        navController.navigate("application_detail/$extraId")
                    }
                }
            }
        }
    }

    val currentRoute = currentDestination?.route ?: ""
    val context = LocalContext.current
    val bottomNavItem = bottomNavItems.find { screen -> currentRoute == screen.route }
    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    val title = when {
        currentRoute == Screen.Home.route -> stringResource(R.string.app_name)
        bottomNavItem != null -> stringResource(bottomNavItem.titleResId)
        currentRoute == "add_application" -> stringResource(R.string.title_add_application)
        currentRoute.startsWith("application_detail") -> stringResource(R.string.title_application_detail)
        currentRoute.startsWith("career_list") -> {
            val titleResId = navBackStackEntry?.arguments?.getString("titleResId")?.toIntOrNull() ?: 0
            val resTitle = remember(titleResId) {
                if (titleResId != 0) {
                    try { context.getString(titleResId) } catch (_: Exception) { null }
                } else null
            }
            resTitle ?: "커리어 목록"
        }
        currentRoute.startsWith("add_career") -> {
            val titleResId = navBackStackEntry?.arguments?.getString("titleResId")?.toIntOrNull() ?: 0
            val prefix = remember(titleResId) {
                if (titleResId != 0) {
                    try { context.getString(titleResId) } catch (_: Exception) { null }
                } else null
            } ?: "커리어"
            "$prefix 기록"
        }
        currentRoute.startsWith("career_detail") -> "상세 보기"
        else -> stringResource(R.string.app_name)
    }

    Scaffold(
        topBar = {
            if (isTopLevelDestination) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 12.dp) // Lower for stability
                        ) {
                            if (currentRoute == Screen.Home.route) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column {
                    AdMobBanner()
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(screen.titleResId),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            CareerNoteNavGraph(navController = navController)
        }
    }
}
