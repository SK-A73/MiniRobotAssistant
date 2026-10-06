package com.minirobot.assistant.ui.robot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minirobot.assistant.robot.RobotViewModel
import kotlin.math.roundToInt

/**
 * Interactive playfield where the robot continuously roams.
 * Detects touches to direct the robot or tap the robot to summon the assistant.
 */
@Composable
fun RobotPlayfield(
    viewModel: RobotViewModel,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val halfWidthPx = with(density) { 48.dp.toPx() }
    val halfHeightPx = with(density) { 59.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                val size = layoutCoordinates.size
                viewModel.updatePlayfieldBounds(
                    width = size.width.toFloat(),
                    height = size.height.toFloat()
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    viewModel.onPlayfieldTapped(offset.x, offset.y)
                }
            }
    ) {
        // --- Robot Companion & Speech Bubble ---
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (viewModel.posX - halfWidthPx).roundToInt(),
                        y = (viewModel.posY - halfHeightPx).roundToInt()
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Speech Bubble above the robot
                AnimatedVisibility(
                    visible = viewModel.assistantState.robotSpeechBubble != null,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.8f)
                ) {
                    val speechText = viewModel.assistantState.robotSpeechBubble ?: ""
                    Box(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = speechText,
                            color = Color(0xFFF8FAFC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // The cute robot character itself
                CuteRobotCanvas(
                    activityState = viewModel.activityState,
                    facing = viewModel.facing,
                    mood = viewModel.mood,
                    walkCycleProgress = viewModel.walkCycleProgress,
                    lookProgress = viewModel.lookProgress,
                    bounceOffset = viewModel.bounceOffset,
                    blinkProgress = viewModel.blinkProgress,
                    rmsAudioLevel = viewModel.assistantState.rmsLevel,
                    modifier = Modifier.clickable {
                        viewModel.onRobotTapped()
                    }
                )
            }
        }
    }
}
