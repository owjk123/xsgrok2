package com.xsgrok2.app.data.repository

import com.xsgrok2.app.data.api.GrokApiService
import com.xsgrok2.app.data.model.*

class GrokRepository(
    private val apiService: GrokApiService
) {

    private fun getSettingDimensions(genre: String): String {
        return when {
            genre.contains("玄幻") || genre.contains("科幻") -> "世界规则、力量体系、种族势力、核心矛盾"
            genre.contains("言情") || genre.contains("都市") -> "时代背景、社交圈层、情感基调、核心误会/羁绊"
            genre.contains("悬疑") || genre.contains("推理") -> "核心谜题、线索链、误导线索、受害者与嫌疑人关系"
            genre.contains("日常") || genre.contains("治愈") -> "生活场景、角色关系网、情感主题、核心日常事件"
            genre.contains("历史") || genre.contains("古风") -> "朝代背景、历史事件、礼制风俗、权力格局"
            else -> "时空背景、社会规则、核心冲突"
        }
    }

    private fun getNarrativeStructure(genre: String): String {
        return when {
            genre.contains("悬疑") || genre.contains("推理") -> "先设计结局真相，倒推线索链，分配到各章制造悬念"
            genre.contains("言情") || genre.contains("都市") -> "按感情线阶段规划：相遇/误解/冲突/和解/在一起"
            genre.contains("日常") || genre.contains("治愈") -> "单元剧式大纲，每章一个独立小故事，暗含人物成长"
            else -> "按起承转合规划，每章需有冲突与推进"
        }
    }

    suspend fun generateNovelSettings(
        apiKey: String,
        model: String,
        genre: String,
        description: String
    ): Result<String> {
        val settingDimensions = getSettingDimensions(genre)
        val narrativeStructure = getNarrativeStructure(genre)

        val systemPrompt = """
你是一位资深中文网络小说作家，精通${genre}类型创作。请根据给定的描述，生成以下内容：

## 输出要求
- 语言：纯中文，严禁中英混杂
- 叙事视角：第三人称有限视角（除非类型要求第一人称）
- 文风：细腻生动，根据类型自动适配

## 禁止事项（极其重要！）
- 严禁使用AI套路词：如"嘴角勾起一抹邪笑"、"眼底闪过一丝凉意"、"心中五味杂陈"、"不禁倒吸一口凉气"等陈词滥调
- 严禁注水：不要堆砌无关环境描写，动作描写要具体，对话要符合中国人日常习惯
- 严禁说教：不要在结尾做道德总结
- 严禁AI身份声明：不要出现"作为一名AI"等元评论

## 输出格式（必须严格遵循以下中文标题）

### 一、${if (genre.contains("玄幻") || genre.contains("科幻")) "世界设定" else "时代背景与环境"}
${settingDimensions}的详细设定。

### 二、核心角色
根据故事需要设定角色（3-8个），每个角色包含：
- 姓名（纯中文名，符合时代背景）
- 性格关键词（3个，不要用空泛词汇）
- 核心动机与内心冲突
- 与其他角色的关系

### 三、故事大纲
${narrativeStructure}。
根据故事规模规划章节数（短篇5-10章、中篇10-30章、长篇30+章），每章包含：
- 章节标题（格式为"第X章 标题"，标题要有吸引力）
- 核心事件（1-2句话，要有冲突或转折）
- 情绪曲线（铺垫/上升/高潮/回落/悬念）
""".trimIndent()

        val userPrompt = "我想写一部${genre}类型的小说，核心构思是：$description"

        return callApi(apiKey, model, systemPrompt, userPrompt, 0.8, 4096)
    }

    suspend fun generateChapter(
        apiKey: String,
        model: String,
        novel: Novel,
        chapterNumber: Int,
        chapterTitle: String,
        previousChapterContent: String = "",
        userNote: String = "",
        instruction: ChapterInstruction? = null,
        lorebookEntries: List<LorebookEntry> = emptyList()
    ): Result<String> {
        val lorebookContext = if (lorebookEntries.isNotEmpty()) {
            "\n【世界词条】\n" + lorebookEntries.joinToString("\n") { "- ${it.keyword}：${it.content}" }
        } else ""

        val instructionBlock = if (instruction != null) {
            buildString {
                appendLine("\n【本章创作指令】")
                if (instruction.coreEvent.isNotBlank()) appendLine("- 核心事件：${instruction.coreEvent}")
                if (instruction.characterChanges.isNotBlank()) appendLine("- 人物变化：${instruction.characterChanges}")
                if (instruction.mood.isNotBlank()) appendLine("- 情绪氛围：${instruction.mood}")
                if (instruction.foreshadowing.isNotBlank()) appendLine("- 伏笔回收：${instruction.foreshadowing}")
                if (instruction.newThreads.isNotBlank()) appendLine("- 新线埋设：${instruction.newThreads}")
                if (instruction.forbiddenElements.isNotBlank()) appendLine("- 禁止出现：${instruction.forbiddenElements}")
                if (instruction.wordCountTarget > 0) appendLine("- 目标字数：${instruction.wordCountTarget}字")
            }
        } else if (userNote.isNotBlank()) {
            "\n【特别要求】：$userNote。必须在剧情中落实此要求！"
        } else ""

        val systemPrompt = """
你是一位资深中文网络小说作家，正在连载一部${novel.genre}小说《${novel.title}》。

## 绝对禁止（违反将导致作品报废）
- 严禁AI套路词（"嘴角勾起"、"眼底闪过"、"心中五味杂陈"等）
- 严禁环境描写注水，严禁对话说教
- 严禁偏离当前章节大纲核心事件

## 写作要求
- 字数要求：至少${instruction?.wordCountTarget ?: novel.targetWordCount}个中文字符
- 角色行为必须符合设定，严禁人物失忆或性格突变
- 结尾要留下悬念或转折，承接下一章
- 对话要自然口语化，符合角色性格
- 动作和场景描写要具体，避免笼统概括
""".trimIndent()

        val userPrompt = buildString {
            appendLine("【核心设定】")
            appendLine(novel.worldSetting)
            appendLine()
            appendLine("【核心角色】")
            appendLine(novel.keyCharacters)
            appendLine()
            appendLine("【故事大纲】")
            appendLine(novel.outline)
            append(lorebookContext)
            appendLine()
            append("第${chapterNumber}章：${chapterTitle}")
            appendLine()
            if (previousChapterContent.isNotEmpty()) {
                appendLine()
                appendLine("【前一章结尾片段】")
                appendLine(previousChapterContent.takeLast(1500))
                appendLine()
            }
            append(instructionBlock)
            appendLine()
            append("请撰写第${chapterNumber}章：${chapterTitle}")
        }

        val temp = if (instruction != null) {
            minOf(novel.temperature + 0.05f, 1.0f)
        } else novel.temperature

        return callApi(apiKey, model, systemPrompt, userPrompt, temp.toDouble(), 4096)
    }

    suspend fun regenerateChapter(
        apiKey: String,
        model: String,
        novel: Novel,
        chapterNumber: Int,
        chapterTitle: String,
        previousChapterContent: String = "",
        currentContent: String = "",
        mode: String = "rewrite",
        instruction: ChapterInstruction? = null,
        userNote: String = "",
        lorebookEntries: List<LorebookEntry> = emptyList()
    ): Result<String> {
        val modeDesc = when (mode) {
            "rewrite" -> "完全重写这一章，不受已有内容限制，但保持与前后的连贯性"
            "improve" -> "基于现有内容改进：保持主线不变，提升文笔、补充细节、修正不合理之处"
            "continue" -> "继续现有内容：保留现有内容，从结尾处继续延伸"
            else -> "重写这一章"
        }

        val lorebookContext = if (lorebookEntries.isNotEmpty()) {
            "\n【世界词条】\n" + lorebookEntries.joinToString("\n") { "- ${it.keyword}：${it.content}" }
        } else ""

        val instructionBlock = if (instruction != null) {
            buildString {
                appendLine("\n【修改指令】")
                if (instruction.coreEvent.isNotBlank()) appendLine("- 调整核心事件为：${instruction.coreEvent}")
                if (instruction.characterChanges.isNotBlank()) appendLine("- 人物变化：${instruction.characterChanges}")
                if (instruction.mood.isNotBlank()) appendLine("- 情绪氛围：${instruction.mood}")
                if (instruction.foreshadowing.isNotBlank()) appendLine("- 伏笔回收：${instruction.foreshadowing}")
                if (instruction.newThreads.isNotBlank()) appendLine("- 新线埋设：${instruction.newThreads}")
                if (instruction.forbiddenElements.isNotBlank()) appendLine("- 禁止出现：${instruction.forbiddenElements}")
            }
        } else if (userNote.isNotBlank()) {
            "\n【修改要求】：$userNote"
        } else ""

        val systemPrompt = """
你是一位资深中文网络小说作家，正在修改一部${novel.genre}小说《${novel.title}》的第${chapterNumber}章。

## 修改模式：${modeDesc}

## 禁止事项
- 严禁AI套路词
- 严禁偏离大纲核心事件（除非指令要求调整）
- 保持与前后章节的连贯性

## 写作要求
- 字数要求：至少${novel.targetWordCount}个中文字符
- 角色行为必须符合设定
- 对话自然口语化
""".trimIndent()

        val userPrompt = buildString {
            appendLine("【核心设定】")
            appendLine(novel.worldSetting)
            appendLine()
            appendLine("【核心角色】")
            appendLine(novel.keyCharacters)
            appendLine()
            appendLine("【故事大纲】")
            appendLine(novel.outline)
            append(lorebookContext)
            appendLine()
            append("第${chapterNumber}章：${chapterTitle}")
            appendLine()
            if (previousChapterContent.isNotEmpty()) {
                appendLine()
                appendLine("【前一章结尾片段】")
                appendLine(previousChapterContent.takeLast(1500))
                appendLine()
            }
            if (currentContent.isNotEmpty() && mode != "rewrite") {
                appendLine()
                appendLine("【当前章节内容】")
                appendLine(currentContent.take(3000))
                appendLine()
            }
            append(instructionBlock)
            appendLine()
            append("请${modeDesc}：第${chapterNumber}章：${chapterTitle}")
        }

        return callApi(apiKey, model, systemPrompt, userPrompt, novel.temperature.toDouble(), 4096)
    }

    suspend fun rewriteSelection(
        apiKey: String,
        model: String,
        novel: Novel,
        selectedText: String,
        instruction: String
    ): Result<String> {
        val systemPrompt = """
你是一位资深中文小说编辑，精通文字润色和改写。

## 规则
- 保持与原文的风格一致
- 严禁AI套路词
- 改写后的文本应比原文更好，而非不同
- 保持上下文语义连贯
""".trimIndent()

        val userPrompt = buildString {
            appendLine("小说类型：${novel.genre}，写作风格：${novel.writingStyle}")
            appendLine()
            appendLine("【原文】")
            appendLine(selectedText)
            appendLine()
            appendLine("【改写指令】$instruction")
            appendLine()
            append("请根据改写指令重写以上原文片段，只输出改写后的内容：")
        }

        return callApi(apiKey, model, systemPrompt, userPrompt, 0.7, 2048)
    }

    private suspend fun callApi(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.8,
        maxTokens: Int = 4096
    ): Result<String> {
        return try {
            val request = ChatRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                temperature = temperature,
                max_tokens = maxTokens
            )

            val response = apiService.chatCompletion("Bearer $apiKey", request)
            if (response.error != null) {
                Result.failure(Exception(response.error.message ?: "API错误"))
            } else if (response.choices.isNullOrEmpty()) {
                Result.failure(Exception("API返回为空"))
            } else {
                val content = response.choices[0].message?.content ?: ""
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
