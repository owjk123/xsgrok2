package com.xsgrok2.app.utils

import com.xsgrok2.app.data.model.Chapter
import com.xsgrok2.app.data.model.Novel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object ExportUtils {

    fun exportToTxt(novel: Novel, chapters: List<Chapter>, outputDir: File): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val safeTitle = novel.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val fileName = "${safeTitle}_${dateFormat.format(System.currentTimeMillis())}.txt"
        val file = File(outputDir, fileName)

        val sb = StringBuilder()
        sb.appendLine("\u300A${novel.title}\u300B")
        sb.appendLine("\u4F5C\u8005\uFF1AAI\u8F85\u52A9\u521B\u4F5C")
        sb.appendLine("\u7C7B\u578B\uFF1A${novel.genre}")
        if (novel.writingStyle.isNotEmpty()) {
            sb.appendLine("\u98CE\u683C\uFF1A${novel.writingStyle}")
        }
        sb.appendLine()
        sb.appendLine("\u3010\u8BBE\u5B9A\u3011")
        sb.appendLine(novel.worldSetting)
        sb.appendLine()
        sb.appendLine("\u3010\u89D2\u8272\u3011")
        sb.appendLine(novel.keyCharacters)
        sb.appendLine()
        sb.appendLine("\u3010\u5927\u7EB2\u3011")
        sb.appendLine(novel.outline)
        sb.appendLine()
        sb.appendLine("=".repeat(40))
        sb.appendLine()

        chapters.sortedBy { it.chapterNumber }.forEach { chapter ->
            sb.appendLine("--- ${chapter.displayTitle()} ---")
            sb.appendLine(chapter.content)
            sb.appendLine()
        }

        file.writeText(sb.toString())
        return file
    }
}
