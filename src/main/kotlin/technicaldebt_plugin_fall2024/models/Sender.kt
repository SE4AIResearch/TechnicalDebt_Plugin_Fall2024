@file:Suppress("UnstableApiUsage")

package technicaldebt_plugin_fall2024.models

import LLMBundle
import com.technicaldebt_plugin_fall2024.models.gemini.GeminiChatMessage
import com.technicaldebt_plugin_fall2024.models.gemini.GeminiRequestBody
import com.technicaldebt_plugin_fall2024.models.ollama.OllamaBody
import com.technicaldebt_plugin_fall2024.models.openai.OpenAiChatMessage
import com.technicaldebt_plugin_fall2024.models.openai.OpenAiChatRequestBody
import com.technicaldebt_plugin_fall2024.settings.LLMSettingsManager
import com.technicaldebt_plugin_fall2024.showAuthorizationFailedNotification
import com.technicaldebt_plugin_fall2024.showRequestFailedNotification
import com.technicaldebt_plugin_fall2024.showUnauthorizedNotification
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import com.technicaldebt_plugin_fall2024.models.*
import java.io.IOException
import java.net.HttpURLConnection

private val logger = Logger.getInstance("#com.technicaldebt_plugin_fall2024.models")

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
                            messages = messages
                    )
            )
    return sendRequest(project, request)
}
fun sendOllamaRequest(
    project: Project,
    prompt: String,
    llmRequestProvider: LLMRequestProvider = OllamaRequestProvider,

    ): LLMBaseResponse? {
    val selectedModel = LLMSettingsManager.getInstance().ollamaModel

    val request =
        llmRequestProvider.createOllamaRequest(OllamaBody(model = selectedModel, prompt = prompt))
    return sendRequest(project, request)
}

fun sendChatRequest(
    project: Project,
    messages: List<OpenAiChatMessage>,
    model: String,
    llmRequestProvider: LLMRequestProvider = GPTRequestProvider,
    temperature: Double = 1.1,
    topP: Double = 0.9,
    numberOfSuggestions: Int = 1
): LLMBaseResponse? {
    val request =
        llmRequestProvider.createChatGPTRequest(
        OpenAiChatRequestBody(
            model = llmRequestProvider.chatModel,
            messages = messages,
            temperature = temperature,
            topP = topP,
            numberOfSuggestions = numberOfSuggestions
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