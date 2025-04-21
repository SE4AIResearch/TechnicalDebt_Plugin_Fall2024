package technicaldebt_plugin_fall2024.toolWindow

import technicaldebt_plugin_fall2024.actions.LLMActivator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import technicaldebt_plugin_fall2024.toolWindow.Without.SATDDatabaseManager
import technicaldebt_plugin_fall2024.toolWindow.Without.SATDFileManager
import com.intellij.openapi.editor.Document
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.ui.components.JBPanel
import javax.swing.JComponent


import java.awt.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.DefaultTableCellRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory

fun getCurrentEditor(project: Project): Editor? {
    return FileEditorManager.getInstance(project).selectedTextEditor
}


private fun getParentNamedElement(editor: Editor): PsiNameIdentifierOwner? {
    val element = PsiUtilBase.getElementAtCaret(editor)
    return PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
}


private fun getLineTextRange(document: Document, editor: Editor): TextRange {
    val lineNumber = document.getLineNumber(editor.caretModel.offset)
    val startOffset = document.getLineStartOffset(lineNumber)
    val endOffset = document.getLineEndOffset(lineNumber)
    return TextRange.create(startOffset, endOffset)
}

class SATDToolWindowFactory : ToolWindowFactory, DumbAware {
    private val satdFileManager = SATDFileManager()
    private val satdDatabaseManager = SATDDatabaseManager()
    private var isJumpedToMethod = false


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = JBPanel<JBPanel<*>>()
        toolWindowPanel.layout = BorderLayout()
//        val tabbedPane = JTabbedPane()

        val label = JBLabel("Retrieve the latest SATD data: ")
//        toolWindowPanel.add(label, BorderLayout.NORTH)
        val button = JButton("Fetch SATD").apply {
////            icon = ImageIcon("src/main/resources/assets/load.png")
//            preferredSize = Dimension(140, 30)
//            background = Color(0x2E8B57)
//            foreground = JBColor.WHITE
//            font = Font("Arial", Font.BOLD, 12)
            toolTipText = "Load the SATD records into the table"
        }

        val pathLabel = JLabel("Path to SATD: ")
        val linesLabel = JBLabel("Lines: [,]")
//        val resolutionLabel = JBLabel("Resolution: []")
//        val refactoringLabel = JBLabel("Refactoring: []")

        val bottomPanel = JPanel(BorderLayout())


        /*val sendToLLMButton = JButton("Send to LLM").apply {
            isEnabled = false
        }

        sendToLLMButton.addActionListener {
            val editor = getCurrentEditor(project)
            val document = editor?.document ?: return@addActionListener
            val selectionModel = editor.selectionModel
            val selectedText = selectionModel.selectedText ?: return@addActionListener
            val textRange = TextRange(selectionModel.selectionStart, selectionModel.selectionEnd)



            val satdType = resolutionLabel.text.removePrefix("Resolution:").trim()


            LLMActivator.transform(project, selectedText, editor, textRange, satdType)
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("LLM Output")
            toolWindow?.show(null)
        }

        val centerPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        centerPanel.add(sendToLLMButton)

        bottomPanel.add(centerPanel, BorderLayout.CENTER)
        //wadwa*/


        // info on the left
        val leftPanel = JPanel(GridBagLayout())
        val leftConstraints = GridBagConstraints().apply {
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(5, 10)
        }
        leftPanel.add(pathLabel, leftConstraints)
        leftConstraints.gridx = 1
        leftPanel.add(linesLabel, leftConstraints)

        // info on the right
        val rightPanel = JPanel(GridBagLayout())
        val rightConstraints = GridBagConstraints().apply {
            anchor = GridBagConstraints.EAST
            insets = JBUI.insets(5, 10)
        }
//      rightPanel.add(resolutionLabel, rightConstraints)
//        rightConstraints.gridx = 1
//        rightPanel.add(refactoringLabel, rightConstraints)
//        rightConstraints.gridx = 2



        // Add left and right subpanels to bottom panel
        bottomPanel.add(leftPanel, BorderLayout.WEST)
        bottomPanel.add(rightPanel, BorderLayout.EAST)

        // Now add this combined bottom panel to the main toolWindowPanel
        toolWindowPanel.add(bottomPanel, BorderLayout.SOUTH)

