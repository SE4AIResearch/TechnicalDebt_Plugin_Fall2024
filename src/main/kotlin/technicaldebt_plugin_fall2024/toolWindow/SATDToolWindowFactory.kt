package technicaldebt_plugin_fall2024.toolWindow

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.DriverManager
import java.util.stream.Collectors
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class SATDToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = JBPanel<JBPanel<*>>()
        toolWindowPanel.layout = BorderLayout()
        val tabbedPane = JTabbedPane()

        val label = JBLabel("Click the button to connect to SATD database...")
        toolWindowPanel.add(label, BorderLayout.NORTH)

        val button = JButton("Fetch SATD Data")
        toolWindowPanel.add(button, BorderLayout.SOUTH)

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

        val scrollPane = JBScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        tabbedPane.addTab("SATD In File", scrollPane)

        val tableModel2 = DefaultTableModel()
        tableModel2.addColumn("SATD ID")
        tableModel2.addColumn("First File")
        tableModel2.addColumn("Second File")
        tableModel2.addColumn("Resolution")
        val table2 = JTable(tableModel2)
        table2.isEnabled = false
        val scrollPane2 = JBScrollPane(table2, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        tabbedPane.addTab("SATD", scrollPane2)

        toolWindowPanel.add(tabbedPane, BorderLayout.CENTER)

        val testRepo = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/test_repo.csv"
        writeTestRepo(testRepo, project)

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = table.rowAtPoint(e.point)
                    val path = table.getValueAt(row, 2) as String
                    val startLineStr = table.getValueAt(row, 3) as String
                    val line = startLineStr.toInt()
                    navigateToCode(project, line, path)
                }
            }
        })

        try {
            Files.createDirectories(Paths.get(PathManager.getConfigPath() + "/databases"))
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }

        val db = File(PathManager.getConfigPath() + "/databases", project.name + ".db")
        try {
            if (db.createNewFile()) {
                initialize(label, project.name, button)
            } else {
                loadDatabase(tableModel, label, table, tableModel2, table2, project.name, button)
            }
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }

        button.addActionListener {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    initializeAndConnectDatabase(tableModel, label, table, tableModel2, table2, project.name, button)
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

    private fun navigateToCode(project: Project, lineNumber: Int, path: String) {
        val homePath = project.basePath
        val fullPath = "$homePath/$path"
        val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
        if (file == null) {
            println("File not found: $fullPath")
            return
        }
        FileEditorManager.getInstance(project).openFile(file, true)
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            println("No editor is currently open")
            return
        }
        val caretModel = editor.caretModel
        if (lineNumber > 0 && lineNumber <= editor.document.lineCount) {
            caretModel.moveToLogicalPosition(LogicalPosition(lineNumber - 1, 0))
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        }
    }

    fun getGitHubUrl(project: Project): String? {
        val gitConfigFile = LocalFileSystem.getInstance().findFileByPath(project.basePath + "/.git/config")
        if (gitConfigFile != null) {
            try {
                BufferedReader(InputStreamReader(gitConfigFile.inputStream)).use { reader ->
                    val configContent = reader.lines().collect(Collectors.joining("\n"))
                    return extractRemoteUrl(configContent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun extractRemoteUrl(configContent: String): String? {
        val lines = configContent.split("\n").toTypedArray()
        for (i in lines.indices) {
            if (lines[i].trim { it <= ' ' } == "[remote \"origin\"]") {
                for (j in i + 1 until lines.size) {
                    if (lines[j].trim { it <= ' ' }.startsWith("url =")) {
                        return lines[j].trim { it <= ' ' }.substring(6).trim { it <= ' ' }
                    }
                }
            }
        }
        return null
    }

    fun writeTestRepo(path: String, project: Project) {
        val gitUrl = getGitHubUrl(project)
        try {
            BufferedWriter(FileWriter(path)).use { writer ->
                writer.write(gitUrl)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initialize(label: JBLabel, projectName: String, button: JButton) {
        button.isEnabled = false
        val sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql"
        val databasePath = PathManager.getConfigPath() + "/databases/$projectName.db"

        val inputStream: InputStream?
        try {
            inputStream = FileInputStream(sqlFilePath)
        } catch (e: FileNotFoundException) {
            label.text = "Error: " + e.message
            return
        }

        try {
            Class.forName("org.sqlite.JDBC")
            println("Driver loaded successfully.")
        } catch (e: ClassNotFoundException) {
            label.text = "SQLite JDBC Driver not found. Check your classpath."
            return
        }

        val url = "jdbc:sqlite:$databasePath"

        try {
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    val sql = inputStream.readAllBytes().toString(Charsets.UTF_8)
                    val queries = sql.split(";").toTypedArray()
                    for (query in queries) {
                        if (query.trim { it <= ' ' }.isNotEmpty()) {
                            stmt.execute(query)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            label.text = "Error: " + e.message
        }

        button.isEnabled = true
    }

    private fun loadDatabase(
        tableModel: DefaultTableModel,
        label: JBLabel,
        table: JTable,
        tableModel2: DefaultTableModel,
        table2: JTable,
        projectName: String,
        button: JButton
    ) {
        button.isEnabled = false
        tableModel.rowCount = 0
        tableModel2.rowCount = 0
        val databasePath = PathManager.getConfigPath() + "/databases/$projectName.db"

        try {
            Class.forName("org.sqlite.JDBC")
            println("Driver loaded successfully.")
        } catch (e: ClassNotFoundException) {
            label.text = "SQLite JDBC Driver not found. Check your classpath."
            return
        }

        val url = "jdbc:sqlite:$databasePath"

        try {
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    var fetchQuery = "SELECT * FROM SATDInFile"
                    var rs = stmt.executeQuery(fetchQuery)
                    while (rs.next()) {
                        val f_id = rs.getInt("f_id")
                        val f_comment = rs.getString("f_comment")
                        val f_path = rs.getString("f_path")
                        val start_line = rs.getInt("start_line")
                        val end_line = rs.getInt("end_line")
                        val containing_class = rs.getString("containing_class")
                        val containing_method = rs.getString("containing_method")
                        tableModel.addRow(arrayOf(f_id, f_comment, f_path, start_line, end_line, containing_class, containing_method))
                    }

                    fetchQuery = "SELECT * FROM SATD"
                    rs = stmt.executeQuery(fetchQuery)
                    while (rs.next()) {
                        val satd_id = rs.getInt("satd_id")
                        val first_file = rs.getInt("first_file")
                        val second_file = rs.getInt("second_file")
                        val resolution = rs.getString("resolution")
                        tableModel2.addRow(arrayOf(satd_id, first_file, second_file, resolution))
                    }

                    adjustColumnWidths(table)
                }
            }
        } catch (e: Exception) {
            label.text = "Error: " + e.message
        }

        button.isEnabled = true
    }

    private fun initializeAndConnectDatabase(
        tableModel: DefaultTableModel,
        label: JBLabel,
        table: JTable,
        tableModel2: DefaultTableModel,
        table2: JTable,
        projectName: String,
        button: JButton
    ) {
        button.isEnabled = false
        tableModel.rowCount = 0
        tableModel2.rowCount = 0
        val sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql"
        val databasePath = PathManager.getConfigPath() + "/databases/$projectName.db"
        val libPath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/"

        val inputStream: InputStream?
        try {
            inputStream = FileInputStream(sqlFilePath)
        } catch (e: FileNotFoundException) {
            label.text = "Error: " + e.message
            return
        }

        val indicator = ProgressManager.getInstance().progressIndicator

        try {
            Class.forName("org.sqlite.JDBC")
            println("Driver loaded successfully.")
        } catch (e: ClassNotFoundException) {
            label.text = "SQLite JDBC Driver not found. Check your classpath."
            return
        }

        val url = "jdbc:sqlite:$databasePath"

        try {
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    label.text = "Connection successful!"

                    val sql = inputStream.readAllBytes().toString(Charsets.UTF_8)
                    val queries = sql.split(";").toTypedArray()
                    for (query in queries) {
                        if (query.trim { it <= ' ' }.isNotEmpty()) {
                            stmt.execute(query)
                        }
                    }

                    indicator?.let {
                        it.text = "Database initialization complete."
                        it.fraction = 0.33
                    }

                    try {
                        val processBuilder = ProcessBuilder(
                            "java",
                            "--add-opens",
                            "java.base/java.lang=ALL-UNNAMED",
                            "-jar",
                            "$libPath/target/satd-analyzer-jar-with-all-dependencies.jar",
                            "-r",
                            "$libPath/test_repo.csv",
                            "-d",
                            databasePath
                        )

                        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)

                        val process = processBuilder.start()
                        val exitCode = process.waitFor()
                        println("Exit code: $exitCode")
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    indicator?.let {
                        it.text = "Database up to Date."
                        it.fraction = 0.66
                    }

                    var fetchQuery = "SELECT * FROM SATDInFile"
                    var rs = stmt.executeQuery(fetchQuery)
                    while (rs.next()) {
                        val f_id = rs.getInt("f_id")
                        val f_comment = rs.getString("f_comment")
                        val f_path = rs.getString("f_path")
                        val start_line = rs.getInt("start_line")
                        val end_line = rs.getInt("end_line")
                        val containing_class = rs.getString("containing_class")
                        val containing_method = rs.getString("containing_method")
                        tableModel.addRow(arrayOf(f_id, f_comment, f_path, start_line, end_line, containing_class, containing_method))
                    }

                    fetchQuery = "SELECT * FROM SATD"
                    rs = stmt.executeQuery(fetchQuery)
                    while (rs.next()) {
                        val satd_id = rs.getInt("satd_id")
                        val first_file = rs.getInt("first_file")
                        val second_file = rs.getInt("second_file")
                        val resolution = rs.getString("resolution")
                        tableModel2.addRow(arrayOf(satd_id, first_file, second_file, resolution))
                    }

                    adjustColumnWidths(table)

                    indicator?.let {
                        it.text = "Data fetching complete."
                        it.fraction = 1.0
                    }
                }
            }
        } catch (e: Exception) {
            label.text = "Connection failed: " + e.message
        }

        button.isEnabled = true
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