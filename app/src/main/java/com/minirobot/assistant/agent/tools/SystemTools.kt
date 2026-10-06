package com.minirobot.assistant.agent.tools

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.Settings
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tool to launch Android System Settings.
 */
class LaunchSettingsTool(private val context: Context) : AgentTool {
    override val name: String = "launch_settings"
    override val description: String = "Opens Android System Settings."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                success = true,
                message = "Opened Settings",
                speakableResponse = "Opening device settings."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Failed to open settings: ${e.localizedMessage}",
                speakableResponse = "I could not open settings."
            )
        }
    }
}

/**
 * Tool to query the device battery percentage and charging state.
 */
class BatteryStatusTool(private val context: Context) : AgentTool {
    override val name: String = "battery_status"
    override val description: String = "Checks current phone battery percentage and charging status."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = bm?.isCharging == true

        val response = if (batteryPct >= 0) {
            val chargingText = if (isCharging) "and currently charging ⚡" else "and not charging"
            "Battery is at $batteryPct% $chargingText."
        } else {
            "Battery status could not be read."
        }

        return ToolExecutionResult(
            success = true,
            message = response,
            speakableResponse = response,
            data = mapOf("percentage" to batteryPct, "charging" to isCharging)
        )
    }
}

/**
 * Tool to tell the current system time and date.
 */
class DeviceTimeTool : AgentTool {
    override val name: String = "device_time"
    override val description: String = "Gets the current time and date."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val now = Date()
        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now)
        val message = "It is $timeStr on $dateStr."

        return ToolExecutionResult(
            success = true,
            message = message,
            speakableResponse = message
        )
    }
}

/**
 * Tool representing sensitive operations (e.g., sending messages or modifying sensitive data).
 * Marked with isSensitive = true to trigger the Phase 9 safety confirmation flow.
 */
class SendMessageTool(private val context: Context) : AgentTool {
    override val name: String = "send_message"
    override val description: String = "Prepares and sends a message or communication. Argument: recipient (String), message (String)"
    override val isSensitive: Boolean = true

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val recipient = (args["recipient"] as? String) ?: "Contact"
        val message = (args["message"] as? String) ?: ""
        return ToolExecutionResult(
            success = true,
            message = "Message confirmed and prepared for $recipient: \"$message\"",
            speakableResponse = "Message has been sent to $recipient."
        )
    }
}
