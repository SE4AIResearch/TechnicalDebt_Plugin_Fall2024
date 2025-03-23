package com.intellij.ml.llm.template.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import java.awt.BorderLayout
import javax.swing.JLabel
import com.intellij.openapi.wm.ToolWindowFactory
import javax.swing.BoxLayout
import java.awt.datatransfer.StringSelection
import com.intellij.ui.components.JBScrollPane
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.Dimension
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.ActionLink
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import javax.swing.BorderFactory
import javax.swing.SwingUtilities
import com.intellij.ml.llm.template.intentions.ApplyTransformationIntention
import com.intellij.ml.llm.template.settings.LLMSettingsManager
import javax.swing.JButton
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.ScrollType
import java.awt.Color
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.ui.EditorTextField

class LLMOutputToolWindow : ToolWindowFactory {
    companion object {
        private val llmSettingsManager: LLMSettingsManager by lazy {
            // Get the LLMSettingsManager service when needed
            service<LLMSettingsManager>()
        }
        private lateinit var textArea: JBTextArea
        private lateinit var applyButton: JButton
        private var latestEditor: Editor? = null
        private var latestProject: Project? = null
        private var latestTextRange: TextRange? = null
        private var editorField: EditorTextField? = null
        private var llmLabel: JLabel? = null

        fun updateOutput(text: String, editor: Editor, project: Project, textRange: TextRange) {
            //(editorField as? EditorTextField)?.setText(text)

            SwingUtilities.invokeLater {
                editorField?.setText(text)
                editorField?.revalidate()
                editorField?.repaint()
                editorField?.parent?.revalidate()
                editorField?.parent?.repaint()
            }

            applyButton?.isEnabled = true
            latestEditor = editor
            latestProject = project
            latestTextRange = textRange
        }

        private fun applyChanges(){
            val editor = latestEditor
            val project = latestProject
            val textRange = latestTextRange
            //val newText = textArea?.text ?: return
            val newText = editorField?.text ?: return

            if (editor != null && project != null && textRange != null) {
                WriteCommandAction.runWriteCommandAction(project) {
                    ApplyTransformationIntention.updateDocument(project, newText, editor.document, textRange)
                }
                applyButton?.isEnabled = false
            }
        }

    }


    private fun highlightTestCase(testName: String, editor: Editor) {
        val line = 5
        highlightLine(editor, line)
    }

    private fun highlightLine(editor: Editor, lineNumber: Int) {
        val document = editor.document
        val startOffset = document.getLineStartOffset(lineNumber)
        val endOffset = document.getLineEndOffset(lineNumber)

        val highlightAttributes = TextAttributes().apply {
            backgroundColor = Color.YELLOW  // Set background color to yellow for highlighting
        }

        // Create a highlight on the selected line
        val highlighter = editor.markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                highlightAttributes,
                HighlighterTargetArea.EXACT_RANGE
        )

        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    fun updateLLMType() {
        SwingUtilities.invokeLater {
            llmLabel?.text = "Active LLM: ${llmSettingsManager.provider}"
            llmLabel?.revalidate()
            llmLabel?.repaint()
        }
    }


    private fun createCodeEditor(project: Project): EditorTextField{
        val factory = EditorFactory.getInstance()
        val document = factory.createDocument("fun main() {\n    println(\"Hello, World!\")\n}")
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("java")

        return EditorTextField(document, project, fileType, true, false).apply {
            (this.editor as? EditorEx)?.apply {
                settings.isLineNumbersShown = true
                settings.isIndentGuidesShown = true
                settings.isFoldingOutlineShown = true
            }
        }

    }
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel().apply{
            val topPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                val pageLabel = JLabel("<1/1>").apply {
                    font = font.deriveFont(14f)
                }
                add(pageLabel)


            }
            add(topPanel, BorderLayout.NORTH)

            editorField = createCodeEditor(project)

