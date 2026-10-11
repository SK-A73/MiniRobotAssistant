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

        // 1. Flashlight / Torch commands
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val isOff = lower.contains("off") || lower.contains("stop") || lower.contains("disable")
            if (isOff) {
                val step = PlanStep(
                    toolName = "flashlight",
                    arguments = mapOf("state" to false, "duration_seconds" to 0),
                    description = "Turn off flashlight"
                )
                return AgentIntentDecision(
                    responseText = "Turning off flashlight.",
                    toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                    planSteps = listOf(step)
                )
            } else {
                // Check for duration (e.g., "for 2 minutes", "for 30 seconds")
                val durationRegex = Regex("for\\s+(\\d+)\\s*(minute|min|second|sec)", RegexOption.IGNORE_CASE)
                val durationMatch = durationRegex.find(lower)
                val durationSeconds = if (durationMatch != null) {
                    val amount = durationMatch.groupValues[1].toIntOrNull() ?: 0
                    val unit = durationMatch.groupValues[2].lowercase()
                    if (unit.startsWith("min")) amount * 60 else amount
                } else 0

                val desc = if (durationSeconds > 0) {
                    "Turn on flashlight for $durationSeconds seconds"
                } else {
                    "Turn on flashlight"
                }

                val step = PlanStep(
                    toolName = "flashlight",
                    arguments = mapOf("state" to true, "duration_seconds" to durationSeconds),
                    description = desc
                )
                val speech = if (durationSeconds > 0) {
                    val mins = durationSeconds / 60
                    if (mins > 0 && durationSeconds % 60 == 0) {
                        "Turning on flashlight for $mins " + if (mins == 1) "minute." else "minutes."
                    } else {
                        "Turning on flashlight for $durationSeconds seconds."
                    }
                } else {
                    "Turning on flashlight."
                }

                return AgentIntentDecision(
                    responseText = speech,
                    toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                    planSteps = listOf(step)
                )
            }
        }

        // 2. Alarm commands (e.g., "set alarm for 7:30 am", "set an alarm at 8:00 pm", "wake me up at 6 am")
        val alarmRegex = Regex(
            "(?:set\\s+(?:an?\\s+)?alarm\\s+(?:for|at)|wake\\s+me\\s+up\\s+at|alarm\\s+(?:for|at))\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?(?:\\s+(?:with\\s+label|labeled|called|for)\\s+(.+))?",
            RegexOption.IGNORE_CASE
        )
        val alarmMatch = alarmRegex.find(query)
        if (alarmMatch != null) {
            var hour = alarmMatch.groupValues[1].toIntOrNull() ?: 7
            val minutes = alarmMatch.groupValues[2].toIntOrNull() ?: 0
            val amPm = alarmMatch.groupValues[3].lowercase()
            val label = alarmMatch.groupValues[4].trim().ifBlank { "Alarm" }

            if (amPm == "pm" && hour < 12) hour += 12
            else if (amPm == "am" && hour == 12) hour = 0

            val step = PlanStep(
                toolName = "set_alarm",
                arguments = mapOf("hour" to hour, "minutes" to minutes, "label" to label),
                description = "Set alarm for $hour:${String.format("%02d", minutes)}"
            )
            return AgentIntentDecision(
                responseText = "Setting alarm for $hour:${String.format("%02d", minutes)}.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 3. Timer commands (e.g., "set a timer for 5 minutes", "timer for 30 seconds", "set 2 minute timer")
        if (lower.contains("timer")) {
            val timerRegex = Regex(
                "(?:set\\s+(?:a\\s+)?)?timer\\s+(?:for\\s+)?(?:(\\d+)\\s*(?:minute|min|m)s?\\s*)?(?:(\\d+)\\s*(?:second|sec|s)s?)?(?:\\s+(?:labeled|called|for)\\s+(.+))?",
                RegexOption.IGNORE_CASE
            )
            val timerMatch = timerRegex.find(query)
            var totalSeconds = 60
            var label = "Timer"
            if (timerMatch != null) {
                val mins = timerMatch.groupValues[1].toIntOrNull() ?: 0
                val secs = timerMatch.groupValues[2].toIntOrNull() ?: 0
                val lbl = timerMatch.groupValues[3].trim()
                if (mins > 0 || secs > 0) {
                    totalSeconds = mins * 60 + secs
                }
                if (lbl.isNotBlank()) label = lbl
            } else {
                // Check simple "5 minute timer"
                val simpleMin = Regex("(\\d+)\\s*(?:minute|min)\\s+timer", RegexOption.IGNORE_CASE).find(query)
                if (simpleMin != null) {
                    totalSeconds = (simpleMin.groupValues[1].toIntOrNull() ?: 1) * 60
                }
            }

            val step = PlanStep(
                toolName = "set_timer",
                arguments = mapOf("duration_seconds" to totalSeconds, "label" to label),
                description = "Set timer for $totalSeconds seconds"
            )
            val durationText = if (totalSeconds >= 60 && totalSeconds % 60 == 0) {
                val m = totalSeconds / 60
                "$m " + if (m == 1) "minute" else "minutes"
            } else {
                "$totalSeconds seconds"
            }
            return AgentIntentDecision(
                responseText = "Setting timer for $durationText.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 4. Phone Call commands (e.g., "call Alice", "make a phone call to Bob", "call 9876543210")
        val callRegex = Regex(
            "^(?:make\\s+(?:a\\s+)?(?:phone\\s+)?call\\s+(?:to\\s+)?|call\\s+|phone\\s+)(.+)$",
            RegexOption.IGNORE_CASE
        )
        val callMatch = callRegex.find(query)
        if (callMatch != null && !lower.contains("message") && !lower.contains("text")) {
            val target = callMatch.groupValues[1].trim().trimEnd('.', '!', '?')
            val isDigitsOnly = target.all { it.isDigit() || it == '+' || it == '-' || it == ' ' } && target.any { it.isDigit() }
            val steps = if (isDigitsOnly) {
                listOf(
                    PlanStep(
                        toolName = "make_phone_call",
                        arguments = mapOf("phone_number" to target, "contact_name" to target),
                        description = "Call $target"
                    )
                )
            } else {
                listOf(
                    PlanStep(
                        toolName = "lookup_contact",
                        arguments = mapOf("name" to target),
                        description = "Look up contact $target"
                    ),
                    PlanStep(
                        toolName = "make_phone_call",
                        arguments = mapOf("contact_name" to target),
                        description = "Call $target"
                    )
                )
            }
            return AgentIntentDecision(
                responseText = "Preparing to call $target.",
                toolCalls = steps.map { ToolCallRequest(it.toolName, it.arguments) },
                planSteps = steps,
                requiresConfirmation = true,
                confirmationPrompt = "Do you want me to call $target?"
            )
        }

        // 5. Messaging / SMS commands (e.g. "send an apology message to Mom", "send message to John saying I'll be late")
        val msgRegex = Regex(
            "^(?:send\\s+(?:an?\\s+)?(?:apology\\s+)?message\\s+to|text|send\\s+(?:a\\s+)?text\\s+to)\\s+([a-zA-Z0-9\\s]+?)(?:\\s+(?:saying|with|that|:)\\s+(.+)|$)",
            RegexOption.IGNORE_CASE
        )
        val msgMatch = msgRegex.find(query)
        if (msgMatch != null || lower.startsWith("send message") || lower.startsWith("text ")) {
            val recipient = msgMatch?.groupValues?.get(1)?.trim() ?: "Contact"
            val explicitText = msgMatch?.groupValues?.getOrNull(2)?.trim() ?: ""

            // Contextual message synthesis for apologies or greetings
            val messageBody = when {
                explicitText.isNotBlank() -> explicitText
                lower.contains("apology") || lower.contains("apologize") ->
                    "I am truly sorry about earlier. Please forgive me!"
                lower.contains("greeting") || lower.contains("say hi") ->
                    "Hi there! Hope you are having a wonderful day!"
                else -> "Hello!"
            }

            val steps = listOf(
                PlanStep(
                    toolName = "lookup_contact",
                    arguments = mapOf("name" to recipient),
                    description = "Look up contact $recipient"
                ),
                PlanStep(
                    toolName = "send_message",
                    arguments = mapOf("recipient" to recipient, "message" to messageBody),
                    description = "Send message to $recipient"
                )
            )

            return AgentIntentDecision(
                responseText = "Preparing message for $recipient: \"$messageBody\"",
                toolCalls = steps.map { ToolCallRequest(it.toolName, it.arguments) },
                planSteps = steps,
                requiresConfirmation = true,
                confirmationPrompt = "Send message to $recipient: \"$messageBody\"?"
            )
        }

        // 6. Specific Android Settings (Wi-Fi, Bluetooth, Display, Sound, Apps, Battery, Accessibility)
        val settingsDetailRegex = Regex(
            "(?:open|launch)\\s+([a-zA-Z\\-]+)\\s+settings",
            RegexOption.IGNORE_CASE
        )
        val settingsDetailMatch = settingsDetailRegex.find(query)
        if (settingsDetailMatch != null) {
            val type = settingsDetailMatch.groupValues[1].lowercase().trim()
            val step = PlanStep(
                toolName = "open_settings_detail",
                arguments = mapOf("setting_type" to type),
                description = "Open $type settings"
            )
            return AgentIntentDecision(
                responseText = "Opening $type settings.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 7. Multi-Step Compound: Open <App> and search for <Query>
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
                responseText = "Opening $appName and searching for $searchTerm.",
                toolCalls = steps.map { ToolCallRequest(it.toolName, it.arguments) },
                planSteps = steps
            )
        }

        // 8. YouTube specific search
        val youtubeSearchRegex = Regex(
            "(?:search|find|play)\\s+(?:youtube\\s+for|on\\s+youtube)\\s+(.+)",
            RegexOption.IGNORE_CASE
        )
        val youtubeSearchMatch = youtubeSearchRegex.find(query)
        if (youtubeSearchMatch != null) {
            val term = youtubeSearchMatch.groupValues[1].trim().trimEnd('.', '!', '?')
            val step = PlanStep(
                toolName = "web_search",
                arguments = mapOf("query" to term, "platform" to "youtube"),
                description = "Search YouTube for $term"
            )
            return AgentIntentDecision(
                responseText = "Searching YouTube for $term.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 9. Web search
        val webSearchRegex = Regex("(?:search\\s+for|google|look\\s+up|search)\\s+(.+)", RegexOption.IGNORE_CASE)
        val webSearchMatch = webSearchRegex.find(query)
        if (webSearchMatch != null && !lower.startsWith("search youtube")) {
            val term = webSearchMatch.groupValues[1].trim().trimEnd('.', '!', '?')
            val step = PlanStep(
                toolName = "web_search",
                arguments = mapOf("query" to term, "platform" to "web"),
                description = "Search web for $term"
            )
            return AgentIntentDecision(
                responseText = "Searching the web for $term.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 10. Open App (e.g. "open youtube", "open chrome", "launch maps", "start camera")
        val openAppRegex = Regex("^(?:open|launch|start)\\s+([a-zA-Z0-9\\s]+?)[.!?]?$", RegexOption.IGNORE_CASE)
        val openAppMatch = openAppRegex.find(query)
        if (openAppMatch != null) {
            val appName = openAppMatch.groupValues[1].trim()
            if (appName.equals("settings", ignoreCase = true)) {
                val step = PlanStep("launch_settings", emptyMap(), "Open Settings")
                return AgentIntentDecision(
                    responseText = "Opening settings.",
                    toolCalls = listOf(ToolCallRequest(step.toolName)),
                    planSteps = listOf(step)
                )
            }
            val step = PlanStep(
                toolName = "open_app",
                arguments = mapOf("app_name" to appName),
                description = "Open $appName"
            )
            return AgentIntentDecision(
                responseText = "Opening $appName.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 11. Navigation: Go back
        if (lower == "go back" || lower == "back" || lower == "navigate back" || lower == "return") {
            val step = PlanStep("go_back", emptyMap(), "Go back")
            return AgentIntentDecision(
                responseText = "Going back.",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 12. Navigation: Go home
        if (lower == "go home" || lower == "home" || lower == "go to home" || lower == "home screen") {
            val step = PlanStep("go_home", emptyMap(), "Go to home screen")
            return AgentIntentDecision(
                responseText = "Returning to home screen.",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 13. Scrolling
        if (lower.contains("scroll")) {
            val direction = if (lower.contains("up")) "up" else "down"
            val step = PlanStep("scroll_screen", mapOf("direction" to direction), "Scroll $direction")
            return AgentIntentDecision(
                responseText = "Scrolling $direction.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 14. Click / Tap
        val tapRegex = Regex("(?:click|tap|press)\\s+(?:on\\s+)?(.+)", RegexOption.IGNORE_CASE)
        val tapMatch = tapRegex.find(query)
        if (tapMatch != null) {
            val target = tapMatch.groupValues[1].trim()
            val step = PlanStep("tap_accessible_element", mapOf("text" to target), "Tap $target")
            return AgentIntentDecision(
                responseText = "Tapping $target.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 15. Type text
        val typeRegex = Regex("(?:type|enter|input)\\s+(.+)", RegexOption.IGNORE_CASE)
        val typeMatch = typeRegex.find(query)
        if (typeMatch != null) {
            val text = typeMatch.groupValues[1].trim()
            val step = PlanStep("type_text", mapOf("text" to text), "Type $text")
            return AgentIntentDecision(
                responseText = "Typing $text.",
                toolCalls = listOf(ToolCallRequest(step.toolName, step.arguments)),
                planSteps = listOf(step)
            )
        }

        // 16. Battery status
        if (lower.contains("battery") || lower.contains("charge")) {
            val step = PlanStep("battery_status", emptyMap(), "Check battery status")
            return AgentIntentDecision(
                responseText = "Checking battery status.",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 17. Device Time
        if (lower.contains("time") || lower.contains("date") || lower.contains("clock")) {
            val step = PlanStep("device_time", emptyMap(), "Check time")
            return AgentIntentDecision(
                responseText = "Checking the time.",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 18. General Settings
        if (lower.contains("settings")) {
            val step = PlanStep("launch_settings", emptyMap(), "Open Settings")
            return AgentIntentDecision(
                responseText = "Opening settings.",
                toolCalls = listOf(ToolCallRequest(step.toolName)),
                planSteps = listOf(step)
            )
        }

        // 19. Conversational fallbacks
        val conversationalReply = when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hello! I am your Mini Robot Assistant. How can I help you today?"
            lower.contains("who are you") || lower.contains("what are you") ->
                "I'm your cute floating robot companion! I can open apps, search the web, toggle flashlight, set alarms and timers, and assist with calls and messages."
            lower.contains("how are you") ->
                "I'm running at peak efficiency and feeling cheerful! Ready for your commands."
            lower.contains("joke") ->
                "Why was the robot tired after work? Because it had a hard drive!"
            else ->
                "I heard '$query'. You can ask me to control flashlight, set alarms, call contacts, send messages, open apps, or search the web."
        }

        return AgentIntentDecision(
            responseText = conversationalReply,
            toolCalls = emptyList()
        )
    }
}
