package com.minirobot.assistant

import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotMood
import com.minirobot.assistant.robot.RobotViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RobotViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RobotViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RobotViewModel()
        viewModel.updatePlayfieldBounds(1080f, 1920f)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        assertEquals(RobotActivityState.IDLE, viewModel.activityState)
        assertFalse(viewModel.assistantState.isSheetVisible)
        assertFalse(viewModel.assistantState.isListening)
        assertEquals("How can I help you?", viewModel.assistantState.greetingText)
    }

    @Test
    fun testOnRobotTappedOpensAssistant() {
        viewModel.onRobotTapped()

        assertEquals(RobotActivityState.TAPPED, viewModel.activityState)
        assertTrue(viewModel.assistantState.isSheetVisible)
        assertEquals("How can I help you?", viewModel.assistantState.greetingText)
        assertEquals(RobotMood.HAPPY, viewModel.mood)
        assertTrue(viewModel.isWaving)
    }

    @Test
    fun testDismissAssistantResumesIdle() {
        viewModel.onRobotTapped()
        assertTrue(viewModel.assistantState.isSheetVisible)

        viewModel.dismissAssistant()
        assertFalse(viewModel.assistantState.isSheetVisible)
        assertEquals(RobotActivityState.IDLE, viewModel.activityState)
        assertFalse(viewModel.isWaving)
    }

    @Test
    fun testMicrophoneTogglePlaceholder() {
        viewModel.onRobotTapped()
        assertFalse(viewModel.assistantState.isListening)

        viewModel.onMicrophoneClicked()
        assertTrue(viewModel.assistantState.isListening)
        assertEquals(RobotMood.THINKING, viewModel.mood)

        viewModel.onMicrophoneClicked()
        assertFalse(viewModel.assistantState.isListening)
        assertEquals(RobotMood.HAPPY, viewModel.mood)
    }

    @Test
    fun testQuickActions() {
        viewModel.onQuickActionSelected("DANCE")
        assertEquals(RobotActivityState.SUCCESS, viewModel.activityState)
        assertEquals(RobotMood.PLAYFUL, viewModel.mood)

        viewModel.onQuickActionSelected("GREET")
        assertEquals(RobotMood.HAPPY, viewModel.mood)
        assertTrue(viewModel.isWaving)
    }

    @Test
    fun testPlayfieldTapMovesRobot() {
        viewModel.onPlayfieldTapped(500f, 800f)
        assertEquals(RobotActivityState.WALKING, viewModel.activityState)
        assertEquals(500f, viewModel.targetX, 0.01f)
        assertEquals(800f, viewModel.targetY, 0.01f)
    }
}
