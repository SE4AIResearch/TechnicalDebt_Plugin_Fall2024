package com.technicaldebt_plugin_fall2024.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import technicaldebt_plugin_fall2024.settings.git.GitHubSettingsManager
import technicaldebt_plugin_fall2024.settings.LLMProvider
import javax.swing.DefaultComboBoxModel

class TechnicalDebtToolConfigurable :
    BoundConfigurable("Technical Debt Tool Settings") {

    private val llmSettings = service<LLMSettingsManager>()
    private val githubSettings = service<GitHubSettingsManager>()

    private lateinit var geminiAiKeyRow: Row
    private lateinit var openAiKeyRow: Row
    private lateinit var openAiOrgRow: Row
    private lateinit var ollamaServerRow: Row
    private lateinit var ollamaModelRow: Row

    override fun createPanel(): DialogPanel {
        return panel {
            group("LLM Configuration") {
                row("LLM Provider") {
                    comboBox(DefaultComboBoxModel(LLMProvider.values()))
                        .bindItem(
                            { llmSettings.getProvider() },
                            { value ->
                                if (value != null) {
                                    llmSettings.updateProvider(value)
                                    updateVisibility()
                                }
                            }
                        )
                        .whenItemSelectedFromUi {
                            updateVisibility()
                        }
                }


                openAiKeyRow = row("OpenAI API Key") {
                    passwordField()
                        .bindText(llmSettings::getOpenAiKey, llmSettings::setOpenAiKey)
                        .comment("Get from https://platform.openai.com/account/api-keys")
                }

                openAiOrgRow = row("OpenAI Organization ID") {
                    passwordField()
                        .bindText(llmSettings::getOpenAiOrganization, llmSettings::setOpenAiOrganization)
                }

                geminiAiKeyRow = row("Gemini API Key") {
                    passwordField()
                        .bindText(llmSettings::getGeminiKey, llmSettings::setGeminiKey)
                }

                ollamaServerRow = row("Ollama Server URL") {
                    textField()
                        .bindText(llmSettings::getOllServer, llmSettings::setOllServer)
                }

                ollamaModelRow = row("Ollama Model") {
                    comboBox(DefaultComboBoxModel(arrayOf("llama2", "deepseek")))
                        .bindItem(
                            { llmSettings.ollamaModel },
                            { llmSettings.ollamaModel = it ?: "llama2" }
                        )
                }
            }

            group("GitHub Credentials") {
                row("GitHub Username") {
                    textField()
                        .bindText(githubSettings::getGitHubUsername, githubSettings::setGitHubUsername)
                }
                row("GitHub Token") {
                    passwordField()
                        .bindText(githubSettings::getGitHubToken, githubSettings::setGitHubToken)
                        .comment("Create a token at https://github.com/settings/tokens")
                }
            }

            onApply {
                // Optional: Validate or trigger updates
            }

            onReset {
                updateVisibility()
            }
        }.apply {
            updateVisibility()
        }
    }

    private fun updateVisibility() {
        val provider = llmSettings.getProvider()
        openAiKeyRow.visible(provider == LLMProvider.OPENAI)
        openAiOrgRow.visible(provider == LLMProvider.OPENAI)
        geminiAiKeyRow.visible(provider == LLMProvider.GEMINI)
        ollamaServerRow.visible(provider == LLMProvider.OLLAMA)
        ollamaModelRow.visible(provider == LLMProvider.OLLAMA)
    }
}