        val tableModel = object : DefaultTableModel(){
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }
        tableModel.addColumn("File ID")
        tableModel.addColumn("Comment")
        tableModel.addColumn("Containing Class")
        tableModel.addColumn("Containing Method")
        tableModel.addColumn("Resolution")
        tableModel.addColumn("Refactoring")


        val table = JTable(tableModel)
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.columnModel.getColumn(1).preferredWidth = 500
        table.isEnabled = false
        table.columnModel.getColumn(1).cellRenderer = TextAreaRenderer()
        table.setShowGrid(false)
        table.intercellSpacing = Dimension(0, 0)

        for (col in 0 until table.columnCount) {
            if (col == 1) {
                table.columnModel.getColumn(col).cellRenderer = ThickTextAreaRenderer()
            } else {
                table.columnModel.getColumn(col).cellRenderer = ThickBorderCellRenderer()
            }
        }

        table.tableHeader.defaultRenderer = ThickHeaderRenderer()

        table.setCellSelectionEnabled(false)
        table.setColumnSelectionAllowed(false)
        table.setRowSelectionAllowed(true)

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        var currResolution: String? = null


        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val fileId = table.getValueAt(row, 0) as Int

                val satdInfo = satdDatabaseManager.getSATDTableInfo(project, fileId)
                currResolution = satdInfo.resolution!!

                var filePath: String? = null

                filePath = satdInfo.filePath!!
                val l1: Int? = satdInfo.startLine!!
                val l2: Int? = satdInfo.endLine!!


