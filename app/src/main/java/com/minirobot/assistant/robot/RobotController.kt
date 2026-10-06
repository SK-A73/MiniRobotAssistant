package com.minirobot.assistant.robot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotFacing
import com.minirobot.assistant.model.RobotMood
import com.minirobot.assistant.service.SafeBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Controller driving the robot companion's natural screen roaming, physics,
 * procedural animation parameters, and state transitions.
 * Engineered for high battery efficiency with low CPU idle consumption.
 */
class RobotController(
    private val scope: CoroutineScope,
    initialX: Float = 200f,
    initialY: Float = 400f
) {

    // --- Position & Movement ---
    var posX by mutableFloatStateOf(initialX)
        private set
    var posY by mutableFloatStateOf(initialY)
        private set

    var targetX by mutableFloatStateOf(initialX)
        private set
    var targetY by mutableFloatStateOf(initialY)
        private set

    var safeBounds by mutableStateOf<SafeBounds?>(null)
        private set

    // --- State & Emotional Mood ---
    var activityState by mutableStateOf(RobotActivityState.IDLE)
    var facing by mutableStateOf(RobotFacing.RIGHT)
    var mood by mutableStateOf(RobotMood.HAPPY)

    // --- Procedural Animation Parameters ---
    var walkCycleProgress by mutableFloatStateOf(0f)
        private set
    var lookProgress by mutableFloatStateOf(0f) // -1.0 (left) to 1.0 (right)
        private set
    var bounceOffset by mutableFloatStateOf(0f)
        private set
    var blinkProgress by mutableFloatStateOf(0f)
        private set

    var isPausedForUserDrag: Boolean = false

    private var movementJob: Job? = null
    private var dwellTimer = 0f
    private var dwellDuration = 3.0f // Natural pause between movements
    private var blinkTimer = 0f
    private var nextBlinkTime = 3.2f

    private val walkSpeed = 90f // pixels per second for cute, steady stroll

    init {
        startControllerLoop()
    }

    fun updateBounds(bounds: SafeBounds) {
        safeBounds = bounds
        posX = posX.coerceIn(bounds.minX, bounds.maxX)
        posY = posY.coerceIn(bounds.minY, bounds.maxY)
        targetX = posX
        targetY = posY
    }

    fun setPosition(x: Float, y: Float) {
        val bounds = safeBounds
        posX = if (bounds != null) x.coerceIn(bounds.minX, bounds.maxX) else x
        posY = if (bounds != null) y.coerceIn(bounds.minY, bounds.maxY) else y
        targetX = posX
        targetY = posY
    }

    private fun startControllerLoop() {
        movementJob?.cancel()
        movementJob = scope.launch {
            var lastTime = System.nanoTime()

            while (isActive) {
                val currentTime = System.nanoTime()
                val dt = ((currentTime - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.08f)
                lastTime = currentTime

                updateBlinking(dt)

                if (!isPausedForUserDrag) {
                    when (activityState) {
                        RobotActivityState.WALKING -> {
                            updateWalking(dt)
                            // 35 FPS tick while walking: battery-friendly yet completely smooth
                            delay(28L)
                        }
                        RobotActivityState.IDLE -> {
                            updateIdle(dt)
                            // Reduced tick while standing idle to conserve battery
                            delay(45L)
                        }
                        RobotActivityState.LOOKING_AROUND -> {
                            updateLookingAround(dt)
                            delay(45L)
                        }
                        RobotActivityState.TAPPED -> {
                            updateTapped(dt)
                            delay(35L)
                        }
                        RobotActivityState.LISTENING,
                        RobotActivityState.THINKING,
                        RobotActivityState.SPEAKING,
                        RobotActivityState.SUCCESS,
                        RobotActivityState.ERROR -> {
                            updateInteractiveStates(dt)
                            delay(45L)
                        }
                    }
                } else {
                    delay(50L)
                }
            }
        }
    }

    private fun updateBlinking(dt: Float) {
        blinkTimer += dt
        if (blinkTimer >= nextBlinkTime) {
            val elapsed = blinkTimer - nextBlinkTime
            if (elapsed < 0.16f) {
                blinkProgress = sin((elapsed / 0.16f) * Math.PI.toFloat())
            } else {
                blinkProgress = 0f
                blinkTimer = 0f
                nextBlinkTime = Random.nextFloat() * 3.5f + 2.5f
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
            // Decelerate / ease near destination
            val speedFactor = (distance / 40f).coerceIn(0.4f, 1.0f)
            val currentSpeed = walkSpeed * speedFactor
            val step = currentSpeed * dt

            val moveX = (dx / distance) * step
            val moveY = (dy / distance) * step

            posX += moveX
            posY += moveY

            // Correct facing direction corresponds strictly to movement
            if (abs(dx) > 1.5f) {
                facing = if (dx > 0) RobotFacing.RIGHT else RobotFacing.LEFT
            }

            // Stride & bounce
            walkCycleProgress = (walkCycleProgress + dt * 3.2f) % 1.0f
            bounceOffset = abs(sin(walkCycleProgress * Math.PI.toFloat() * 2f)) * 7.5f
            lookProgress = 0f
        } else {
            // Reached destination -> switch to IDLE with random dwell time
            posX = targetX
            posY = targetY
            activityState = RobotActivityState.IDLE
            walkCycleProgress = 0f
            bounceOffset = 0f
            dwellTimer = 0f
            // Random pause between 2.5s and 5.0s
            dwellDuration = Random.nextFloat() * 2.5f + 2.5f
        }
    }

    private fun updateIdle(dt: Float) {
        dwellTimer += dt
        walkCycleProgress = 0f
        lookProgress = 0f

        // Gentle breathing bob
        bounceOffset = sin(dwellTimer * 2.8f) * 2.8f

        if (dwellTimer >= dwellDuration) {
            dwellTimer = 0f
            // 60% chance to look around, 40% chance to walk directly
            if (Random.nextFloat() < 0.6f) {
                activityState = RobotActivityState.LOOKING_AROUND
                mood = RobotMood.CURIOUS
            } else {
                pickNaturalDestination()
            }
        }
    }

    private fun updateLookingAround(dt: Float) {
        dwellTimer += dt
        walkCycleProgress = 0f
        bounceOffset = sin(dwellTimer * 2.2f) * 1.8f

        when {
            dwellTimer < 1.1f -> {
                lookProgress = -0.85f // Look left
            }
            dwellTimer < 1.7f -> {
                lookProgress = 0f     // Look center
            }
            dwellTimer < 2.8f -> {
                lookProgress = 0.85f  // Look right
            }
            else -> {
                lookProgress = 0f
                dwellTimer = 0f
                mood = RobotMood.HAPPY
                pickNaturalDestination()
            }
        }
    }

    private fun updateTapped(dt: Float) {
        dwellTimer += dt
        bounceOffset = abs(sin(dwellTimer * 8f)) * 12f
        walkCycleProgress = (dwellTimer * 3f) % 1.0f
        if (dwellTimer > 1.2f) {
            dwellTimer = 0f
            activityState = RobotActivityState.IDLE
        }
    }

    private fun updateInteractiveStates(dt: Float) {
        dwellTimer += dt
        bounceOffset = sin(dwellTimer * 3f) * 2.5f
        walkCycleProgress = 0f
        lookProgress = 0f
    }

    fun pickNaturalDestination() {
        val bounds = safeBounds ?: return
        targetX = Random.nextFloat() * (bounds.maxX - bounds.minX) + bounds.minX
        targetY = Random.nextFloat() * (bounds.maxY - bounds.minY) + bounds.minY
        activityState = RobotActivityState.WALKING
    }

    fun walkTo(x: Float, y: Float) {
        val bounds = safeBounds
        targetX = if (bounds != null) x.coerceIn(bounds.minX, bounds.maxX) else x
        targetY = if (bounds != null) y.coerceIn(bounds.minY, bounds.maxY) else y
        activityState = RobotActivityState.WALKING
    }

    fun triggerTapReaction() {
        activityState = RobotActivityState.TAPPED
        mood = RobotMood.HAPPY
        dwellTimer = 0f
        targetX = posX
        targetY = posY
    }

    fun destroy() {
        movementJob?.cancel()
    }
}
