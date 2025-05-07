package com.technicaldebt_plugin_fall2024.settings

import com.technicaldebt_plugin_fall2024.models.CredentialsHolder
import com.intellij.openapi.components.*
import technicaldebt_plugin_fall2024.settings.LLMProvider
import technicaldebt_plugin_fall2024.settings.LLMSettings

@Service(Service.Level.APP)
@State(
    name = "LLMSettings",
    storages = [Storage(value = "llm.for.code.xml", roamingType = RoamingType.DISABLED, exportable = true)]
)
class LLMSettingsManager : PersistentStateComponent<LLMSettings> {

    companion object {
        fun getInstance() = service<LLMSettingsManager>()
    }

    private var state = LLMSettings()

    override fun getState(): LLMSettings = state

    override fun loadState(newState: LLMSettings) {
        state = newState
    }

    // Provider
    fun getProvider(): LLMProvider = state.provider
    fun updateProvider(value: LLMProvider) {
        state.provider = value
    }

    // Ollama (non-sensitive)
    fun getOllServer(): String = state.ollamaServer
    fun setOllServer(url: String) {
        state.ollamaServer = url
    }

    var ollamaModel: String
        get() = state.ollamaModel
        set(value) {
            state.ollamaModel = value
        }

    // Secure credentials (via PasswordSafe)
    fun getOpenAiKey(): String = CredentialsHolder.getInstance().getOpenAiApiKey() ?: ""
    fun setOpenAiKey(key: String) {
        CredentialsHolder.getInstance().setOpenAiApiKey(key)
    }

    fun getGeminiKey(): String = CredentialsHolder.getInstance().getGeminiKey() ?: ""
    fun setGeminiKey(key: String) {
        CredentialsHolder.getInstance().setGeminiApiKey(key)
    }

    fun getOpenAiOrganization(): String = CredentialsHolder.getInstance().getOpenAiOrganization() ?: ""
    fun setOpenAiOrganization(org: String) {
        CredentialsHolder.getInstance().setOpenAiOrganization(org)
    }
}