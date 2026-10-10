package com.minirobot.assistant.agent

import com.minirobot.assistant.agent.ai.AiProviderManager
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotMood
import com.minirobot.assistant.voice.TextToSpeechManager

/**
 * Coordinates end-to-end task execution.
 * Delegates to the modular AiAgent pipeline.
 */
class TaskAgent(
    val toolRegistry: ToolRegistry,
    val aiProviderManager: AiProviderManager,
    private val ttsManager: TextToSpeechManager,
    onStateChange: (RobotActivityState) -> Unit,
    onMoodChange: (RobotMood) -> Unit,
    onSpeechBubble: (String) -> Unit,
    onPromptConfirmation: (
        title: String,
        description: String,
        onConfirm: () -> Unit,
        onReject: () -> Unit
    ) -> Unit
) {

    val aiAgent = AiAgent(
        toolRegistry = toolRegistry,
        aiProviderManager = aiProviderManager,
        ttsManager = ttsManager,
        onStateChange = onStateChange,
        onMoodChange = onMoodChange,
        onStatusUpdate = { _, _ -> },
        onSpeechBubble = onSpeechBubble,
        onPromptConfirmation = onPromptConfirmation
    )

    suspend fun processUserCommand(userInput: String): String {
        return aiAgent.processUserCommand(userInput)
    }
}

