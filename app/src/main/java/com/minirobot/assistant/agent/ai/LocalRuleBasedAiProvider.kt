package com.minirobot.assistant.agent.ai

import com.minirobot.assistant.agent.AgentTool

/**
 * Robust local on-device AI intent matcher.
 * Provides instantaneous, zero-latency, offline execution of common commands.
 */
class LocalRuleBasedAiProvider : AiProvider {

    override val name: String = "Local Offline Engine"

    override suspend fun decideIntent(
        userInput: String,
        availableTools: List<AgentTool>
    ): AgentIntentDecision {
        val query = userInput.trim()
        val lower = query.lowercase()

        // 1. YouTube specific search (e.g. "search youtube for c# tutorials" or "find c# tutorials on youtube")
        val youtubeSearchRegex = Regex("(?:search|find|play)\\s+(?:youtube\\s+for|on\\s+youtube)\\s+(.+)", RegexOption.IGNORE_CASE)
        val youtubeSearchMatch = youtubeSearchRegex.find(query)
        if (youtubeSearchMatch != null) {
            val term = youtubeSearchMatch.groupValues[1].trim()
            return AgentIntentDecision(
                responseText = "Searching YouTube for $term",
                toolCalls = listOf(ToolCallRequest("web_search", mapOf("query" to term, "platform" to "youtube")))
            )
        }

        // 2. Web search (e.g. "search for c# tutorials" or "google python tutorials" or "search c# tutorials")
        val webSearchRegex = Regex("(?:search\\s+for|google|look\\s+up|search)\\s+(.+)", RegexOption.IGNORE_CASE)
        val webSearchMatch = webSearchRegex.find(query)
        if (webSearchMatch != null && !lower.startsWith("search youtube")) {
            val term = webSearchMatch.groupValues[1].trim()
            return AgentIntentDecision(
                responseText = "Searching the web for $term",
                toolCalls = listOf(ToolCallRequest("web_search", mapOf("query" to term, "platform" to "web")))
            )
        }

        // 3. Open App (e.g. "open youtube", "open chrome", "launch maps", "start camera")
        val openAppRegex = Regex("(?:open|launch|start)\\s+([a-zA-Z0-9\\s]+)", RegexOption.IGNORE_CASE)
        val openAppMatch = openAppRegex.find(query)
        if (openAppMatch != null) {
            val appName = openAppMatch.groupValues[1].trim()
            // Ignore if it's "settings"
            if (appName.equals("settings", ignoreCase = true)) {
                return AgentIntentDecision(
                    responseText = "Opening settings",
                    toolCalls = listOf(ToolCallRequest("launch_settings"))
                )
            }
            return AgentIntentDecision(
                responseText = "Opening $appName",
                toolCalls = listOf(ToolCallRequest("open_app", mapOf("app_name" to appName)))
            )
        }

        // 4. Back navigation (e.g. "go back", "back", "return")
        if (lower == "go back" || lower == "back" || lower == "navigate back" || lower == "return") {
            return AgentIntentDecision(
                responseText = "Going back",
                toolCalls = listOf(ToolCallRequest("go_back"))
            )
        }

        // 5. Home navigation (e.g. "go home", "home", "home screen")
        if (lower == "go home" || lower == "home" || lower == "go to home" || lower == "home screen") {
            return AgentIntentDecision(
                responseText = "Returning to home screen",
                toolCalls = listOf(ToolCallRequest("go_home"))
            )
        }

        // 6. Scrolling (e.g. "scroll down", "scroll up", "scroll")
        if (lower.contains("scroll")) {
            val direction = if (lower.contains("up")) "up" else "down"
            return AgentIntentDecision(
                responseText = "Scrolling $direction",
                toolCalls = listOf(ToolCallRequest("scroll_screen", mapOf("direction" to direction)))
            )
        }

        // 7. Click / Tap (e.g. "click subscribe", "tap next", "press ok")
        val tapRegex = Regex("(?:click|tap|press)\\s+(?:on\\s+)?(.+)", RegexOption.IGNORE_CASE)
        val tapMatch = tapRegex.find(query)
        if (tapMatch != null) {
            val target = tapMatch.groupValues[1].trim()
            return AgentIntentDecision(
                responseText = "Tapping $target",
                toolCalls = listOf(ToolCallRequest("tap_element", mapOf("text" to target)))
            )
        }

        // 8. Type text (e.g. "type hello world")
        val typeRegex = Regex("(?:type|enter|input)\\s+(.+)", RegexOption.IGNORE_CASE)
        val typeMatch = typeRegex.find(query)
        if (typeMatch != null) {
            val text = typeMatch.groupValues[1].trim()
            return AgentIntentDecision(
                responseText = "Typing $text",
                toolCalls = listOf(ToolCallRequest("type_text", mapOf("text" to text)))
            )
        }

        // 9. Battery status (e.g. "battery", "battery percentage", "check battery")
        if (lower.contains("battery") || lower.contains("charge")) {
            return AgentIntentDecision(
                responseText = "Checking battery status",
                toolCalls = listOf(ToolCallRequest("battery_status"))
            )
        }

        // 10. Device Time (e.g. "what time is it", "time", "date")
        if (lower.contains("time") || lower.contains("date") || lower.contains("clock")) {
            return AgentIntentDecision(
                responseText = "Checking the time",
                toolCalls = listOf(ToolCallRequest("device_time"))
            )
        }

        // 11. Open Settings
        if (lower.contains("settings")) {
            return AgentIntentDecision(
                responseText = "Opening settings",
                toolCalls = listOf(ToolCallRequest("launch_settings"))
            )
        }

        // 12. Sensitive operation demonstration (e.g. "send message to Alice")
        if (lower.startsWith("send message") || lower.startsWith("text ")) {
            return AgentIntentDecision(
                responseText = "Preparing to send message.",
                toolCalls = listOf(
                    ToolCallRequest(
                        "send_message",
                        mapOf("recipient" to "Contact", "message" to query)
                    )
                ),
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
