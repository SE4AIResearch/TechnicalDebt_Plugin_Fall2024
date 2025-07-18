package com.technicaldebt_plugin_fall2024.settings

import com.technicaldebt_plugin_fall2024.models.CredentialsHolder
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.OptionTag

@Service(Service.Level.APP)
@State(
    name = "LLMSettings",
    storages = [Storage(value = "llm.for.code.xml", roamingType = RoamingType.DISABLED, exportable = true)]
)
class LLMSettingsManager : PersistentStateComponent<LLMSettings> {


    var provider: LLMProvider = LLMProvider.OPENAI
//    var openAiKey: String = ""
    var ollamaServer: String = ""
//    var GeminiKey: String = ""


    companion object {

        fun getInstance() = service<LLMSettingsManager>()
    }

    private var state = LLMSettings()

    override fun getState(): LLMSettings = state

    override fun loadState(newState: LLMSettings) {
        state = newState
    }


    //fun getProvider(): LLMProvider = provider
    fun updateProvider(value: LLMProvider) {
        provider = value
    }

    fun getOpenAiKey(): String {
        return CredentialsHolder.getInstance().getOpenAiApiKey() ?: ""
    }
    fun setOpenAiKey(key: String) {
        CredentialsHolder.getInstance().setOpenAiApiKey(key)
    }
    fun getOllServer(): String {
        return state.ollamaServer
    }

    fun setOllServer(url: String) {
        state.ollamaServer = url
    }

    fun getGeminiKey(): String {
        return CredentialsHolder.getInstance().getGeminiKey() ?: ""
    }

    fun setGeminiKey(key: String) {
        CredentialsHolder.getInstance().setGeminiApiKey(key)
    }




    fun getOpenAiOrganization(): String {
        return CredentialsHolder.getInstance().getOpenAiOrganization() ?: ""
    }

    fun setOpenAiOrganization(key: String) {
        CredentialsHolder.getInstance().setOpenAiOrganization(key)
    }

    fun useOpenAiCompletion() = state.useOpenAi

    fun getTemperature(): Double = state.openAi.temperature.toDouble()

    fun setTemperature(temperature: Double) {
        state.openAi.temperature = temperature.toFloat()
    }

    fun getPresencePenalty(): Double = state.openAi.presencePenalty.toDouble()

    fun setPresencePenalty(penalty: Double) {
        state.openAi.presencePenalty = penalty.toFloat()
    }

    fun getFrequencyPenalty(): Double = state.openAi.frequencyPenalty.toDouble()

    fun setFrequencyPenalty(penalty: Double) {
        state.openAi.frequencyPenalty = penalty.toFloat()
    }

    fun getTopP(): Double = state.openAi.topP.toDouble()

    fun setTopP(topP: Double) {
        state.openAi.topP = topP.toFloat()
    }

    fun getNumberOfSamples(): Int = state.openAi.numberOfSamples

    fun getMaxTokens(): Int = state.openAi.maxTokens

    fun getPromptLength(): Int = state.openAi.promptLength

    fun getSuffixLength(): Int = state.openAi.suffixLength

}

class LLMSettings : BaseState() {
    @get:OptionTag("data_sharing")
    var isDataSharingEnabled by property(false)

    @get:OptionTag("data_sharing_initialized")
    var isDataSharingOptionInitialized by property(false)

    fun setDataSharingOption(newValue: Boolean) {
        isDataSharingOptionInitialized = true
        isDataSharingEnabled = newValue
    }

    @get:OptionTag("use_open_ai")
    var useOpenAi by property(true)

    @get:OptionTag("open_ai")
    var openAi by property(OpenAISettings()) { it == OpenAISettings() }

    @get:OptionTag("ollama_server")
    //var ollamaServer by string("")
    var ollamaServer: String = ""

    @get:OptionTag("UseGemini")
    var useGemini by property(false)

    @get:OptionTag("GeminiKey")
    var GeminiKey by string("")

}

class OpenAISettings : BaseState() {
    @get:OptionTag("temperature")
    var temperature by property(0.0f)

    @get:OptionTag("presence_penalty")
    var presencePenalty by property(0.0f)

    @get:OptionTag("frequency_penalty")
    var frequencyPenalty by property(0.0f)

    @get:OptionTag("top_p")
    var topP by property(1.0f)

    @get:OptionTag("number_of_samples")
    var numberOfSamples by property(1)

    @get:OptionTag("max_tokens")
    var maxTokens by property(64)

    @get:OptionTag("prompt_length")
    var promptLength by property(256)

    @get:OptionTag("suffix_length")
    var suffixLength by property(256)


}
