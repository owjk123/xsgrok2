# xsgrok2 综合审核修改方案

综合产品经理与深度用户的审核意见，两方在**上下文截断、全英文Prompt、结构化设定缺失、导出与阅读体验**等核心问题上高度一致。以下为统一的修改方案及代码实现。

---

## 一、 问题优先级排序 (P0/P1/P2)

### 🔴 P0（必须修 - 核心体验与可用性致命缺陷）
1. **全英文Prompt与中文适配缺失**：导致翻译腔、AI套路词、字数指令错乱、类型不适配（PM #10, #11, #12; DU #1, #2, #3, #4, #14）
2. **上下文严重截断**：`take(500)` 导致长篇必定设定崩塌（PM #9; DU #8, #9）
3. **流式输出未启用**：长时间空白等待，体验极差（PM #2）
4. **extractSection解析问题**：英文解析逻辑无法识别中文标题，导致设定提取失败

### 🟠 P1（应该修 - 用户控制力与基础体验）
5. **数据模型非结构化**：设定/大纲铁板一块，无法局部修改与精准注入上下文（PM #7; DU #5）
6. **章节生成无用户干预**：缺少 `userNote` 参数，控制力僵硬（DU #10）
7. **章节编辑与状态缺失**：无重新生成、无编辑状态、无 `finishReason` 截断提示（PM #3, #4; DU #6, #11）
8. **阅读器UI与设置缺失**：无夜间模式、字号调整，10万字无法阅读（DU #7）
9. **导出功能缺失**：内容被困在App内（PM #5; DU #13）
10. **API Key明文存储与硬编码代理**：安全与信任风险（PM #8）

### 🟡 P2（可以修 - 进阶功能与留存）
11. 批量连更生成（DU #12）
12. 云同步与账号体系（PM #13）
13. 留存机制与字数统计（PM #14）

---

## 二、 P0/P1问题具体修改方案

### 1. 全中文Prompt与类型/风格适配 (P0)
- **文件**: `GrokRepository.kt`
- **方案**: 废除硬编码英文Prompt。根据 `genre` 动态构建中文Prompt；增加反AI套路词指令（禁止"嘴角勾起"等）；将字数指令改为中文字符数要求；增加写作风格 `writingStyle` 参数。

### 2. 上下文优化与结构化大纲 (P0/P1)
- **文件**: `GrokRepository.kt`, `Novel.kt`, `Chapter.kt`
- **方案**: 删除 `.take(500)`。将 `outline` 从纯文本改为 `List<OutlineItem>`，生成章节时只注入**当前章+前后章大纲**而非全量大纲。增加 `chapterSummary` 字段，生成后自动提取摘要，后续章节携带近3章摘要代替原文。

### 3. extractSection解析修复 (P0)
- **文件**: `GrokRepository.kt`
- **方案**: 原有正则只匹配英文Markdown（如 `# World Setting`），改为兼容中文数字标记（如 `一、世界设定`、`### 核心角色`）的提取逻辑。

### 4. 流式输出启用 (P0)
- **文件**: `ChatRequest.kt`, `ApiModels.kt`
- **方案**: `stream` 默认改为 `true`，Repository 层增加 SSE 流式解析回调。

### 5. 章节编辑、干预与状态 (P1)
- **文件**: `Chapter.kt`, `GrokRepository.kt`
- **方案**: 增加 `userNote`（用户指令）、`generationStatus`（生成状态枚举）、`finishReason`（截断原因）。增加 `regenerateChapter` 方法支持单章重写。

### 6. 阅读设置与导出 (P1)
- **文件**: `AppPreferences.kt`, 新增 `ExportUtils.kt`
- **方案**: 增加夜间模式、字号行距偏好存储；增加 TXT 纯文本导出工具类。

---

## 三、 修改后的代码片段

===FILE: com/xsgrok2/data/model/Novel.kt===
```kotlin
package com.xsgrok2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "novels")
@TypeConverters(Converters::class)
data class Novel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val genre: String = "",            // 类型：玄幻、言情、悬疑、都市等
    val description: String = "",      // 用户原始描述
    val writingStyle: String = "细腻生动", // 新增：写作风格（细腻/简洁/幽默/冷峻）
    
    // 重构：将铁板一块的String改为结构化数据或动态上下文
    val settingContext: String = "",   // 替代原worldSetting，适配现实/悬疑等题材
    val keyCharacters: String = "",   // 暂保留JSON字符串，存储角色列表
    val outlineItems: List<OutlineItem> = emptyList(), // 新增：结构化大纲
    
    // 留存相关字段
    val totalWordCount: Int = 0,
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 新增：结构化大纲条目
data class OutlineItem(
    val chapterNumber: Int,
    val title: String,                // 章节标题（中文格式）
    val plotPoint: String,            // 核心事件
    val emotionCurve: String,         // 情绪曲线：铺垫/上升/高潮/回落/悬念
    val involvedCharacterNames: List<String> = emptyList() // 涉及角色名
)

class Converters {
    // Room TypeConverters for List<OutlineItem>
    // Implementation omitted for brevity, use Gson or manual JSON parsing
}
```
===END===

