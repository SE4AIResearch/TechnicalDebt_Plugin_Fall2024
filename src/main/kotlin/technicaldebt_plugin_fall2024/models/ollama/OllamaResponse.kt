package com.technicaldebt_plugin_fall2024.models.ollama

import com.google.gson.annotations.SerializedName
import com.technicaldebt_plugin_fall2024.models.LLMBaseResponse
import com.technicaldebt_plugin_fall2024.models.LLMResponseChoice


data class OllamaResponse(
    @SerializedName("model")
    val model: String,

    @SerializedName("created")
    val created: Long,

    @SerializedName("response")
    val response: String,

    @SerializedName("done_reason")
    val finishReason: String
) : LLMBaseResponse {
    override fun getSuggestions():
            List<LLMResponseChoice> = listOf(LLMResponseChoice(response, finishReason))
}
