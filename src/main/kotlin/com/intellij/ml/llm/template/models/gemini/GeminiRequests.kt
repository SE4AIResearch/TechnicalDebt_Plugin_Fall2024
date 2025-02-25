package com.intellij.ml.llm.template.models.openai

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.ml.llm.template.models.AuthorizationException
import com.intellij.ml.llm.template.models.CredentialsHolder
import com.intellij.ml.llm.template.models.LLMBaseRequest
import com.intellij.ml.llm.template.models.gemini.GeminiBody
import com.intellij.ml.llm.template.models.gemini.GeminiContents
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection

open class GeminiBaseRequest<Body>( body: Body) : LLMBaseRequest<Body>(body) {

    override fun sendSync(): GeminiResponse? {
        val apiKey = CredentialsHolder.getInstance().getGeminiKey()?.ifEmpty { null }
            ?: throw AuthorizationException("Gemini API Key is not provided")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        return HttpRequests.post(url, "application/json")
            .connect { request ->
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

class GeminiRequest(body: GeminiBody) :
        GeminiBaseRequest<GeminiBody>(body)

//class OpenAIEditRequest(body: OpenAiEditRequestBody) :
//    OllamaAIRequests<OpenAiEditRequestBody>("edits", body)

//class OpenAICompletionRequest(body: OpenAiCompletionRequestBody) :
//    OpenAIBaseRequest<OpenAiCompletionRequestBody>("completions", body)

