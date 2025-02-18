package com.intellij.ml.llm.template.models.ollama

import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMResponseChoice


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
