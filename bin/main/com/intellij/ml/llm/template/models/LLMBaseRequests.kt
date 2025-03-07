package com.intellij.ml.llm.template.models

import com.intellij.ml.llm.template.models.ollama.OllamaBaseRequest
import com.intellij.ml.llm.template.models.openai.OpenAIChatRequest
//import com.intellij.ml.llm.template.models.openai.OpenAICompletionRequest
//import com.intellij.ml.llm.template.models.openai.OpenAIEditRequest

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