package com.minirobot.assistant

import com.minirobot.assistant.agent.ai.LocalRuleBasedAiProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskAgentTest {

    private lateinit var aiProvider: LocalRuleBasedAiProvider

    @Before
    fun setup() {
        aiProvider = LocalRuleBasedAiProvider()
    }

    @Test
    fun testOpenYouTubeIntent() = runTest {
        val decision = aiProvider.decideIntent("Open YouTube", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("open_app", decision.toolCalls[0].toolName)
        assertEquals("YouTube", decision.toolCalls[0].arguments["app_name"])
        assertFalse(decision.requiresConfirmation)

        // Also test with trailing period
        val decisionWithPeriod = aiProvider.decideIntent("Open YouTube.", emptyList())
        assertEquals(1, decisionWithPeriod.toolCalls.size)
        assertEquals("open_app", decisionWithPeriod.toolCalls[0].toolName)
        assertEquals("YouTube", decisionWithPeriod.toolCalls[0].arguments["app_name"])
    }

    @Test
    fun testOpenChromeIntent() = runTest {
        val decision = aiProvider.decideIntent("Open Chrome", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("open_app", decision.toolCalls[0].toolName)
        assertEquals("Chrome", decision.toolCalls[0].arguments["app_name"])
        assertFalse(decision.requiresConfirmation)

        // Also test with trailing period
        val decisionWithPeriod = aiProvider.decideIntent("Open Chrome.", emptyList())
        assertEquals(1, decisionWithPeriod.toolCalls.size)
        assertEquals("open_app", decisionWithPeriod.toolCalls[0].toolName)
        assertEquals("Chrome", decisionWithPeriod.toolCalls[0].arguments["app_name"])
    }

    @Test
    fun testGoBackIntent() = runTest {
        val decision = aiProvider.decideIntent("Go back", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("go_back", decision.toolCalls[0].toolName)
        assertFalse(decision.requiresConfirmation)

        // Also test with trailing period
        val decisionWithPeriod = aiProvider.decideIntent("Go back.", emptyList())
        assertEquals(1, decisionWithPeriod.toolCalls.size)
        assertEquals("go_back", decisionWithPeriod.toolCalls[0].toolName)
    }

    @Test
    fun testGoHomeIntent() = runTest {
        val decision = aiProvider.decideIntent("Go home", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("go_home", decision.toolCalls[0].toolName)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testWebSearchIntent() = runTest {
        val decision = aiProvider.decideIntent("Search for C# tutorials", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("web_search", decision.toolCalls[0].toolName)
        assertEquals("C# tutorials", decision.toolCalls[0].arguments["query"])
        assertEquals("web", decision.toolCalls[0].arguments["platform"])
        assertFalse(decision.requiresConfirmation)

        // Also test with trailing period
        val decisionWithPeriod = aiProvider.decideIntent("Search for C# tutorials.", emptyList())
        assertEquals(1, decisionWithPeriod.toolCalls.size)
        assertEquals("web_search", decisionWithPeriod.toolCalls[0].toolName)
        assertEquals("C# tutorials", decisionWithPeriod.toolCalls[0].arguments["query"])
    }

    @Test
    fun testYouTubeSearchIntent() = runTest {
        val decision = aiProvider.decideIntent("Search YouTube for C# tutorials", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("web_search", decision.toolCalls[0].toolName)
        assertEquals("C# tutorials", decision.toolCalls[0].arguments["query"])
        assertEquals("youtube", decision.toolCalls[0].arguments["platform"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testScrollDownIntent() = runTest {
        val decision = aiProvider.decideIntent("Scroll down", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("scroll_screen", decision.toolCalls[0].toolName)
        assertEquals("down", decision.toolCalls[0].arguments["direction"])
    }

    @Test
    fun testBatteryStatusIntent() = runTest {
        val decision = aiProvider.decideIntent("Check battery", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("battery_status", decision.toolCalls[0].toolName)
    }

    @Test
    fun testSensitiveActionRequiresConfirmation() = runTest {
        val decision = aiProvider.decideIntent("Send message to John saying hello", emptyList())
        assertTrue(decision.requiresConfirmation)
        assertTrue(decision.confirmationPrompt != null)
    }

    @Test
    fun testMultiStepOpenAndSearchIntent() = runTest {
        val query = "Open YouTube and search for beginner C# tutorials"
        val decision = aiProvider.decideIntent(query, emptyList())
        assertEquals(2, decision.planSteps.size)
        assertEquals(2, decision.toolCalls.size)

        // Step 1: Open YouTube
        assertEquals("open_app", decision.planSteps[0].toolName)
        assertEquals("YouTube", decision.planSteps[0].arguments["app_name"])

        // Step 2: Search for beginner C# tutorials on YouTube
        assertEquals("web_search", decision.planSteps[1].toolName)
        assertEquals("beginner C# tutorials", decision.planSteps[1].arguments["query"])
        assertEquals("youtube", decision.planSteps[1].arguments["platform"])

        val plan = decision.toTaskPlan(query)
        assertEquals(2, plan.steps.size)
        assertEquals("Open YouTube", plan.steps[0].description)
        assertFalse(plan.requiresConfirmation)

        // Also test with trailing period
        val decisionWithPeriod = aiProvider.decideIntent("Open YouTube and search for beginner C# tutorials.", emptyList())
        assertEquals(2, decisionWithPeriod.planSteps.size)
        assertEquals("beginner C# tutorials", decisionWithPeriod.planSteps[1].arguments["query"])
    }

    @Test
    fun testTapAccessibleElementIntent() = runTest {
        val decision = aiProvider.decideIntent("Tap subscribe", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("tap_accessible_element", decision.planSteps[0].toolName)
        assertEquals("subscribe", decision.planSteps[0].arguments["text"])
    }

    @Test
    fun testTypeTextIntent() = runTest {
        val decision = aiProvider.decideIntent("Type hello world", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("type_text", decision.planSteps[0].toolName)
        assertEquals("hello world", decision.planSteps[0].arguments["text"])
    }

    @Test
    fun testConversationalFallback() = runTest {
        val decision = aiProvider.decideIntent("Hello there", emptyList())
        assertTrue(decision.toolCalls.isEmpty())
        assertTrue(decision.planSteps.isEmpty())
        assertTrue(decision.responseText.contains("Hello"))
    }

    @Test
    fun testFlashlightOnIntent() = runTest {
        val decision = aiProvider.decideIntent("Turn on flashlight", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("flashlight", decision.planSteps[0].toolName)
        assertEquals(true, decision.planSteps[0].arguments["state"])
        assertEquals(0, decision.planSteps[0].arguments["duration_seconds"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testFlashlightOffIntent() = runTest {
        val decision = aiProvider.decideIntent("Turn off flashlight", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("flashlight", decision.planSteps[0].toolName)
        assertEquals(false, decision.planSteps[0].arguments["state"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testFlashlightTimedIntent() = runTest {
        val decision = aiProvider.decideIntent("Turn on flashlight for 2 minutes", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("flashlight", decision.planSteps[0].toolName)
        assertEquals(true, decision.planSteps[0].arguments["state"])
        assertEquals(120, decision.planSteps[0].arguments["duration_seconds"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testSetAlarmIntent() = runTest {
        val decision = aiProvider.decideIntent("Set alarm for 7:30 am", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("set_alarm", decision.planSteps[0].toolName)
        assertEquals(7, decision.planSteps[0].arguments["hour"])
        assertEquals(30, decision.planSteps[0].arguments["minutes"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testSetAlarmPmWithLabelIntent() = runTest {
        val decision = aiProvider.decideIntent("Set an alarm at 8:00 pm with label Workout", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("set_alarm", decision.planSteps[0].toolName)
        assertEquals(20, decision.planSteps[0].arguments["hour"])
        assertEquals(0, decision.planSteps[0].arguments["minutes"])
        assertEquals("Workout", decision.planSteps[0].arguments["label"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testSetTimerIntent() = runTest {
        val decision = aiProvider.decideIntent("Set a timer for 5 minutes", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("set_timer", decision.planSteps[0].toolName)
        assertEquals(300, decision.planSteps[0].arguments["duration_seconds"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testSetTimerSecondsIntent() = runTest {
        val decision = aiProvider.decideIntent("Timer for 30 seconds", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("set_timer", decision.planSteps[0].toolName)
        assertEquals(30, decision.planSteps[0].arguments["duration_seconds"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testCallContactRequiresConfirmation() = runTest {
        val decision = aiProvider.decideIntent("Call Alice", emptyList())
        assertEquals(2, decision.planSteps.size)
        assertEquals("lookup_contact", decision.planSteps[0].toolName)
        assertEquals("Alice", decision.planSteps[0].arguments["name"])
        assertEquals("make_phone_call", decision.planSteps[1].toolName)
        assertTrue(decision.requiresConfirmation)
        assertTrue(decision.confirmationPrompt?.contains("Alice") == true)
    }

    @Test
    fun testCallDirectNumberRequiresConfirmation() = runTest {
        val decision = aiProvider.decideIntent("Call 1234567890", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("make_phone_call", decision.planSteps[0].toolName)
        assertEquals("1234567890", decision.planSteps[0].arguments["phone_number"])
        assertTrue(decision.requiresConfirmation)
    }

    @Test
    fun testSendApologyMessageGeneratesTextAndRequiresConfirmation() = runTest {
        val decision = aiProvider.decideIntent("Send an apology message to Mom", emptyList())
        assertEquals(2, decision.planSteps.size)
        assertEquals("lookup_contact", decision.planSteps[0].toolName)
        assertEquals("Mom", decision.planSteps[0].arguments["name"])
        assertEquals("send_message", decision.planSteps[1].toolName)
        val msg = decision.planSteps[1].arguments["message"] as String
        assertTrue(msg.contains("sorry", ignoreCase = true))
        assertTrue(decision.requiresConfirmation)
    }

    @Test
    fun testSendCustomMessageRequiresConfirmation() = runTest {
        val decision = aiProvider.decideIntent("Send message to John saying I will be late", emptyList())
        assertEquals(2, decision.planSteps.size)
        assertEquals("lookup_contact", decision.planSteps[0].toolName)
        assertEquals("John", decision.planSteps[0].arguments["name"])
        assertEquals("send_message", decision.planSteps[1].toolName)
        assertEquals("I will be late", decision.planSteps[1].arguments["message"])
        assertTrue(decision.requiresConfirmation)
    }

    @Test
    fun testOpenWifiSettingsIntent() = runTest {
        val decision = aiProvider.decideIntent("Open wifi settings", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("open_settings_detail", decision.planSteps[0].toolName)
        assertEquals("wifi", decision.planSteps[0].arguments["setting_type"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testOpenBluetoothSettingsIntent() = runTest {
        val decision = aiProvider.decideIntent("Open bluetooth settings", emptyList())
        assertEquals(1, decision.planSteps.size)
        assertEquals("open_settings_detail", decision.planSteps[0].toolName)
        assertEquals("bluetooth", decision.planSteps[0].arguments["setting_type"])
        assertFalse(decision.requiresConfirmation)
    }
}
