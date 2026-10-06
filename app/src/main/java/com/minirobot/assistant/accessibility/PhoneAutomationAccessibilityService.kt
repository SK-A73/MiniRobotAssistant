package com.minirobot.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Dedicated accessibility service for authorized on-device automation.
 * Supports navigating back, going home, clicking UI elements, typing text, and scrolling.
 * Isolated from the robot UI layer for safety and modularity.
 */
class PhoneAutomationAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "PhoneAutomationAccessibilityService connected and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can track active window/app changes if needed for contextual AI
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        Log.i(TAG, "PhoneAutomationAccessibilityService destroyed")
    }

    /**
     * Executes the system Back action.
     */
    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Executes the system Home action.
     */
    fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Opens the Recents / Overview screen.
     */
    fun performRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Pulls down the notifications shade.
     */
    fun performNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Finds and clicks an on-screen element matching the specified text.
     */
    fun clickElementByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (nodes.isNullOrEmpty()) return false

        for (node in nodes) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) {
                    val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                current = current.parent
            }
        }
        return false
    }

    /**
     * Types text into the currently focused input field.
     */
    fun typeTextIntoCurrentFocus(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    /**
     * Scrolls the current screen forward (down) or backward (up).
     */
    fun scrollScreen(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(root) ?: root

        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return scrollableNode.performAction(action)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    companion object {
        private const val TAG = "MiniRobotAccessibility"

        @Volatile
        var instance: PhoneAutomationAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null
    }
}
