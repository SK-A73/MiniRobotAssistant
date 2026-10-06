package com.minirobot.assistant.agent

/**
 * Result returned by a tool execution.
 */
data class ToolExecutionResult(
    val success: Boolean,
    val message: String,
    val speakableResponse: String = message,
    val data: Map<String, Any?> = emptyMap()
)

/**
 * Modular interface for capabilities executable by the AI Agent.
 */
interface AgentTool {
    /** Unique identifier of the tool */
    val name: String

    /** Human and LLM readable description of what the tool does */
    val description: String

    /** Whether the tool requires explicit user confirmation before executing */
    val isSensitive: Boolean

    /**
     * Executes the tool with given arguments.
     */
    suspend fun execute(args: Map<String, Any?>): ToolExecutionResult
}
