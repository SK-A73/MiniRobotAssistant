package com.minirobot.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.robot.RobotViewModel
import com.minirobot.assistant.ui.assistant.AssistantSheet
import com.minirobot.assistant.ui.robot.RobotPlayfield
import com.minirobot.assistant.ui.theme.RobotCardBorder
import com.minirobot.assistant.ui.theme.RobotCyan
import com.minirobot.assistant.ui.theme.RobotDarkBg
import com.minirobot.assistant.ui.theme.RobotSkyBlue
import com.minirobot.assistant.ui.theme.RobotSurface
import com.minirobot.assistant.ui.theme.RobotTextPrimary
import com.minirobot.assistant.ui.theme.RobotTextSecondary

/**
 * Main application screen displaying the robot companion playfield,
 * top navigation bar with status indicator, and assistant sheet.
 */
@Composable
fun MainScreen(
    viewModel: RobotViewModel = viewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        RobotDarkBg,
                        Color(0xFF0F172A),
                        Color(0xFF131D31)
                    )
                )
            )
    ) {
        // --- 1. Playfield (Robot roaming surface) ---
        RobotPlayfield(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. Top App Bar & Status Banner ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(RobotSurface.copy(alpha = 0.85f))
                    .border(width = 1.dp, color = RobotCardBorder, shape = RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🤖",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Mini Robot",
                        color = RobotTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Assistant",
                        color = RobotSkyBlue,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                // Activity State Pill
                val (stateEmoji, stateLabel, stateColor) = when (viewModel.activityState) {
                    RobotActivityState.WALKING -> Triple("🚶", "Exploring", RobotCyan)
                    RobotActivityState.IDLE -> Triple("⏸️", "Observing", RobotSkyBlue)
                    RobotActivityState.LOOKING_AROUND -> Triple("👀", "Looking Around", RobotSkyBlue)
                    RobotActivityState.TAPPED -> Triple("💬", "Assisting", Color(0xFF34D399))
                    RobotActivityState.SUCCESS -> Triple("💃", "Dancing!", Color(0xFFF472B6))
                    RobotActivityState.LISTENING -> Triple("🎙️", "Listening", Color(0xFF38BDF8))
                    RobotActivityState.THINKING -> Triple("⚡", "Thinking", Color(0xFFA78BFA))
                    RobotActivityState.SPEAKING -> Triple("🗣️", "Speaking", Color(0xFF2DD4BF))
                    RobotActivityState.ERROR -> Triple("⚠️", "Alert", Color(0xFFEF4444))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(stateColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(stateColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$stateEmoji $stateLabel",
                        color = stateColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick interaction tip (hidden when assistant is open)
            AnimatedVisibility(
                visible = !viewModel.assistantState.isSheetVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "Tap the robot to chat • Tap screen to guide movement",
                    color = RobotTextSecondary.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .padding(top = 8.dp, start = 8.dp)
                )
            }
        }

        // --- 3. Assistant Bottom Sheet Overlay ---
        AssistantSheet(
            state = viewModel.assistantState,
            onMicrophoneClick = { viewModel.onMicrophoneClicked() },
            onCommandSelected = { action -> viewModel.onQuickActionSelected(action) },
            onDismiss = { viewModel.dismissAssistant() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
