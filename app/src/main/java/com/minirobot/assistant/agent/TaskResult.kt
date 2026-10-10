package com.minirobot.assistant.agent

/**
 * High-level execution phases for the AI task agent.
 * Directly reflects the agent's progress through the task pipeline.
 */
enum class TaskExecutionState {
    IDLE,
    LISTENING,
    UNDERSTANDING,
    PLANNING,
    EXECUTING,
    VERIFYING,
    COMPLETED,
    FAILED
}

/**
 * A discrete executable step in an ordered task plan.
 */
data class PlanStep(
    val toolName: String,
    val arguments: Map<String, Any?> = emptyMap(),
    val description: String = ""
)

/**
 * Ordered task plan created by the TaskPlanner.
 */
data class TaskPlan(
    val rawQuery: String,
    val steps: List<PlanStep> = emptyList(),
    val conversationalResponse: String? = null,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
)

/**
 * Outcome of executing an individual step.
 */
data class StepExecutionResult(
    val step: PlanStep,
    val success: Boolean,
    val message: String,
    val speakableResponse: String = message,
    val data: Map<String, Any?> = emptyMap()
)

/**
 * Comprehensive result of executing a TaskPlan.
 */
data class TaskResult(
    val overallSuccess: Boolean,
    val completedSteps: List<StepExecutionResult> = emptyList(),
    val failedStep: StepExecutionResult? = null,
    val errorMessage: String? = null,
    val speakableResponse: String = "",
    val summaryMessage: String = ""
)
