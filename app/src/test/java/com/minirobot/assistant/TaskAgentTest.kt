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
}
