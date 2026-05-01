package com.xsgrok2.app.utils

import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.Novel
import java.io.File
import java.nio.charset.Charsets
import java.text.SimpleDateFormat
import java.util.Locale

object ExportUtils {

    fun exportToTxt(novel: Novel, chapters: List<Chapter>, outputDir: File): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val safeTitle = novel.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val fileName = "${safeTitle}_${dateFormat.format(System.currentTimeMillis())}.txt"
        val file = File(outputDir, fileName)

        val sb = StringBuilder()
        sb.appendLine("《${novel.title}》")
        sb.appendLine("作者：AI辅助创作")
        sb.appendLine("类型：${novel.genre}")
        if (novel.writingStyle.isNotEmpty()) {
            sb.appendLine("风格：${novel.writingStyle}")
        }
        sb.appendLine()
        sb.appendLine("【设定】")
        sb.appendLine(novel.worldSetting)
        sb.appendLine()
        sb.appendLine("【角色】")
        sb.appendLine(novel.keyCharacters)
        sb.appendLine()
        sb.appendLine("【大纲】")
        sb.appendLine(novel.outline)
        sb.appendLine()
        sb.appendLine("=" .repeat(40))
        sb.appendLine()

        chapters.sortedBy { it.chapterNumber }.forEach { chapter ->
            sb.appendLine("--- ${chapter.title} ---")
            sb.appendLine(chapter.content)
            sb.appendLine()
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }
}
