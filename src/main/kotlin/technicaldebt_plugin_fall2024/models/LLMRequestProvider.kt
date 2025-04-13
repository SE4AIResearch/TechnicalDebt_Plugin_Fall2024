package com.technicaldebt_plugin_fall2024.models

import com.technicaldebt_plugin_fall2024.models.gemini.GeminiRequest
import com.technicaldebt_plugin_fall2024.models.gemini.GeminiRequestBody
import com.technicaldebt_plugin_fall2024.models.ollama.OllamaBody
import com.technicaldebt_plugin_fall2024.models.ollama.OllamaRequest
import com.technicaldebt_plugin_fall2024.models.openai.*
import com.technicaldebt_plugin_fall2024.settings.LLMSettingsManager
import com.intellij.openapi.diagnostic.Logger
import com.technicaldebt_plugin_fall2024.settings.LLMSettingsManager.LLMProvider
import technicaldebt_plugin_fall2024.models.MockCompletionRequests


/**
 * Available options: https://beta.openai.com/docs/models/codex
 */
//private const val CODEX_COMPLETION_MODEL = "gpt-3.5-turbo"
//private const val CODEX_EDIT_MODEL = "gpt-3.5-turbo"
//
//private const val GPT_COMPLETION_MODEL = "gpt-3.5-turbo"
//private const val GPT_EDIT_MODEL = "gpt-3.5-turbo"

private const val CHAT_GPT_3_5_TURBO = "gpt-3.5-turbo"
private val logger = Logger.getInstance("#com.technicaldebt_plugin_fall2024.models")
private const val GEMINI = "gemini-2.0-flash"

//val CodexRequestProvider = LLMRequestProvider(CODEX_COMPLETION_MODEL, CODEX_EDIT_MODEL, CHAT_GPT_3_5_TURBO)

private val MODEL = LLMSettingsManager.LLMProvider.OLLAMA.toString()

val GPTRequestProvider = LLMRequestProvider(CHAT_GPT_3_5_TURBO)
val OllamaRequestProvider = LLMRequestProvider(MODEL)
val GeminiRequestProvider = LLMRequestProvider(GEMINI)

public class LLMRequestProvider(
    val chatModel: String,
) {


    fun createRequest(prompt: String, settings: LLMSettingsManager): LLMBaseRequest<*> {
        return when (settings.provider) {

            LLMProvider.OPENAI -> {
                println("Using OpenAI with prompt: $prompt")
                MockCompletionRequests()
            }

            LLMProvider.GEMINI ->{
            println("Using Gemini with prompt: $prompt")
            MockCompletionRequests()
            }

            LLMProvider.OLLAMA -> {
                println("Using Ollama with prompt: $prompt and server:")
                MockCompletionRequests()
            }
        }
    }


    fun createGeminiRequest(
            body: GeminiRequestBody,
    ): LLMBaseRequest<*> {
        return GeminiRequest(body)
    }

    fun createOllamaRequest(
        body: OllamaBody,
    ): LLMBaseRequest<*> {
        return OllamaRequest(body)
    }


    fun createChatGPTRequest(
        body: OpenAiChatRequestBody,
    ): LLMBaseRequest<*> {
        return OpenAIChatRequest(body)
    }}
