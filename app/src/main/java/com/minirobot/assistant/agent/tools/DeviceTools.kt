package com.minirobot.assistant.agent.tools

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.AlarmClock
import android.provider.Settings
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tool to control the device flashlight (torch), with support for timed auto-shutdown.
 */
class FlashlightTool(private val context: Context) : AgentTool {
    override val name: String = "flashlight"
    override val description: String = "Turns device flashlight (torch) on or off, or on for a specific duration. Arguments: state (Boolean, default true), duration_seconds (Int, optional)."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val state = when (val s = args["state"]) {
            is Boolean -> s
            is String -> s.equals("on", ignoreCase = true) || s.equals("true", ignoreCase = true)
            else -> true
        }

        val durationSeconds = when (val d = args["duration_seconds"]) {
            is Number -> d.toInt()
            is String -> d.toIntOrNull() ?: 0
            else -> 0
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ToolExecutionResult(
                success = false,
                message = "Camera service unavailable.",
                speakableResponse = "I cannot access the camera service on this device."
            )

        val cameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }

        if (cameraId == null) {
            return ToolExecutionResult(
                success = false,
                message = "No flashlight/flash hardware found on this device.",
                speakableResponse = "This device does not appear to have a supported flashlight."
            )
        }

        return try {
            // Cancel any previously running auto-off timer
            autoOffJob?.cancel()
            autoOffJob = null

            cameraManager.setTorchMode(cameraId, state)
            isTorchOn = state

            if (state && durationSeconds > 0) {
                // Schedule auto-shutdown
                autoOffJob = timerScope.launch {
                    delay(durationSeconds * 1000L)
                    try {
                        cameraManager.setTorchMode(cameraId, false)
                        isTorchOn = false
                    } catch (_: Exception) {}
                }

                val durationText = if (durationSeconds >= 60 && durationSeconds % 60 == 0) {
                    val minutes = durationSeconds / 60
                    "$minutes " + if (minutes == 1) "minute" else "minutes"
                } else {
                    "$durationSeconds seconds"
                }

                ToolExecutionResult(
                    success = true,
                    message = "Flashlight turned on for $durationText",
                    speakableResponse = "Flashlight is on for $durationText.",
                    data = mapOf("state" to true, "duration_seconds" to durationSeconds)
                )
            } else {
                val stateText = if (state) "on" else "off"
                ToolExecutionResult(
                    success = true,
                    message = "Flashlight turned $stateText",
                    speakableResponse = "Flashlight turned $stateText.",
                    data = mapOf("state" to state)
                )
            }
        } catch (e: CameraAccessException) {
            ToolExecutionResult(
                success = false,
                message = "Flashlight hardware error: ${e.localizedMessage}",
                speakableResponse = "Could not control the flashlight right now."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Flashlight error: ${e.localizedMessage}",
                speakableResponse = "Failed to toggle flashlight."
            )
        }
    }

    companion object {
        var isTorchOn: Boolean = false
            private set
        private var autoOffJob: Job? = null
        private val timerScope = CoroutineScope(Dispatchers.Default)
    }
}

/**
 * Tool to set an Android clock alarm.
 */
class SetAlarmTool(private val context: Context) : AgentTool {
    override val name: String = "set_alarm"
    override val description: String = "Sets an alarm for a given hour and minute. Arguments: hour (Int 0-23), minutes (Int 0-59), label (String, optional), skip_ui (Boolean, default true)."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val hour = when (val h = args["hour"]) {
            is Number -> h.toInt()
            is String -> h.toIntOrNull() ?: 7
            else -> 7
        }

        val minutes = when (val m = args["minutes"]) {
            is Number -> m.toInt()
            is String -> m.toIntOrNull() ?: 0
            else -> 0
        }

