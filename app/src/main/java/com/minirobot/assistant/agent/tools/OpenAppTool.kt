package com.minirobot.assistant.agent.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult

/**
 * Tool to find and launch applications installed on the device (e.g., YouTube, Chrome, Maps, Settings).
 */
class OpenAppTool(private val context: Context) : AgentTool {

    override val name: String = "open_app"
    override val description: String = "Opens an installed application on the device. Argument: app_name (String)"
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val appQuery = (args["app_name"] as? String)?.trim()
        if (appQuery.isNullOrEmpty()) {
            return ToolExecutionResult(
                success = false,
                message = "Please specify which application to open.",
                speakableResponse = "Which application would you like me to open?"
            )
        }

        val pm = context.packageManager

        // Known direct package shortcuts for speed and accuracy
        val directPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "google chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "whatsapp" to "com.whatsapp",
            "gmail" to "com.google.android.gm",
            "camera" to "com.android.camera",
            "settings" to "com.android.settings",
            "clock" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator"
        )

        val cleanQuery = appQuery.lowercase()
        val directPkg = directPackages[cleanQuery]
        if (directPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolExecutionResult(
                    success = true,
                    message = "Opened $appQuery",
                    speakableResponse = "Opening $appQuery now!"
                )
            }
        }

        // Query all installed launcher activities
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        // Find best match by label
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString()
            if (label.contains(cleanQuery, ignoreCase = true) || cleanQuery.contains(label.lowercase())) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolExecutionResult(
                        success = true,
                        message = "Opened $label",
                        speakableResponse = "Opening $label now!"
                    )
                }
            }
        }

        return ToolExecutionResult(
            success = false,
            message = "Could not find an installed app matching '$appQuery'.",
            speakableResponse = "I couldn't find $appQuery installed on your phone."
        )
    }
}
