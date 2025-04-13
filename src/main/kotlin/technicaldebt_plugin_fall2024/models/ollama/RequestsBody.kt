package com.technicaldebt_plugin_fall2024.models.ollama

import com.google.gson.annotations.SerializedName
@Suppress("unused")
class OllamaBody(
    @SerializedName("model")
    val model: String,

    @SerializedName("prompt")
    val prompt: String,
//
//    @SerializedName("stream")
//    val stream: String
)