        val label = (args["label"] as? String)?.trim() ?: (args["message"] as? String)?.trim() ?: "Mini Robot Alarm"
        val skipUi = (args["skip_ui"] as? Boolean) ?: true

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            if (label.isNotBlank()) {
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
            }
            putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)

            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val amPm = if (hour < 12) "AM" else "PM"
            val timeFormatted = String.format("%d:%02d %s", displayHour, minutes, amPm)
            val msg = if (label.isNotBlank() && label != "Mini Robot Alarm") {
                "Alarm set for $timeFormatted with label \"$label\"."
            } else {
                "Alarm set for $timeFormatted."
            }

            ToolExecutionResult(
                success = true,
                message = msg,
                speakableResponse = msg,
                data = mapOf("hour" to hour, "minutes" to minutes, "label" to label)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Failed to set alarm: ${e.localizedMessage}",
                speakableResponse = "I couldn't set that alarm on this device."
            )
        }
    }
}

/**
 * Tool to set an Android countdown timer.
 */
class SetTimerTool(private val context: Context) : AgentTool {
    override val name: String = "set_timer"
    override val description: String = "Sets a countdown timer for a duration in seconds. Arguments: duration_seconds (Int), label (String, optional), skip_ui (Boolean, default true)."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val durationSeconds = when (val d = args["duration_seconds"]) {
            is Number -> d.toInt()
            is String -> d.toIntOrNull() ?: 60
            else -> 60
        }

        val label = (args["label"] as? String)?.trim() ?: (args["message"] as? String)?.trim() ?: "Mini Robot Timer"
        val skipUi = (args["skip_ui"] as? Boolean) ?: true

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
            if (label.isNotBlank()) {
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
            }
            putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)

            val readableDuration = when {
                durationSeconds >= 60 && durationSeconds % 60 == 0 -> {
                    val mins = durationSeconds / 60
                    "$mins " + if (mins == 1) "minute" else "minutes"
                }
                durationSeconds > 60 -> {
                    val mins = durationSeconds / 60
                    val secs = durationSeconds % 60
                    "$mins min $secs sec"
                }
                else -> "$durationSeconds seconds"
            }

            val msg = "Timer set for $readableDuration."
            ToolExecutionResult(
                success = true,
                message = msg,
                speakableResponse = msg,
                data = mapOf("duration_seconds" to durationSeconds, "label" to label)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Failed to set timer: ${e.localizedMessage}",
                speakableResponse = "I couldn't set the timer."
            )
        }
    }
}

/**
 * Tool to open specific Android device settings screens.
 */
class OpenSettingsDetailTool(private val context: Context) : AgentTool {
    override val name: String = "open_settings_detail"
    override val description: String = "Opens specific device settings. Arguments: setting_type (String: wifi, bluetooth, display, sound, apps, battery, accessibility, general)."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val settingType = ((args["setting_type"] as? String) ?: "general").lowercase().trim()

        val (action, displayName) = when (settingType) {
            "wifi", "wi-fi", "internet", "network" -> Pair(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
            "bluetooth" -> Pair(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
            "display", "brightness", "screen" -> Pair(Settings.ACTION_DISPLAY_SETTINGS, "Display Settings")
            "sound", "volume", "audio" -> Pair(Settings.ACTION_SOUND_SETTINGS, "Sound Settings")
            "apps", "applications" -> Pair(Settings.ACTION_APPLICATION_SETTINGS, "Apps Settings")
            "battery", "power" -> Pair(Intent.ACTION_POWER_USAGE_SUMMARY, "Battery Settings")
            "accessibility" -> Pair(Settings.ACTION_ACCESSIBILITY_SETTINGS, "Accessibility Settings")
            else -> Pair(Settings.ACTION_SETTINGS, "System Settings")
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(
                success = true,
                message = "Opened $displayName",
                speakableResponse = "Opening $displayName."
            )
        } catch (e: Exception) {
            // Fall back to general settings if specific action fails
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ToolExecutionResult(
                    success = true,
                    message = "Opened Settings",
                    speakableResponse = "Opening device settings."
                )
            } catch (e2: Exception) {
                ToolExecutionResult(
                    success = false,
                    message = "Failed to open settings: ${e2.localizedMessage}",
                    speakableResponse = "I could not open settings."
                )
            }
        }
    }
}
