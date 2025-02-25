package com.intellij.ml.llm.template.models.gemini

import com.google.gson.annotations.SerializedName

@Suppress("unused")
/**
 * Documentation: https://beta.openai.com/docs/api-reference/edits
 */
class GeminiBody(
        @SerializedName("contents")
        val contents: GeminiContents,
)
data class GeminiContents(
        @SerializedName("parts")
        val parts: GeminiParts
)
data class GeminiParts(
        @SerializedName("text")
        val text: String
)

