package com.minirobot.assistant.agent

import android.util.Log
import com.minirobot.assistant.accessibility.PhoneAutomationAccessibilityService
import kotlinx.coroutines.delay

/**
 * Executes steps of a TaskPlan with validation, safety prompts, and verification.
 */
class ToolExecutor(
    private val toolRegistry: ToolRegistry,
    private val robotStateController: RobotStateController
) {

    /**
     * Executes an entire TaskPlan step-by-step.
     * Stops immediately if any step fails and honestly reports the outcome.
     */
    suspend fun executePlan(
        plan: TaskPlan,
        onConfirmationRequested: (
            title: String,
            description: String,
            onConfirm: () -> Unit,
            onReject: () -> Unit
        ) -> Unit
    ): TaskResult {
        if (plan.steps.isEmpty()) {
            val conversational = plan.conversationalResponse
                ?: "I'm not sure how to perform that task yet."
            return TaskResult(
                overallSuccess = true,
                completedSteps = emptyList(),
                speakableResponse = conversational,
                summaryMessage = conversational
            )
        }

        // Check if any step requires sensitive confirmation
        val sensitiveStep = plan.steps.firstOrNull { step ->
            val tool = toolRegistry.getTool(step.toolName)
            tool?.isSensitive == true
        }

        if (sensitiveStep != null || plan.requiresConfirmation) {
            val tool = toolRegistry.getTool(sensitiveStep?.toolName ?: "")
            val title = "Confirm Action"
            val prompt = plan.confirmationPrompt
                ?: "Do you want me to proceed with ${tool?.name ?: "this sensitive action"}?"

            var userConfirmed = false
            var waiting = true

            onConfirmationRequested(
                title,
                prompt,
                {
                    userConfirmed = true
                    waiting = false
                },
                {
                    userConfirmed = false
                    waiting = false
                }
            )

            // Wait for user interaction
            while (waiting) {
                delay(100L)
            }

            if (!userConfirmed) {
                robotStateController.transitionTo(TaskExecutionState.IDLE, "Action cancelled by user.")
                return TaskResult(
                    overallSuccess = false,
                    speakableResponse = "Action was cancelled.",
                    summaryMessage = "User rejected confirmation for action."
                )
            }
        }

        val completedSteps = mutableListOf<StepExecutionResult>()

        for ((index, step) in plan.steps.withIndex()) {
            val stepLabel = "Step ${index + 1}/${plan.steps.size}: ${step.description.ifBlank { step.toolName }}"
            Log.i(TAG, "Executing $stepLabel")

            robotStateController.transitionTo(
                TaskExecutionState.EXECUTING,
                "Executing $stepLabel"
            )

            val stepResult = executeSingleStep(step)

            if (!stepResult.success) {
                Log.w(TAG, "Step ${index + 1} failed: ${stepResult.message}")
                robotStateController.transitionTo(
                    TaskExecutionState.FAILED,
                    "Failed at ${step.description.ifBlank { step.toolName }}: ${stepResult.message}"
                )

                // Honest explanation of partial success vs failure
                val failureSpeech = if (completedSteps.isNotEmpty()) {
                    val prevDesc = completedSteps.last().step.description.ifBlank { completedSteps.last().step.toolName }
                    "I completed $prevDesc, but couldn't ${step.description.ifBlank { step.toolName }}. ${stepResult.speakableResponse}"
                } else {
                    stepResult.speakableResponse
                }

                return TaskResult(
                    overallSuccess = false,
                    completedSteps = completedSteps,
                    failedStep = stepResult,
                    errorMessage = stepResult.message,
                    speakableResponse = failureSpeech,
                    summaryMessage = "Task stopped at step ${index + 1}: ${stepResult.message}"
                )
            }

            completedSteps.add(stepResult)

            // Small pause between multi-step commands for OS / app UI transitions
            if (index < plan.steps.size - 1) {
                delay(800L)
            }
        }

        robotStateController.transitionTo(TaskExecutionState.COMPLETED, "All steps completed! ✨")

        val finalSpeech = when {
            completedSteps.size == 1 -> completedSteps.first().speakableResponse
            completedSteps.size > 1 -> {
                val firstDesc = completedSteps.first().step.description
                val lastDesc = completedSteps.last().step.description
                "Completed $firstDesc and $lastDesc."
            }
            else -> "Task completed."
        }

        return TaskResult(
            overallSuccess = true,
            completedSteps = completedSteps,
            speakableResponse = finalSpeech,
            summaryMessage = "Successfully executed ${completedSteps.size} steps."
        )
    }

    private suspend fun executeSingleStep(step: PlanStep): StepExecutionResult {
        val tool = toolRegistry.getTool(step.toolName)
            ?: return StepExecutionResult(
                step = step,
                success = false,
                message = "Tool '${step.toolName}' is not registered.",
                speakableResponse = "I don't have the tool '${step.toolName}' available."
            )

        // Check Accessibility Requirement for UI tools
        val requiresAccessibility = step.toolName in listOf(
            "go_back",
            "scroll_screen",
            "type_text",
            "tap_accessible_element",
            "tap_element"
        )

        if (requiresAccessibility && !PhoneAutomationAccessibilityService.isServiceRunning()) {
            return StepExecutionResult(
                step = step,
                success = false,
                message = "Accessibility service is not enabled.",
                speakableResponse = "Please enable Mini Robot in Accessibility Settings to allow ${step.toolName.replace('_', ' ')}."
            )
        }

        val execResult = try {
            tool.execute(step.arguments)
        } catch (e: Exception) {
            Log.e(TAG, "Tool '${tool.name}' threw exception", e)
            return StepExecutionResult(
                step = step,
                success = false,
                message = "Execution failed: ${e.localizedMessage}",
                speakableResponse = "An error occurred while executing ${tool.name}."
            )
        }

        // Verify result
        robotStateController.transitionTo(
            TaskExecutionState.VERIFYING,
            "Verifying ${step.description.ifBlank { tool.name }}..."
        )
        delay(350L)

        val verifiedSuccess = verifyStepResult(step, execResult)

        return StepExecutionResult(
            step = step,
            success = verifiedSuccess,
            message = execResult.message,
            speakableResponse = execResult.speakableResponse,
            data = execResult.data
        )
    }

    private fun verifyStepResult(step: PlanStep, result: ToolExecutionResult): Boolean {
        // Genuine verification: respect tool execution result and system state
        if (!result.success) return false

        // Additional sanity checks for specific tools
        return when (step.toolName) {
            "go_back", "scroll_screen", "type_text", "tap_accessible_element", "tap_element" -> {
                PhoneAutomationAccessibilityService.isServiceRunning() && result.success
            }
            else -> result.success
        }
    }

    companion object {
        private const val TAG = "ToolExecutor"
    }
}
