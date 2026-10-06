package com.minirobot.assistant.agent

import android.content.Context
import com.minirobot.assistant.agent.tools.BackTool
import com.minirobot.assistant.agent.tools.BatteryStatusTool
import com.minirobot.assistant.agent.tools.DeviceTimeTool
import com.minirobot.assistant.agent.tools.HomeTool
import com.minirobot.assistant.agent.tools.LaunchSettingsTool
import com.minirobot.assistant.agent.tools.OpenAppTool
import com.minirobot.assistant.agent.tools.OpenUrlTool
import com.minirobot.assistant.agent.tools.ScrollTool
import com.minirobot.assistant.agent.tools.SendMessageTool
import com.minirobot.assistant.agent.tools.TapTool
import com.minirobot.assistant.agent.tools.TypeTextTool
import com.minirobot.assistant.agent.tools.WebSearchTool

/**
 * Registry holding all available tools that the robot assistant can invoke.
 */
class ToolRegistry(context: Context) {

    private val toolsMap = mutableMapOf<String, AgentTool>()

    init {
        register(OpenAppTool(context))
        register(WebSearchTool(context))
        register(OpenUrlTool(context))
        register(BackTool())
        register(HomeTool(context))
        register(ScrollTool())
        register(TapTool())
        register(TypeTextTool())
        register(LaunchSettingsTool(context))
        register(BatteryStatusTool(context))
        register(DeviceTimeTool())
        register(SendMessageTool(context))
    }

    fun register(tool: AgentTool) {
        toolsMap[tool.name] = tool
    }

    fun getTool(name: String): AgentTool? = toolsMap[name]

    fun getAllTools(): List<AgentTool> = toolsMap.values.toList()
}
