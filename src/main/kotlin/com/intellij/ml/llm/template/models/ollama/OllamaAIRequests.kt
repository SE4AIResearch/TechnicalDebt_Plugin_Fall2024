package com.intellij.ml.llm.template.models.openai

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.ml.llm.template.models.LLMBaseRequest
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection

//TODO: implement option for local ai (e.g DeepSeek )
open class OllamaAIRequests<Body>(server: String, body: Body) : LLMBaseRequest<Body>(body) {
    private val url = "$server/api/generate/"

    override fun sendSync(): OllamaAIResponse? {
        // TODO: Implement OllamaAIResponse so you can code this
    }
}
