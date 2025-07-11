package com.technicaldebt_plugin_fall2024.models.gemini

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import technicaldebt_plugin_fall2024.AuthorizationException
import com.technicaldebt_plugin_fall2024.models.CredentialsHolder
import com.technicaldebt_plugin_fall2024.models.LLMBaseRequest
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection

open class GeminiBaseRequest<Body>( body: Body) : LLMBaseRequest<Body>(body) {

    override fun sendSync(): GeminiResponse? {
        val apiKey = CredentialsHolder.getInstance().getGeminiKey()?.ifEmpty { null }
                ?: throw AuthorizationException("Gemini API Key is not provided")

        val url = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"

        return HttpRequests.post(url, "application/json")
                .tuner {
                    it.setRequestProperty("Authorization", "Bearer $apiKey")
                }.connect { request ->
                    print("Body here: ${GsonBuilder().create().toJson(body)}")
                    request.write(GsonBuilder().create().toJson(body))
                    val responseCode = (request.connection as HttpURLConnection).responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = request.readString()
                        Gson().fromJson(response, GeminiResponse::class.java)
                    } else {
                        null
                    }
                }
    }
}
class GeminiRequest(body: GeminiRequestBody) :
        GeminiBaseRequest<GeminiRequestBody>(body)

//class OpenAIEditRequest(body: OpenAiEditRequestBody) :
//    OllamaAIRequests<OpenAiEditRequestBody>("edits", body)

//class OpenAICompletionRequest(body: OpenAiCompletionRequestBody) :
//    OpenAIBaseRequest<OpenAiCompletionRequestBody>("completions", body)

