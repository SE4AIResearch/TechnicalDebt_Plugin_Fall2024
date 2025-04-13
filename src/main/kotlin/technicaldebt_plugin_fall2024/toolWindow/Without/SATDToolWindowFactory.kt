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

class SATDToolWindowFactory : ToolWindowFactory, DumbAware {
    private val satdFileManager = SATDFileManager()
    private val satdDatabaseManager = SATDDatabaseManager()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = JPanel(BorderLayout())

        val label = JBLabel("Right-click in the table area to update SATD data")
        // val button = JButton("Fetch").apply {
        //     icon = ImageIcon("src/main/resources/assets/load.png")
        //     preferredSize = Dimension(140, 30)
        //     background = Color(0x2E8B57)
        //     foreground = Color.WHITE
        //     font = Font("Arial", Font.BOLD, 12)
        //     toolTipText = "Load the SATD records into the table"
        // }



        val topPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        topPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        topPanel.add(label)
        //topPanel.add(button)
        toolWindowPanel.add(topPanel, BorderLayout.SOUTH)

        val tableModel = DefaultTableModel()
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
        table.isEnabled = false
        table.getColumnModel().getColumn(1).cellRenderer = TextAreaRenderer()
        table.setCellSelectionEnabled(true)
        table.setRowSelectionAllowed(true)

        table.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selectedRow = table.selectedRow
                if (selectedRow != -1) {
                    for (i in 0 until table.columnCount) {
                        val value = table.getValueAt(selectedRow, i)
                        println("Column $i: $value")
                    }
                }
            }
        }

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = table.rowAtPoint(e.point)
                    val path = table.getValueAt(row, 2) as String
                    val startLineStr = table.getValueAt(row, 3) as Int
                    val line = startLineStr.toInt()
                    satdFileManager.navigateToCode(project, line, path)
                }
            }
        })

        val scrollPane = JBScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        scrollPane.preferredSize = Dimension(900, 250)
        scrollPane.border = BorderFactory.createEmptyBorder(5, 10, 10, 10)
        toolWindowPanel.add(scrollPane, BorderLayout.CENTER)

        val tableModel2 = DefaultTableModel()
        tableModel2.addColumn("SATD ID")
        tableModel2.addColumn("First File")
        tableModel2.addColumn("Second File")
        tableModel2.addColumn("Resolution")

        val table2 = JTable(tableModel2)
        table2.isEnabled = false
        val scrollPane2 = JBScrollPane(table2, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

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
                val message = "Loading existing SATD data for this  project. May not include most recent commits. Right-click and select \"Fetch SATD Data\" to update data"
                Messages.showWarningDialog(message, "")
                satdDatabaseManager.loadDatabase(tableModel, label, table, tableModel2, table2, project.name)
            }
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }
        
        // Create context menu for tables
        val contextMenu = JPopupMenu()
        val fetchSATDMenuItem = JMenuItem("Fetch SATD Data")
        contextMenu.add(fetchSATDMenuItem)

        // Add context menu to both tables
        setupContextMenu(table, contextMenu)
        setupContextMenu(table2, contextMenu)

        // Add context menu to scrollpanes (for when tables are empty)
        setupContextMenu(scrollPane.viewport, contextMenu)
        setupContextMenu(scrollPane2.viewport, contextMenu)
        
        // Set action for fetch SATD menu item
        fetchSATDMenuItem.addActionListener {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    satdDatabaseManager.initializeAndConnectDatabase(tableModel, label, table, tableModel2, table2, project.name)
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

    private fun setupContextMenu(component: JComponent, contextMenu: JPopupMenu) {
        component.componentPopupMenu = contextMenu
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
