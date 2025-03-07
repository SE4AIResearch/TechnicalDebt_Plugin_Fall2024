package com.intellij.ml.llm.template.models.gemini
import com.google.gson.annotations.SerializedName

data class GeminiChatMessage (
        @SerializedName("role")
        val role: String,

        @SerializedName("content")
        val content: String,
)

class GeminiRequestBody(
        @SerializedName("model")
        val model: String,

        @SerializedName("messages")
        val messages: List<GeminiChatMessage>,

        @SerializedName("temperature")
        val temperature: Double? = null,

        @SerializedName("top_p")
        val topP: Double? = null,

        @SerializedName("n")
        val numberOfSuggestions: Int? = null,

        @SerializedName("stream")
        var stream: Boolean? = null,

        @SerializedName("stop")
        val stop: String? = null,

        @SerializedName("max_tokens")
        val maxTokens: Int? = null,

        @SerializedName("presence_penalty")
        val presencePenalty: Double? = null,

        @SerializedName("frequency_penalty")
        val frequencyPenalty: Double? = null,

        @SerializedName("logit_bias")
        val logitBias: Map<String, Int>? = null,

        @SerializedName("user")
        val user: String? = null,
)

