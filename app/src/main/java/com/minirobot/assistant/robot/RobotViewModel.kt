package com.minirobot.assistant.robot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minirobot.assistant.model.AssistantUiState
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotFacing
import com.minirobot.assistant.model.RobotMood
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Main ViewModel driving the cute robot companion's behavior, roaming loop,
 * physics, animations, and assistant dialogue.
 */
class RobotViewModel : ViewModel() {

    // --- Screen and Playfield Boundaries ---
    var playfieldWidth by mutableFloatStateOf(0f)
        private set
    var playfieldHeight by mutableFloatStateOf(0f)
        private set

    // --- Robot Transform & State ---
    var posX by mutableFloatStateOf(200f)
        private set
    var posY by mutableFloatStateOf(400f)
        private set

    var targetX by mutableFloatStateOf(200f)
        private set
    var targetY by mutableFloatStateOf(400f)
        private set

    var activityState by mutableStateOf(RobotActivityState.IDLE)
        private set
    var facing by mutableStateOf(RobotFacing.RIGHT)
        private set
    var mood by mutableStateOf(RobotMood.HAPPY)
        private set

    // --- Procedural Animation Drivers ---
    var walkCycleProgress by mutableFloatStateOf(0f)
        private set
    var lookProgress by mutableFloatStateOf(0f) // -1.0 (left) to +1.0 (right)
        private set
    var bounceOffset by mutableFloatStateOf(0f) // Vertical bounce in pixels
        private set
    var blinkProgress by mutableFloatStateOf(0f) // 0.0 (open) to 1.0 (closed)
        private set
    var isWaving by mutableStateOf(false)
        private set

    // --- Assistant State ---
    var assistantState by mutableStateOf(AssistantUiState())
        private set

    // --- Internal Loop Control ---
    private var roamingJob: Job? = null
    private var speechBubbleJob: Job? = null
    private var stateTimer = 0f
    private var idleDwellDuration = 2.5f
    private var blinkTimer = 0f
    private var nextBlinkTime = 2.8f

    // Margins so the robot stays comfortably inside screen bounds
    private val marginHorizontal = 60f
    private val marginTop = 120f
    private val marginBottom = 140f
    private val walkSpeed = 95f // pixels per second

    init {
        startCompanionLoop()
    }

    /**
     * Called when the composable layout measures its dimensions.
     */
    fun updatePlayfieldBounds(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return

        val boundsChanged = (playfieldWidth != width || playfieldHeight != height)
        playfieldWidth = width
        playfieldHeight = height

        if (boundsChanged) {
            // Keep robot safely within bounds
            posX = posX.coerceIn(marginHorizontal, (width - marginHorizontal).coerceAtLeast(marginHorizontal))
            posY = posY.coerceIn(marginTop, (height - marginBottom).coerceAtLeast(marginTop))
            targetX = posX
            targetY = posY
        }
    }

