package com.minirobot.assistant.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.minirobot.assistant.accessibility.AccessibilityUtils
import com.minirobot.assistant.data.SettingsRepository
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotFacing
import com.minirobot.assistant.model.RobotMood
import com.minirobot.assistant.service.RobotOverlayService
import com.minirobot.assistant.ui.robot.CuteRobotCanvas
import com.minirobot.assistant.ui.theme.RobotCardBorder
import com.minirobot.assistant.ui.theme.RobotCyan
import com.minirobot.assistant.ui.theme.RobotDarkBg
import com.minirobot.assistant.ui.theme.RobotGreen
import com.minirobot.assistant.ui.theme.RobotSkyBlue
import com.minirobot.assistant.ui.theme.RobotSurface
import com.minirobot.assistant.ui.theme.RobotSurfaceVariant
import com.minirobot.assistant.ui.theme.RobotTextPrimary
import com.minirobot.assistant.ui.theme.RobotTextSecondary

/**
 * Control Hub for Mini Robot Assistant:
 * - Start / Stop persistent floating robot
 * - System permissions checklist (Overlay, Accessibility, Microphone)
 * - AI Provider selection & API Key configuration
 * - Interactive companion preview
 */
@Composable
fun MainControlScreen(
    context: Context = LocalContext.current
) {
    val settingsRepo = remember { SettingsRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live permission states
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasAccessibilityPermission by remember { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context)) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isOverlayRunning by remember { mutableStateOf(settingsRepo.isOverlayServiceRunning) }

    // AI Settings states
    var aiProviderMode by remember { mutableStateOf(settingsRepo.aiProviderMode) }
    var geminiApiKey by remember { mutableStateOf(settingsRepo.geminiApiKey) }
    var backendUrl by remember { mutableStateOf(settingsRepo.backendUrl) }
    var apiKeySavedFeedback by remember { mutableStateOf(false) }

    // Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    // Auto-prompt for microphone permission on screen launch if not yet granted
    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Refresh permission statuses whenever the activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasAccessibilityPermission = AccessibilityUtils.isAccessibilityServiceEnabled(context)
                hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                isOverlayRunning = settingsRepo.isOverlayServiceRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RobotDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Cute Robot Preview
        CuteRobotCanvas(
            activityState = if (isOverlayRunning) RobotActivityState.WALKING else RobotActivityState.IDLE,
            facing = RobotFacing.RIGHT,
            mood = RobotMood.HAPPY,
            walkCycleProgress = 0.5f,
            lookProgress = 0f,
            bounceOffset = 0f,
            blinkProgress = 0f
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mini Robot Companion",
            style = MaterialTheme.typography.headlineMedium,
            color = RobotTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Your persistent AI assistant across Android apps",
            color = RobotTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- Master Floating Service Control ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RobotSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RobotCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Overlay Companion",
                            color = RobotTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isOverlayRunning) "Active & Roaming on Screen" else "Service Stopped",
                            color = if (isOverlayRunning) RobotGreen else RobotTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isOverlayRunning) RobotGreen else Color(0xFF64748B))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val canDraw = Settings.canDrawOverlays(context)
                            hasOverlayPermission = canDraw
                            if (!canDraw) {
                                requestOverlayPermission(context)
                            } else {
                                RobotOverlayService.start(context)
                                isOverlayRunning = true
                                settingsRepo.isOverlayServiceRunning = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RobotSkyBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Start Robot", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            RobotOverlayService.stop(context)
                            isOverlayRunning = false
                            settingsRepo.isOverlayServiceRunning = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Stop Robot")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- Permissions Checklist ---
        Text(
            text = "Required Permissions",
            color = RobotSkyBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // 1. Overlay Permission Card
        PermissionStatusCard(
            title = "Display Over Other Apps",
            description = "Allows the robot to stay visible when using Chrome, YouTube, or other apps.",
            isGranted = hasOverlayPermission,
            actionLabel = "Grant Overlay",
            onAction = { requestOverlayPermission(context) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Accessibility Permission Card
        PermissionStatusCard(
            title = "Accessibility Automation & Keyboard Detection",
            description = "Enables hands-free actions (Back, Home, Click) and automatically moves the robot above keyboards so it never covers typing.",
            isGranted = hasAccessibilityPermission,
            actionLabel = "Enable in Settings",
            onAction = { AccessibilityUtils.openAccessibilitySettings(context) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Microphone Permission Card
        PermissionStatusCard(
            title = "Microphone & Tap-to-Speak",
            description = "Tap the roaming robot directly to speak commands immediately. Long-press opens full assistant menu.",
            isGranted = hasMicPermission,
            actionLabel = "Grant Microphone",
            onAction = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- AI Provider Settings ---
        Text(
            text = "AI Intelligence Engine",
            color = RobotSkyBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RobotSurface),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RobotCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Radio option 1: Local Rule Engine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = aiProviderMode == SettingsRepository.PROVIDER_LOCAL,
                        onClick = {
                            aiProviderMode = SettingsRepository.PROVIDER_LOCAL
                            settingsRepo.aiProviderMode = SettingsRepository.PROVIDER_LOCAL
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = RobotSkyBlue)
                    )
                    Column {
                        Text(
                            text = "Local Offline Engine (Default)",
                            color = RobotTextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Instant, offline tool execution for phone tasks. No API key needed.",
                            color = RobotTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Radio option 2: Google Gemini
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = aiProviderMode == SettingsRepository.PROVIDER_GEMINI,
                        onClick = {
                            aiProviderMode = SettingsRepository.PROVIDER_GEMINI
                            settingsRepo.aiProviderMode = SettingsRepository.PROVIDER_GEMINI
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = RobotSkyBlue)
                    )
                    Column {
                        Text(
                            text = "Google Gemini LLM",
                            color = RobotTextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Advanced natural language reasoning and tool planning.",
                            color = RobotTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                AnimatedVisibility(visible = aiProviderMode == SettingsRepository.PROVIDER_GEMINI) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "Get your free key at aistudio.google.com/app/apikey, or specify your backend proxy URL.",
                            color = RobotTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = backendUrl,
                            onValueChange = {
                                backendUrl = it
                                apiKeySavedFeedback = false
                            },
                            label = { Text("Backend Proxy URL (Optional)") },
                            placeholder = { Text("e.g. http://10.0.2.2:8000") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = RobotTextPrimary,
                                unfocusedTextColor = RobotTextPrimary,
                                focusedBorderColor = RobotSkyBlue,
                                unfocusedBorderColor = RobotCardBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = geminiApiKey,
                            onValueChange = {
                                geminiApiKey = it
                                apiKeySavedFeedback = false
                            },
                            label = { Text("Gemini API Key (Google AI Studio)") },
                            placeholder = { Text("Enter your Gemini API key") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = RobotTextPrimary,
                                unfocusedTextColor = RobotTextPrimary,
                                focusedBorderColor = RobotSkyBlue,
                                unfocusedBorderColor = RobotCardBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                settingsRepo.geminiApiKey = geminiApiKey
                                settingsRepo.backendUrl = backendUrl
                                apiKeySavedFeedback = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RobotSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (apiKeySavedFeedback) "Saved ✓" else "Save AI Configuration",
                                color = RobotTextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RobotSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RobotCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isGranted) RobotGreen else Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = RobotTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    color = RobotTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isGranted) {
                OutlinedButton(
                    onClick = onAction,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RobotSkyBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = actionLabel, fontSize = 11.sp)
                }
            } else {
                Text(
                    text = "Granted ✓",
                    color = RobotGreen,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

private fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
