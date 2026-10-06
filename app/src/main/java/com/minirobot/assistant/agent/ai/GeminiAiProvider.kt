package com.minirobot.assistant.agent.ai

import android.util.Log
import com.minirobot.assistant.agent.AgentTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI Provider utilizing Google Gemini API for natural language understanding and tool dispatch.
 */
class GeminiAiProvider(
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash"
) : AiProvider {

    override val name: String = "Google Gemini ($model)"

    private val localFallback = LocalRuleBasedAiProvider()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun decideIntent(
        userInput: String,
        availableTools: List<AgentTool>
    ): AgentIntentDecision {
        if (apiKey.isBlank()) {
            Log.d(TAG, "No API key configured, using local rule-based AI")
            return localFallback.decideIntent(userInput, availableTools)
        }

        return withContext(Dispatchers.IO) {
            try {
                callGeminiApi(userInput, availableTools)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API call failed (${e.message}), falling back to local provider", e)
                localFallback.decideIntent(userInput, availableTools)
            }
        }
    }

    private fun callGeminiApi(userInput: String, availableTools: List<AgentTool>): AgentIntentDecision {
        val toolsDescription = availableTools.joinToString("\n") { "- ${it.name}: ${it.description} (sensitive: ${it.isSensitive})" }

        val systemPrompt = """
            You are Mini Robot Assistant, a helpful cute companion on an Android phone.
            Decide if the user query requires invoking a tool or a conversational response.
            Available tools:
            $toolsDescription
            
            Respond STRICTLY in JSON format with this structure:
            {
               "response": "friendly speech for the robot",
               "tool": "tool_name or empty string if no tool needed",
               "args": { "key": "value" },
               "is_sensitive": true/false
            }
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "$systemPrompt\n\nUser: $userInput") })
                    })
                })
            }
            put("contents", contents)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                Log.w(TAG, "Gemini returned HTTP ${response.code}: $err")
                return localFallback.decideIntentBlocking(userInput, availableTools)
            }

            val respBody = response.body?.string() ?: ""
            val jsonRoot = JSONObject(respBody)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            parseGeminiOutput(text, userInput, availableTools)
        }
    }

    private fun parseGeminiOutput(
        geminiText: String,
        fallbackInput: String,
        availableTools: List<AgentTool>
    ): AgentIntentDecision {
        val clean = geminiText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        return try {
            val obj = JSONObject(clean)
            val responseText = obj.optString("response", "Processing request...")
            val toolName = obj.optString("tool", "").trim()
            val isSensitive = obj.optBoolean("is_sensitive", false)

            val toolCalls = if (toolName.isNotEmpty() && toolName != "none" && toolName != "null") {
                val argsObj = obj.optJSONObject("args")
                val argsMap = mutableMapOf<String, Any?>()
                if (argsObj != null) {
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                }
                listOf(ToolCallRequest(toolName, argsMap))
            } else {
                emptyList()
            }

            AgentIntentDecision(
                responseText = responseText,
                toolCalls = toolCalls,
                requiresConfirmation = isSensitive,
                confirmationPrompt = if (isSensitive) "Do you want me to continue with $toolName?" else null
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Gemini JSON output: $geminiText", e)
            localFallback.decideIntentBlocking(fallbackInput, availableTools)
        }
    }

    private fun LocalRuleBasedAiProvider.decideIntentBlocking(
        userInput: String,
        tools: List<AgentTool>
    ): AgentIntentDecision {
        return kotlinx.coroutines.runBlocking {
            decideIntent(userInput, tools)
        }
    }

    companion object {
        private const val TAG = "MiniRobotGemini"
    }
}
