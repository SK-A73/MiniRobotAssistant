package com.minirobot.assistant.agent.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult

/**
 * Tool to open specific web URLs in the default browser.
 */
class OpenUrlTool(private val context: Context) : AgentTool {

    override val name: String = "open_url"
    override val description: String = "Opens a web link in browser. Argument: url (String)"
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        var url = (args["url"] as? String)?.trim() ?: return ToolExecutionResult(
            success = false,
            message = "URL is missing.",
            speakableResponse = "Which website would you like me to open?"
        )

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                success = true,
                message = "Opened $url",
                speakableResponse = "Opening website now."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Unable to open URL: ${e.localizedMessage}",
                speakableResponse = "I could not open that link."
            )
        }
    }
}
