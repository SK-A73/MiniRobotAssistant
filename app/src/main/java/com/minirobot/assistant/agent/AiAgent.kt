package com.minirobot.assistant.agent

import android.util.Log
import com.minirobot.assistant.agent.ai.AiProviderManager
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotMood
import com.minirobot.assistant.voice.TextToSpeechManager

/**
 * Modular AI Agent coordinating the end-to-end task pipeline:
 * Voice Input -> Speech-to-Text -> AI Planner -> Tool Executor -> Verify Result -> Voice Response.
 */
class AiAgent(
    val toolRegistry: ToolRegistry,
    val aiProviderManager: AiProviderManager,
    private val ttsManager: TextToSpeechManager,
    onStateChange: (RobotActivityState) -> Unit,
    onMoodChange: (RobotMood) -> Unit,
    onStatusUpdate: (TaskExecutionState, String) -> Unit,
    onSpeechBubble: (String) -> Unit,
    private val onPromptConfirmation: (
        title: String,
        description: String,
        onConfirm: () -> Unit,
        onReject: () -> Unit
    ) -> Unit
) {

    val robotStateController = RobotStateController(
        onStateChange = onStateChange,
        onMoodChange = onMoodChange,
        onStatusUpdate = onStatusUpdate,
        onSpeechBubble = onSpeechBubble
    )

    val taskPlanner = TaskPlanner(
        aiProviderManager = aiProviderManager,
        toolRegistry = toolRegistry
    )

    val toolExecutor = ToolExecutor(
        toolRegistry = toolRegistry,
        robotStateController = robotStateController
    )

    /**
     * Executes a full natural language command from start to finish.
     */
    suspend fun processUserCommand(userInput: String): String {
        Log.i(TAG, "AiAgent received user command: '$userInput'")

        // 1. Understanding phase
        robotStateController.transitionTo(
            TaskExecutionState.UNDERSTANDING,
            "Understanding: \"$userInput\""
        )

        // 2. Planning phase
        robotStateController.transitionTo(
            TaskExecutionState.PLANNING,
            "Planning task execution..."
        )
        val plan = taskPlanner.createPlan(userInput)

        // 3. Execution & Verification phase
        val result = toolExecutor.executePlan(plan) { title, desc, onConfirm, onReject ->
            onPromptConfirmation(title, desc, onConfirm, onReject)
        }

        // 4. Voice & Speech Response
        val spokenText = result.speakableResponse
        speakResponse(spokenText)

        return spokenText
    }

    private fun speakResponse(text: String) {
        robotStateController.showBubble(text)
        ttsManager.speak(
            text = text,
            onStart = {
                // Keep robot speaking animation
            },
            onDone = {
                if (robotStateController.currentState != TaskExecutionState.FAILED) {
                    robotStateController.transitionTo(TaskExecutionState.IDLE, "Ready for next command.")
                }
            }
        )
    }

    companion object {
        private const val TAG = "AiAgent"
    }
}
