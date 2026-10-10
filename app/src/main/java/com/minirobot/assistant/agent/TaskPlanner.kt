package com.minirobot.assistant.agent

import android.util.Log
import com.minirobot.assistant.agent.ai.AiProviderManager

/**
 * Task planner responsible for breaking down high-level user commands into an ordered
 * sequence of actionable PlanSteps using the selected AI Provider.
 */
class TaskPlanner(
    private val aiProviderManager: AiProviderManager,
    private val toolRegistry: ToolRegistry
) {

    suspend fun createPlan(userInput: String): TaskPlan {
        val query = userInput.trim()
        Log.i(TAG, "Creating task plan for query: '$query'")

        val provider = aiProviderManager.getActiveProvider()
        val tools = toolRegistry.getAllTools()

        return try {
            val decision = provider.decideIntent(query, tools)
            decision.toTaskPlan(query)
        } catch (e: Exception) {
            Log.e(TAG, "AI Provider planning failed: ${e.message}", e)
            TaskPlan(
                rawQuery = query,
                steps = emptyList(),
                conversationalResponse = "I had trouble understanding and planning that task: ${e.localizedMessage}"
            )
        }
    }

    companion object {
        private const val TAG = "TaskPlanner"
    }
}
