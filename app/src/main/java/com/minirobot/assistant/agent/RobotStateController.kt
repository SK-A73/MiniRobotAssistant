package com.minirobot.assistant.agent

import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotMood

/**
 * Controller bridging the AI task agent execution lifecycle to the cute robot avatar animations
 * and emotional mood reactions.
 */
class RobotStateController(
    private val onStateChange: (RobotActivityState) -> Unit,
    private val onMoodChange: (RobotMood) -> Unit,
    private val onStatusUpdate: (TaskExecutionState, String) -> Unit,
    private val onSpeechBubble: (String) -> Unit
) {

    var currentState: TaskExecutionState = TaskExecutionState.IDLE
        private set

    fun transitionTo(state: TaskExecutionState, detailMessage: String = "") {
        currentState = state

        when (state) {
            TaskExecutionState.IDLE -> {
                onStateChange(RobotActivityState.IDLE)
                onMoodChange(RobotMood.HAPPY)
            }
            TaskExecutionState.LISTENING -> {
                onStateChange(RobotActivityState.LISTENING)
                onMoodChange(RobotMood.CURIOUS)
            }
            TaskExecutionState.UNDERSTANDING -> {
                onStateChange(RobotActivityState.THINKING)
                onMoodChange(RobotMood.THINKING)
            }
            TaskExecutionState.PLANNING -> {
                onStateChange(RobotActivityState.THINKING)
                onMoodChange(RobotMood.THINKING)
            }
            TaskExecutionState.EXECUTING -> {
                onStateChange(RobotActivityState.WALKING)
                onMoodChange(RobotMood.PLAYFUL)
            }
            TaskExecutionState.VERIFYING -> {
                onStateChange(RobotActivityState.LOOKING_AROUND)
                onMoodChange(RobotMood.CURIOUS)
            }
            TaskExecutionState.COMPLETED -> {
                onStateChange(RobotActivityState.SUCCESS)
                onMoodChange(RobotMood.PROUD)
            }
            TaskExecutionState.FAILED -> {
                onStateChange(RobotActivityState.ERROR)
                onMoodChange(RobotMood.CONCERNED)
            }
        }

        val displayMsg = if (detailMessage.isNotBlank()) detailMessage else getDefaultStatus(state)
        onStatusUpdate(state, displayMsg)
    }

    fun showBubble(text: String) {
        onSpeechBubble(text)
    }

    private fun getDefaultStatus(state: TaskExecutionState): String = when (state) {
        TaskExecutionState.IDLE -> "Ready for commands."
        TaskExecutionState.LISTENING -> "Listening for your voice... 🎙️"
        TaskExecutionState.UNDERSTANDING -> "Understanding request..."
        TaskExecutionState.PLANNING -> "Planning action steps..."
        TaskExecutionState.EXECUTING -> "Executing task..."
        TaskExecutionState.VERIFYING -> "Verifying result..."
        TaskExecutionState.COMPLETED -> "Task completed successfully! ✨"
        TaskExecutionState.FAILED -> "Task could not be completed."
    }
}
