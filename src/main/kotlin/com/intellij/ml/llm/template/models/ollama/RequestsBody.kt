package com.intellij.ml.llm.template.models.ollama

import com.google.gson.annotations.SerializedName
@Suppress("unused")
class OllamaBody(
    @SerializedName("model")
    val model: String,

    @SerializedName("prompt")
    val prompt: String,

    @SerializedName("stream")
    val stream: String
)
