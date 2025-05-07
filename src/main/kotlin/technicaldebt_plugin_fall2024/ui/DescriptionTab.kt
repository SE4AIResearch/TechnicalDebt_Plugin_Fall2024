package com.technicaldebt_plugin_fall2024.ui

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.technicaldebt_plugin_fall2024.settings.LLMConfigurable
import com.technicaldebt_plugin_fall2024.settings.TechnicalDebtToolConfigurable
import technicaldebt_plugin_fall2024.ui.PluginLabelsBundle
import technicaldebt_plugin_fall2024.ui.TechnicalDebtIcons
import java.awt.*
import java.net.URI
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.HyperlinkEvent

class DescriptionTab(private val project: Project?) {
    private val titlePanel = JPanel(BorderLayout()).apply {
        border = EmptyBorder(0, 0, 10, 0)
        add(JLabel(TechnicalDebtIcons.pluginIcon), BorderLayout.WEST)
        add(JLabel(PluginLabelsBundle.get("quickAccess")).apply {
            font = Font("Segoe UI", Font.BOLD, 20)
        }, BorderLayout.CENTER)
    }

    private val pluginDescription = makeTextPane { getCommonDescriptionText(content.preferredSize.width) }
    private val llmDescription = makeTextPane { getLLMDescriptionText(content.preferredSize.width) }

    private val llmSettingsButton = makeButton(PluginLabelsBundle.get("llmSettingsLink"), TechnicalDebtIcons.settings) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, LLMConfigurable::class.java)
    }

    private val settingsButton = makeButton(PluginLabelsBundle.get("settingsLink"), TechnicalDebtIcons.settings) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, TechnicalDebtToolConfigurable::class.java)
    }

    private val documentationButton = makeButton(PluginLabelsBundle.get("documentationLink"), TechnicalDebtIcons.documentation) {
        Desktop.getDesktop().browse(URI("https://github.com/SE4AIResearch/TechnicalDebt_Plugin_Fall2024"))
    }

    private val mainPanel = FormBuilder.createFormBuilder()
        .addComponent(titlePanel)
        .addComponent(makeInfoCard("About the Plugin", pluginDescription))
        .addComponent(makeInfoCard("LLM Integration", llmDescription))
        .addComponent(makeButtonPanel())
        .addComponentFillVertically(JPanel(), 20)
        .panel

    val content: JComponent
        get() = JBScrollPane(mainPanel)

    private fun makeTextPane(contentProvider: () -> String): JTextPane =
        JTextPane().apply {
            isEditable = false
            isOpaque = false
            contentType = "text/html"
            addHyperlinkListener { e ->
                if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.url.toURI())
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
            text = contentProvider()
        }

    private fun makeButton(label: String, icon: Icon, action: () -> Unit): JButton =
        JButton(label, icon).apply {
            isOpaque = false
            isFocusPainted = false
            isContentAreaFilled = false
            border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
            preferredSize = Dimension(preferredSize.width, 32)
            addActionListener { action() }
        }

    private fun makeInfoCard(title: String, content: JComponent): JPanel =
        JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(200, 200, 200), 1), // 🔳 Light gray line
                BorderFactory.createEmptyBorder(10, 10, 20, 10)           // Padding inside the border
            )
            background = Color(250, 250, 250) // Optional: light background for card contrast

            add(JLabel(title).apply {
                font = Font("Segoe UI", Font.BOLD, 16)
                border = BorderFactory.createEmptyBorder(0, 0, 8, 0)
            }, BorderLayout.NORTH)

            add(content, BorderLayout.CENTER)
        }


    private fun makeButtonPanel(): JPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)
            add(wrapButton(settingsButton))
            add(Box.createVerticalStrut(20))
            add(wrapButton(documentationButton))
        }

    private fun wrapButton(button: JButton): JPanel =
        JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            )
            background = Color(245, 245, 245)

            // Constrain the panel size to tightly wrap the button
            maximumSize = Dimension(button.preferredSize.width + 20, button.preferredSize.height + 10)
            alignmentX = Component.LEFT_ALIGNMENT

            add(button, BorderLayout.WEST)
        }



    private fun getCommonDescriptionText(width: Int): String = """
        <html>
        <body style='width: ${width - 40}px; font-family: Segoe UI, Arial, sans-serif; font-size: 13px;'>
            <p>The Technical Debt Plugin helps you identify and resolve Self-Admitted Technical Debt (SATD) 
            in your Java codebase at class, method, and line levels.</p>
            <p>Key features include:</p>
            <ul>
                <li>Automated SATD detection</li>
                <li>Visualization of technical debt</li>
                <li>Code improvement suggestions</li>
                <li>Debt prioritization tools</li>
            </ul>
            <p>Developed under <a href='https://www.stevens.edu/profile/ealomar'>Professor Eman AlOmar's Lab</a> 
            at Stevens Institute of Technology.</p>
        </body>
        </html>
    """.trimIndent()

    private fun getLLMDescriptionText(width: Int): String = """
        <html>
        <body style='width: ${width - 40}px; font-family: Segoe UI, Arial, sans-serif; font-size: 13px;'>
            <p>The plugin leverages AI models to generate test cases and suggest code improvements 
            for addressing technical debt.</p>
            <p>Configure your preferred AI provider:</p>
            <ul>
                <li><a href='https://openai.com'>OpenAI</a> - GPT models</li>
                <li><a href='https://gemini.google.com'>Google Gemini</a> - Gemini models</li>
                <li>Local <a href='https://ollama.com'>Ollama</a> setup</li>
            </ul>
            <p>Set up your API tokens and model preferences in the LLM Settings.</p>
        </body>
        </html>
    """.trimIndent()
}