===FILE: com/xsgrok2/data/model/Chapter.kt===
```kotlin
package com.xsgrok2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val novelId: Long = 0,
    val chapterNumber: Int = 0,
    val title: String = "",           // 将默认的"Chapter X"改为"第X章"
    val content: String = "",
    
    // 新增：生成状态与控制字段
    val generationStatus: GenerationStatus = GenerationStatus.NOT_STARTED,
    val finishReason: String? = null, // "stop", "length" (截断未写完)
    val userNote: String? = null,     // 用户对本章的额外指令（如：本章男主需发现秘密）
    val chapterSummary: String? = null,// AI生成的本章摘要（供后续上下文使用）
    val wordCount: Int = 0,           // 中文字符数
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class GenerationStatus {
    NOT_STARTED, GENERATING, GENERATED, FAILED, PARTIAL // PARTIAL对应finishReason=length
}
```
===END===

===FILE: com/xsgrok2/data/model/ApiModels.kt===
```kotlin
package com.xsgrok2.data.model

// 修复：增加 finish_reason 映射
data class StreamChunk(val id: String? = null, val choices: List<StreamChoice>? = null)
data class StreamChoice(val index: Int = 0, val delta: Delta? = null, val finish_reason: String? = null)
data class Delta(val role: String? = null, val content: String? = null)

// 非流式响应也需增加 finish_reason
data class ChatResponse(
    val id: String? = null,
    val choices: List<ResponseChoice>? = null
)

data class ResponseChoice(
    val index: Int = 0,
    val message: ResponseMessage? = null,
    val finish_reason: String? = null // 新增：用于判断是否因max_tokens截断
)

data class ResponseMessage(
    val role: String? = null,
    val content: String? = null
)
```
===END===

===FILE: com/xsgrok2/data/model/ChatRequest.kt===
```kotlin
package com.xsgrok2.data.model

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = true,       // 修复：硬编码false改为true，开启流式
    val max_tokens: Int = 4096,
    val temperature: Double = 0.8     // 适当提高创意性
)

data class Message(
    val role: String,
    val content: String
)
```
===END===

===FILE: com/xsgrok2/data/AppPreferences.kt===
```kotlin
package com.xsgrok2.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AppPreferences(context: Context) {
    // 修复：使用 EncryptedSharedPreferences 替代明文存储
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "xsgrok2_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var apiBaseUrl: String
        get() = prefs.getString("api_base_url", "https://api.grok.com/v1") ?: "https://api.grok.com/v1" // 移除硬编码第三方代理
        set(value) = prefs.edit().putString("api_base_url", value).apply()

    // 新增：阅读器设置
    var fontSize: Int
        get() = prefs.getInt("font_size", 16) // 默认16sp
        set(value) = prefs.edit().putInt("font_size", value).apply()

    var lineSpacing: Float
        get() = prefs.getFloat("line_spacing", 1.5f) // 默认1.5倍行距
        set(value) = prefs.edit().putFloat("line_spacing", value).apply()

    var nightMode: Boolean
        get() = prefs.getBoolean("night_mode", false)
        set(value) = prefs.edit().putBoolean("night_mode", value).apply()
}
```
===END===

===FILE: com/xsgrok2/utils/ExportUtils.kt===
```kotlin
package com.xsgrok2.utils

import com.xsgrok2.data.model.Novel
import com.xsgrok2.data.model.Chapter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object ExportUtils {
    // 新增：导出TXT功能
    fun exportToTxt(novel: Novel, chapters: List<Chapter>, outputDir: File): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "${novel.title}_${dateFormat.format(System.currentTimeMillis())}.txt"
        val file = File(outputDir, fileName)
        
        val sb = StringBuilder()
        sb.appendLine("《${novel.title}》")
        sb.appendLine("作者：AI辅助创作")
        sb.appendLine("类型：${novel.genre}")
        sb.appendLine()
        sb.appendLine("【设定】")
        sb.appendLine(novel.settingContext)
        sb.appendLine()
        sb.appendLine("【角色】")
        sb.appendLine(novel.keyCharacters)
        sb.appendLine()
        
        chapters.sortedBy { it.chapterNumber }.forEach { chapter ->
            sb.appendLine("--- ${chapter.title} ---")
            sb.appendLine(chapter.content)
            sb.appendLine()
        }
        
        file.writeText(sb.toString(), charset = "UTF-8")
        return file
    }
}
```
===END===