            val scrollPane = JBScrollPane(editorField).apply {
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = Dimension(400, 300)
            }

            llmLabel = JLabel("Active LLM: ${llmSettingsManager.provider}").apply{
                font = font.deriveFont(14f)
            }
            add(llmLabel)



            val copyButton = JButton("📋").apply {
                toolTipText = "Copy to Clipboard"
                border = BorderFactory.createEmptyBorder()
                isContentAreaFilled = false
                addActionListener {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    val selection = StringSelection(editorField?.text ?: "")
                    clipboard.setContents(selection, selection)


                    background = Color.LIGHT_GRAY
                    isContentAreaFilled = true


                    SwingUtilities.invokeLater {
                        Thread.sleep(200)  // Delay for visual feedback
                        isContentAreaFilled = false
                        repaint()
                    }
                }
            }



            val centerPanel = JPanel(BorderLayout()).apply {
                add(scrollPane, BorderLayout.CENTER)
                val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                    add(copyButton)
                }
                add(buttonPanel, BorderLayout.NORTH)
            }

            add(centerPanel, BorderLayout.CENTER)

            applyButton = JButton("Apply Changes").apply {
                isEnabled = false
                addActionListener { applyChanges() }
            }

            val rejectButton = JButton("Reject").apply {
                addActionListener {
                    editorField?.text = "fun main() {\n    println(\"Hello, World!\")\n}"
                    toolWindow.hide(null)
                }
            }

            val buttonPanel = JPanel().apply {
                add(applyButton)
                add(rejectButton)
            }
            add(buttonPanel, BorderLayout.SOUTH)
        }



        /*layout = BoxLayout(this, BoxLayout.Y_AXIS)
        /*textArea = JBTextArea(20, 50).apply {
            isEditable = false
            lineWrap = false
            wrapStyleWord = true
            text = "Welcome to the LLM Output Window!\n\nThis is an example text.\nNew output will be displayed here."
        }*/






        //val scrollPane = JBScrollPane(textArea)

        editorField = createCodeEditor(project)

        val scrollPane = JBScrollPane(editorField).apply {
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            preferredSize = Dimension(500, 300)
        }


        applyButton = JButton("Apply Changes").apply {
            isEnabled = false  // Initially disabled until an update is received
            addActionListener { applyChanges() }
        }

        val rejectButton = JButton("Reject").apply {
            addActionListener {
                editorField?.text = "fun main() {\n    println(\"Hello, World!\")\n}"
                toolWindow.hide(null)
            }
        }

        val copyButton =  JButton("📋").apply {
            toolTipText = "Copy to Clipboard"
            border = BorderFactory.createEmptyBorder()  // Remove button border
            isContentAreaFilled = false  // Remove background fill
            addActionListener {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(editorField?.text ?: "")
                clipboard.setContents(selection, selection)
            }
        }


        val coveragePanel = createCoveragePanel(project)
        //panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(scrollPane)
        panel.add(editorField)
        panel.add(applyButton)
        panel.add(rejectButton)
        panel.add(copyButton)
        //panel.add(coveragePanel)

*/
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)


    }


    private fun createCoveragePanel(project: Project): JPanel {
        val panel = JPanel()
        val test = "Test Result"  // Mock test result
        val mutant = "Mutant Result"  // Mock mutant result

        // Add actionable links
        panel.add(ActionLink(test) {
            // When the test result is clicked, highlight the relevant line in the editor
            latestEditor?.let { highlightTestCase(test, it) }
        })

        panel.add(ActionLink(mutant) {
            // Highlight relevant mutant when clicked
            latestEditor?.let { highlightMutantsInToolwindow(mutant, it) }
        })

        return panel
    }

    private fun highlightMutantsInToolwindow(mutantName: String, editor: Editor) {
        // You can adjust this to highlight based on the mutant position or logic
        // For now, we just highlight a mock line
        val line = 10  // Example mutant line number
        highlightLine(editor, line)
    }
}
