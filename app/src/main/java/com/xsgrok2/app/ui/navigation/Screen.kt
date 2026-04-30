package com.xsgrok2.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object CreateNovel : Screen("create_novel")
    object NovelDetail : Screen("novel_detail/{novelId}") {
        fun createRoute(novelId: Long) = "novel_detail/$novelId"
    }
    object ChapterReader : Screen("chapter_reader/{novelId}/{chapterId}") {
        fun createRoute(novelId: Long, chapterId: Long) = "chapter_reader/$novelId/$chapterId"
    }
}
