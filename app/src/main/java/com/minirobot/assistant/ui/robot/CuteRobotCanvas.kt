package com.minirobot.assistant.ui.robot

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotFacing
import com.minirobot.assistant.model.RobotMood
import com.minirobot.assistant.ui.theme.RobotAccentPurple
import com.minirobot.assistant.ui.theme.RobotBlushPink
import com.minirobot.assistant.ui.theme.RobotChassisDark
import com.minirobot.assistant.ui.theme.RobotChassisWhite
import com.minirobot.assistant.ui.theme.RobotCyan
import com.minirobot.assistant.ui.theme.RobotEyeGlow
import com.minirobot.assistant.ui.theme.RobotGreen
import com.minirobot.assistant.ui.theme.RobotSkyBlue
import com.minirobot.assistant.ui.theme.RobotVisorBlack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-quality vector rendering of the cute robot companion.
 * Proportioned for clear readability on high-density phone screens.
 * Visualizes states: IDLE, WALKING, LOOKING_AROUND, TAPPED, LISTENING, THINKING, SPEAKING, SUCCESS, ERROR.
 */
@Composable
fun CuteRobotCanvas(
    activityState: RobotActivityState,
    facing: RobotFacing,
    mood: RobotMood,
    walkCycleProgress: Float,
    lookProgress: Float,
    bounceOffset: Float,
    blinkProgress: Float,
    rmsAudioLevel: Float = 0f,
    modifier: Modifier = Modifier
) {
    val animatedLook by animateFloatAsState(
        targetValue = lookProgress,
        animationSpec = tween(durationMillis = 180),
        label = "animatedLook"
    )

    // Increased floating size for high visibility: 110dp x 132dp
    Box(
        modifier = modifier.size(width = 110.dp, height = 132.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f

            // Robot body offset based on bounce
            val robotYOffset = -bounceOffset

            // Reference coordinates for body torso
            val torsoTop = 48f + robotYOffset
            val torsoWidth = 54f
            val torsoHeight = 42f
            val torsoBottom = torsoTop + torsoHeight
            val torsoLeft = centerX - torsoWidth / 2f

            // Ground level where feet stand when not bouncing
            val legHeight = 16f
            val standingGroundY = 48f + torsoHeight - 3f + legHeight + 1.5f

            // --- 1. Soft Dynamic Ground Shadow ---
            val shadowFactor = (1f - (bounceOffset / 35f)).coerceIn(0.45f, 1.1f)
            val shadowWidth = 58f * shadowFactor
            val shadowHeight = 14f * shadowFactor
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x66000000),
                        Color(0x20000000),
                        Color.Transparent
                    ),
                    center = Offset(centerX, standingGroundY),
                    radius = shadowWidth / 2f
                ),
                topLeft = Offset(centerX - shadowWidth / 2f, standingGroundY - shadowHeight / 2f),
                size = Size(shadowWidth, shadowHeight)
            )

            // Flip horizontally according to movement facing direction
            val scaleX = if (facing == RobotFacing.LEFT) -1f else 1f

            scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(centerX, canvasH / 2f)) {
                // --- 2. Animated Legs & Boots (Connected directly to bottom of torso) ---
                drawEnhancedLegs(
                    centerX = centerX,
                    bodyBottom = torsoBottom,
                    activityState = activityState,
                    walkCycleProgress = walkCycleProgress,
                    legHeight = legHeight
                )

                // --- 3. Body Torso ---
                // Chassis body with metallic gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(RobotChassisWhite, Color(0xFFCBD5E1))
                    ),
                    topLeft = Offset(torsoLeft, torsoTop),
                    size = Size(torsoWidth, torsoHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // Torso belt segment
                drawRoundRect(
                    color = RobotChassisDark,
                    topLeft = Offset(torsoLeft + 8f, torsoTop + 28f),
                    size = Size(torsoWidth - 16f, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Power Core Glowing Heart / Arc Reactor
                val corePulse = when (activityState) {
                    RobotActivityState.THINKING -> (sin(walkCycleProgress * 10 * PI.toFloat()) * 0.3f + 0.85f).coerceIn(0.6f, 1.2f)
                    RobotActivityState.LISTENING -> (0.8f + rmsAudioLevel * 0.5f).coerceIn(0.8f, 1.3f)
                    else -> (sin(walkCycleProgress * 2 * PI.toFloat()) * 0.15f + 0.9f).coerceIn(0.75f, 1.05f)
                }

                val coreColor = when (activityState) {
                    RobotActivityState.SUCCESS -> RobotGreen
                    RobotActivityState.ERROR -> Color(0xFFEF4444)
                    RobotActivityState.LISTENING -> Color(0xFF38BDF8)
                    RobotActivityState.THINKING -> RobotAccentPurple
                    else -> RobotEyeGlow
                }

                val coreRadius = 7f * corePulse
                drawCircle(
                    color = coreColor.copy(alpha = 0.35f),
                    radius = coreRadius + 5f,
                    center = Offset(centerX, torsoTop + 15f)
                )
                drawCircle(
                    color = coreColor,
                    radius = coreRadius,
                    center = Offset(centerX, torsoTop + 15f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.4f,
                    center = Offset(centerX - 1.8f, torsoTop + 13.5f)
                )

                // --- 4. Animated Arms ---
                drawEnhancedArms(
                    centerX = centerX,
                    torsoTop = torsoTop,
                    activityState = activityState,
                    walkCycleProgress = walkCycleProgress
                )

                // --- 5. Neck Joint ---
                drawRoundRect(
                    color = RobotChassisDark,
                    topLeft = Offset(centerX - 12f, torsoTop - 6f),
                    size = Size(24f, 7f),
                    cornerRadius = CornerRadius(3.5f, 3.5f)
                )

                // --- 6. Head, Visor, Eyes, Ears & Antenna ---
                drawEnhancedHead(
                    centerX = centerX,
                    headTop = 8f + robotYOffset,
                    activityState = activityState,
                    mood = mood,
                    lookProgress = animatedLook,
                    blinkProgress = blinkProgress,
                    rmsAudioLevel = rmsAudioLevel
                )
            }
        }
    }
}

