package technicaldebt_plugin_fall2024.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.technicaldebt_plugin_fall2024.settings.LLMSettingsManager
import com.technicaldebt_plugin_fall2024.ui.DescriptionTab
import technicaldebt_plugin_fall2024.actions.LLMActivator
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.border.LineBorder

class LLMOutputToolWindow : ToolWindowFactory {
    companion object {
        private val settings = service<LLMSettingsManager>()
        private var textArea: JBTextArea? = null
        private var applyButton: JButton? = null
        private var latestEditor: Editor? = null
        private var latestProject: Project? = null
        private var latestTextRange: TextRange? = null
        private var editorField1: EditorTextField? = null
        private var editorField2: EditorTextField? = null
        private var llmLabel: JLabel? = null
        private var editor1Selected: Boolean = false
        private var editor2Selected: Boolean = false
        private var llmRequested: Boolean = true

        fun updateOutput(text1: String, text2: String, editor: Editor, project: Project, textRange: TextRange) {
            SwingUtilities.invokeLater {
                editorField1?.setText(text1)
                editorField1?.revalidate()
                editorField1?.repaint()
                editorField1?.parent?.revalidate()
                editorField1?.parent?.repaint()

                editorField2?.setText(text2)
                editorField2?.revalidate()
                editorField2?.repaint()
                editorField2?.parent?.revalidate()
                editorField2?.parent?.repaint()
            }
            llmRequested = true
            latestEditor = editor
            latestProject = project
            latestTextRange = textRange
        }

        private fun applyChanges() {
            val editor = latestEditor
            val project = latestProject
            val textRange = latestTextRange

            val newText = if (editor1Selected) {
                editorField1?.text ?: return
            } else {
                editorField2?.text ?: return
            }

            if (editor != null && project != null && textRange != null) {
                WriteCommandAction.runWriteCommandAction(project) {
                    LLMActivator.updateDocument(project, newText, editor.document, textRange)
                }
                applyButton?.isEnabled = false

                editorField1?.text = ""
                editorField2?.text = ""

                llmRequested = false
            }
        }
    }

    private fun runFileInIDEA(project: Project, editor: Editor) {
        // Placeholder for running the file in IDEA
        // Implementation depends on your specific requirements
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
            backgroundColor = Color.YELLOW
        }

