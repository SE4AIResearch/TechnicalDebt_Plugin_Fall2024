@file:Suppress("UnstableApiUsage")

package com.intellij.ml.llm.template.models

import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.gemini.GeminiChatMessage
import com.intellij.ml.llm.template.models.gemini.GeminiRequestBody
import com.intellij.ml.llm.template.models.ollama.OllamaBody
import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import com.intellij.ml.llm.template.models.openai.OpenAiChatRequestBody
import com.intellij.ml.llm.template.settings.LLMSettingsManager
import com.intellij.ml.llm.template.showAuthorizationFailedNotification
import com.intellij.ml.llm.template.showRequestFailedNotification
import com.intellij.ml.llm.template.showUnauthorizedNotification
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import java.io.IOException
import java.net.HttpURLConnection

private val logger = Logger.getInstance("#com.intellij.ml.llm.template.models")

//fun sendEditRequest(
//    project: Project,
//    input: String,
//    instruction: String,
//    temperature: Double? = null,
//    topP: Double? = null,
//    numberOfSuggestions: Int? = null,
//    llmRequestProvider: LLMRequestProvider = CodexRequestProvider,
//): LLMBaseResponse? {
//    val settings = LLMSettingsManager.getInstance()
//
//    val request = llmRequestProvider.createEditRequest(
//        input = input,
//        instruction = instruction,
//        temperature = temperature ?: settings.getTemperature(),
//        topP = topP ?: settings.getTopP(),
//        numberOfSuggestions = numberOfSuggestions ?: settings.getNumberOfSamples()
//    )
//
//    return sendRequest(project, request)
//}

//fun sendCompletionRequest(
//    project: Project,
//    input: String,
//    suffix: String,
//    maxTokens: Int? = null,
//    temperature: Double? = null,
//    presencePenalty: Double? = null,
//    frequencyPenalty: Double? = null,
//    topP: Double? = null,
//    numberOfSuggestions: Int? = null,
//    llmRequestProvider: LLMRequestProvider = GPTRequestProvider,
//): LLMBaseResponse? {
//    val request = llmRequestProvider.createCompletionRequest(
//        input = input,
//        suffix = suffix,
//        maxTokens = maxTokens,
//        temperature = temperature,
//        topP = topP,
//        numberOfSuggestions = numberOfSuggestions,
//        presencePenalty = presencePenalty,
//        frequencyPenalty = frequencyPenalty,
//        logProbs = 1
//    )
//
//    return sendRequest(project, request)
//}


fun sendGeminiRequest(
        project: Project,
        messages: List<GeminiChatMessage>,
        model: String,
        llmRequestProvider: LLMRequestProvider = GeminiRequestProvider,
): LLMBaseResponse? {
    val request =
            llmRequestProvider.createGeminiRequest(
                    GeminiRequestBody(
                            model = llmRequestProvider.chatModel,
                            messages = messages,
                            n = 2
                    )
            )
    return sendRequest(project, request)
}
fun sendOllamaRequest(
    project: Project,
    prompt: String,
    llmRequestProvider: LLMRequestProvider = OllamaRequestProvider,

): LLMBaseResponse? {
    val request =
        llmRequestProvider.createOllamaRequest(OllamaBody(model = llmRequestProvider.chatModel, prompt = prompt))
    return sendRequest(project, request)
}

fun sendChatRequest(
    project: Project,
    messages: List<OpenAiChatMessage>,
    model: String,
    llmRequestProvider: LLMRequestProvider = GPTRequestProvider
): LLMBaseResponse? {
    val request =
        llmRequestProvider.createChatGPTRequest(
        OpenAiChatRequestBody(
            model = llmRequestProvider.chatModel,
            messages = messages
        )
    )
    return sendRequest(project, request)
}

private fun sendRequest(project: Project, request: LLMBaseRequest<*>): LLMBaseResponse? {
    val settings = LLMSettingsManager.getInstance()




    try {
        return request.sendSync()
    } catch (e: AuthorizationException) {
        showUnauthorizedNotification(project)
    } catch (e: HttpRequests.HttpStatusException) {
        when (e.statusCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> showAuthorizationFailedNotification(project)
            else -> {
                showRequestFailedNotification(
                    project, LLMBundle.message("notification.request.failed.message", e.message ?: "")
                )
                logger.warn(e)
            }
        }
    } catch (e: IOException) {
        showRequestFailedNotification(
            project, LLMBundle.message("notification.request.failed.message", e.message ?: "")
        )
        logger.warn(e)
    }
    return null
}