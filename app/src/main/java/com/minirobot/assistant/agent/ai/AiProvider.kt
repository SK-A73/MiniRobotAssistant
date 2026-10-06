package com.minirobot.assistant.agent.ai

import com.minirobot.assistant.agent.AgentTool

/**
 * Representation of a tool call selected by the AI agent.
 */
data class ToolCallRequest(
    val toolName: String,
    val arguments: Map<String, Any?> = emptyMap()
)

/**
 * High-level decision produced by the AI Provider from user utterance.
 */
data class AgentIntentDecision(
    val responseText: String,
    val toolCalls: List<ToolCallRequest> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
)

/**
 * Modular AI Provider interface for intent resolution and tool selection.
 */
interface AiProvider {
    val name: String
    suspend fun decideIntent(userInput: String, availableTools: List<AgentTool>): AgentIntentDecision
}
