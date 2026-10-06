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
    }

    @Test
    fun testOpenChromeIntent() = runTest {
        val decision = aiProvider.decideIntent("Open Chrome", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("open_app", decision.toolCalls[0].toolName)
        assertEquals("Chrome", decision.toolCalls[0].arguments["app_name"])
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun testGoBackIntent() = runTest {
        val decision = aiProvider.decideIntent("Go back", emptyList())
        assertEquals(1, decision.toolCalls.size)
        assertEquals("go_back", decision.toolCalls[0].toolName)
        assertFalse(decision.requiresConfirmation)
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
    fun testConversationalFallback() = runTest {
        val decision = aiProvider.decideIntent("Hello there", emptyList())
        assertTrue(decision.toolCalls.isEmpty())
        assertTrue(decision.responseText.contains("Hello"))
    }
}
