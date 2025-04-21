package technicaldebt_plugin_fall2024.toolWindow.Without

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import technicaldebt_plugin_fall2024.toolWindow.SATDToolWindowFactory.Companion.adjustColumnWidths
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.sql.DriverManager
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

class SATDDatabaseManager {
    data class SATDInfo(
        val resolution: String?,
        val startLine: Int?,
        val endLine: Int?,
        val filePath: String?,
        val refactoring: String?
    )

    fun initialize(label: JBLabel, project: Project) {
        val sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/create_tables_including_refactorings.sql"
        val databasePath = PathManager.getConfigPath() + "/databases/${project.name}.db"

        val inputStream: InputStream = try {
            FileInputStream(sqlFilePath)
        } catch (e: FileNotFoundException) {
            label.text = "Error: ${e.message}"
            return
        }

        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            label.text = "SQLite JDBC Driver not found. Check your classpath."
            return
        }

        val url = "jdbc:sqlite:$databasePath"

        try {
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    val sql = inputStream.readAllBytes().toString(Charsets.UTF_8)
                    val queries = sql.split(";")
                    for (query in queries) {
                        if (query.trim().isNotEmpty()) {
                            stmt.execute(query)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            label.text = "Error: ${e.message}"
        }
    }

    fun loadDatabase(
        tableModel: DefaultTableModel,
        label: JBLabel,
        table: JTable,
        project: Project
    ) {
        tableModel.rowCount = 0
        val databasePath = PathManager.getConfigPath() + "/databases/${project.name}.db"

        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            label.text = "SQLite JDBC Driver not found. Check your classpath."
            return
        }

        val url = "jdbc:sqlite:$databasePath"

        try {
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT * FROM SATDInFile")
                    while (rs.next()) {
                        val f_id = rs.getInt("f_id")
                        val f_comment = rs.getString("f_comment")
                        val containing_class = rs.getString("containing_class")
                        val containing_method = rs.getString("containing_method")
                        tableModel.addRow(arrayOf(f_id, f_comment, containing_class, containing_method))
                    }
                    adjustColumnWidths(table)
                }
            }
        } catch (e: Exception) {
            label.text = "Error: ${e.message}"
        }
    }

    fun getSATDTableInfo(project: Project, fileID: Int): SATDInfo {
        val databasePath = PathManager.getConfigPath() + "/databases/${project.name}.db"
        val url = "jdbc:sqlite:$databasePath"
        var satdInfo = SATDInfo(null, null, null, null, null)

        try {
            DriverManager.getConnection(url).use { conn ->
                val inFileQuery = "SELECT start_line, end_line, f_path FROM SATDInFile WHERE f_id = ?"
                val satdQuery = "SELECT resolution, second_commit FROM SATD WHERE second_file = ?"

                conn.prepareStatement(inFileQuery).use { pstmt ->
                    pstmt.setInt(1, fileID)
                    val rs = pstmt.executeQuery()
                    if (rs.next()) {
                        satdInfo = satdInfo.copy(
                            startLine = rs.getInt("start_line"),
                            endLine = rs.getInt("end_line"),
                            filePath = rs.getString("f_path")
                        )
                    }
                }

                conn.prepareStatement(satdQuery).use { pstmt ->
                    pstmt.setInt(1, fileID)
                    val rs = pstmt.executeQuery()
                    if (rs.next()) {
                        val resolution = rs.getString("resolution")
                        val secondCommit = rs.getString("second_commit")

                        val refactoringsQuery = "SELECT type FROM RefactoringsRmv WHERE commit_hash = ?"
                        conn.prepareStatement(refactoringsQuery).use { refStmt ->
                            refStmt.setString(1, secondCommit)
                            val refRs = refStmt.executeQuery()
                            val refactoring = if (refRs.next()) refRs.getString("type") else "N/A"
                            satdInfo = satdInfo.copy(resolution = resolution, refactoring = refactoring)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            error("Error fetching refactorings: ${e.message}")
        }

        return satdInfo
    }

    fun initializeAndConnectDatabase(
        tableModel: DefaultTableModel,
        label: JBLabel,
        table: JTable,
        project: Project
    ) {
        tableModel.rowCount = 0
        val sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/satdsql.sql"
        val databasePath = PathManager.getConfigPath() + "/databases/${project.name}.db"
        val libPath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/SATDBailiff/"

        val inputStream: InputStream = try {
            FileInputStream(sqlFilePath)
        } catch (e: FileNotFoundException) {
            label.text = "Error: ${e.message}"
            return
        }

        try {
            Class.forName("org.sqlite.JDBC")
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
                    val queries = sql.split(";")
                    for (query in queries) {
                        if (query.trim().isNotEmpty()) {
                            stmt.execute(query)
                        }
                    }

                    val task = object : Task.Backgroundable(project, "Running SATD Analysis", true) {
                        override fun run(indicator: ProgressIndicator) {
                            try {
                                val processBuilder = ProcessBuilder(
                                    "java",
                                    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                                    "-jar", "$libPath/target/satd-analyzer-jar-with-all-dependencies.jar",
                                    "-r", "$libPath/test_repo.csv",
                                    "-d", databasePath
                                )

                                val logDir = File(System.getProperty("user.home"), ".technical_debt/logs")
                                if (!logDir.exists()) logDir.mkdirs()

                                processBuilder.redirectOutput(File(System.getProperty("user.home"),".technical_debt/logs/satd_stdout.log"))
                                processBuilder.redirectError(File(System.getProperty("user.home"),".technical_debt/logs/satd_stderr.log"))


                                val process = processBuilder.start()
                                val exitCode = process.waitFor()
                                println("SATD Analyzer exited with code: $exitCode")

                                if (exitCode == 0) {
                                    val newRows = mutableListOf<Array<Any>>()
                                    val query = """
                                        SELECT
                                            s_in_file.f_id,
                                            s_in_file.f_comment,
                                            s_in_file.containing_class,
                                            s_in_file.containing_method,
                                            satd.resolution,
                                            COALESCE(rmv.type, 'N/A') AS refactoring
                                        FROM SATDInFile s_in_file
                                        LEFT JOIN SATD satd ON s_in_file.f_id = satd.second_file
                                        LEFT JOIN RefactoringsRmv rmv ON satd.second_commit = rmv.commit_hash
                                    """.trimIndent()

                                    DriverManager.getConnection(url).use { conn ->
                                        conn.createStatement().use { stmt ->
                                            val rs = stmt.executeQuery(query)
                                            while (rs.next()) {
                                                val f_id = rs.getInt("f_id")
                                                val f_comment = rs.getString("f_comment")
                                                val containing_class = rs.getString("containing_class")
                                                val containing_method = rs.getString("containing_method")
                                                val resolution = rs.getString("resolution") ?: "N/A"
                                                val refactoring = rs.getString("refactoring") ?: "N/A"

                                                newRows.add(
                                                    arrayOf(
                                                        f_id,
                                                        f_comment,
                                                        containing_class,
                                                        containing_method,
                                                        resolution,
                                                        refactoring
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    SwingUtilities.invokeLater {
                                        tableModel.rowCount = 0
                                        for (row: Array<Any> in newRows) {
                                            tableModel.addRow(row)
                                        }
                                        adjustColumnWidths(table)
                                    }
                                } else {
                                    println("SATD Analyzer failed with exit code $exitCode")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    task.queue()
                }
            }
        } catch (e: Exception) {
            label.text = "Connection failed: ${e.message}"
        }
    }
}
