package com.minirobot.assistant.agent.ai

import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.PlanStep

/**
 * Robust local on-device AI intent matcher.
 * Provides instantaneous, zero-latency, offline execution of single and multi-step commands.
 */
class LocalRuleBasedAiProvider : AiProvider {

    override val name: String = "Local Offline Engine"

    override suspend fun decideIntent(
        userInput: String,
        availableTools: List<AgentTool>
    ): AgentIntentDecision {
        val query = userInput.trim()
        val lower = query.lowercase().trimEnd('.', '!', '?')

        // 1. Multi-Step Compound: Open <App> and search for <Query>
        // Example: "Open YouTube and search for beginner C# tutorials"
        val compoundOpenSearchRegex = Regex(
            "(?:open|launch|start)\\s+([a-zA-Z0-9\\s]+?)\\s+(?:and|then)\\s+(?:search\\s+(?:for\\s+)?|find\\s+|look\\s+up\\s+)(.+)",
            RegexOption.IGNORE_CASE
        )
        val compoundMatch = compoundOpenSearchRegex.find(query)
        if (compoundMatch != null) {
            val appName = compoundMatch.groupValues[1].trim()
            val searchTerm = compoundMatch.groupValues[2].trim().trimEnd('.', '!', '?')
            val isYouTube = appName.equals("youtube", ignoreCase = true)
            val platform = if (isYouTube) "youtube" else "web"

            val steps = listOf(
                PlanStep(
                    toolName = "open_app",
                    arguments = mapOf("app_name" to appName),
                    description = "Open $appName"
                ),
                PlanStep(
                    toolName = "web_search",
                    arguments = mapOf("query" to searchTerm, "platform" to platform),
                    description = "Search for $searchTerm on $appName"
                )
            )

            return AgentIntentDecision(
                responseText = "Opening $appName and searching for $searchTerm",
                toolCalls = steps.map { ToolCallRequest(it.toolName, it.arguments) },
                planSteps = steps
            )
        }

        // 2. YouTube specific search (e.g. "search youtube for c# tutorials" or "find c# tutorials on youtube")
        val youtubeSearchRegex = Regex("(?:search|find|play)\\s+(?:youtube\\s+for|on\\s+youtube)\\s+(.+)", RegexOption.IGNORE_CASE)
        val youtubeSearchMatch = youtubeSearchRegex.find(query)
        if (youtubeSearchMatch != null) {
            val term = youtubeSearchMatch.groupValues[1].trim().trimEnd('.', '!', '?')
            val step = PlanStep("web_search", mapOf("query" to term, "platform" to "youtube"), "Search YouTube for $term")
            return AgentIntentDecision(
                responseText = "Searching YouTube for $term",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 3. Web search (e.g. "search for c# tutorials" or "google python tutorials" or "search c# tutorials")
        val webSearchRegex = Regex("(?:search\\s+for|google|look\\s+up|search)\\s+(.+)", RegexOption.IGNORE_CASE)
        val webSearchMatch = webSearchRegex.find(query)
        if (webSearchMatch != null && !lower.startsWith("search youtube")) {
            val term = webSearchMatch.groupValues[1].trim().trimEnd('.', '!', '?')
            val step = PlanStep("web_search", mapOf("query" to term, "platform" to "web"), "Search web for $term")
            return AgentIntentDecision(
                responseText = "Searching the web for $term",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 4. Open App (e.g. "open youtube", "open chrome", "launch maps", "start camera")
        val openAppRegex = Regex("^(?:open|launch|start)\\s+([a-zA-Z0-9\\s]+?)[.!?]?$", RegexOption.IGNORE_CASE)
        val openAppMatch = openAppRegex.find(query)
        if (openAppMatch != null) {
            val appName = openAppMatch.groupValues[1].trim()
            if (appName.equals("settings", ignoreCase = true)) {
                val step = PlanStep("launch_settings", emptyMap(), "Open Settings")
                return AgentIntentDecision(
                    responseText = "Opening settings",
                    toolCalls = listOf(ToolCallRequest(step.toolName)),
                    planSteps = listOf(step)
                )
            }
            val step = PlanStep("open_app", mapOf("app_name" to appName), "Open $appName")
            return AgentIntentDecision(
                responseText = "Opening $appName",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 5. Back navigation (e.g. "go back", "back", "return")
        if (lower == "go back" || lower == "back" || lower == "navigate back" || lower == "return") {
            val step = PlanStep("go_back", emptyMap(), "Go back")
            return AgentIntentDecision(
                responseText = "Going back",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 6. Home navigation (e.g. "go home", "home", "home screen")
        if (lower == "go home" || lower == "home" || lower == "go to home" || lower == "home screen") {
            val step = PlanStep("go_home", emptyMap(), "Go to home screen")
            return AgentIntentDecision(
                responseText = "Returning to home screen",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 7. Scrolling (e.g. "scroll down", "scroll up", "scroll")
        if (lower.contains("scroll")) {
            val direction = if (lower.contains("up")) "up" else "down"
            val step = PlanStep("scroll_screen", mapOf("direction" to direction), "Scroll $direction")
            return AgentIntentDecision(
                responseText = "Scrolling $direction",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 8. Click / Tap (e.g. "click subscribe", "tap next", "press ok")
        val tapRegex = Regex("(?:click|tap|press)\\s+(?:on\\s+)?(.+)", RegexOption.IGNORE_CASE)
        val tapMatch = tapRegex.find(query)
        if (tapMatch != null) {
            val target = tapMatch.groupValues[1].trim()
            val step = PlanStep("tap_accessible_element", mapOf("text" to target), "Tap $target")
            return AgentIntentDecision(
                responseText = "Tapping $target",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 9. Type text (e.g. "type hello world")
        val typeRegex = Regex("(?:type|enter|input)\\s+(.+)", RegexOption.IGNORE_CASE)
        val typeMatch = typeRegex.find(query)
        if (typeMatch != null) {
            val text = typeMatch.groupValues[1].trim()
            val step = PlanStep("type_text", mapOf("text" to text), "Type $text")
            return AgentIntentDecision(
                responseText = "Typing $text",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 10. Battery status (e.g. "battery", "battery percentage", "check battery")
        if (lower.contains("battery") || lower.contains("charge")) {
            val step = PlanStep("battery_status", emptyMap(), "Check battery status")
            return AgentIntentDecision(
                responseText = "Checking battery status",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 11. Device Time (e.g. "what time is it", "time", "date")
        if (lower.contains("time") || lower.contains("date") || lower.contains("clock")) {
            val step = PlanStep("device_time", emptyMap(), "Check time")
            return AgentIntentDecision(
                responseText = "Checking the time",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 12. Open Settings
        if (lower.contains("settings")) {
            val step = PlanStep("launch_settings", emptyMap(), "Open Settings")
            return AgentIntentDecision(
                responseText = "Opening settings",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 13. Sensitive operation demonstration (e.g. "send message to Alice")
        if (lower.startsWith("send message") || lower.startsWith("text ")) {
            val step = PlanStep(
                "send_message",
                mapOf("recipient" to "Contact", "message" to query),
                "Send message to contact"
            )
            return AgentIntentDecision(
                responseText = "Preparing to send message.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step),
                requiresConfirmation = true,
                confirmationPrompt = "Do you want me to send this message?"
            )
        }

        // 13. Conversational fallbacks
        val conversationalReply = when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hello! I am your Mini Robot Assistant. How can I help you today?"
            lower.contains("who are you") || lower.contains("what are you") ->
                "I'm your cute floating robot companion! I can open apps, search the web, navigate, and help you control your phone."
            lower.contains("how are you") ->
                "I'm running at peak efficiency and feeling cheerful! Ready for your commands."
            lower.contains("joke") ->
                "Why was the robot tired after work? Because it had a hard drive!"
            else ->
                "I heard '$query'. You can ask me to open apps like YouTube or Chrome, search the web, navigate back, go home, or check the battery."
        }

        return AgentIntentDecision(
            responseText = conversationalReply,
            toolCalls = emptyList()
        )
    }
}
