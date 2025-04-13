package technicaldebt_plugin_fall2024.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressManager
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
import com.intellij.ml.llm.template.intentions.ApplyTransformationIntention
import com.intellij.openapi.editor.Document

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

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

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = JPanel(BorderLayout())
//        val tabbedPane = JTabbedPane()

        val label = JBLabel("Retrieve the latest SATD data: ")
        val button = JButton("Fetch").apply {
            icon = ImageIcon("src/main/resources/assets/load.png")
            preferredSize = Dimension(140, 30)
            background = Color(0x2E8B57)
            foreground = Color.WHITE
            font = Font("Arial", Font.BOLD, 12)
            toolTipText = "Load the SATD records into the table"
        }

        val resolutionLabel = JBLabel("Resolution:")
        val refactoringLabel = JBLabel("Refactoring:")

        val bottomPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        bottomPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        bottomPanel.add(label)
        bottomPanel.add(button)
        bottomPanel.add(resolutionLabel)
        bottomPanel.add(refactoringLabel)
        toolWindowPanel.add(bottomPanel, BorderLayout.SOUTH)

        val tableModel = object : DefaultTableModel(){
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }
        tableModel.addColumn("File ID")
        tableModel.addColumn("Comment")
        tableModel.addColumn("Path")
        tableModel.addColumn("Start Line")
        tableModel.addColumn("End Line")
        tableModel.addColumn("Containing Class")
        tableModel.addColumn("Containing Method")

        val table = JTable(tableModel)
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.getColumnModel().getColumn(1).preferredWidth = 500
        table.isEnabled = true
        table.getColumnModel().getColumn(1).cellRenderer = TextAreaRenderer()

        table.setCellSelectionEnabled(false)
        table.setColumnSelectionAllowed(false)
        table.setRowSelectionAllowed(true)

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val path = table.getValueAt(row, 2) as String
                val line = table.getValueAt(row, 3) as Int


                val file_id = table.getValueAt(row, 0) as Int

                if (e.clickCount == 1) {
                    val (resolution, refactoring) = satdDatabaseManager.getResolutionAndRefactorings(project.name, file_id, label)
                    resolutionLabel.text = "Resolution: $resolution"
                    refactoringLabel.text = "Refactoring: $refactoring"
                    table.setRowSelectionInterval(row, row)

                }
                else if (e.clickCount == 2){
                    satdFileManager.navigateToCode(project, line, path)
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
        scrollPane.border = BorderFactory.createEmptyBorder(5, 10, 10, 10)
        toolWindowPanel.add(scrollPane, BorderLayout.CENTER)


        // Start mining SATD here
        val testRepo = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/test_repo.csv"
        ApplicationManager.getApplication().executeOnPooledThread {
            satdFileManager.writeTestRepo(testRepo, project)
        }

        try {
            Files.createDirectories(Paths.get(PathManager.getConfigPath() + "/databases"))
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }

        val db = File(PathManager.getConfigPath() + "/databases", project.name + ".db")
        try {
            if (db.createNewFile()) {
                satdDatabaseManager.initialize(label, project.name, button)
            } else {
                val message = "Loading existing SATD data for this  project. May not include most recent commits. Click \"Fetch SATD Data\" to update data"
                Messages.showWarningDialog(message, "")
                satdDatabaseManager.loadDatabase(tableModel, label, table, project.name, button)
            }
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }

        button.addActionListener {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    satdDatabaseManager.initializeAndConnectDatabase(tableModel, label, table, project.name, button)
                },
                "Fetching SATD Data",
                false,
                project
            )
        }

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(toolWindowPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    class TextAreaRenderer : JTextArea(), TableCellRenderer {
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
