package com.minirobot.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.minirobot.assistant.ui.screens.MainControlScreen
import com.minirobot.assistant.ui.theme.MiniRobotAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniRobotAssistantTheme {
                MainControlScreen()
            }
        }
    }
}
