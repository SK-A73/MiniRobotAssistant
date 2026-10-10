package com.minirobot.assistant.agent.ai

import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.PlanStep
import com.minirobot.assistant.agent.TaskPlan

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
    val planSteps: List<PlanStep> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
) {
    /**
     * Converts decision into an ordered TaskPlan.
     */
    fun toTaskPlan(rawQuery: String): TaskPlan {
        val steps = if (planSteps.isNotEmpty()) {
            planSteps
        } else {
            toolCalls.map { PlanStep(it.toolName, it.arguments, it.toolName) }
        }
        return TaskPlan(
            rawQuery = rawQuery,
            steps = steps,
            conversationalResponse = if (steps.isEmpty()) responseText else null,
            requiresConfirmation = requiresConfirmation,
            confirmationPrompt = confirmationPrompt
        )
    }
}

/**
 * Modular AI Provider interface for intent resolution and tool selection.
 */
interface AiProvider {
    val name: String
    suspend fun decideIntent(userInput: String, availableTools: List<AgentTool>): AgentIntentDecision
}

