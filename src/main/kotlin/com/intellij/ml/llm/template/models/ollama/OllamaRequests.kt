package com.intellij.ml.llm.template.models.ollama

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.ml.llm.template.models.LLMBaseRequest
import com.intellij.ml.llm.template.models.openai.OpenAIResponse
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection

//TODO: implement option for local ai (e.g DeepSeek )
open class OpenAIBaseRequest<Body>(port: String, body: Body) : LLMBaseRequest<Body>(body) {
    private val url = "http://localhost:$port"+ "/api/chat"
    val model = "llama2";


    val payload = Gson().fromJson(
        "\"role\": \"user\","+
        "\"content\" : \"Send a response back!\"", OllamaResponse::class.java)


    override fun sendSync(): OllamaResponse? {
        return HttpRequests.post(url, "application/json")
            .tuner {
                it.setRequestProperty("model", "$model")
                it.setRequestProperty("messages", "$payload")
            }
            .connect { request ->
                request.write(GsonBuilder().create().toJson(body))
                val responseCode = (request.connection as HttpURLConnection).responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = request.readString()
                    Gson().fromJson(response, OpenAIResponse::class.java)
                } else {
                    null
                }
            }
    }


    }