                if (e.clickCount == 1) {
                    //sendToLLMButton.isEnabled = false

                    //ActionManager.getInstance().getAction("Send to LLM").templatePresentation.isEnabled = false
                    isJumpedToMethod = false

                    pathLabel.text = "Path to SATD: $filePath   "
                    linesLabel.text = "Lines: [$l1, $l2]"



                    table.setRowSelectionInterval(row, row)

                }
                else if (e.clickCount == 2){
                    val jumped = satdFileManager.navigateToCode(project, l1, filePath)

                    if (jumped){
                        //sendToLLMButton.isEnabled = true
                        //ActionManager.getInstance().getAction("Send to LLM").templatePresentation.isEnabled = true
                        isJumpedToMethod = true
                    }
                    //invoke

                    val editor = getCurrentEditor(project)

                    val document = editor?.document
                    val selectionModel = editor?.selectionModel
                    val selectedText = selectionModel?.selectedText
                    if (selectedText != null) {
                        val textRange = TextRange.create(selectionModel.selectionStart, selectionModel.selectionEnd)
//                        transform(project, selectedText, editor, textRange)
                        // We don't want to transform just yet
                    } else {
                        val namedElement = editor?.let { getParentNamedElement(it) }
                        if (namedElement != null) {
                            val queryText = namedElement.text
                            val textRange = namedElement.textRange
                            selectionModel?.setSelection(textRange.startOffset, textRange.endOffset)
//                            transform(project, queryText, editor, textRange)
                        } else {
                            selectionModel?.selectLineAtCaret()
                            val textRange = editor?.let {
                                if (document != null) {
                                    getLineTextRange(document, it)
                                }
                            }
//                            transform(project, document.getText(textRange), editor, textRange)
                        }


                    }
                }
            }
        })

        val scrollPane = JBScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        scrollPane.preferredSize = Dimension(900, 250)
        scrollPane.setBorder(
            BorderFactory.createLineBorder(Color.BLACK, /*thickness=*/1)
        );
        toolWindowPanel.add(scrollPane, BorderLayout.CENTER)


        // Start mining SATD here
        val testRepo = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/test_repo.csv"
        ApplicationManager.getApplication().executeOnPooledThread {
            satdFileManager.writeTestRepo(testRepo, project)
        }

        try {
            Files.createDirectories(Paths.get(PathManager.getConfigPath() + "/databases"))
        } catch (e: IOException) {
            Messages.showWarningDialog("Error: " + e.message, "")
        }

        val db = File(PathManager.getConfigPath() + "/databases", project.name + ".db")
        try {
            if (db.createNewFile()) {
                satdDatabaseManager.initialize(label, project)
            } else {
                val message = "Loading existing SATD data for this  project. May not include most recent commits. Click \"Fetch SATD Data\" to update data"
                Messages.showWarningDialog(message, "")
                satdDatabaseManager.loadDatabase(tableModel, label, table, project)
            }
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }

        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Fetch SATD", "Load the SATD records into the table", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                satdDatabaseManager.initializeAndConnectDatabase(tableModel, label, table, project)
                }
            })

            add(object : AnAction("Send to LLM", "Send selected code to LLM for processing", AllIcons.RunConfigurations.Remote) {
                override fun update(e: AnActionEvent) {
                    // Only enable the action when text is selected in the editor
                    val editor = getCurrentEditor(project)
                    val selectionModel = editor?.selectionModel
                    e.presentation.isEnabled = isJumpedToMethod && selectionModel?.hasSelection() == true
                }

                override fun actionPerformed(e: AnActionEvent) {
                    val editor = getCurrentEditor(project)
                    val document = editor?.document ?: return
                    val selectionModel = editor.selectionModel
                    val selectedText = selectionModel.selectedText ?: return
                    val textRange = TextRange(selectionModel.selectionStart, selectionModel.selectionEnd)
//w
                    val satdType = currResolution

                    if(satdType == null)
                    {
                        Messages.showWarningDialog("Please select a resolution before sending to LLM", "")
                        return
                    }
                    LLMActivator.transform(project, selectedText, editor, textRange, satdType)
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("LLM Output")
                    toolWindow?.show(null)
                }
            })

    }

        val toolbar = ActionManager.getInstance().createActionToolbar("SATDToolbar", actionGroup, true)
        toolbar.targetComponent = toolWindowPanel
        toolWindow.setTitleActions(actionGroup.getChildren(null).toList())

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(toolWindowPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    open class TextAreaRenderer : JTextArea(), TableCellRenderer {
        init {
            lineWrap = true
            wrapStyleWord = true
            isOpaque = true
        }

        override fun getTableCellRendererComponent(
                table: JTable,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
        ): Component {
            text = value?.toString() ?: ""
            setSize(table.columnModel.getColumn(column).width, preferredSize.height)

            if (table.getRowHeight(row) != preferredSize.height) {
                table.setRowHeight(row, preferredSize.height)
            }

            background = if (isSelected) table.selectionBackground else table.background
            foreground = if (isSelected) table.selectionForeground else table.foreground
            return this
        }
    }
    class ThickTextAreaRenderer : TextAreaRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean,
            hasFocus: Boolean, row: Int, column: Int
        ): Component {
            val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JComponent

            // Outer border is now 1px; inner padding remains.
            val thinBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK)
            val padding = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            comp.border = BorderFactory.createCompoundBorder(thinBorder, padding)
            return comp
        }
    }

    class ThickBorderCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean,
            row: Int, column: Int
        ): Component {
            val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JComponent

            val thinBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK)
            val padding = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            comp.border = BorderFactory.createCompoundBorder(thinBorder, padding)
            return comp
        }
    }

    class ThickHeaderRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JComponent

            comp.background = Color.DARK_GRAY
            comp.foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD)

            val thinBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK)
            val padding = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            comp.border = BorderFactory.createCompoundBorder(thinBorder, padding)

            return comp
        }
    }

    companion object {
        fun adjustColumnWidths(table: JTable) {
            for (col in 0 until table.columnCount) {
                if (col != 1 && col != 8 && col != 9) {
                    val column = table.columnModel.getColumn(col)
                    val minWidth = getTextWidth(table, column.headerValue.toString(), table.font)
                    var maxWidth = minWidth

                    for (row in 0 until table.rowCount) {
                        val value = table.getValueAt(row, col)
                        if (value != null) {
                            val cellWidth = getTextWidth(table, value.toString(), table.font)
                            if (cellWidth > maxWidth) {
                                maxWidth = cellWidth
                            }
                        }
                    }
                    column.minWidth = minWidth + 10
                    column.preferredWidth = maxWidth + 20
                }
            }
        }

        private fun getTextWidth(table: JTable, text: String, font: Font): Int {
            val metrics = table.getFontMetrics(font)
            return metrics.stringWidth(text)
        }
    }
}
