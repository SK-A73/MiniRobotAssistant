package com.minirobot.assistant.agent

import android.util.Log
import com.minirobot.assistant.agent.ai.AiProviderManager
import com.minirobot.assistant.agent.ai.ToolCallRequest
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotMood
import com.minirobot.assistant.voice.TextToSpeechManager

/**
 * Coordinates end-to-end task execution:
 * Intent Resolution -> Safety Checks -> Tool Execution -> Result Speech & State Management.
 */
class TaskAgent(
    private val toolRegistry: ToolRegistry,
    private val aiProviderManager: AiProviderManager,
    private val ttsManager: TextToSpeechManager,
    private val onStateChange: (RobotActivityState) -> Unit,
    private val onMoodChange: (RobotMood) -> Unit,
    private val onSpeechBubble: (String) -> Unit,
    private val onPromptConfirmation: (
        title: String,
        description: String,
        onConfirm: () -> Unit,
        onReject: () -> Unit
    ) -> Unit
) {

    suspend fun processUserCommand(userInput: String): String {
        Log.i(TAG, "Processing user command: $userInput")

        // 1. Enter THINKING state
        onStateChange(RobotActivityState.THINKING)
        onMoodChange(RobotMood.THINKING)

        val provider = aiProviderManager.getActiveProvider()
        val allTools = toolRegistry.getAllTools()

        val decision = try {
            provider.decideIntent(userInput, allTools)
        } catch (e: Exception) {
            Log.e(TAG, "AI Provider failed to resolve intent", e)
            onStateChange(RobotActivityState.ERROR)
            onMoodChange(RobotMood.CONCERNED)
            val errText = "I had trouble understanding that command."
            speakResponse(errText)
            return errText
        }

        // 2. Check for required tool executions
        if (decision.toolCalls.isEmpty()) {
            // Conversational answer
            onStateChange(RobotActivityState.SUCCESS)
            onMoodChange(RobotMood.HAPPY)
            speakResponse(decision.responseText)
            return decision.responseText
        }

        // 3. Check for Sensitive Operations (Phase 9 Safety)
        val firstCall = decision.toolCalls.first()
        val tool = toolRegistry.getTool(firstCall.toolName)

        if (tool == null) {
            val missingText = "Tool '${firstCall.toolName}' is not registered."
            onStateChange(RobotActivityState.ERROR)
            onMoodChange(RobotMood.CONCERNED)
            speakResponse(missingText)
            return missingText
        }

        if (tool.isSensitive || decision.requiresConfirmation) {
            val confirmTitle = "Confirm Action"
            val confirmDesc = decision.confirmationPrompt
                ?: "Do you want me to proceed with ${tool.name}?"

            onStateChange(RobotActivityState.IDLE)
            onMoodChange(RobotMood.CURIOUS)

            onPromptConfirmation(
                confirmTitle,
                confirmDesc,
                {
                    executeToolDirectly(tool, firstCall, decision.responseText)
                },
                {
                    onStateChange(RobotActivityState.IDLE)
                    onMoodChange(RobotMood.HAPPY)
                    val cancelText = "Action was cancelled."
                    speakResponse(cancelText)
                }
            )
            return confirmDesc
        } else {
            // Harmless operations execute directly
            return executeToolDirectly(tool, firstCall, decision.responseText)
        }
    }

    private fun executeToolDirectly(
        tool: AgentTool,
        call: ToolCallRequest,
        fallbackSpeech: String
    ): String {
        onStateChange(RobotActivityState.WALKING)

        val result = try {
            kotlinx.coroutines.runBlocking {
                tool.execute(call.arguments)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tool execution threw exception", e)
            ToolExecutionResult(
                success = false,
                message = "Tool execution failed: ${e.localizedMessage}",
                speakableResponse = "An error occurred while executing ${tool.name}."
            )
        }

        if (result.success) {
            onStateChange(RobotActivityState.SUCCESS)
            onMoodChange(RobotMood.PROUD)
        } else {
            onStateChange(RobotActivityState.ERROR)
            onMoodChange(RobotMood.CONCERNED)
        }

        val speech = if (result.speakableResponse.isNotBlank()) {
            result.speakableResponse
        } else {
            fallbackSpeech
        }

        speakResponse(speech)
        return speech
    }

    private fun speakResponse(text: String) {
        onSpeechBubble(text)
        ttsManager.speak(
            text = text,
            onStart = {
                onStateChange(RobotActivityState.SPEAKING)
            },
            onDone = {
                onStateChange(RobotActivityState.IDLE)
                onMoodChange(RobotMood.HAPPY)
            }
        )
    }

    companion object {
        private const val TAG = "MiniRobotTaskAgent"
    }
}
