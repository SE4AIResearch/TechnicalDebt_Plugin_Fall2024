package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.toolWindow

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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
import java.awt.event.ActionEvent
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
        val toolWindowPanel: JPanel = JBPanel<JBPanel<*>>()
        toolWindowPanel.layout = BorderLayout()
        val tabbedPane = JTabbedPane()

        val label = JBLabel("Click the button to connect to SATD database...")
        toolWindowPanel.add(label, BorderLayout.NORTH)

        // Create a button to start the process
        val button = JButton("Fetch SATD Data")
        toolWindowPanel.add(button, BorderLayout.SOUTH)

        // Create a table model with column names
        val tableModel = DefaultTableModel()
        tableModel.addColumn("File ID")
        tableModel.addColumn("Comment")
        tableModel.addColumn("Path")
        tableModel.addColumn("Start Line")
        tableModel.addColumn("End Line")
        tableModel.addColumn("Containing Class")
        tableModel.addColumn("Containing Method")

        // Create the JTable with the model
        val table = JTable(tableModel)
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF // Disable auto-resizing for better control

        // Set preferred widths for the columns
        table.columnModel.getColumn(0) // Comment Number
        table.columnModel.getColumn(1).preferredWidth = 500 // Comment
        table.columnModel.getColumn(2) //Path
        table.columnModel.getColumn(3) //Start Line
        table.columnModel.getColumn(4) //End Line
        table.columnModel.getColumn(5) //Containing Class
        table.columnModel.getColumn(6) //Containing Method

        //Makes it so table cannot be edited
        table.isEnabled = false

        // Set custom renderer for the columns to allow text wrapping
        table.columnModel.getColumn(1).cellRenderer = TextAreaRenderer()

        // TEST: Create the scroll pane and add to the tabbed pane
        val scrollPane =
            JBScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        tabbedPane.addTab("SATD In File", scrollPane)

        //Create 2nd Table
        val tableModel2 = DefaultTableModel()
        tableModel2.addColumn("SATD ID")
        tableModel2.addColumn("First File")
        tableModel2.addColumn("Second File")
        tableModel2.addColumn("Resolution")
        val table2 = JTable(tableModel2)
        table2.isEnabled = (false)
        val scrollPane2 =
            JBScrollPane(table2, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

        tabbedPane.addTab("SATD", scrollPane2)

        toolWindowPanel.add(tabbedPane, BorderLayout.CENTER)

        val testRepo = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/test_repo.csv"
        writeTestRepo(testRepo, project)

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) { // Double-click to navigate

                    val row = table.rowAtPoint(e.point)
                    //int column = table.columnAtPoint(e.getPoint());
                    val path = table.getValueAt(row, 2) as String
                    val startLineStr = table.getValueAt(row, 3) as String
                    val line = startLineStr.toInt()
                    navigateToCode(project, line, path)
                }
            }
        })

        //Creates the directory if it doesn't exist.
        try {
            Files.createDirectories(Paths.get(PathManager.getConfigPath() + "/databases"))
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }

        val db = File(PathManager.getConfigPath() + "/databases", project.name + ".db")
        try {
            //If database file is created, initialize it.
            if (db.createNewFile()) {
                initialize(label, project.name, button).execute()
            } else {
                val title = "Warning"
                val message = "Loading existing SATD data for this  project. May not include most recent commits."
                Messages.showWarningDialog(message, title)
                loadDatabase(tableModel, label, table, tableModel2, table2, project.name, button).execute()
            }
        } catch (e: IOException) {
            label.text = "Error: " + e.message
        }

        // Set button action
        button.addActionListener { e: ActionEvent? ->
            //TODO: Fix progress manager so that it actually waits for the task to finish

            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    initializeAndConnectDatabase(
                        tableModel,
                        label,
                        table,
                        tableModel2,
                        table2,
                        project.name,
                        button
                    ).execute()
                },
                "Fetching SATD Data",
                false,
                project
            )
        }

        // Adds our panel to IntelliJ's content factory
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(toolWindowPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun navigateToCode(project: Project, lineNumber: Int, path: String) {
        var path = path
        val homePath = project.basePath
        path = "$homePath/$path"
        val lastSlashIndex = path.lastIndexOf('/')
        val fileName = path.substring(lastSlashIndex + 1)
        val file = LocalFileSystem.getInstance().findFileByPath(path)
        if (file == null) {
            println("File $fileName not found: $path")
            val message = "File not found at given path"
            val title = "Error"
            Messages.showErrorDialog(message, title)
            return
        }
        FileEditorManager.getInstance(project).openFile(file, true)
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            println("No editor is currently open")
            return
        }
        // Move the caret to the desired line
        val caretModel = editor.caretModel
        if (lineNumber > 0 && lineNumber <= editor.document.lineCount) {
            caretModel.moveToLogicalPosition(LogicalPosition(lineNumber - 1, 0))
            //Scroll to the desired line to make it visible
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        }
    }

    fun writeTestRepo(path: String, project: Project) {
        var gitUrl = getGitHubUrl(project)
        if (gitUrl!!.endsWith(".git")) {
            gitUrl = gitUrl.substring(0, gitUrl.length - 4)
        }
        try {
            BufferedWriter(FileWriter(path)).use { writer ->
                // This will empty the file and write the new content
                writer.write(gitUrl)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private class initialize(private val label: JBLabel, private val projectName: String, private val button: JButton) :
        SwingWorker<Void?, Void?>() {
        @Throws(Exception::class)
        override fun doInBackground(): Void? {
            SwingUtilities.invokeLater { button.isEnabled = false }
            val sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql"
            val databasePath = PathManager.getConfigPath() + "/databases/" + projectName + ".db"

            var inputStream: InputStream? = null
            try {
                inputStream = FileInputStream(sqlFilePath)
            } catch (e: FileNotFoundException) {
                SwingUtilities.invokeLater { label.text = "Error: " + e.message }
            }

            try {
                Class.forName("org.sqlite.JDBC") // This will throw an exception if the driver is not found
                println("Driver loaded successfully.")
            } catch (e: ClassNotFoundException) {
                SwingUtilities.invokeLater { label.text = "SQLite JDBC Driver not found. Check your classpath." }
            }

            val url = "jdbc:sqlite:$databasePath"

            try {
                DriverManager.getConnection(url).use { conn ->
                    conn.createStatement().use { stmt ->
                        val sql = String(inputStream!!.readAllBytes())
                        val queries = sql.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                        for (query in queries) {
                            if (!query.trim { it <= ' ' }.isEmpty()) {
                                stmt.execute(query)
                            }
                        }

                        stmt.close()
                        conn.close()
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater { label.text = "Error: " + e.message }
            }

            SwingUtilities.invokeLater { button.isEnabled = true }
            return null
        }
    }

    private class loadDatabase(
        private val tableModel: DefaultTableModel,
        private val label: JBLabel,
        private val table: JTable,
        private val tableModel2: DefaultTableModel,
        private val table2: JTable,
        private val projectName: String,
        private val button: JButton
    ) :
        SwingWorker<Void?, Void?>() {
        @Throws(Exception::class)
        override fun doInBackground(): Void? {
            SwingUtilities.invokeLater { button.isEnabled = false }
            SwingUtilities.invokeLater { tableModel.rowCount = 0 }
            SwingUtilities.invokeLater { tableModel2.rowCount = 0 }
            val databasePath = PathManager.getConfigPath() + "/databases/" + projectName + ".db"

            try {
                Class.forName("org.sqlite.JDBC") // This will throw an exception if the driver is not found
                println("Driver loaded successfully.")
            } catch (e: ClassNotFoundException) {
                SwingUtilities.invokeLater { label.text = "SQLite JDBC Driver not found. Check your classpath." }
            }

            val url = "jdbc:sqlite:$databasePath"

            try {
                DriverManager.getConnection(url).use { conn ->
                    conn.createStatement().use { stmt ->
                        var fetchQuery = "SELECT * FROM SATDInFile"
                        var rs = stmt.executeQuery(fetchQuery)

                        // Displaying query results
                        while (rs.next()) {
                            val f_id = rs.getInt("f_id")
                            val f_comment = rs.getString("f_comment") // Replace with actual column name
                            val f_path = rs.getString("f_path")
                            val start_line = rs.getInt("start_line")
                            val end_line = rs.getInt("end_line")
                            val containing_class = rs.getString("containing_class")
                            val containing_method = rs.getString("containing_method")
                            SwingUtilities.invokeLater {
                                tableModel.addRow(
                                    arrayOf<Any>(
                                        f_id,
                                        f_comment,
                                        f_path,
                                        start_line,
                                        end_line,
                                        containing_class,
                                        containing_method
                                    )
                                )
                            }
                        }

                        fetchQuery = "SELECT * FROM SATD"
                        rs = stmt.executeQuery(fetchQuery)

                        while (rs.next()) {
                            val satd_id = rs.getInt("satd_id")
                            val first_file = rs.getInt("first_file")
                            val second_file = rs.getInt("second_file")
                            val resolution = rs.getString("resolution")
                            SwingUtilities.invokeLater {
                                tableModel2.addRow(
                                    arrayOf<Any>(
                                        satd_id,
                                        first_file,
                                        second_file,
                                        resolution
                                    )
                                )
                            }
                        }

                        SwingUtilities.invokeLater {
                            adjustColumnWidths(
                                table
                            )
                        }

                        rs.close()
                        stmt.close()
                        conn.close()
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater { label.text = "Error: " + e.message }
            }

            SwingUtilities.invokeLater { button.isEnabled = true }
            return null
        }
    }

    private class initializeAndConnectDatabase(
        private val tableModel: DefaultTableModel,
        private val label: JBLabel,
        private val table: JTable,
        private val tableModel2: DefaultTableModel,
        private val table2: JTable,
        private val projectName: String,
        private val button: JButton
    ) :
        SwingWorker<Void?, Void?>() {
        @Throws(Exception::class)
        override fun doInBackground(): Void? {
            //Gets the sql filepath from sql folder
            SwingUtilities.invokeLater { button.isEnabled = false }
            SwingUtilities.invokeLater { tableModel.rowCount = 0 }
            SwingUtilities.invokeLater { tableModel2.rowCount = 0 }
            val sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql"
            val databasePath = PathManager.getConfigPath() + "/databases/" + projectName + ".db"
            val libPath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/"

            var inputStream: InputStream? = null
            try {
                inputStream = FileInputStream(sqlFilePath)
            } catch (e: FileNotFoundException) {
                SwingUtilities.invokeLater { label.text = "Error: " + e.message }
            }

            val indicator = ProgressManager.getInstance().progressIndicator
            indicator.isIndeterminate = true


            // Load the MySQL JDBC Driver
            try {
                Class.forName("org.sqlite.JDBC") // This will throw an exception if the driver is not found
                println("Driver loaded successfully.")
            } catch (e: ClassNotFoundException) {
                SwingUtilities.invokeLater { label.text = "SQLite JDBC Driver not found. Check your classpath." }
            }

            val url = "jdbc:sqlite:$databasePath"

            try {
                DriverManager.getConnection(url).use { conn ->
                    conn.createStatement().use { stmt ->
                        SwingUtilities.invokeLater { label.text = "Connection successful!" }
                        // Read and execute the SQL file
                        val sql = String(inputStream!!.readAllBytes())
                        val queries = sql.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                        for (query in queries) {
                            if (!query.trim { it <= ' ' }.isEmpty()) {
                                stmt.execute(query)
                            }
                        }

                        // Update progress
                        if (indicator != null) {
                            SwingUtilities.invokeLater { indicator.text = "Database initialization complete." }
                        }

                        try {
                            val processBuilder = ProcessBuilder(
                                "java",
                                "--add-opens",
                                "java.base/java.lang=ALL-UNNAMED",
                                "-jar",
                                (libPath + "target/satd-analyzer-jar-with-all-dependencies.jar"),
                                "-r",
                                (libPath + "test_repo.csv"),
                                "-d",
                                (databasePath)
                            )

                            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)

                            val process = processBuilder.start()

                            val exitCode = process.waitFor()
                            println("Exit code:$exitCode")
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                        // Update progress
                        if (indicator != null) {
                            SwingUtilities.invokeLater { indicator.text = "Database up to Date." }
                        }

                        var fetchQuery = "SELECT * FROM SATDInFile"
                        var rs = stmt.executeQuery(fetchQuery)

                        // Displaying query results
                        while (rs.next()) {
                            val f_id = rs.getInt("f_id")
                            val f_comment = rs.getString("f_comment") // Replace with actual column name
                            val f_path = rs.getString("f_path")
                            val start_line = rs.getInt("start_line")
                            val end_line = rs.getInt("end_line")
                            val containing_class = rs.getString("containing_class")
                            val containing_method = rs.getString("containing_method")
                            SwingUtilities.invokeLater {
                                tableModel.addRow(
                                    arrayOf<Any>(
                                        f_id,
                                        f_comment,
                                        f_path,
                                        start_line,
                                        end_line,
                                        containing_class,
                                        containing_method
                                    )
                                )
                            }
                        }

                        fetchQuery = "SELECT * FROM SATD"
                        rs = stmt.executeQuery(fetchQuery)

                        while (rs.next()) {
                            val satd_id = rs.getInt("satd_id")
                            val first_file = rs.getInt("first_file")
                            val second_file = rs.getInt("second_file")
                            val resolution = rs.getString("resolution")
                            val second_commit = rs.getString("second_commit")

                            val refactoringsQuery = "SELECT type FROM RefactoringsRmv WHERE commit_hash = ?"
                            val pstmt = conn.prepareStatement(refactoringsQuery)
                            pstmt.setString(1, second_commit)
                            val refactoringsRs = pstmt.executeQuery()

                            println("Looking for commit_hash: '${second_commit}'")
                                                
                            val refactorings = if (refactoringsRs.next() && resolution == "SATD_REMOVED") {
                                val found = refactoringsRs.getString("type")
                                println(" Match found: $found")
                                found
                            } else {
                                println("No match found")
                                "N/A"
                            }

                            SwingUtilities.invokeLater {
                                tableModel2.addRow(
                                    arrayOf<Any>(
                                        satd_id,
                                        first_file,
                                        second_file,
                                        resolution,
                                        refactorings
                                    )
                                )
                            }
                        }

                        SwingUtilities.invokeLater {
                            adjustColumnWidths(
                                table
                            )
                        }

                        //adjustColumnWidths(table2);

                        // Update progress
                        if (indicator != null) {
                            SwingUtilities.invokeLater { indicator.text = "Data fetching complete." }
                        }


                        // Close resources
                        rs.close()
                        stmt.close()
                        conn.close()
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater { label.text = "Connection failed: " + e.message }
            }

            SwingUtilities.invokeLater { button.isEnabled = true }
            return null
        }
    }

    // Custom renderer to wrap text in the "Comment" column
    internal class TextAreaRenderer : JTextArea(), TableCellRenderer {
        init {
            lineWrap = true
            wrapStyleWord = true
            isOpaque = true // So it paints the background correctly
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

            // Adjust row height to fit the wrapped text
            if (table.getRowHeight(row) != preferredSize.height) {
                table.setRowHeight(row, preferredSize.height)
            }

            // Set background colors based on selection
            if (isSelected) {
                background = table.selectionBackground
                foreground = table.selectionForeground
            } else {
                background = table.background
                foreground = table.foreground
            }
            return this
        }
    }

    companion object {
        fun getGitHubUrl(project: Project): String? {
            val gitConfigFile = LocalFileSystem.getInstance()
                .findFileByPath(project.basePath + "/.git/config")

            if (gitConfigFile != null) {
                try {
                    BufferedReader(
                        InputStreamReader(gitConfigFile.inputStream)
                    ).use { reader ->
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
            val lines = configContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in lines.indices) {
                if (lines[i].trim { it <= ' ' } == "[remote \"origin\"]") {
                    for (j in i + 1..<lines.size) {
                        if (lines[j].trim { it <= ' ' }.startsWith("url =")) {
                            return lines[j].trim { it <= ' ' }.substring(6).trim { it <= ' ' }
                        }
                    }
                }
            }
            return null
        }

        // Method to adjust column widths dynamically
        fun adjustColumnWidths(table: JTable) {
            for (col in 0..<table.columnCount) {
                if ((col != 1) && (col != 8) && (col != 9)) {
                    val column = table.columnModel.getColumn(col)
                    val minWidth = getTextWidth(table, column.headerValue.toString(), table.font)
                    var maxWidth = minWidth

                    // Iterate through rows to find the maximum width
                    for (row in 0..<table.rowCount) {
                        val value = table.getValueAt(row, col)
                        if (value != null) {
                            val cellWidth = getTextWidth(table, value.toString(), table.font)
                            if (cellWidth > maxWidth) {
                                maxWidth = cellWidth
                            }
                        }
                    }
                    // Set the column width (minimum width as the header's width, preferred as the max width)
                    column.minWidth = minWidth + 10 // Adding some padding
                    column.preferredWidth = maxWidth + 20 // Adding padding for readability
                }
            }
        }

        // Helper method to calculate the pixel width of a given text with a specific font
        private fun getTextWidth(table: JTable, text: String, font: Font): Int {
            val metrics = table.getFontMetrics(font)
            return metrics.stringWidth(text)
        }
    }
}