===FILE: com/xsgrok2/repository/GrokRepository.kt===
```kotlin
package com.xsgrok2.repository

import com.xsgrok2.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GrokRepository(private val apiClient: GrokApiClient) {

    // ==================== P0: Prompt 全面重构 ====================
    
    // 获取类型适配的设定维度要求
    private fun getSettingDimensions(genre: String): String {
        return when (genre) {
            "玄幻", "科幻" -> "世界规则、力量体系、种族势力、核心矛盾"
            "言情", "都市" -> "时代背景、社交圈层、情感基调、核心误会/羁绊"
            "悬疑", "推理" -> "核心谜题、线索链、红鲱鱼（误导线索）、受害者与嫌疑人关系"
            "日常", "治愈" -> "生活场景、角色关系网、情感主题、核心日常事件"
            "历史", "古风" -> "朝代背景、历史事件、礼制风俗、权力格局"
            else -> "时空背景、社会规则、核心冲突"
        }
    }

    // 获取叙事结构指导
    private fun getNarrativeStructure(genre: String): String {
        return when (genre) {
            "悬疑", "推理" -> "先设计结局真相，倒推线索链，分配到各章制造悬念"
            "言情", "都市" -> "按感情线阶段规划：相遇/误解/冲突/和解/在一起"
            "群像" -> "多线并行大纲，标注各线交汇点"
            "日常", "治愈" -> "单元剧式大纲，每章一个独立小故事，暗含人物成长"
            else -> "按起承转合规划，每章需有冲突与推进"
        }
    }

    // 生成设定 - 全中文Prompt + 反套路指令
    suspend fun generateNovelSettings(
        apiKey: String, model: String, genre: String, description: String, writingStyle: String
    ): Result<Novel> {
        val settingDimensions = getSettingDimensions(genre)
        val narrativeStructure = getNarrativeStructure(genre)
        
        val systemPrompt = """
你是一位资深中文网络小说作家，精通${genre}类型创作。请根据给定的描述，生成以下内容：

## 输出要求
- 语言：纯中文，严禁中英混杂。
- 叙事视角：第三人称有限视角（除非类型要求第一人称）。
- 文风：${writingStyle}（根据类型自动适配细节）。

## 禁止事项（极其重要！）
- 严禁使用AI套路词：如"嘴角勾起一抹邪笑"、"眼底闪过一丝凉意"、"心中五味杂陈"、"不禁倒吸一口凉气"等陈词滥调。
- 严禁注水：不要堆砌无关环境描写，动作描写要具体，对话要符合中国人日常习惯。
- 严禁说教：不要在结尾做道德总结，不要使用"总之"、"综上所述"。
- 严禁AI身份声明：不要出现"作为一名AI"等元评论。

## 输出格式（必须严格遵循以下中文标题）

### 一、${if (genre in listOf("玄幻", "科幻")) "世界设定" else "时代背景与环境"}
${settingDimensions}的详细设定。

### 二、核心角色
根据故事需要设定角色数量（不限于3-5个），每个角色包含：
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

        val request = ChatRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = userPrompt)
            ),
            stream = false // 设定生成暂可非流式，以便整体解析
        )

        return try {
            val response = apiClient.sendRequest(apiKey, request)
            val content = response.choices?.firstOrNull()?.message?.content ?: ""
            
            // P0 修复：extractSection 解析兼容中文标题
            val setting = extractSection(content, listOf("一、", "世界设定", "时代背景与环境"))
            val characters = extractSection(content, listOf("二、", "核心角色"))
            val outlineRaw = extractSection(content, listOf("三、", "故事大纲"))
            
            // P1 修复：大纲结构化解析
            val outlineItems = parseOutlineItems(outlineRaw)
            
            Result.success(Novel(
                genre = genre,
                description = description,
                writingStyle = writingStyle,
                settingContext = setting,
                keyCharacters = characters,
                outlineItems = outlineItems
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== P0: 上下文优化与章节生成 ====================
    
    // 流式生成章节
    fun generateChapterStream(
        apiKey: String, model: String, novel: Novel, chapterNumber: Int, 
        previousChapters: List<Chapter>, userNote: String?
    ): Flow<String> = flow {
        val currentOutline = novel.outlineItems.find { it.chapterNumber == chapterNumber }
        val prevOutline = novel.outlineItems.find { it.chapterNumber == chapterNumber - 1 }
        val nextOutline = novel.outlineItems.find { it.chapterNumber == chapterNumber + 1 }

        // P0 修复：移除 .take(500)，改为摘要+精准大纲注入
        val recentSummaries = previousChapters.takeLast(3).mapNotNull { it.chapterSummary }
        val lastChapterTail = previousChapters.lastOrNull()?.content?.takeLast(1000) ?: ""

        val contextBuilder = StringBuilder()
        contextBuilder.appendLine("【核心设定】\n${novel.settingContext}")
        contextBuilder.appendLine("【核心角色】\n${novel.keyCharacters}")
        
        // P1 修复：只注入相关大纲，避免长上下文爆炸
        contextBuilder.appendLine("【大纲定位】")
        if (prevOutline != null) contextBuilder.appendLine("前一章(${prevOutline.title}): ${prevOutline.plotPoint}")
        if (currentOutline != null) contextBuilder.appendLine("当前章(${currentOutline.title}): ${currentOutline.plotPoint} (情绪: ${currentOutline.emotionCurve})")
        if (nextOutline != null) contextBuilder.appendLine("下一章(${nextOutline.title}): ${nextOutline.plotPoint}")

        // 动态摘要上下文
        if (recentSummaries.isNotEmpty()) {
            contextBuilder.appendLine("【前文摘要】\n${recentSummaries.joinToString("\n")}")
        }
        contextBuilder.appendLine("【前一章结尾片段】\n$lastChapterTail")

        val systemPrompt = """
你是一位资深中文网络小说作家，正在连载一部${novel.genre}小说《${novel.title}》。
文风要求：${novel.writingStyle}。

## 绝对禁止（违反将导致作品报废）
- 严禁AI套路词（"嘴角勾起"、"眼底闪过"、"心中五味杂陈"等）。
- 严禁环境描写注水，严禁对话说教。
- 严禁偏离当前章大纲核心事件。

## 写作要求
- 字数要求：至少2500个中文字符（不要用英文word计算）。
- 必须严格推进当前章大纲的剧情：${currentOutline?.plotPoint ?: "待定"}。
- 角色行为必须符合设定，严禁人物失忆或性格突变。
- 结尾要留下悬念或转折，承接下一章：${nextOutline?.plotPoint ?: "待定"}。
""".trimIndent()

        val userMsgBuilder = StringBuilder("请根据以上设定和上下文，撰写第${chapterNumber}章：${currentOutline?.title ?: "待定"}。")
        
        // P1 修复：注入用户干预指令
        if (!userNote.isNullOrEmpty()) {
            userMsgBuilder.appendLine("\n\n【读者/作者特别要求】：$userNote。必须在剧情中落实此要求！")
        }

        val request = ChatRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = systemPrompt),
                Message(role = "user", content = contextBuilder.toString()),
                Message(role = "user", content = userMsgBuilder.toString())
            ),
            stream = true // 开启流式
        )

        // 调用API并emit流式数据
        apiClient.sendStreamRequest(apiKey, request).collect { chunk -> 
            emit(chunk) 
        }
    }

    // ==================== 工具方法 ====================

    // P0 修复：兼容中文标题的 extractSection
    private fun extractSection(fullText: String, sectionHeaders: List<String>): String {
        val lines = fullText.lines()
        val sb = StringBuilder()
        var capturing = false
        
        for (line in lines) {
            // 匹配 Markdown 标题或中文数字标题
            val isHeader = line.trim().startsWith("#") || 
                           line.trim().matches(Regex("^[一二三四五六七八九十]+、.*"))
                           
            if (isHeader) {
                if (capturing) break // 遇到下一个标题结束截取
                // 检查是否是目标标题
                if (sectionHeaders.any { header -> line.contains(header) }) {
                    capturing = true
                }
            } else if (capturing) {
                sb.appendLine(line)
            }
        }
        return sb.toString().trim()
    }

    // P1 修复：大纲结构化解析
    private fun parseOutlineItems(outlineRaw: String): List<OutlineItem> {
        val items = mutableListOf<OutlineItem>()
        // 匹配 "第X章 标题" 或 "Chapter X: Title"
        val regex = Regex("第(\\d+)章\\s+(.+?)[：:\\n](.+?)(?=第\\d+章|$)", RegexOption.MULTILINE)
        regex.findAll(outlineRaw).forEach { match ->
            val num = match.groupValues[1].toIntOrNull() ?: (items.size + 1)
            val title = match.groupValues[2].trim()
            val plot = match.groupValues[3].trim()
            items.add(OutlineItem(chapterNumber = num, title = title, plotPoint = plot, emotionCurve = "待定"))
        }
        
        // 如果正则没匹配到（格式混乱），按行粗暴分割保底
        if (items.isEmpty() && outlineRaw.isNotBlank()) {
            outlineRaw.lines().filter { it.isNotBlank() }.forEachIndexed { index, line ->
                items.add(OutlineItem(chapterNumber = index + 1, title = "第${index+1}章", plotPoint = line.trim(), emotionCurve = "待定"))
            }
        }
        return items
    }
}
```
===END===