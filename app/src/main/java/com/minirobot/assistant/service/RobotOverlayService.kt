package com.minirobot.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.minirobot.assistant.MainActivity
import com.minirobot.assistant.R
import com.minirobot.assistant.agent.AiAgent
import com.minirobot.assistant.agent.TaskAgent
import com.minirobot.assistant.agent.TaskExecutionState
import com.minirobot.assistant.agent.ToolRegistry
import com.minirobot.assistant.agent.ai.AiProviderManager
import com.minirobot.assistant.data.SettingsRepository
import com.minirobot.assistant.model.AssistantUiState
import com.minirobot.assistant.model.RobotActivityState
import com.minirobot.assistant.model.RobotMood
import com.minirobot.assistant.robot.RobotController
import com.minirobot.assistant.ui.assistant.AssistantSheet
import com.minirobot.assistant.ui.robot.CuteRobotCanvas
import com.minirobot.assistant.ui.robot.FloatingRobotTouchHandler
import com.minirobot.assistant.ui.theme.MiniRobotAssistantTheme
import com.minirobot.assistant.voice.SpeechRecognitionManager
import com.minirobot.assistant.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service hosting the persistent Floating Robot Companion across apps.
 * Utilizes WindowManager with TYPE_APPLICATION_OVERLAY without blocking underlying phone apps.
 */
class RobotOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var robotLifecycleOwner: OverlayLifecycleOwner
    private lateinit var assistantLifecycleOwner: OverlayLifecycleOwner

    private lateinit var robotController: RobotController
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var aiProviderManager: AiProviderManager
    private lateinit var ttsManager: TextToSpeechManager
    private var speechManager: SpeechRecognitionManager? = null
    private lateinit var aiAgent: AiAgent
    private lateinit var taskAgent: TaskAgent

    private var robotView: ComposeView? = null
    private lateinit var robotLayoutParams: WindowManager.LayoutParams

    private var assistantView: ComposeView? = null
    private lateinit var assistantLayoutParams: WindowManager.LayoutParams

    private var assistantUiState by mutableStateOf(AssistantUiState())
    private var positionSyncJob: Job? = null
    private var speechBubbleJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Creating RobotOverlayService")

        // 1. Promote to foreground service immediately to satisfy Android 14 requirements
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 2. Check overlay permission before doing any WindowManager work
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted. Stopping service.")
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        settingsRepository = SettingsRepository(this)
        settingsRepository.isOverlayServiceRunning = true

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        robotLifecycleOwner = OverlayLifecycleOwner().apply { onCreate() }
        assistantLifecycleOwner = OverlayLifecycleOwner().apply { onCreate() }

        // Initialize Robot Controller & AI System
        robotController = RobotController(serviceScope)
        toolRegistry = ToolRegistry(this)
        aiProviderManager = AiProviderManager(settingsRepository)
        ttsManager = TextToSpeechManager(this)

        aiAgent = AiAgent(
            toolRegistry = toolRegistry,
            aiProviderManager = aiProviderManager,
            ttsManager = ttsManager,
            onStateChange = { state -> robotController.activityState = state },
            onMoodChange = { mood -> robotController.mood = mood },
            onStatusUpdate = { execState, msg ->
                assistantUiState = assistantUiState.copy(
                    executionState = execState,
                    statusMessage = msg
                )
            },
            onSpeechBubble = { text -> showSpeechBubble(text, 4000L) },
            onPromptConfirmation = { title, desc, onConfirm, onReject ->
                assistantUiState = assistantUiState.copy(
                    isSafetyConfirmationVisible = true,
                    pendingActionTitle = title,
                    pendingActionDescription = desc,
                    onConfirmPendingAction = {
                        assistantUiState = assistantUiState.copy(isSafetyConfirmationVisible = false)
                        onConfirm()
                    },
                    onRejectPendingAction = {
                        assistantUiState = assistantUiState.copy(isSafetyConfirmationVisible = false)
                        onReject()
                    }
                )
                showAssistantOverlay()
            }
        )

        taskAgent = TaskAgent(
            toolRegistry = toolRegistry,
            aiProviderManager = aiProviderManager,
            ttsManager = ttsManager,
            onStateChange = { state -> robotController.activityState = state },
            onMoodChange = { mood -> robotController.mood = mood },
            onSpeechBubble = { text -> showSpeechBubble(text, 4000L) },
            onPromptConfirmation = { title, desc, onConfirm, onReject ->
                assistantUiState = assistantUiState.copy(
                    isSafetyConfirmationVisible = true,
                    pendingActionTitle = title,
                    pendingActionDescription = desc,
                    onConfirmPendingAction = {
                        assistantUiState = assistantUiState.copy(isSafetyConfirmationVisible = false)
                        onConfirm()
                    },
                    onRejectPendingAction = {
                        assistantUiState = assistantUiState.copy(isSafetyConfirmationVisible = false)
                        onReject()
                    }
                )
                showAssistantOverlay()
            }
        )

        setupSpeechRecognizer()

        createFloatingRobotOverlay()
        startPositionSyncLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.i(TAG, "Received ACTION_STOP")
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Ensure foreground notification remains attached
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun setupSpeechRecognizer() {
        speechManager = SpeechRecognitionManager(
            context = this,
            onReady = {
                robotController.activityState = RobotActivityState.LISTENING
                assistantUiState = assistantUiState.copy(
                    isListening = true,
                    statusMessage = "Listening for your voice... 🎙️"
                )
            },
            onRmsChanged = { rms ->
                assistantUiState = assistantUiState.copy(rmsLevel = rms)
            },
            onPartialResult = { partial ->
                assistantUiState = assistantUiState.copy(recognizedSpeech = partial)
            },
            onFinalResult = { finalSpeech ->
                assistantUiState = assistantUiState.copy(
                    recognizedSpeech = finalSpeech,
                    isListening = false,
                    executionState = TaskExecutionState.UNDERSTANDING
                )
                serviceScope.launch {
                    val response = aiAgent.processUserCommand(finalSpeech)
                    assistantUiState = assistantUiState.copy(
                        statusMessage = response,
                        isListening = false
                    )
                }
            },
            onError = { errorMsg ->
                robotController.activityState = RobotActivityState.ERROR
                assistantUiState = assistantUiState.copy(
                    statusMessage = errorMsg,
                    isListening = false
                )
            }
        )
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RobotOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.action_stop_robot), stopIntent)
            .addAction(0, getString(R.string.action_open_app), openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_overlay_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_overlay_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    /**
     * Creates and attaches the non-blocking floating robot view to WindowManager.
     */
    private fun createFloatingRobotOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot create overlay: permission not granted")
            return
        }

        val density = resources.displayMetrics.density
        val robotWidthPx = (110 * density).toInt()
        val robotHeightPx = (132 * density).toInt()

        // Update bounds
        val safeBounds = ScreenBoundsHelper.calculateSafeBounds(this, robotWidthPx.toFloat(), robotHeightPx.toFloat())
        robotController.updateBounds(safeBounds)

        robotLayoutParams = WindowManager.LayoutParams(
            robotWidthPx,
            robotHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = robotController.posX.toInt()
            y = robotController.posY.toInt()
        }

        robotView = ComposeView(this).apply {
            robotLifecycleOwner.attachToView(this)
            setContent {
                MiniRobotAssistantTheme {
                    Box(contentAlignment = Alignment.TopCenter) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Speech bubble above robot
                            if (assistantUiState.robotSpeechBubble != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E293B),
                                    tonalElevation = 6.dp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = assistantUiState.robotSpeechBubble ?: "",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // The high-quality cute animated robot
                            CuteRobotCanvas(
                                activityState = robotController.activityState,
                                facing = robotController.facing,
                                mood = robotController.mood,
                                walkCycleProgress = robotController.walkCycleProgress,
                                lookProgress = robotController.lookProgress,
                                bounceOffset = robotController.bounceOffset,
                                blinkProgress = robotController.blinkProgress,
                                rmsAudioLevel = assistantUiState.rmsLevel
                            )
                        }
                    }
                }
            }

            setOnTouchListener(
                FloatingRobotTouchHandler(
                    windowManager = windowManager,
                    layoutParams = robotLayoutParams,
                    onSingleTap = { onRobotTapped() },
                    onDoubleTap = { openMainActivity() },
                    onDragStart = { robotController.isPausedForUserDrag = true },
                    onDragEnd = { fx, fy ->
                        robotController.setPosition(fx, fy)
                        robotController.isPausedForUserDrag = false
                        robotController.pickNaturalDestination()
                    }
                )
            )
        }

        try {
            windowManager.addView(robotView, robotLayoutParams)
            Log.i(TAG, "Floating robot overlay added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding robot overlay to WindowManager", e)
        }
    }

    /**
     * Loops to update the robot's physical on-screen position as it roams.
     */
    private fun startPositionSyncLoop() {
        positionSyncJob?.cancel()
        positionSyncJob = serviceScope.launch {
            while (isActive) {
                if (!robotController.isPausedForUserDrag && robotView != null) {
                    val targetX = robotController.posX.toInt()
                    val targetY = robotController.posY.toInt()

                    if (robotLayoutParams.x != targetX || robotLayoutParams.y != targetY) {
                        robotLayoutParams.x = targetX
                        robotLayoutParams.y = targetY
                        try {
                            windowManager.updateViewLayout(robotView, robotLayoutParams)
                        } catch (_: Exception) {}
                    }
                }
                delay(30L)
            }
        }
    }

    /**
     * Displays the assistant bottom sheet overlay.
     */
    private fun showAssistantOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot show assistant overlay: permission not granted")
            return
        }

        if (assistantView != null) {
            assistantUiState = assistantUiState.copy(isSheetVisible = true)
            return
        }

        assistantLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        assistantView = ComposeView(this).apply {
            assistantLifecycleOwner.attachToView(this)
            setContent {
                MiniRobotAssistantTheme {
                    AssistantSheet(
                        state = assistantUiState,
                        onMicrophoneClick = { toggleMicrophone() },
                        onCommandSelected = { cmd -> executeVoiceCommand(cmd) },
                        onDismiss = { hideAssistantOverlay() },
                        onStopRobotClick = { stopSelf() }
                    )
                }
            }
        }

        assistantUiState = assistantUiState.copy(isSheetVisible = true)
        try {
            windowManager.addView(assistantView, assistantLayoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding assistant view to WindowManager", e)
        }
    }

    private fun hideAssistantOverlay() {
        assistantUiState = assistantUiState.copy(
            isSheetVisible = false,
            isListening = false,
            recognizedSpeech = ""
        )
        speechManager?.stopListening()
        if (assistantView != null) {
            try {
                windowManager.removeView(assistantView)
            } catch (_: Exception) {}
            assistantView = null
        }
        robotController.activityState = RobotActivityState.IDLE
    }

    private fun onRobotTapped() {
        robotController.triggerTapReaction()
        showSpeechBubble("How can I help you?", 3500L)
        showAssistantOverlay()
    }

    private fun toggleMicrophone() {
        if (speechManager?.isListening == true) {
            speechManager?.stopListening()
            assistantUiState = assistantUiState.copy(
                isListening = false,
                statusMessage = "Stopped listening. Ready."
            )
            robotController.activityState = RobotActivityState.IDLE
        } else {
            speechManager?.startListening()
        }
    }

    private fun executeVoiceCommand(command: String) {
        assistantUiState = assistantUiState.copy(
            recognizedSpeech = command,
            statusMessage = "Processing: $command",
            executionState = TaskExecutionState.UNDERSTANDING
        )
        serviceScope.launch {
            val result = aiAgent.processUserCommand(command)
            assistantUiState = assistantUiState.copy(statusMessage = result)
        }
    }

    private fun showSpeechBubble(text: String, durationMs: Long) {
        speechBubbleJob?.cancel()
        assistantUiState = assistantUiState.copy(robotSpeechBubble = text)
        speechBubbleJob = serviceScope.launch {
            delay(durationMs)
            assistantUiState = assistantUiState.copy(robotSpeechBubble = null)
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Destroying RobotOverlayService")
        if (::settingsRepository.isInitialized) {
            settingsRepository.isOverlayServiceRunning = false
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)

        positionSyncJob?.cancel()
        speechBubbleJob?.cancel()
        if (::robotController.isInitialized) {
            robotController.destroy()
        }
        speechManager?.destroy()
        if (::ttsManager.isInitialized) {
            ttsManager.shutdown()
        }

        if (robotView != null && ::windowManager.isInitialized) {
            try {
                windowManager.removeView(robotView)
            } catch (_: Exception) {}
            robotView = null
        }

        if (assistantView != null && ::windowManager.isInitialized) {
            try {
                windowManager.removeView(assistantView)
            } catch (_: Exception) {}
            assistantView = null
        }

        if (::robotLifecycleOwner.isInitialized) {
            robotLifecycleOwner.onDestroy()
        }
        if (::assistantLifecycleOwner.isInitialized) {
            assistantLifecycleOwner.onDestroy()
        }
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "RobotOverlayService"
        private const val CHANNEL_ID = "mini_robot_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.minirobot.assistant.ACTION_STOP_ROBOT"

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Cannot start overlay service: Settings.canDrawOverlays is false")
                return
            }
            val intent = Intent(context, RobotOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RobotOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
