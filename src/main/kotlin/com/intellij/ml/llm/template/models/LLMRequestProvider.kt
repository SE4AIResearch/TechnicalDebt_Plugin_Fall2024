package com.intellij.ml.llm.template.models

import com.intellij.ml.llm.template.models.ollama.OllamaBody
import com.intellij.ml.llm.template.models.ollama.OllamaRequest
import com.intellij.ml.llm.template.models.openai.*
import com.intellij.ml.llm.template.settings.LLMSettingsManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.ml.llm.template.settings.LLMSettingsManager.LLMProvider




/**
 * Available options: https://beta.openai.com/docs/models/codex
 */
//private const val CODEX_COMPLETION_MODEL = "gpt-3.5-turbo"
//private const val CODEX_EDIT_MODEL = "gpt-3.5-turbo"
//
//private const val GPT_COMPLETION_MODEL = "gpt-3.5-turbo"
//private const val GPT_EDIT_MODEL = "gpt-3.5-turbo"

private const val CHAT_GPT_3_5_TURBO = "gpt-3.5-turbo"

private val logger = Logger.getInstance("#com.intellij.ml.llm.template.models")

//val CodexRequestProvider = LLMRequestProvider(CODEX_COMPLETION_MODEL, CODEX_EDIT_MODEL, CHAT_GPT_3_5_TURBO)

private const val MODEL = "deepseek-r1"

val GPTRequestProvider = LLMRequestProvider(CHAT_GPT_3_5_TURBO)
val OllamaRequestProvider = LLMRequestProvider(MODEL)

public class LLMRequestProvider(
    val chatModel: String,
) {


    fun createRequest(prompt: String, settings: LLMSettingsManager): LLMBaseRequest<*> {
        return when (settings.provider) {
            LLMProvider.OPENAI -> {
                println("Using OpenAI with prompt: $prompt")
                MockCompletionRequests()
            }
            LLMProvider.OLLAMA -> {
                println("Using Ollama with prompt: $prompt and server:")
                MockCompletionRequests()
            }
        }
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
