package com.roubao.autopilot.skills

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.roubao.autopilot.controller.AppScanner
import com.roubao.autopilot.tools.ToolManager
import com.roubao.autopilot.vlm.VLMClient
import org.json.JSONObject

/**
 * Skill 管理器
 *
 * 作为 Skill 层的统一入口，负责：
 * - 初始化和加载 Skills
 * - 意图识别和 Skill 匹配（使用 LLM 语义理解）
 * - 基于已安装 App 选择最佳执行方案
 * - Skill 执行调度
 */
class SkillManager private constructor(
    private val context: Context,
    private val toolManager: ToolManager,
    private val appScanner: AppScanner
) {

    private val registry: SkillRegistry = SkillRegistry.init(context, appScanner)

    // VLM 客户端（用于意图匹配）
    private var vlmClient: VLMClient? = null

    /**
     * 设置 VLM 客户端（用于 LLM 意图匹配）
     */
    fun setVLMClient(client: VLMClient) {
        this.vlmClient = client
    }

    /**
     * 初始化：加载 Skills 配置
     */
    fun initialize() {
        val loadedCount = registry.loadFromAssets("skills.json")
        println("[SkillManager] 已加载 $loadedCount 个 Skills")
    }

    /**
     * 刷新已安装应用列表
     */
    fun refreshInstalledApps() {
        registry.refreshInstalledApps()
    }

    /**
     * 处理用户意图（新方法：返回最佳可用应用）
     *
     * @param query 用户输入
     * @return 可用应用匹配结果，如果没有则返回 null
     */
    fun matchAvailableApp(query: String): AvailableAppMatch? {
        return registry.getBestAvailableApp(query, minScore = 0.3f)
    }

    /**
     * 获取所有匹配的可用应用
     */
    fun matchAllAvailableApps(query: String): List<AvailableAppMatch> {
        return registry.matchAvailableApps(query, minScore = 0.2f)
    }

    /**
     * 使用 LLM 进行意图匹配（异步方法）
     *
     * @param query 用户输入
     * @return 匹配的 Skill ID，如果没有匹配返回 null
     */
    suspend fun matchIntentWithLLM(query: String): LLMIntentMatch? {
        val client = vlmClient ?: return null

        // 构建 Skills 列表描述
        val skillsInfo = buildString {
            append(context.getString(R.string.skill_available_list) + "：\n")
            for (skill in registry.getAll()) {
                val config = skill.config
                // 只展示有已安装应用的 Skill
                val installedApps = config.relatedApps.filter { registry.isAppInstalled(it.packageName) }
                if (installedApps.isNotEmpty()) {
                    append("- ID: ${config.id}\n")
                    append("  ${context.getString(R.string.skill_name_label)}: ${config.name}\n")
                    append("  ${context.getString(R.string.skill_desc_label)}: ${config.description}\n")
                    append("  ${context.getString(R.string.skill_keyword_label)}: ${config.keywords.joinToString(", ")}\n")
                    append("  ${context.getString(R.string.skill_apps_label)}: ${installedApps.joinToString(", ") { it.name }}\n\n")
                }
            }
        }

        val prompt = """${context.getString(R.string.skill_intent_assistant)}
 
$skillsInfo
 
用户输入: "$query"
 
${context.getString(R.string.skill_json_format)}
{
  "skill_id": "匹配的技能ID，如果没有匹配返回 null",
  "confidence": 0.0-1.0 的置信度,
  "reasoning": "${context.getString(R.string.skill_json_reasoning)}"
}
 
${context.getString(R.string.skill_json_note)}
${context.getString(R.string.skill_json_note1)}
${context.getString(R.string.skill_json_note2)}
${context.getString(R.string.skill_json_note3)}
${context.getString(R.string.skill_json_example1)}
${context.getString(R.string.skill_json_example2)}"""

        return try {
            val result = client.predict(prompt)
            result.getOrNull()?.let { response ->
                parseIntentResponse(response)
            }
        } catch (e: Exception) {
            println("[SkillManager] LLM 意图匹配失败: ${e.message}")
            null
        }
    }

    /**
     * 解析 LLM 返回的意图匹配结果
     */
    private fun parseIntentResponse(response: String): LLMIntentMatch? {
        return try {
            // 提取 JSON（可能被 markdown 包裹）
            val jsonStr = response
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(jsonStr)
            val skillId = json.optString("skill_id", null)?.takeIf { it != "null" && it.isNotEmpty() }
            val confidence = json.optDouble("confidence", 0.0).toFloat()
            val reasoning = json.optString("reasoning", "")

            if (skillId != null) {
                LLMIntentMatch(
                    skillId = skillId,
                    confidence = confidence,
                    reasoning = reasoning
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("[SkillManager] 解析意图响应失败: ${e.message}")
            null
        }
    }

    /**
     * 使用 LLM 匹配意图并返回可用应用（组合方法）
     */
    suspend fun matchAvailableAppWithLLM(query: String): AvailableAppMatch? {
        // 先尝试 LLM 匹配
        val llmMatch = matchIntentWithLLM(query)

        if (llmMatch != null && llmMatch.confidence >= 0.5f) {
            println("[SkillManager] LLM 匹配: ${llmMatch.skillId} (置信度: ${llmMatch.confidence})")
            println("[SkillManager] 理由: ${llmMatch.reasoning}")

            // 获取对应的 Skill 和已安装应用
            val skill = registry.get(llmMatch.skillId)
            if (skill != null) {
                println("[SkillManager] 找到 Skill: ${skill.config.name}")
                println("[SkillManager] 关联应用: ${skill.config.relatedApps.map { "${it.name}(${it.packageName})" }}")

                // 检查每个应用的安装状态
                for (app in skill.config.relatedApps) {
                    val installed = registry.isAppInstalled(app.packageName)
                    println("[SkillManager] ${app.name}(${app.packageName}): ${if (installed) "已安装" else "未安装"}")
                }

                val availableApp = skill.config.relatedApps
                    .filter { registry.isAppInstalled(it.packageName) }
                    .maxByOrNull { it.priority }

                if (availableApp != null) {
                    println("[SkillManager] 选中应用: ${availableApp.name}")
                    val params = skill.extractParams(query)
                    return AvailableAppMatch(
                        skill = skill,
                        app = availableApp,
                        params = params,
                        score = llmMatch.confidence
                    )
                } else {
                    println("[SkillManager] 没有可用应用（都未安装）")
                }
            } else {
                println("[SkillManager] 未找到 Skill: ${llmMatch.skillId}")
            }
        }

        // 如果 LLM 匹配失败，回退到关键词匹配
        println("[SkillManager] LLM 未匹配或无可用应用，回退到关键词匹配")
        return matchAvailableApp(query)
    }

    /**
     * 生成给 Agent 的上下文提示（使用 LLM 匹配）
     */
    suspend fun generateAgentContextWithLLM(query: String): String {
        // 使用 LLM 匹配
        val match = matchAvailableAppWithLLM(query)

        if (match == null) {
            return context.getString(R.string.skill_no_match)
        }

        return buildString {
            val config = match.skill.config
            val app = match.app

            append(context.getString(R.string.skill_matched_intent) + "\n\n")
            append("【${config.name}】(${context.getString(R.string.skill_confidence_label)}: ${(match.score * 100).toInt()}%)\n")
            append("${context.getString(R.string.skill_desc_label)}: ${config.description}\n\n")
 
            // 显示提示词约束（如小红书100字限制）
            if (!config.promptHint.isNullOrBlank()) {
                append("⚠️ ${context.getString(R.string.skill_important_hint)}: ${config.promptHint}\n\n")
            }

            val typeLabel = when (app.type) {
                ExecutionType.DELEGATION -> context.getString(R.string.skill_type_delegation)
                ExecutionType.GUI_AUTOMATION -> context.getString(R.string.skill_type_automation)
            }

            append("${context.getString(R.string.skill_apps_label)}: ${app.name} $typeLabel\n")
 
            if (app.type == ExecutionType.DELEGATION && app.deepLink != null) {
                append("DeepLink: ${app.deepLink}\n")
            }
 
            if (!app.steps.isNullOrEmpty()) {
                append("${context.getString(R.string.history_steps)}: ${app.steps.joinToString(" → ")}\n")
            }

            app.description?.let {
                append("说明: $it\n")
            }

            append("\n${context.getString(R.string.capabilities_responsibility)}：")
            if (app.type == ExecutionType.DELEGATION) {
                append(context.getString(R.string.skill_suggest_deeplink, app.name))
            } else {
                append(context.getString(R.string.skill_suggest_automation, app.name))
            }
        }
    }

    /**
     * 执行 Skill（核心执行方法）
     *
     * @param match 可用应用匹配结果
     * @return 执行结果
     */
    suspend fun execute(match: AvailableAppMatch): SkillResult {
        val skill = match.skill
        val app = match.app
        val params = match.params

        println("[SkillManager] 执行: ${skill.config.name} -> ${app.name} (${app.type})")

        return when (app.type) {
            ExecutionType.DELEGATION -> {
                // 委托模式：通过 DeepLink 打开
                executeDelegation(skill, app, params)
            }
            ExecutionType.GUI_AUTOMATION -> {
                // GUI 自动化模式：返回执行计划
                executeAutomation(skill, app, params)
            }
        }
    }

    /**
     * 执行委托（DeepLink）
     */
    private fun executeDelegation(
        skill: Skill,
        app: RelatedApp,
        params: Map<String, Any?>
    ): SkillResult {
        val deepLink = skill.generateDeepLink(app, params)

        if (deepLink.isEmpty()) {
            return SkillResult.Failed(
                error = context.getString(R.string.skill_error_no_deeplink),
                suggestion = context.getString(R.string.skill_suggest_gui)
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // 明确指定目标包名，避免系统选择其他能响应此 scheme 的应用
                setPackage(app.packageName)
            }
            context.startActivity(intent)

            SkillResult.Delegated(
                app = app,
                deepLink = deepLink,
                message = context.getString(R.string.skill_delegation_opened, app.name)
            )
        } catch (e: Exception) {
            // 如果指定包名失败，尝试不指定包名的方式
            println("[SkillManager] 指定包名打开失败，尝试通用方式: ${e.message}")
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)

                SkillResult.Delegated(
                    app = app,
                    deepLink = deepLink,
                    message = context.getString(R.string.skill_delegation_opened_fallback, app.name)
                )
            } catch (e2: Exception) {
                SkillResult.Failed(
                    error = context.getString(R.string.skill_error_open_app, app.name, e2.message ?: ""),
                    suggestion = context.getString(R.string.skill_deeplink_fallback_suggestion)
                )
            }
        }
    }

    /**
     * 执行 GUI 自动化（返回执行计划给 Agent）
     */
    private fun executeAutomation(
        skill: Skill,
        app: RelatedApp,
        params: Map<String, Any?>
    ): SkillResult {
        val plan = ExecutionPlan(
            skillId = skill.config.id,
            skillName = skill.config.name,
            app = app,
            params = params,
            isInstalled = true,
            promptHint = skill.config.promptHint
        )

        return SkillResult.NeedAutomation(
            plan = plan,
            message = context.getString(R.string.skill_automation_needed, app.name)
        )
    }

    /**
     * 判断是否应该使用快速路径
     *
     * 条件：
     * 1. 高置信度匹配 (score >= 0.8)
     * 2. 最佳应用是委托类型 (delegation)
     * 3. 应用已安装
     */
    fun shouldUseFastPath(query: String): AvailableAppMatch? {
        val match = matchAvailableApp(query) ?: return null

        // 只有委托类型且高置信度才走快速路径
        if (match.app.type == ExecutionType.DELEGATION && match.score >= 0.8f) {
            return match
        }

        return null
    }

    /**
     * 生成给 Agent 的上下文提示
     *
     * 包含：匹配的意图、可用应用列表、推荐操作步骤
     */
    fun generateAgentContext(query: String): String {
        val matches = matchAllAvailableApps(query)

        if (matches.isEmpty()) {
            return context.getString(R.string.skill_no_match)
        }

        return buildString {
            append(context.getString(R.string.skill_available_schemes) + "\n\n")

            // 按 Skill 分组
            val groupedBySkill = matches.groupBy { it.skill.config.id }

            for ((_, skillMatches) in groupedBySkill) {
                val firstMatch = skillMatches.first()
                val config = firstMatch.skill.config

                append("【${config.name}】(${context.getString(R.string.skill_confidence_label)}: ${(firstMatch.score * 100).toInt()}%)\n")

                for ((index, match) in skillMatches.withIndex()) {
                    val app = match.app
                    val typeLabel = when (app.type) {
                        ExecutionType.DELEGATION -> context.getString(R.string.skill_type_delegation)
                        ExecutionType.GUI_AUTOMATION -> context.getString(R.string.skill_type_automation)
                    }
 
                    append("  ${index + 1}. ${app.name} $typeLabel (${context.getString(R.string.skill_priority_label)}: ${app.priority})\n")

                    if (app.type == ExecutionType.DELEGATION && app.deepLink != null) {
                        append("     DeepLink: ${app.deepLink}\n")
                    }

                    if (!app.steps.isNullOrEmpty()) {
                        append("     ${context.getString(R.string.history_steps)}: ${app.steps.joinToString(" → ")}\n")
                    }

                    app.description?.let {
                        append("     ${context.getString(R.string.skill_desc_label)}: $it\n")
                    }
                }
                append("\n")
            }
 
            append(context.getString(R.string.skill_suggest_general))
        }
    }

    /**
     * 获取 Skill 信息
     */
    fun getSkillInfo(skillId: String): SkillConfig? {
        return registry.get(skillId)?.config
    }

    /**
     * 获取所有 Skills 描述（给 LLM）
     */
    fun getSkillsDescription(): String {
        return registry.getSkillsDescription()
    }

    /**
     * 获取所有 Skills
     */
    fun getAllSkills(): List<Skill> {
        return registry.getAll()
    }

    /**
     * 按分类获取 Skills
     */
    fun getSkillsByCategory(category: String): List<Skill> {
        return registry.getByCategory(category)
    }

    /**
     * 检查意图是否有可用应用
     */
    fun hasAvailableApp(query: String): Boolean {
        return matchAvailableApp(query) != null
    }

    /**
     * 获取意图的所有关联应用（不管是否安装）
     */
    fun getAllRelatedApps(query: String): List<RelatedApp> {
        val skillMatch = registry.matchBest(query) ?: return emptyList()
        return skillMatch.skill.config.relatedApps
    }

    /**
     * 获取缺失的应用推荐（用户没装但可以装的）
     */
    fun getMissingAppSuggestions(query: String): List<RelatedApp> {
        val skillMatch = registry.matchBest(query) ?: return emptyList()
        return skillMatch.skill.config.relatedApps
            .filter { !registry.isAppInstalled(it.packageName) }
            .sortedByDescending { it.priority }
    }

    companion object {
        @Volatile
        private var instance: SkillManager? = null

        fun init(context: Context, toolManager: ToolManager, appScanner: AppScanner): SkillManager {
            return instance ?: synchronized(this) {
                instance ?: SkillManager(context.applicationContext, toolManager, appScanner).also {
                    it.initialize()
                    instance = it
                }
            }
        }

        fun getInstance(): SkillManager {
            return instance ?: throw IllegalStateException("SkillManager 未初始化，请先调用 init()")
        }

        fun isInitialized(): Boolean = instance != null
    }
}