        editor.markupModel.addRangeHighlighter(
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
            llmLabel?.text = "Active LLM: ${settings.getProvider()}"
            llmLabel?.revalidate()
            llmLabel?.repaint()
        }
    }

    private fun createCodeEditor(project: Project): EditorTextField {
        val factory = EditorFactory.getInstance()
        val document = factory.createDocument("fun main() {\n    println(\"LLM Output Will be Displayed Here!\")\n}")
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("java")

        return EditorTextField(document, project, fileType, false, false).apply {
            (this.editor as? EditorEx)?.apply {
                settings.isLineNumbersShown = true
                settings.isIndentGuidesShown = true
                settings.isFoldingOutlineShown = true
            }
        }
    }

    fun createButtonPanel(buttonSize: Dimension, editorField: EditorTextField): JPanel {
        return JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
            val copyButton = JButton(TechnicalDebtIcons.copy).apply {
                toolTipText = "Copy to Clipboard"
                isBorderPainted = false
                isContentAreaFilled = false
                preferredSize = buttonSize
                addActionListener {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    val selection = StringSelection(editorField?.text ?: "")
                    clipboard.setContents(selection, selection)
                    background = Color.LIGHT_GRAY
                    isContentAreaFilled = true
                    SwingUtilities.invokeLater {
                        Thread.sleep(200)
                        isContentAreaFilled = false
                        repaint()
                    }
                }
            }
            add(copyButton)
        }
    }

    private fun createDescriptionPanel(): JPanel {
        val panel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        val descriptionText = """
        This panel provides the following functionalities:
        - Copy: Allows copying the selected output text.
        - Select Output: You must select an output to enable the Apply button.
    """.trimIndent()

        val descriptionTextArea = JTextArea(descriptionText).apply {
            font = UIManager.getFont("Label.font")?.deriveFont(14f) ?: Font("SansSerif", Font.PLAIN, 14)
            lineWrap = true
            wrapStyleWord = true
            isEditable = false
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            background = panel.background
        }

        val scrollPane = JScrollPane(descriptionTextArea).apply {
            preferredSize = Dimension(300, 100)
            border = BorderFactory.createLineBorder(Color.GRAY, 1)
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabbedPane = JTabbedPane()

        // Create the DescriptionTab as the landing page
        val descriptionTab = DescriptionTab(project)
        val descriptionPanel = descriptionTab.content

        val selectButton1 = JToggleButton().apply {
            toolTipText = "Select Output 1"
            icon = TechnicalDebtIcons.accept
            isContentAreaFilled = false
            isFocusPainted = false
            border = BorderFactory.createEmptyBorder()
            background = Color.GRAY
            preferredSize = Dimension(20, 20)
        }

        val selectButton2 = JToggleButton().apply {
            toolTipText = "Select Output 2"
            icon = TechnicalDebtIcons.accept
            isContentAreaFilled = false
            isFocusPainted = false
            border = BorderFactory.createEmptyBorder()
            background = Color.GRAY
            preferredSize = Dimension(20, 20)
        }

        selectButton1.addActionListener {
            if (!editor1Selected) {
                selectButton1.background = Color.BLUE
                selectButton2.background = Color.GRAY
                editorField1?.border = BorderFactory.createLineBorder(Color.BLUE)
                editorField2?.border = BorderFactory.createEmptyBorder()
                editor1Selected = true
                editor2Selected = false
                if (llmRequested) applyButton?.isEnabled = true
            } else {
                selectButton1.background = Color.GRAY
                editorField1?.border = BorderFactory.createEmptyBorder()
                editor1Selected = false
                applyButton?.isEnabled = false
            }
        }

        selectButton2.addActionListener {
            if (!editor2Selected) {
                selectButton2.background = Color.BLUE
                selectButton1.background = Color.GRAY
                editorField2?.border = BorderFactory.createLineBorder(Color.BLUE)
                editorField1?.border = BorderFactory.createEmptyBorder()
                editor2Selected = true
                editor1Selected = false
                if (llmRequested) applyButton?.isEnabled = true
            } else {
                selectButton2.background = Color.GRAY
                editorField2?.border = BorderFactory.createEmptyBorder()
                editor2Selected = false
                applyButton?.isEnabled = false
            }
        }

        val panel = JPanel(BorderLayout()).apply {
            editorField1 = createCodeEditor(project)
            editorField2 = createCodeEditor(project)

            val scrollPane = JBScrollPane(editorField1).apply {
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = Dimension(400, 300)
            }

            val scrollPane2 = JBScrollPane(editorField2).apply {
                horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                preferredSize = Dimension(400, 300)
            }

            val buttonSize = Dimension(30, 30)

            llmLabel = JLabel("Active LLM: ${settings.getProvider()}").apply {
                font = font.deriveFont(Font.BOLD, 16f)
                horizontalAlignment = SwingConstants.CENTER
                border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
            }

            val combinedTopPanel = JPanel(BorderLayout()).apply {
                add(llmLabel, BorderLayout.NORTH)
                add(JSeparator(JSeparator.HORIZONTAL).apply {
                    border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
                }, BorderLayout.CENTER)
            }

            add(combinedTopPanel, BorderLayout.NORTH)

            val firstWindow = JPanel(BorderLayout()).apply {
                val topPanel = JPanel(BorderLayout()).apply {
                    val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        val pageLabel = JLabel("< 1/1 >").apply {
                            font = font.deriveFont(14f)
                        }
                        add(pageLabel)
                    }
                    add(leftPanel, BorderLayout.WEST)

                    val editorFieldSafe1 = editorField1 ?: return
                    val rightPanel = createButtonPanel(buttonSize, editorFieldSafe1)
                    add(rightPanel, BorderLayout.EAST)
                }

                val combinedPanel = JPanel(BorderLayout()).apply {
                    add(topPanel, BorderLayout.NORTH)
                    add(scrollPane, BorderLayout.CENTER)
                    add(selectButton1, BorderLayout.WEST)
                    val editor = latestEditor
                    val runButton = JButton("Run", TechnicalDebtIcons.run).apply {
                        toolTipText = "Execute the current file"
                        isFocusPainted = false
                        margin = Insets(5, 15, 5, 15)
                        addActionListener {
                            editor1Selected = true
                            editor2Selected = false
                            applyChanges()

                            editorField1?.text = ""
                            editorField2?.text = ""
                            applyButton?.isEnabled = false
                            llmRequested = false
                            toolWindow.hide(null)

                            if (editor != null) {
                                runFileInIDEA(project, editor)
                            }
                        }
                    }
                    val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        border = BorderFactory.createEmptyBorder(5, 5, 5, 10)
                        add(runButton)
                    }
                    add(buttonPanel, BorderLayout.SOUTH)
                }
                add(combinedPanel, BorderLayout.CENTER)
            }

            val secondWindow = JPanel(BorderLayout()).apply {
                val topPanel2 = JPanel(BorderLayout()).apply {
                    val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        val pageLabel = JLabel("< 1/1 >").apply {
                            font = font.deriveFont(14f)
                        }
                        add(pageLabel)
                    }
                    add(leftPanel, BorderLayout.WEST)

                    val editorFieldSafe2 = editorField2 ?: return
                    val rightPanel = createButtonPanel(buttonSize, editorFieldSafe2)
                    add(rightPanel, BorderLayout.EAST)
                }
                val combinedPanel = JPanel(BorderLayout()).apply {
                    add(topPanel2, BorderLayout.NORTH)
                    add(selectButton2, BorderLayout.WEST)
                    add(scrollPane2, BorderLayout.CENTER)
                    val editor = latestEditor
                    val runButton = JButton("Run", TechnicalDebtIcons.run).apply {
                        toolTipText = "Execute the current file"
                        isFocusPainted = false
                        margin = Insets(5, 15, 5, 15)
                        addActionListener {
                            editor2Selected = true
                            editor1Selected = false
                            applyChanges()

                            editorField1?.text = ""
                            editorField2?.text = ""
                            applyButton?.isEnabled = false
                            llmRequested = false
                            toolWindow.hide(null)

                            if (editor != null) {
                                runFileInIDEA(project, editor)
                            }
                        }
                    }
                    val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                        border = BorderFactory.createEmptyBorder(5, 5, 5, 10)
                        add(runButton)
                    }
                    add(buttonPanel, BorderLayout.SOUTH)
                }
                add(combinedPanel, BorderLayout.CENTER)
            }

            val combinedWindow = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(firstWindow)

                val separatorPanel = JPanel(BorderLayout()).apply {
                    border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
                    add(JSeparator(JSeparator.HORIZONTAL), BorderLayout.CENTER)
                }
                add(separatorPanel)
                add(secondWindow)
            }

            add(combinedWindow, BorderLayout.CENTER)

            applyButton = JButton("Apply", TechnicalDebtIcons.accept).apply {
                isEnabled = false
                addActionListener {
                    applyChanges()
                    toolWindow.hide(null)
                }
            }


            val separatorPanel = JPanel(BorderLayout()).apply {
                border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
                add(JSeparator(JSeparator.HORIZONTAL), BorderLayout.CENTER)
            }

            val bottomPanel = JPanel(BorderLayout()).apply {
                val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER)).apply {
                    add(applyButton)
                }
                add(separatorPanel, BorderLayout.NORTH)
                add(buttonPanel, BorderLayout.CENTER)
            }

            add(bottomPanel, BorderLayout.SOUTH)
        }

        // Add tabs to the tabbed pane
        tabbedPane.addTab("Description", descriptionPanel)
        tabbedPane.addTab("Generated Output", panel)

        // Set "Description" as the default tab
        tabbedPane.selectedIndex = 0

        val newPanel = JPanel(BorderLayout())
        newPanel.add(tabbedPane, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(newPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createCoveragePanel(project: Project): JPanel {
        val panel = JPanel()
        val test = "Test Result"
        val mutant = "Mutant Result"

        panel.add(ActionLink(test) {
            latestEditor?.let { highlightTestCase(test, it) }
        })

        panel.add(ActionLink(mutant) {
            latestEditor?.let { highlightMutantsInToolwindow(mutant, it) }
        })

        return panel
    }

    private fun highlightMutantsInToolwindow(mutantName: String, editor: Editor) {
        val line = 10
        highlightLine(editor, line)
    }
}