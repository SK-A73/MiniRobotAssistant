package com.minirobot.assistant.model

import com.minirobot.assistant.agent.TaskExecutionState

/**
 * State representing the Assistant UI overlay, live speech recognition,
 * AI thoughts, dialog, and safety confirmation modals.
 */
data class AssistantUiState(
    val isSheetVisible: Boolean = false,
    val greetingText: String = "How can I help you?",
    val statusMessage: String = "Tap the microphone to speak or choose a command.",
    val recognizedSpeech: String = "",
    val isListening: Boolean = false,
    val executionState: TaskExecutionState = TaskExecutionState.IDLE,
    val rmsLevel: Float = 0f,
    val robotSpeechBubble: String? = null,
    val activeMood: RobotMood = RobotMood.HAPPY,
    val isSafetyConfirmationVisible: Boolean = false,
    val pendingActionTitle: String? = null,
    val pendingActionDescription: String? = null,
    val onConfirmPendingAction: (() -> Unit)? = null,
    val onRejectPendingAction: (() -> Unit)? = null
)

