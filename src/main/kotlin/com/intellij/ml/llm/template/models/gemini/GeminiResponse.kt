package com.intellij.ml.llm.template.models.openai

import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMResponseChoice

data class GeminiResponse(

    @SerializedName("id")
    val id: String,

    @SerializedName("object")
    val type: String,

    @SerializedName("created")
    val created: Long,

    @SerializedName("choices")
    val choices: List<ResponseChoice>,

    @SerializedName("usage")
    val usage: ResponseUsage,
) : LLMBaseResponse {
    override fun getSuggestions(): List<LLMResponseChoice> = choices.map {
        LLMResponseChoice(it.message.content, it.finishReason)
    }
}


data class GeminiResponseChoice(
        @SerializedName("index") val index: Long,
        @SerializedName("message") val message: ResponseMessage,
        @SerializedName("logprobs") val logprobs: ResponseLogprobs?,  // Nullable
        @SerializedName("finish_reason") val finishReason: String
)


data class GeminiResponseMessage(
        @SerializedName("role")
        val role: String,

        @SerializedName("content")
        val content: String,

)
data class GeminiResponseLogprobs(
        @SerializedName("tokens") val tokens: List<String> = emptyList(),
        @SerializedName("token_logprobs") val tokenLogprobs: List<Double> = emptyList()
)


data class GeminiResponseUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Long,

    @SerializedName("completion_tokens")
    val completionTokens: Long,

    @SerializedName("total_tokens")
    val totalTokens: Long,
)