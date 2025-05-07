package technicaldebt_plugin_fall2024.settings

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.OptionTag

enum class LLMProvider {
    OPENAI, OLLAMA, GEMINI
}

class LLMSettings : BaseState() {

    @get:OptionTag("llm_provider")
    var providerRaw: String = LLMProvider.OPENAI.name
        set(value) {
            if (field != value) {
                field = value
                incrementModificationCount()
            }
        }

    var provider: LLMProvider
        get() = runCatching { LLMProvider.valueOf(providerRaw) }.getOrDefault(LLMProvider.OPENAI)
        set(value) {
            providerRaw = value.name
        }

    @get:OptionTag("ollama_server")
    var ollamaServer: String = "http://localhost:11434"
        set(value) {
            if (field != value) {
                field = value
                incrementModificationCount()
            }
        }

    @get:OptionTag("ollama_model")
    var ollamaModel: String = "llama2"
        set(value) {
            if (field != value) {
                field = value
                incrementModificationCount()
            }
        }
}