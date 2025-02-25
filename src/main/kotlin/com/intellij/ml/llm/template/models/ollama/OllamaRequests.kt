package com.intellij.ml.llm.template.models.ollama

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.ml.llm.template.models.LLMBaseRequest
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.OllamaRequestProvider
import com.intellij.ml.llm.template.settings.LLMSettingsManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection



//TODO: implement option for local ai (e.g DeepSeek )
open class OllamaBaseRequest<Body>(body: OllamaBody) : LLMBaseRequest<OllamaBody>(body) {

    val settings = LLMSettingsManager.getInstance()

    val DEFAULT_PORT = "11434"
    private val url = settings.getOllServer().ifBlank{ "http://127.0.0.1:$DEFAULT_PORT/api/generate" }

    //


    val logger = Logger.getInstance(OllamaBaseRequest::class.java)
    init{
        logger.info(url)
    }


    override fun sendSync(): LLMBaseResponse? {
//        val model = "llama2";
        val payload = mapOf(
            "model" to OllamaRequestProvider.chatModel,  // Correct model inclusion
            "prompt" to body.prompt,
            "stream" to false
        )

        return HttpRequests.post(url, "application/json").connect {request ->
                val jsonPayload = Gson().toJson(payload)
                println("Payload: $jsonPayload")

                println("Awaiting response from ${OllamaRequestProvider.chatModel}...")
                request.write(jsonPayload)

                val responseCode = (request.connection as HttpURLConnection).responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = request.readString()

                    println("Received response: ${response}")

                    GsonBuilder().serializeNulls().create().fromJson(response, OllamaResponse::class.java)
                } else {
                    null
                }
            }
    }
}

class OllamaRequest(body: OllamaBody) :
    OllamaBaseRequest<OllamaBody>(body)
