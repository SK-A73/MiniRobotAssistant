package com.minirobot.assistant.agent.ai

import android.util.Log
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.PlanStep
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
 * AI Provider utilizing Google Gemini API or a secure backend proxy for natural language
 * understanding, structured task planning, and strict tool validation against an allowlist.
 */
class GeminiAiProvider(
    private val apiKey: String = "",
    private val backendUrl: String = "",
    private val model: String = "gemini-1.5-flash"
) : AiProvider {

    override val name: String = if (backendUrl.isNotBlank()) "Gemini Backend Proxy" else "Google Gemini ($model)"

    private val localFallback = LocalRuleBasedAiProvider()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun decideIntent(
        userInput: String,
        availableTools: List<AgentTool>
    ): AgentIntentDecision {
        if (backendUrl.isBlank() && apiKey.isBlank()) {
            Log.d(TAG, "No Gemini backend URL or API key configured, using local offline engine")
            return localFallback.decideIntent(userInput, availableTools)
        }

        return withContext(Dispatchers.IO) {
            try {
                if (backendUrl.isNotBlank()) {
                    callBackendProxy(userInput, availableTools)
                } else {
                    callGeminiApi(userInput, availableTools)
                }
            } catch (e: Exception) {
                val safeMsg = sanitizeError(e.message ?: "Unknown error")
                Log.w(TAG, "Gemini provider call failed ($safeMsg), falling back to local provider")
                localFallback.decideIntent(userInput, availableTools)
            }
        }
    }

    /**
     * Calls a self-hosted secure backend proxy that holds the GEMINI_API_KEY environment variable.
     */
    private fun callBackendProxy(userInput: String, availableTools: List<AgentTool>): AgentIntentDecision {
        val rootUrl = backendUrl.trimEnd('/')
        val endpoint = if (rootUrl.endsWith("/api/plan")) rootUrl else "$rootUrl/api/plan"

        val toolsJson = JSONArray().apply {
            availableTools.forEach { tool ->
                put(JSONObject().apply {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("is_sensitive", tool.isSensitive)
                })
            }
        }

        val requestPayload = JSONObject().apply {
            put("query", userInput)
            put("tools", toolsJson)
        }

        val body = requestPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = sanitizeError(response.body?.string() ?: "")
                Log.w(TAG, "Backend proxy returned HTTP ${response.code}: $err")
                return localFallback.decideIntentBlocking(userInput, availableTools)
            }

            val respBody = response.body?.string() ?: ""
            parseGeminiOutput(respBody, userInput, availableTools)
        }
    }

    /**
     * Direct Gemini API call when user provides their personal development API key.
     */
    private fun callGeminiApi(userInput: String, availableTools: List<AgentTool>): AgentIntentDecision {
        val toolsDescription = availableTools.joinToString("\n") { "- ${it.name}: ${it.description} (sensitive: ${it.isSensitive})" }

        val systemPrompt = """
            You are Mini Robot Assistant, a helpful cute companion on an Android phone.
            Decide if the user query requires invoking one or more tools in sequence, or a conversational response.
            Available tools:
            $toolsDescription
            
            Respond STRICTLY in JSON format with this structure:
            {
               "response": "friendly speech for the robot",
               "plan": [
                  { "tool": "tool_name", "args": { "key": "value" }, "description": "what this step accomplishes" }
               ],
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
                val err = sanitizeError(response.body?.string() ?: "")
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

        val allowedToolMap = availableTools.associateBy { it.name }

        return try {
            val obj = JSONObject(clean)
            val responseText = obj.optString("response", "Processing request...")
            var isSensitive = obj.optBoolean("is_sensitive", false)

            val planArray = obj.optJSONArray("plan")
            val planSteps = mutableListOf<PlanStep>()

            if (planArray != null) {
                for (i in 0 until planArray.length()) {
                    val stepObj = planArray.optJSONObject(i) ?: continue
                    val tName = stepObj.optString("tool", "").trim()
                    if (tName.isEmpty() || tName == "none" || tName == "null") continue

                    // Validate tool against allowlist of implemented tools
                    val toolDef = allowedToolMap[tName]
                    if (toolDef == null) {
                        Log.w(TAG, "Tool '$tName' rejected: not in implemented tools allowlist")
                        continue
                    }

                    if (toolDef.isSensitive) {
                        isSensitive = true
                    }

                    val desc = stepObj.optString("description", tName)
                    val argsObj = stepObj.optJSONObject("args")
                    val argsMap = mutableMapOf<String, Any?>()
                    if (argsObj != null) {
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            argsMap[key] = argsObj.get(key)
                        }
                    }
                    planSteps.add(PlanStep(tName, argsMap, desc))
                }
            } else {
                // Fallback for single tool format
                val toolName = obj.optString("tool", "").trim()
                val toolDef = allowedToolMap[toolName]
                if (toolDef != null) {
                    if (toolDef.isSensitive) isSensitive = true
                    val argsObj = obj.optJSONObject("args")
                    val argsMap = mutableMapOf<String, Any?>()
                    if (argsObj != null) {
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            argsMap[key] = argsObj.get(key)
                        }
                    }
                    planSteps.add(PlanStep(toolName, argsMap, toolName))
                }
            }

            AgentIntentDecision(
                responseText = responseText,
                toolCalls = planSteps.map { ToolCallRequest(it.toolName, it.arguments) },
                planSteps = planSteps,
                requiresConfirmation = isSensitive,
                confirmationPrompt = if (isSensitive) "Do you want me to proceed with this sensitive action?" else null
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Gemini output, falling back to local provider: ${sanitizeError(e.message ?: "")}")
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

    private fun sanitizeError(msg: String): String {
        return msg.replace(Regex("key=[a-zA-Z0-9_\\-]+"), "key=REDACTED")
    }

    companion object {
        private const val TAG = "MiniRobotGemini"
    }
}