    /**
     * Main animation and behavior game loop running at ~60fps.
     */
    private fun startCompanionLoop() {
        roamingJob?.cancel()
        roamingJob = viewModelScope.launch {
            var lastTime = System.nanoTime()

            while (isActive) {
                val currentTime = System.nanoTime()
                val dt = ((currentTime - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastTime = currentTime

                updateBlinking(dt)

                when (activityState) {
                    RobotActivityState.WALKING -> updateWalking(dt)
                    RobotActivityState.IDLE -> updateIdle(dt)
                    RobotActivityState.LOOKING_AROUND -> updateLookingAround(dt)
                    RobotActivityState.SUCCESS -> updateDancing(dt)
                    RobotActivityState.TAPPED -> updateInteracting(dt)
                    else -> updateIdle(dt)
                }

                delay(16L)
            }
        }
    }

    private fun updateBlinking(dt: Float) {
        blinkTimer += dt
        if (blinkTimer >= nextBlinkTime) {
            // Quick 150ms blink
            val blinkElapsed = blinkTimer - nextBlinkTime
            if (blinkElapsed < 0.15f) {
                blinkProgress = sin((blinkElapsed / 0.15f) * Math.PI.toFloat())
            } else {
                blinkProgress = 0f
                blinkTimer = 0f
                nextBlinkTime = Random.nextFloat() * 3f + 2.0f // Blink every 2-5s
            }
        } else {
            blinkProgress = 0f
        }
    }

    private fun updateWalking(dt: Float) {
        val dx = targetX - posX
        val dy = targetY - posY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > 6f) {
            val step = walkSpeed * dt
            val moveX = (dx / distance) * step
            val moveY = (dy / distance) * step

            posX += moveX
            posY += moveY

            // Orientation
            if (abs(dx) > 2f) {
                facing = if (dx > 0) RobotFacing.RIGHT else RobotFacing.LEFT
            }

            // Walking stride & bounce cycle
            walkCycleProgress = (walkCycleProgress + dt * 3.5f) % 1.0f
            bounceOffset = abs(sin(walkCycleProgress * Math.PI.toFloat() * 2f)) * 7f
            lookProgress = 0f
        } else {
            // Reached destination -> switch to idle standing
            posX = targetX
            posY = targetY
            activityState = RobotActivityState.IDLE
            walkCycleProgress = 0f
            bounceOffset = 0f
            stateTimer = 0f
            idleDwellDuration = Random.nextFloat() * 2.0f + 1.8f // 1.8s - 3.8s
        }
    }

    private fun updateIdle(dt: Float) {
        stateTimer += dt
        walkCycleProgress = 0f
        lookProgress = 0f

        // Subtle gentle breathing motion
        bounceOffset = sin(stateTimer * 3.0f) * 2.5f

        if (stateTimer >= idleDwellDuration) {
            stateTimer = 0f
            // 65% chance to look around, 35% chance to start walking immediately
            if (Random.nextFloat() < 0.65f) {
                activityState = RobotActivityState.LOOKING_AROUND
                mood = RobotMood.CURIOUS
            } else {
                pickNewDestination()
            }
        }
    }

    private fun updateLookingAround(dt: Float) {
        stateTimer += dt
        walkCycleProgress = 0f
        bounceOffset = sin(stateTimer * 2.0f) * 1.5f

        // Phase 1: Look left (0.0s - 1.0s)
        // Phase 2: Look center (1.0s - 1.5s)
        // Phase 3: Look right (1.5s - 2.5s)
        when {
            stateTimer < 1.0f -> {
                lookProgress = -0.85f
            }
            stateTimer < 1.5f -> {
                lookProgress = 0f
            }
            stateTimer < 2.5f -> {
                lookProgress = 0.85f
            }
            else -> {
                lookProgress = 0f
                stateTimer = 0f
                mood = RobotMood.HAPPY
                pickNewDestination()
            }
        }
    }

    private fun updateDancing(dt: Float) {
        stateTimer += dt
        // Energetic happy dance jumps
        bounceOffset = abs(sin(stateTimer * 12.0f)) * 14f
        walkCycleProgress = (stateTimer * 4f) % 1.0f
        isWaving = true

        // Wiggle head gaze
        lookProgress = sin(stateTimer * 8.0f) * 0.7f

        if (stateTimer > 2.8f) {
            stateTimer = 0f
            isWaving = false
            lookProgress = 0f
            mood = RobotMood.HAPPY
            activityState = RobotActivityState.IDLE
            idleDwellDuration = 1.5f
        }
    }

    private fun updateInteracting(dt: Float) {
        stateTimer += dt
        // Attentive posture, facing user
        bounceOffset = sin(stateTimer * 3.0f) * 2.5f
        lookProgress = 0f
    }

    /**
     * Choose a new reachable coordinate within safe playfield bounds.
     */
    private fun pickNewDestination() {
        if (playfieldWidth <= 0f || playfieldHeight <= 0f) return

        val minX = marginHorizontal
        val maxX = (playfieldWidth - marginHorizontal).coerceAtLeast(minX + 20f)
        val minY = marginTop
        val maxY = (playfieldHeight - marginBottom).coerceAtLeast(minY + 20f)

        targetX = Random.nextFloat() * (maxX - minX) + minX
        targetY = Random.nextFloat() * (maxY - minY) + minY

        activityState = RobotActivityState.WALKING
    }

    // --- User Gestures & Assistant Interactions ---

    /**
     * User tapped directly on the robot!
     * Triggers a joyful reaction and summons the assistant UI.
     */
    fun onRobotTapped() {
        activityState = RobotActivityState.TAPPED
        mood = RobotMood.HAPPY
        isWaving = true
        bounceOffset = 12f // Joyful hop
        targetX = posX
        targetY = posY

        showSpeechBubble("Hello there! I'm here to help! 🤖", 3500L)

        assistantState = assistantState.copy(
            isSheetVisible = true,
            greetingText = "How can I help you?",
            statusMessage = "Tap the microphone to speak or choose a quick action below.",
            isListening = false,
            activeMood = RobotMood.HAPPY
        )

        // Wave for 1.2s then relax
        viewModelScope.launch {
            delay(1200L)
            if (activityState == RobotActivityState.TAPPED) {
                isWaving = false
            }
        }
    }

    /**
     * User tapped on empty ground: robot eagerly walks to that spot!
     */
    fun onPlayfieldTapped(x: Float, y: Float) {
        if (assistantState.isSheetVisible) {
            // Dismiss assistant if active
            dismissAssistant()
            return
        }

        val clampedX = x.coerceIn(marginHorizontal, (playfieldWidth - marginHorizontal).coerceAtLeast(marginHorizontal))
        val clampedY = y.coerceIn(marginTop, (playfieldHeight - marginBottom).coerceAtLeast(marginTop))

        targetX = clampedX
        targetY = clampedY
        activityState = RobotActivityState.WALKING
        mood = RobotMood.PLAYFUL
    }

    /**
     * Dismiss the assistant bottom sheet and resume companion roaming.
     */
    fun dismissAssistant() {
        assistantState = assistantState.copy(
            isSheetVisible = false,
            isListening = false
        )
        activityState = RobotActivityState.IDLE
        idleDwellDuration = 1.0f
        stateTimer = 0f
        isWaving = false
    }

    /**
     * Toggle the microphone placeholder.
     */
    fun onMicrophoneClicked() {
        val newListeningState = !assistantState.isListening
        assistantState = if (newListeningState) {
            mood = RobotMood.THINKING
            assistantState.copy(
                isListening = true,
                statusMessage = "Listening for your voice... (AI placeholder) 🎙️"
            )
        } else {
            mood = RobotMood.HAPPY
            showSpeechBubble("Voice received! AI processing placeholder ✨", 3000L)
            assistantState.copy(
                isListening = false,
                statusMessage = "Ready! Tap microphone again or choose a quick prompt."
            )
        }
    }

    /**
     * Quick action buttons for user interaction.
     */
    fun onQuickActionSelected(action: String) {
        when (action) {
            "GREET" -> {
                mood = RobotMood.HAPPY
                isWaving = true
                showSpeechBubble("Hello! Nice to meet you! 😊", 3500L)
                assistantState = assistantState.copy(
                    statusMessage = "Mini Robot: 'Hello! Nice to meet you!'"
                )
                viewModelScope.launch {
                    delay(1500L)
                    isWaving = false
                }
            }
            "DANCE" -> {
                mood = RobotMood.PLAYFUL
                activityState = RobotActivityState.SUCCESS
                stateTimer = 0f
                showSpeechBubble("Yay, dance time! 💃🕺", 3000L)
                assistantState = assistantState.copy(
                    statusMessage = "Mini Robot is doing a happy dance!"
                )
            }
            "STATUS" -> {
                mood = RobotMood.HAPPY
                showSpeechBubble("Power 100% • System OK! ⚡", 3500L)
                assistantState = assistantState.copy(
                    statusMessage = "Status: Online • Cute level: Max • Battery: 100%"
                )
            }
            "PHONE_CONTROL" -> {
                mood = RobotMood.THINKING
                showSpeechBubble("Phone control coming in next update! 📱", 4000L)
                assistantState = assistantState.copy(
                    statusMessage = "Phone automation & accessibility features are ready for integration."
                )
            }
        }
    }

    private fun showSpeechBubble(text: String, durationMs: Long) {
        speechBubbleJob?.cancel()
        assistantState = assistantState.copy(robotSpeechBubble = text)
        speechBubbleJob = viewModelScope.launch {
            delay(durationMs)
            assistantState = assistantState.copy(robotSpeechBubble = null)
        }
    }
}
