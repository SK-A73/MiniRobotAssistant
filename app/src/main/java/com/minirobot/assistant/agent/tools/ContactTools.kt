package com.minirobot.assistant.agent.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.minirobot.assistant.agent.AgentTool
import com.minirobot.assistant.agent.ToolExecutionResult

data class ContactMatch(
    val name: String,
    val number: String,
    val type: String
)

/**
 * Tool to resolve contact details from the phone's address book.
 * Transparently disambiguates if multiple matches are found.
 */
class ContactLookupTool(private val context: Context) : AgentTool {
    override val name: String = "lookup_contact"
    override val description: String = "Searches device contacts by name. Returns matching name and phone number. Argument: name (String)."
    override val isSensitive: Boolean = false

    override suspend fun execute(args: Map<String, Any?>): ToolExecutionResult {
        val queryName = (args["name"] as? String)?.trim() ?: ""
        if (queryName.isBlank()) {
            return ToolExecutionResult(
                success = false,
                message = "Contact name query is empty.",
                speakableResponse = "Whom would you like me to look up?"
            )
        }

        // Verify READ_CONTACTS permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return ToolExecutionResult(
                success = false,
                message = "Contacts permission (READ_CONTACTS) is not granted.",
                speakableResponse = "Please allow contacts permission in the Mini Robot settings to find contacts."
            )
        }

        val matches = mutableListOf<ContactMatch>()
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$queryName%")

            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

                while (c.moveToNext()) {
                    val name = if (nameIdx != -1) c.getString(nameIdx) ?: "" else ""
                    val number = if (numberIdx != -1) c.getString(numberIdx) ?: "" else ""
                    val typeInt = if (typeIdx != -1) c.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                    val typeStr = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        typeInt,
                        "Mobile"
                    ).toString()

                    if (number.isNotBlank()) {
                        matches.add(ContactMatch(name = name, number = number, type = typeStr))
                    }
                }
            }
        } catch (e: Exception) {
            return ToolExecutionResult(
                success = false,
                message = "Error querying contacts: ${e.localizedMessage}",
                speakableResponse = "I could not access contacts right now."
            )
        }

        // Deduplicate distinct contact names / numbers
        val distinctMatches = matches.distinctBy { it.name.lowercase() to it.number.replace(Regex("[^0-9+]"), "") }

        return when {
            distinctMatches.isEmpty() -> {
                ToolExecutionResult(
                    success = false,
                    message = "No contacts found matching \"$queryName\".",
                    speakableResponse = "I couldn't find any contact named $queryName."
                )
            }
            distinctMatches.size == 1 -> {
                val match = distinctMatches.first()
                ToolExecutionResult(
                    success = true,
                    message = "Found ${match.name}: ${match.number}",
                    speakableResponse = "Found ${match.name}.",
                    data = mapOf(
                        "name" to match.name,
                        "number" to match.number,
                        "type" to match.type
                    )
                )
            }
            else -> {
                // Multiple matches found - prompt clarification
                val namesList = distinctMatches.map { it.name }.distinct().take(4).joinToString(", ")
                val promptMsg = "I found multiple contacts for \"$queryName\": $namesList. Which one would you like?"
                ToolExecutionResult(
                    success = true,
                    message = promptMsg,
                    speakableResponse = promptMsg,
                    data = mapOf(
                        "multiple_matches" to true,
                        "matches" to distinctMatches.map { mapOf("name" to it.name, "number" to it.number) }
                    )
                )
            }
        }
    }
}
