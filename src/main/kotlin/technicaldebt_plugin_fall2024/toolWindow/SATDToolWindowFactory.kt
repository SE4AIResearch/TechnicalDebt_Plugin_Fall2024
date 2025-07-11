package technicaldebt_plugin_fall2024.toolWindow

import technicaldebt_plugin_fall2024.actions.LLMActivator
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
import com.intellij.openapi.editor.Document
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.ui.JBUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter

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
        var selectedFileId: Int? = null
        val toolWindowPanel = JPanel(BorderLayout())

        val label = JBLabel("Retrieve the latest SATD data: ")

        val pathLabel = JLabel("Path to SATD: ")
        val linesLabel = JBLabel("Lines: [,]")

        val bottomPanel = JPanel(BorderLayout())

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

        // Add left and right subpanels to bottom panel
        bottomPanel.add(leftPanel, BorderLayout.WEST)
        bottomPanel.add(rightPanel, BorderLayout.EAST)

        // Now add this combined bottom panel to the main toolWindowPanel
        toolWindowPanel.add(bottomPanel, BorderLayout.SOUTH)

        val tableModel = object : DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }
        tableModel.addColumn("SATD ID")
        tableModel.addColumn("Initial Comment")
        tableModel.addColumn("Final Comment")
        tableModel.addColumn("Containing Class")
        tableModel.addColumn("Containing Method")
        tableModel.addColumn("Resolution")
        tableModel.addColumn("Refactoring")

        val table = JTable(tableModel)
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.columnModel.getColumn(1).preferredWidth = 500
        table.columnModel.getColumn(2).preferredWidth = 500
        table.isEnabled = true
        table.columnModel.getColumn(1).cellRenderer = TextAreaRenderer()
        table.columnModel.getColumn(2).cellRenderer = TextAreaRenderer()

        val sorter = TableRowSorter(tableModel)
        table.rowSorter = sorter

        sorter.setComparator(0, Comparator<Int> { o1, o2 -> o1.compareTo(o2) })

        for (i in 0 until table.columnCount) {
            sorter.setSortable(i, true)
        }

        for (col in 0 until table.columnCount) {
            if (col == 1 || col == 2) {
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
                selectedFileId = fileId

                var filePath: String? = null
                var l1: Int? = null
                var l2: Int? = null
                var satdType: String? = null

                val satdInfo = satdDatabaseManager.getSATDTableInfo(project, fileId)
                currResolution = satdInfo.resolution

                filePath = satdInfo.filePath
                l1 = satdInfo.startLine
                l2 = satdInfo.endLine
                satdType = satdInfo.resolution

                if (e.clickCount == 1) {
                    pathLabel.text = "Path to SATD: $filePath   "
                    linesLabel.text = "Lines: [$l1, $l2]"
                    isJumpedToMethod = false
                    table.setRowSelectionInterval(row, row)
                } else if (e.clickCount == 2) {
                    var expectedMethodName = table.getValueAt(row, 4) as String
                    val jumped = filePath?.let { satdFileManager.navigateToCode(project, expectedMethodName, it) }

                    if (jumped == true) {
                        isJumpedToMethod = true
                    }

                    val editor = getCurrentEditor(project)
                    val element = editor?.let { PsiUtilBase.getElementAtCaret(it) }
                    val method = element?.let { PsiTreeUtil.getParentOfType(it, PsiNameIdentifierOwner::class.java) }
                    val actualMethodName = method?.name
                    println(method?.name)

                    expectedMethodName = expectedMethodName.substringBefore("(").trim()

                    if (isJumpedToMethod) {
                        if (actualMethodName != null && actualMethodName != expectedMethodName) {
                            Messages.showErrorDialog(
                                project,
                                "The SATD method name \"$expectedMethodName\" doesn't match the current method name \"$actualMethodName\". The database entry might be outdated.",
                                "Method Mismatch Detected"
                            )
                        } else {
                            val selectionModel = editor?.selectionModel
                            val namedElement = editor?.let { getParentNamedElement(it) }
                            if (namedElement != null) {
                                val queryText = namedElement.text
                                val textRange = namedElement.textRange
                                selectionModel?.setSelection(textRange.startOffset, textRange.endOffset)
                            } else {
                                selectionModel?.selectLineAtCaret()
                            }
                        }
                    }
                }
            }
        })

        table.columnModel.getColumn(1).preferredWidth = 500
        table.columnModel.getColumn(2).preferredWidth = 500
        table.fillsViewportHeight = true

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
                satdDatabaseManager.initialize(label, project.name)
            } else {
                val message = "Loading existing SATD data for this project. May not include most recent commits. Click \"Fetch SATD Data\" to update data"
                Messages.showWarningDialog(message, "")
                satdDatabaseManager.loadDatabase(tableModel, label, table, project.name)
            }
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }

        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Reverse Order", "Reverse the current table order", AllIcons.RunConfigurations.SortbyDuration) {
                override fun actionPerformed(e: AnActionEvent) {
                    val currentKeys = table.rowSorter?.sortKeys

                    if (currentKeys.isNullOrEmpty()) {
                        sorter.setSortKeys(listOf(RowSorter.SortKey(0, SortOrder.DESCENDING)))
                    } else {
                        val currentKey = currentKeys[0]
                        val newOrder = if (currentKey.sortOrder == SortOrder.ASCENDING) {
                            SortOrder.DESCENDING
                        } else {
                            SortOrder.ASCENDING
                        }
                        sorter.setSortKeys(listOf(RowSorter.SortKey(0, newOrder)))
                    }
                }
            })

            add(object : AnAction("Fetch SATD", "Load the SATD records into the table", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    ProgressManager.getInstance().runProcessWithProgressSynchronously(
                        {
                            satdDatabaseManager.initializeAndConnectDatabase(tableModel, label, table, project)
                        },
                        "Fetching SATD Data",
                        false,
                        project
                    )
                }
            })

            add(object : AnAction("Send to LLM", "Send selected code to LLM", AllIcons.RunConfigurations.Remote) {
                override fun update(e: AnActionEvent) {
                    val editor = getCurrentEditor(project)
                    val selectionModel = editor?.selectionModel
                    e.presentation.isEnabled = isJumpedToMethod && selectionModel?.hasSelection() == true
                }

                override fun actionPerformed(e: AnActionEvent) {
                    val editor = getCurrentEditor(project) ?: return
                    val selectionModel = editor.selectionModel
                    val selectedText = selectionModel.selectedText ?: return
                    val textRange = TextRange(selectionModel.selectionStart, selectionModel.selectionEnd)

                    val satdType = currResolution

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
            isEditable = false
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

            if (table.getRowHeight(row) != preferredSize.height && table.getRowHeight(row) <= preferredSize.height) {
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
                if (col != 1 && col != 2 && col != 8 && col != 9) {
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