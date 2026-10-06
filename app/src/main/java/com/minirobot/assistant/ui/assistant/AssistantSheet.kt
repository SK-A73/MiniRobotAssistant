package com.minirobot.assistant.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minirobot.assistant.model.AssistantUiState
import com.minirobot.assistant.ui.theme.RobotCardBorder
import com.minirobot.assistant.ui.theme.RobotCyan
import com.minirobot.assistant.ui.theme.RobotSkyBlue
import com.minirobot.assistant.ui.theme.RobotSurface
import com.minirobot.assistant.ui.theme.RobotSurfaceVariant
import com.minirobot.assistant.ui.theme.RobotTextPrimary
import com.minirobot.assistant.ui.theme.RobotTextSecondary

/**
 * Assistant overlay interface displayed when user taps the floating robot.
 * Contains "How can I help you?", real Speech-to-Text microphone, live transcription,
 * and quick voice action suggestions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantSheet(
    state: AssistantUiState,
    onMicrophoneClick: () -> Unit,
    onCommandSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onStopRobotClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isSheetVisible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(280)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(240)) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            color = RobotSurface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top drag indicator
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(RobotCardBorder)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Safety Confirmation Modal (if active)
                if (state.isSafetyConfirmationVisible && state.pendingActionDescription != null) {
                    SafetyConfirmationDialog(
                        actionTitle = state.pendingActionTitle ?: "Confirm Sensitive Operation",
                        actionDescription = state.pendingActionDescription,
                        onConfirm = { state.onConfirmPendingAction?.invoke() },
                        onReject = { state.onRejectPendingAction?.invoke() }
                    )
                    return@Column
                }

                // Header status row with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = if (state.isListening) Color(0xFFEF4444) else RobotCyan
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isListening) "LISTENING..." else "ONLINE COMPANION",
                            color = RobotSkyBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RobotTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Prominent Greeting
                Text(
                    text = state.greetingText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = RobotTextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Live transcription or status subtitle
                val displayText = if (state.recognizedSpeech.isNotEmpty()) {
                    "\"${state.recognizedSpeech}\""
                } else {
                    state.statusMessage
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.recognizedSpeech.isNotEmpty()) RobotCyan else RobotTextSecondary,
                    textAlign = TextAlign.Center,
                    fontWeight = if (state.recognizedSpeech.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Real Microphone Button with live RMS audio pulsing
                RealMicrophoneButton(
                    isListening = state.isListening,
                    rmsLevel = state.rmsLevel,
                    onClick = onMicrophoneClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (state.isListening) "Tap to finish listening" else "Tap microphone to speak your command",
                    color = if (state.isListening) Color(0xFFF87171) else RobotTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Suggested Voice Commands (Phases 6 & 12 verification tasks)
                Text(
                    text = "Quick Voice Commands",
                    color = RobotSkyBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceCommandChip(label = "▶️ Open YouTube", onClick = { onCommandSelected("Open YouTube") })
                    VoiceCommandChip(label = "🌐 Open Chrome", onClick = { onCommandSelected("Open Chrome") })
                    VoiceCommandChip(label = "⬅️ Go back", onClick = { onCommandSelected("Go back") })
                    VoiceCommandChip(label = "🏠 Go home", onClick = { onCommandSelected("Go home") })
                    VoiceCommandChip(label = "🔍 Search for C# tutorials", onClick = { onCommandSelected("Search for C# tutorials") })
                    VoiceCommandChip(label = "🔋 Battery Status", onClick = { onCommandSelected("Battery") })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: Dismiss & Stop Robot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ElevatedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = RobotSurfaceVariant,
                            contentColor = RobotTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Hide Assistant", fontSize = 13.sp)
                    }

                    if (onStopRobotClick != null) {
                        ElevatedButton(
                            onClick = onStopRobotClick,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFF331620),
                                contentColor = Color(0xFFFCA5A5)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Stop Robot", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Microphone button with visual pulse based on real speech audio amplitude.
 */
@Composable
private fun RealMicrophoneButton(
    isListening: Boolean,
    rmsLevel: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dynamicScale = if (isListening) (pulseScale + rmsLevel * 0.25f).coerceIn(1f, 1.45f) else 1f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(92.dp)
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(dynamicScale)
                    .clip(CircleShape)
                    .background(Color(0x33EF4444))
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(
                    elevation = if (isListening) 16.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = if (isListening) Color(0xFFEF4444) else RobotCyan
                )
                .clip(CircleShape)
                .background(
                    brush = if (isListening) {
                        Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                    } else {
                        Brush.linearGradient(listOf(RobotSkyBlue, RobotCyan))
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun VoiceCommandChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(RobotSurfaceVariant)
            .border(width = 1.dp, color = RobotCardBorder, shape = RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = RobotTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
