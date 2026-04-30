package com.xsgrok2.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xsgrok2.app.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToCreateNovel = { navController.navigate(Screen.CreateNovel.route) },
                onNavigateToNovelDetail = { novelId ->
                    navController.navigate(Screen.NovelDetail.createRoute(novelId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateNovel.route) {
            CreateNovelScreen(
                onNavigateBack = { navController.popBackStack() },
                onNovelCreated = { novelId ->
                    navController.popBackStack()
                    navController.navigate(Screen.NovelDetail.createRoute(novelId))
                }
            )
        }

        composable(
            route = Screen.NovelDetail.route,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: return@composable
            NovelDetailScreen(
                novelId = novelId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChapter = { novelId, chapterId ->
                    navController.navigate(Screen.ChapterReader.createRoute(novelId, chapterId))
                }
            )
        }

        composable(
            route = Screen.ChapterReader.route,
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType },
                navArgument("chapterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: return@composable
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
            ChapterReaderScreen(
                novelId = novelId,
                chapterId = chapterId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
