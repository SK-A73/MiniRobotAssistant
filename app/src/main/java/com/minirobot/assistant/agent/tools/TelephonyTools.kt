package com.minirobot.assistant.agent.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult

/**
 * Tool to initiate phone calls.
 * Sensitive action requiring confirmation.
 * Falls back safely to ACTION_DIAL if CALL_PHONE permission is not granted.
 */
class PhoneCallTool(private val context: Context) : AgentTool {
    override val name: String = "make_phone_call"
    override val description: String = "Initiates a phone call or opens the dialer with a phone number. Arguments: phone_number (String), contact_name (String, optional)."
    override val isSensitive: Boolean = true

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val rawNumber = (args["phone_number"] as? String)?.trim() ?: ""
        val contactName = (args["contact_name"] as? String)?.trim() ?: ""

        val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            return ToolExecutionResult(
                success = false,
                message = "Phone number is empty or invalid.",
                speakableResponse = "I need a valid phone number to make a call."
            )
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val targetName = contactName.ifBlank { cleanNumber }

        return try {
            if (hasCallPermission) {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolExecutionResult(
                    success = true,
                    message = "Calling $targetName ($cleanNumber)",
                    speakableResponse = "Calling $targetName.",
                    data = mapOf("phone_number" to cleanNumber, "direct_call" to true)
                )
            } else {
                // Safe, permitted fallback: open dialer with number pre-filled
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ToolExecutionResult(
                    success = true,
                    message = "Opened dialer for $targetName ($cleanNumber)",
                    speakableResponse = "Opening the phone dialer for $targetName.",
                    data = mapOf("phone_number" to cleanNumber, "direct_call" to false)
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Failed to initiate call: ${e.localizedMessage}",
                speakableResponse = "I could not start the phone call."
            )
        }
    }
}

/**
 * Tool to prepare and compose SMS / text messages.
 * Sensitive action requiring confirmation.
 * Dispatches via ACTION_SENDTO with SMS URI and body.
 */
class SendMessageTool(private val context: Context) : AgentTool {
    override val name: String = "send_message"
    override val description: String = "Composes and sends an SMS or text message. Arguments: recipient (String), message (String), phone_number (String, optional)."
    override val isSensitive: Boolean = true

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val recipient = (args["recipient"] as? String)?.trim() ?: "Contact"
        val message = (args["message"] as? String)?.trim() ?: ""
        val phoneNumber = (args["phone_number"] as? String)?.trim() ?: ""

        if (message.isBlank()) {
            return ToolExecutionResult(
                success = false,
                message = "Message text is empty.",
                speakableResponse = "What message would you like to send?"
            )
        }

        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val targetUri = if (cleanNumber.isNotBlank()) {
            Uri.parse("smsto:$cleanNumber")
        } else {
            Uri.parse("smsto:")
        }

        return try {
            val intent = Intent(Intent.ACTION_SENDTO, targetUri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            ToolExecutionResult(
                success = true,
                message = "Message composed for $recipient: \"$message\"",
                speakableResponse = "Opening message for $recipient: $message",
                data = mapOf(
                    "recipient" to recipient,
                    "phone_number" to cleanNumber,
                    "message" to message
                )
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                message = "Failed to launch messaging app: ${e.localizedMessage}",
                speakableResponse = "I could not open the messaging app."
            )
        }
    }
}
