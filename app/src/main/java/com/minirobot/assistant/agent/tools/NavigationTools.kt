package com.minirobot.assistant.agent.tools

import android.content.Context
import android.content.Intent
import com.minirobot.assistant.accessibility.PhoneAutomationAccessibilityService
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult

/**
 * Tool to trigger the system Back button.
 */
class BackTool : AgentTool {
    override val name: String = "go_back"
    override val description: String = "Navigates back to the previous screen or closes current dialog."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val service = PhoneAutomationAccessibilityService.instance
        return if (service != null) {
            val success = service.performBack()
            if (success) {
                ToolExecutionResult(
                    success = true,
                    message = "Navigated back",
                    speakableResponse = "Going back."
                )
            } else {
                ToolExecutionResult(
                    success = false,
                    message = "Failed to perform back action",
                    speakableResponse = "I couldn't navigate back."
                )
            }
        } else {
            ToolExecutionResult(
                success = false,
                message = "Accessibility service is not active. Please enable it in Mini Robot settings.",
                speakableResponse = "Please enable Accessibility permission in settings to allow Back navigation."
            )
        }
    }
}

/**
 * Tool to navigate to the Android Home screen.
 */
class HomeTool(private val context: Context) : AgentTool {
    override val name: String = "go_home"
    override val description: String = "Navigates to the Android home screen."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val service = PhoneAutomationAccessibilityService.instance
        if (service != null && service.performHome()) {
            return ToolExecutionResult(
                success = true,
                message = "Navigated home",
                speakableResponse = "Going to home screen."
            )
        }

        // Reliable Android Intent fallback to go home
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(homeIntent)
            ToolExecutionResult(
                success = true,
                message = "Navigated home",
                speakableResponse = "Going to home screen."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Could not navigate home: ${e.localizedMessage}",
                speakableResponse = "I couldn't return to the home screen."
            )
        }
    }
}

/**
 * Tool to scroll the current screen up or down.
 */
class ScrollTool : AgentTool {
    override val name: String = "scroll_screen"
    override val description: String = "Scrolls the screen. Argument: direction ('up' or 'down')"
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val direction = (args["direction"] as? String)?.lowercase() ?: "down"
        val forward = direction != "up"

        val service = PhoneAutomationAccessibilityService.instance
        return if (service != null) {
            val success = service.scrollScreen(forward)
            if (success) {
                ToolExecutionResult(
                    success = true,
                    message = "Scrolled $direction",
                    speakableResponse = "Scrolling $direction."
                )
            } else {
                ToolExecutionResult(
                    success = false,
                    message = "Could not scroll current screen content.",
                    speakableResponse = "I couldn't find scrollable content here."
                )
            }
        } else {
            ToolExecutionResult(
                success = false,
                message = "Accessibility service is not active. Please enable it in Settings.",
                speakableResponse = "Accessibility permission is required for scrolling."
            )
        }
    }
}
