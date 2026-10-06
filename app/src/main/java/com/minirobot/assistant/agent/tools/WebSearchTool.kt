package com.minirobot.assistant.agent.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult
import java.net.URLEncoder

/**
 * Tool to execute web searches (Google Search, YouTube Search, etc.) via default browser or dedicated apps.
 */
class WebSearchTool(private val context: Context) : AgentTool {

    override val name: String = "web_search"
    override val description: String = "Searches the web or YouTube. Arguments: query (String), platform (Optional String: 'web' or 'youtube')"
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val query = (args["query"] as? String)?.trim()
        val platform = (args["platform"] as? String)?.trim()?.lowercase() ?: "web"

        if (query.isNullOrEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Search query is empty.",
                speakableResponse = "What would you like me to search for?"
            )
        }

        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            if (platform == "youtube" || query.startsWith("youtube ", ignoreCase = true)) {
                val cleanQuery = query.removePrefix("youtube ").removePrefix("YouTube ").trim()
                val youtubeUri = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                val intent = Intent(Intent.ACTION_VIEW, youtubeUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolExecutionResult(
                    success = true,
                    message = "Searching YouTube for: $cleanQuery",
                    speakableResponse = "Searching YouTube for $cleanQuery."
                )
            } else {
                val searchUri = Uri.parse("https://www.google.com/search?q=$encodedQuery")
                val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolExecutionResult(
                    success = true,
                    message = "Searching web for: $query",
                    speakableResponse = "Searching for $query."
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Failed to launch search: ${e.localizedMessage}",
                speakableResponse = "I couldn't open the search results."
            )
        }
    }
}
