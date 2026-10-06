package com.minirobot.assistant.accessibility

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * Utility functions for checking and requesting Android Accessibility Service permissions.
 */
object AccessibilityUtils {

    /**
     * Checks whether the user has enabled our accessibility service in system settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (PhoneAutomationAccessibilityService.isServiceRunning()) {
            return true
        }

        val expectedServiceName = "${context.packageName}/${PhoneAutomationAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Opens the system Accessibility settings screen so the user can enable the service.
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
