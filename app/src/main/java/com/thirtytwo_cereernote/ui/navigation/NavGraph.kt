package com.thirtytwo_cereernote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thirtytwo_cereernote.ui.screen.*

@Composable
fun CareerNoteNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onApplicationClick = { appId -> 
                    navController.navigate("application_detail/$appId")
                },
                onTipClick = { categoryName ->
                    navController.navigate("career_tips_detail/$categoryName")
                },
                onAddApplicationClick = {
                    navController.navigate("add_application")
                }
            )
        }
        composable(Screen.Applications.route) {
            ApplicationsScreen(
                onAddClick = { navController.navigate("add_application") },
                onItemClick = { id -> navController.navigate("application_detail/$id") }
            )
        }
        composable("add_application") {
            AddApplicationScreen(onBack = { navController.popBackStack() })
        }
        composable("application_detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
            ApplicationDetailScreen(
                applicationId = id,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Career.route) {
            CareerScreen(
                onMenuItemClick = { id, titleResId -> 
                    navController.navigate("career_list/$id/$titleResId") 
                },
                onRecentItemClick = { categoryId, itemId ->
                    navController.navigate("career_detail/$categoryId/$itemId")
                }
            )
        }
        composable("career_list/{id}/{titleResId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val titleResId = backStackEntry.arguments?.getString("titleResId")?.toIntOrNull() ?: 0
            val title = if (titleResId != 0) stringResource(titleResId) else ""
            CareerListScreen(
                id = id,
                title = title,
                onAddClick = { navController.navigate("add_career/$id/$titleResId") },
                onItemClick = { itemId -> navController.navigate("career_detail/$id/$itemId") }
            )
        }
        composable(
            route = "add_career/{id}/{titleResId}?itemId={itemId}",
            arguments = listOf(
                androidx.navigation.navArgument("itemId") { defaultValue = "0" }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull() ?: 0L
            AddCareerScreen(
                id = id,
                itemId = itemId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("career_detail/{id}/{itemId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull() ?: return@composable
            CareerDetailScreen(
                id = id,
                itemId = itemId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("add_career/$id/0?itemId=$itemId") }
            )
        }
        composable(Screen.Report.route) {
            ReportScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onCareerTipsClick = { navController.navigate("career_tips") }
            )
        }
        composable("career_tips") {
            CareerTipsScreen(
                onCategoryClick = { category -> 
                    navController.navigate("career_tips_detail/${category.name}")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("career_tips_detail/{categoryName}") { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CareerTipsDetailScreen(
                categoryName = categoryName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
