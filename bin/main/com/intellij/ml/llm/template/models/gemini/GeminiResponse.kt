package com.technicaldebt_plugin_fall2024.models.gemini

import com.google.gson.annotations.SerializedName
import com.technicaldebt_plugin_fall2024.models.LLMBaseResponse
import com.technicaldebt_plugin_fall2024.models.LLMResponseChoice
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


data class ResponseChoice(
        @SerializedName("index") val index: Long,
        @SerializedName("message") val message: ResponseMessage,
        @SerializedName("logprobs") val logprobs: ResponseLogprobs?,  // Nullable
        @SerializedName("finish_reason") val finishReason: String
)

data class ResponseMessage(
        @SerializedName("role")
        val role: String,

        @SerializedName("content")
        val content: String,

)
data class ResponseLogprobs(
        @SerializedName("tokens") val tokens: List<String> = emptyList(),
        @SerializedName("token_logprobs") val tokenLogprobs: List<Double> = emptyList()
)

data class ResponseUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Long,

    @SerializedName("completion_tokens")
    val completionTokens: Long,

    @SerializedName("total_tokens")
    val totalTokens: Long,
)