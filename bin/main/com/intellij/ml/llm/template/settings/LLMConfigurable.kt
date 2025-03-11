package com.intellij.ml.llm.template.settings

import com.intellij.ml.llm.template.LLMBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComponentPredicate
import javax.swing.DefaultComboBoxModel
import javax.swing.JTextArea
import javax.swing.JScrollPane




class LLMConfigurable : BoundConfigurable(LLMBundle.message("settings.configurable.display.name")) {
    private val settings = service<LLMSettingsManager>()

    private lateinit var geminiAiKeyRow: Row
    private lateinit var openAiKeyRow: Row
    private lateinit var openAiOrgRow: Row
    private lateinit var ollamaServerRow: Row

    private val outputTextArea = JTextArea().apply {
        isEditable = false  // Prevent user input
        lineWrap = true
        wrapStyleWord = true
    }



    override fun createPanel(): DialogPanel {
        return panel {

            lateinit var providerComboBox: Cell<ComboBox<LLMSettingsManager.LLMProvider>>

            openAiKeyRow = row(LLMBundle.message("settings.configurable.openai.key.label")) {

                passwordField().bindText(
                    settings::getOpenAiKey, settings::setOpenAiKey
                )
                browserLink("Sign up for API key", "https://platform.openai.com/signup")
            }

            openAiOrgRow = row(LLMBundle.message("settings.configurable.openai.organization.label")) {
                passwordField().bindText(
                    settings::getOpenAiOrganization, settings::setOpenAiOrganization
                )
            }

            ollamaServerRow = row(LLMBundle.message("settings.configurable.ollama.server.label")) {
                textField().bindText(settings::getOllServer, settings::setOllServer)
            }

            ollamaServerRow.visible(false)

            geminiAiKeyRow = row(LLMBundle.message("settings.configurable.gemini.key.label")) {
                passwordField().bindText(
                        settings::getGeminiKey, settings::setGeminiKey
                )
            }

            row(LLMBundle.message("settings.configurable.llm.provider.label")) {
                providerComboBox = comboBox(
                    DefaultComboBoxModel(LLMSettingsManager.LLMProvider.values())
                ).bindItem(
                    {settings.provider},
                    {value ->
                        if (value != null) {
                            settings.updateProvider(value)
                            updateVisibility()
                        }
                    }
                ).also{
                    it.whenItemSelectedFromUi { updateVisibility() }
                }
            }

        }

        updateVisibility()
    }

    fun updateVisibility() {
        val isGemini = settings.provider == LLMSettingsManager.LLMProvider.GEMINI
        val isOpenAi = settings.provider == LLMSettingsManager.LLMProvider.OPENAI
        val isOllama = settings.provider == LLMSettingsManager.LLMProvider.OLLAMA
        geminiAiKeyRow.visible(isGemini)
        openAiKeyRow.visible(isOpenAi)
        openAiOrgRow.visible(isOpenAi)
        ollamaServerRow.visible(isOllama)

        apply()


    }


    fun updateOutput(output: String) {
        outputTextArea.text = output
    }
    override fun apply() {
        super.apply()
        updateVisibility()
    }
}

fun openSettingsDialog(project: Project?) {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, LLMConfigurable::class.java)
}
