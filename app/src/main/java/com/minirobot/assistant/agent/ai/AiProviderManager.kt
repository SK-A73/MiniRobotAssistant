package com.minirobot.assistant.agent.ai

import com.minirobot.assistant.data.SettingsRepository

/**
 * Manages active AI provider selection based on user settings and API key availability.
 */
class AiProviderManager(private val settingsRepository: SettingsRepository) {

    fun getActiveProvider(): AiProvider {
        val mode = settingsRepository.aiProviderMode
        val apiKey = settingsRepository.geminiApiKey

        return if (mode == SettingsRepository.PROVIDER_GEMINI && apiKey.isNotBlank()) {
            GeminiAiProvider(
                apiKey = apiKey,
                model = settingsRepository.geminiModel
            )
        } else {
            LocalRuleBasedAiProvider()
        }
    }
}
