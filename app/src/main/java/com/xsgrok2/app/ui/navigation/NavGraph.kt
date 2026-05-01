package com.xsgrok2.app.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xsgrok2.app.App
import com.xsgrok2.app.data.repository.GrokRepository
import com.xsgrok2.app.data.repository.NovelRepository
import com.xsgrok2.app.data.api.RetrofitClient
import com.xsgrok2.app.ui.screens.*
import com.xsgrok2.app.ui.viewmodel.*

@Composable
fun NavGraph(navController: NavHostController) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as App
    val database = app.database
    val preferences = app.preferences

    val novelRepository = NovelRepository(
        database.novelDao(),
        database.chapterDao(),
        database.lorebookEntryDao(),
        database.chapterInstructionDao()
    )
    val grokRepository = GrokRepository(
        RetrofitClient.create(preferences.apiBaseUrl).create(com.xsgrok2.app.data.api.GrokApiService::class.java)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(novelRepository))
            val uiState by homeViewModel.uiState.collectAsState()
            HomeScreen(
                uiState = uiState,
                onNavigateToCreate = { navController.navigate(Screen.CreateNovel.route) },
                onNavigateToDetail = { novelId -> navController.navigate(Screen.NovelDetail.createRoute(novelId)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onDeleteNovel = { novel -> homeViewModel.deleteNovel(novel) }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(preferences))
            val uiState by settingsViewModel.uiState.collectAsState()
            SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onUpdateApiKey = { settingsViewModel.updateApiKey(it) },
                onUpdateModel = { settingsViewModel.updateModel(it) },
                onUpdateApiBaseUrl = { settingsViewModel.updateApiBaseUrl(it) },
                onUpdateWritingStyle = { settingsViewModel.updateWritingStyle(it) },
                onUpdateFontSize = { settingsViewModel.updateFontSize(it) },
                onUpdateNightMode = { settingsViewModel.updateNightMode(it) },
                onSave = { settingsViewModel.saveSettings() }
            )
        }

        composable(Screen.CreateNovel.route) {
            val createViewModel: CreateNovelViewModel = viewModel(factory = CreateNovelViewModel.Factory(novelRepository, grokRepository, preferences))
            val uiState by createViewModel.uiState.collectAsState()
            CreateNovelScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onUpdateGenre = { createViewModel.updateGenre(it) },
                onUpdateDescription = { createViewModel.updateDescription(it) },
                onUpdateWritingStyle = { createViewModel.updateWritingStyle(it) },
                onUpdateGeneratedSettings = { createViewModel.updateGeneratedSettings(it) },
                onUpdateTargetWordCount = { createViewModel.updateTargetWordCount(it) },
                onUpdateTemperature = { createViewModel.updateTemperature(it) },
                onGenerate = { createViewModel.generateSettings() },
                onConfirm = { createViewModel.confirmAndCreateNovel() },
                onNavigateToDetail = { novelId ->
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
            val detailViewModel: NovelDetailViewModel = viewModel(factory = NovelDetailViewModel.Factory(novelId, novelRepository, grokRepository, preferences))
            val uiState by detailViewModel.uiState.collectAsState()
            NovelDetailScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onGenerateNextChapter = { title, instruction, note ->
                    detailViewModel.generateNextChapter(title, instruction, note)
                },
                onInsertChapterAt = { position, title, instruction, note ->
                    detailViewModel.insertChapterAt(position, title, instruction, note)
                },
                onRegenerateChapter = { chapterId, mode, instruction, note ->
                    detailViewModel.regenerateChapter(chapterId, mode, instruction, note)
                },
                onDeleteChapter = { chapterId -> detailViewModel.deleteChapter(chapterId) },
                onUpdateChapterTitle = { chapterId, title -> detailViewModel.updateChapterTitle(chapterId, title) },
                onNavigateToChapter = { chapterId ->
                    navController.navigate(Screen.ChapterReader.createRoute(novelId, chapterId))
                },
                onDeleteNovel = {
                    detailViewModel.deleteNovel()
                    navController.popBackStack()
                },
                onUpdateNovelSetting = { field, content ->
                    detailViewModel.updateNovelSetting(field, content)
                },
                onRegenerateSettings = { detailViewModel.regenerateSettings() },
                onAddLorebookEntry = { keyword, content, importance ->
                    detailViewModel.addLorebookEntry(keyword, content, importance)
                },
                onDeleteLorebookEntry = { entry ->
                    detailViewModel.deleteLorebookEntry(entry)
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
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
            val readerViewModel: ChapterReaderViewModel = viewModel(
                factory = ChapterReaderViewModel.Factory(chapterId, novelRepository, grokRepository, preferences)
            )
            val uiState by readerViewModel.uiState.collectAsState()
            ChapterReaderScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onStartEditing = { readerViewModel.startEditing() },
                onSaveEdit = { content -> readerViewModel.updateEditContent(content); readerViewModel.saveEdit() },
                onCancelEdit = { readerViewModel.cancelEdit() },
                onRewriteSelection = { start, end, instruction ->
                    readerViewModel.rewriteSelection(start, end, instruction)
                },
                onRewriteFull = { instruction ->
                    readerViewModel.rewriteFullContent(instruction)
                },
                onGenerateNextChapter = { readerViewModel.generateNextChapterFromReader() },
                onNavigateToChapter = { newChapterId ->
                    readerViewModel.navigateToChapter(newChapterId)
                },
                fontSize = preferences.fontSize,
                nightMode = preferences.nightMode
            )
        }
    }
}
