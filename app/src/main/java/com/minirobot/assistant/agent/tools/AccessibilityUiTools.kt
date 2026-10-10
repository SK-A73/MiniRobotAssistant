package com.minirobot.assistant.agent.tools

import com.minirobot.assistant.accessibility.PhoneAutomationAccessibilityService
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult

/**
 * Tool to click on an on-screen button or element by matching visible text using AccessibilityService.
 */
open class TapAccessibleElementTool : AgentTool {
    override val name: String = "tap_accessible_element"
    override val description: String = "Clicks an on-screen element with matching text using AccessibilityService. Argument: text (String)"
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val targetText = (args["text"] as? String)?.trim() ?: return ToolExecutionResult(
            success = false,
            message = "Element text to click was not provided.",
            speakableResponse = "What element would you like me to tap?"
        )

        val service = PhoneAutomationAccessibilityService.instance
        return if (service != null) {
            val clicked = service.clickElementByText(targetText)
            if (clicked) {
                ToolExecutionResult(
                    success = true,
                    message = "Tapped element '$targetText'",
                    speakableResponse = "Tapped $targetText."
                )
            } else {
                ToolExecutionResult(
                    success = false,
                    message = "Could not find a clickable element matching '$targetText' on current screen.",
                    speakableResponse = "I couldn't find $targetText on screen to tap."
                )
            }
        } else {
            ToolExecutionResult(
                success = false,
                message = "Accessibility service is not active. Please enable it in Settings.",
                speakableResponse = "Accessibility permission is needed to tap on-screen elements."
            )
        }
    }
}

/**
 * Backward-compatible alias for TapAccessibleElementTool under 'tap_element'.
 */
class TapTool : TapAccessibleElementTool() {
    override val name: String = "tap_element"
}

/**
 * Tool to type text into the currently focused text field on screen.
 */
class TypeTextTool : AgentTool {
    override val name: String = "type_text"
    override val description: String = "Types text into the active focused input field. Argument: text (String)"
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val textToType = (args["text"] as? String)?.trim() ?: return ToolExecutionResult(
            success = false,
            message = "Text to type was not provided.",
            speakableResponse = "What text would you like me to type?"
        )

        val service = PhoneAutomationAccessibilityService.instance
        return if (service != null) {
            val typed = service.typeTextIntoCurrentFocus(textToType)
            if (typed) {
                ToolExecutionResult(
                    success = true,
                    message = "Typed: '$textToType'",
                    speakableResponse = "Typed $textToType."
                )
            } else {
                ToolExecutionResult(
                    success = false,
                    message = "No focused input field found on screen to type into.",
                    speakableResponse = "Please tap an input box first so I can type into it."
                )
            }
        } else {
            ToolExecutionResult(
                success = false,
                message = "Accessibility service is not active.",
                speakableResponse = "Accessibility permission is required for typing text."
            )
        }
    }
}
