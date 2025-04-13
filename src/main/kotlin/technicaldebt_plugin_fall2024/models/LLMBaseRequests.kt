package com.technicaldebt_plugin_fall2024.models

import com.technicaldebt_plugin_fall2024.models.ollama.OllamaBaseRequest
import com.technicaldebt_plugin_fall2024.models.openai.OpenAIChatRequest
//import com.technicaldebt_plugin_fall2024.models.openai.OpenAICompletionRequest
//import com.technicaldebt_plugin_fall2024.models.openai.OpenAIEditRequest

data class LLMResponseChoice(val text: String, val finishReason: String)

interface LLMBaseResponse {
    fun getSuggestions(): List<LLMResponseChoice>
}

abstract class LLMBaseRequest<Body>(val body: Body) {
    abstract fun sendSync(): LLMBaseResponse?
}

enum class LLMRequestType {
    OPENAI_CHAT, MOCK, OLLAMA;

    companion object {
        fun byRequest(request: LLMBaseRequest<*>): LLMRequestType {
            return when (request) {
//                is OpenAIEditRequest -> OPENAI_EDIT
//                is OpenAICompletionRequest -> OPENAI_COMPLETION
                is OllamaBaseRequest<*> -> OLLAMA
                is OpenAIChatRequest -> OPENAI_CHAT
                else -> MOCK
            }
        }
    }
}