/**
 * Draws proportional legs and boots connected directly to the bottom of the robot body chassis,
 * with alternating walking strides and lifting.
 */
private fun DrawScope.drawEnhancedLegs(
    centerX: Float,
    bodyBottom: Float,
    activityState: RobotActivityState,
    walkCycleProgress: Float,
    legHeight: Float = 16f
) {
    val legWidth = 12f
    val bootHeight = 6f
    // Attach legs directly inside the bottom chassis (3px overlap) so they stay seamlessly attached
    val legAttachY = bodyBottom - 3f

    val leftLegOffsetX: Float
    val leftLegLiftY: Float
    val rightLegOffsetX: Float
    val rightLegLiftY: Float

    if (activityState == RobotActivityState.WALKING) {
        val stridePhase = walkCycleProgress * 2 * PI.toFloat()
        leftLegOffsetX = sin(stridePhase) * 9f
        leftLegLiftY = (-cos(stridePhase).coerceAtLeast(0f) * 6f)

        rightLegOffsetX = sin(stridePhase + PI.toFloat()) * 9f
        rightLegLiftY = (-cos(stridePhase + PI.toFloat()).coerceAtLeast(0f) * 6f)
    } else {
        leftLegOffsetX = 0f
        leftLegLiftY = 0f
        rightLegOffsetX = 0f
        rightLegLiftY = 0f
    }

    // Left Boot (Back leg)
    val leftLegX = centerX - 18f + leftLegOffsetX
    val leftLegTop = legAttachY + leftLegLiftY
    drawRoundRect(
        color = RobotChassisDark,
        topLeft = Offset(leftLegX, leftLegTop),
        size = Size(legWidth, legHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = RobotSkyBlue,
        topLeft = Offset(leftLegX - 2.5f, leftLegTop + legHeight - bootHeight),
        size = Size(legWidth + 5f, bootHeight),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Right Boot (Front leg)
    val rightLegX = centerX + 6f + rightLegOffsetX
    val rightLegTop = legAttachY + rightLegLiftY
    drawRoundRect(
        color = RobotChassisDark,
        topLeft = Offset(rightLegX, rightLegTop),
        size = Size(legWidth, legHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = RobotSkyBlue,
        topLeft = Offset(rightLegX - 2.5f, rightLegTop + legHeight - bootHeight),
        size = Size(legWidth + 5f, bootHeight),
        cornerRadius = CornerRadius(3f, 3f)
    )
}

/**
 * Draws arms with walking swings, waving, or attentive postures.
 */
private fun DrawScope.drawEnhancedArms(
    centerX: Float,
    torsoTop: Float,
    activityState: RobotActivityState,
    walkCycleProgress: Float
) {
    val armWidth = 10f
    val armHeight = 26f

    val isWaving = activityState == RobotActivityState.TAPPED ||
            activityState == RobotActivityState.SUCCESS

    // Left Arm
    val leftArmSwing = if (activityState == RobotActivityState.WALKING) {
        -sin(walkCycleProgress * 2 * PI.toFloat()) * 6f
    } else 0f

    drawRoundRect(
        color = RobotChassisDark,
        topLeft = Offset(centerX - 35f, torsoTop + 4f + leftArmSwing),
        size = Size(armWidth, armHeight),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawCircle(
        color = RobotChassisWhite,
        radius = 6f,
        center = Offset(centerX - 30f, torsoTop + 4f + armHeight + leftArmSwing)
    )

    // Right Arm
    if (isWaving) {
        val waveSwing = sin(walkCycleProgress * 8 * PI.toFloat()) * 5f
        val waveArmPath = Path().apply {
            moveTo(centerX + 26f, torsoTop + 6f)
            lineTo(centerX + 40f + waveSwing, torsoTop - 14f)
        }
        drawPath(
            path = waveArmPath,
            color = RobotChassisDark,
            style = Stroke(width = 10f, cap = StrokeCap.Round)
        )
        drawCircle(
            color = RobotEyeGlow,
            radius = 7.5f,
            center = Offset(centerX + 40f + waveSwing, torsoTop - 14f)
        )
    } else {
        val rightArmSwing = if (activityState == RobotActivityState.WALKING) {
            sin(walkCycleProgress * 2 * PI.toFloat()) * 6f
        } else 0f

        drawRoundRect(
            color = RobotChassisDark,
            topLeft = Offset(centerX + 25f, torsoTop + 4f + rightArmSwing),
            size = Size(armWidth, armHeight),
            cornerRadius = CornerRadius(5f, 5f)
        )
        drawCircle(
            color = RobotChassisWhite,
            radius = 6f,
            center = Offset(centerX + 30f, torsoTop + 4f + armHeight + rightArmSwing)
        )
    }
}

/**
 * Draws head, antenna, ears, glossy visor, blush, and expressive eyes/waveforms for all states.
 */
private fun DrawScope.drawEnhancedHead(
    centerX: Float,
    headTop: Float,
    activityState: RobotActivityState,
    mood: RobotMood,
    lookProgress: Float,
    blinkProgress: Float,
    rmsAudioLevel: Float
) {
    // --- Antenna ---
    val antennaStemTop = headTop - 14f
    drawLine(
        color = Color(0xFF64748B),
        start = Offset(centerX, headTop),
        end = Offset(centerX, antennaStemTop),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )

    // Antenna orb color adapts to state
    val antennaColor = when (activityState) {
        RobotActivityState.LISTENING -> Color(0xFF38BDF8)
        RobotActivityState.THINKING -> RobotAccentPurple
        RobotActivityState.SUCCESS -> RobotGreen
        RobotActivityState.ERROR -> Color(0xFFEF4444)
        RobotActivityState.SPEAKING -> RobotCyan
        else -> RobotSkyBlue
    }

    drawCircle(
        color = antennaColor.copy(alpha = 0.35f),
        radius = 9.5f,
        center = Offset(centerX, antennaStemTop)
    )
    drawCircle(
        color = antennaColor,
        radius = 6f,
        center = Offset(centerX, antennaStemTop)
    )
    drawCircle(
        color = Color.White,
        radius = 2.2f,
        center = Offset(centerX - 1.5f, antennaStemTop - 1.5f)
    )

    // --- Side Ear Bumpers / Audio Sensors ---
    val earRadius = 6f
    val earY = headTop + 20f
    val earColor = if (activityState == RobotActivityState.LISTENING) RobotCyan else RobotSkyBlue
    drawCircle(
        color = earColor,
        radius = earRadius,
        center = Offset(centerX - 35f, earY)
    )
    drawCircle(
        color = earColor,
        radius = earRadius,
        center = Offset(centerX + 35f, earY)
    )

    // --- Head Chassis ---
    val headW = 68f
    val headH = 44f
    val headLeft = centerX - headW / 2f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(RobotChassisWhite, Color(0xFFE2E8F0))
        ),
        topLeft = Offset(headLeft, headTop),
        size = Size(headW, headH),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // --- Glossy Black Visor Screen ---
    val visorW = 54f
    val visorH = 30f
    val visorLeft = centerX - visorW / 2f
    val visorTop = headTop + 7f
    drawRoundRect(
        color = RobotVisorBlack,
        topLeft = Offset(visorLeft, visorTop),
        size = Size(visorW, visorH),
        cornerRadius = CornerRadius(10f, 10f)
    )

    // Glass reflection gleam
    val glarePath = Path().apply {
        moveTo(visorLeft + 5f, visorTop + 4f)
        lineTo(visorLeft + 22f, visorTop + 4f)
        lineTo(visorLeft + 12f, visorTop + 14f)
        lineTo(visorLeft + 5f, visorTop + 14f)
        close()
    }
    drawPath(path = glarePath, color = Color.White.copy(alpha = 0.14f))

    // --- Expressive Eyes / State Visualizer ---
    val eyeLookShift = lookProgress * 5f
    val eyeCenterY = visorTop + 14f
    val leftEyeCenterX = visorLeft + 15f + eyeLookShift
    val rightEyeCenterX = visorLeft + 39f + eyeLookShift

    when (activityState) {
        RobotActivityState.LISTENING -> {
            // Audio visualizer bars on visor
            val barCount = 5
            val barSpacing = 7f
            val startX = centerX - (barCount - 1) * barSpacing / 2f
            for (i in 0 until barCount) {
                val waveHeight = 6f + ((rmsAudioLevel * 14f) * (0.5f + (i % 2) * 0.5f)).coerceIn(2f, 18f)
                drawLine(
                    color = RobotSkyBlue,
                    start = Offset(startX + i * barSpacing, eyeCenterY - waveHeight / 2f),
                    end = Offset(startX + i * barSpacing, eyeCenterY + waveHeight / 2f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }

        RobotActivityState.THINKING -> {
            // Rotating / pulsing thought dots
            val dotSpacing = 8f
            for (i in -1..1) {
                val dotRadius = 3f + (i * 0.8f)
                drawCircle(
                    color = RobotAccentPurple,
                    radius = dotRadius.coerceAtLeast(2f),
                    center = Offset(centerX + i * dotSpacing, eyeCenterY)
                )
            }
        }

        RobotActivityState.SPEAKING -> {
            // Cheerful speaking wave / smile pulse
            drawHappyEye(leftEyeCenterX, eyeCenterY)
            drawHappyEye(rightEyeCenterX, eyeCenterY)
            // Visor mouth wave
            val speechWave = Path().apply {
                moveTo(centerX - 6f, visorTop + 22f)
                quadraticTo(centerX, visorTop + 26f, centerX + 6f, visorTop + 22f)
            }
            drawPath(
                path = speechWave,
                color = RobotEyeGlow,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }

        RobotActivityState.SUCCESS, RobotActivityState.TAPPED -> {
            // Happy ^ ^ eyes
            drawHappyEye(leftEyeCenterX, eyeCenterY)
            drawHappyEye(rightEyeCenterX, eyeCenterY)
            // Smile
            val smile = Path().apply {
                moveTo(centerX - 5f, visorTop + 22f)
                quadraticTo(centerX, visorTop + 26f, centerX + 5f, visorTop + 22f)
            }
            drawPath(smile, color = RobotGreen, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        RobotActivityState.ERROR -> {
            // Concerned flat/cross eyes
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(leftEyeCenterX - 5f, eyeCenterY - 3f),
                end = Offset(leftEyeCenterX + 5f, eyeCenterY + 3f),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(leftEyeCenterX - 5f, eyeCenterY + 3f),
                end = Offset(leftEyeCenterX + 5f, eyeCenterY - 3f),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(rightEyeCenterX - 5f, eyeCenterY - 3f),
                end = Offset(rightEyeCenterX + 5f, eyeCenterY + 3f),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(rightEyeCenterX - 5f, eyeCenterY + 3f),
                end = Offset(rightEyeCenterX + 5f, eyeCenterY - 3f),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
        }

        else -> {
            // Normal blinking or oval eyes
            if (blinkProgress > 0.3f) {
                drawLine(
                    color = RobotEyeGlow,
                    start = Offset(leftEyeCenterX - 5f, eyeCenterY),
                    end = Offset(leftEyeCenterX + 5f, eyeCenterY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = RobotEyeGlow,
                    start = Offset(rightEyeCenterX - 5f, eyeCenterY),
                    end = Offset(rightEyeCenterX + 5f, eyeCenterY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            } else {
                val eyeW = 8.5f
                val eyeH = 11f
                drawOval(
                    color = RobotEyeGlow,
                    topLeft = Offset(leftEyeCenterX - eyeW / 2f, eyeCenterY - eyeH / 2f),
                    size = Size(eyeW, eyeH)
                )
                drawOval(
                    color = RobotEyeGlow,
                    topLeft = Offset(rightEyeCenterX - eyeW / 2f, eyeCenterY - eyeH / 2f),
                    size = Size(eyeW, eyeH)
                )

                // White catchlight sparkle dots
                drawCircle(
                    color = Color.White,
                    radius = 2f,
                    center = Offset(leftEyeCenterX - 1.2f, eyeCenterY - 3f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2f,
                    center = Offset(rightEyeCenterX - 1.2f, eyeCenterY - 3f)
                )
            }
        }
    }

    // --- Cute Pink Blush Cheeks ---
    drawOval(
        color = RobotBlushPink.copy(alpha = 0.55f),
        topLeft = Offset(visorLeft + 6f + eyeLookShift * 0.4f, visorTop + 21f),
        size = Size(8f, 5f)
    )
    drawOval(
        color = RobotBlushPink.copy(alpha = 0.55f),
        topLeft = Offset(visorLeft + 40f + eyeLookShift * 0.4f, visorTop + 21f),
        size = Size(8f, 5f)
    )
}

/**
 * Draws a cute joyful inverted curved eye (`^`).
 */
private fun DrawScope.drawHappyEye(centerX: Float, centerY: Float) {
    val eyePath = Path().apply {
        moveTo(centerX - 5.5f, centerY + 2.5f)
        quadraticTo(centerX, centerY - 4.5f, centerX + 5.5f, centerY + 2.5f)
    }
    drawPath(
        path = eyePath,
        color = RobotEyeGlow,
        style = Stroke(width = 3.2f, cap = StrokeCap.Round)
    )
}
