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
import java.util.concurrent.TimeUnit
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel

class SATDDatabaseManager {
    data class SATDInfo(
        val resolution: String?,
        val startLine: Int?,
        val endLine: Int?,
        val filePath: String?,
    )

    fun getSATDTableInfo(project: Project, fileID: Int): SATDInfo {
        val databasePath = PathManager.getConfigPath() + "/databases/${project.name}.db"
        val url = "jdbc:sqlite:$databasePath"
        var satdInfo = SATDInfo(null, null, null, null)

        val inFileQuery = "SELECT start_line, end_line, f_path FROM SATDInFile WHERE f_id = ?"
        val satdQuery = "SELECT resolution, second_commit FROM SATD WHERE second_file = ?"

        try {
            DriverManager.getConnection(url).use { conn ->
                // First query for startLine, endLine, filePath
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

                // Then separately query for resolution
                conn.prepareStatement(satdQuery).use { pstmt ->
                    pstmt.setInt(1, fileID)
                    val rs = pstmt.executeQuery()
                    if (rs.next()) {
                        satdInfo = satdInfo.copy(
                            resolution = rs.getString("resolution")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            error("Error fetching SATD info: ${e.message}")
        }


        return satdInfo
    }

    private fun fillTableModel(databasePath: String, tableModel: DefaultTableModel, table: JTable, label: JBLabel) {
        val url = "jdbc:sqlite:$databasePath"
        try {
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(
                        "SELECT SATD.satd_id, " +
                                "Projects.p_name AS project_name, " +
                                "SATD.satd_instance_id, " +
                                "SATD.resolution, " +
                                "SecondCommit.commit_hash AS resolution_commit, " +
                                "FirstCommit.author_name, " +
                                "FirstFile.f_path AS v1_path, " +
                                "FirstFile.containing_class AS v1_class, " +
                                "FirstFile.containing_method AS v1_method, " +
                                "FirstFile.f_comment AS v1_comment, " +
                                "SecondCommit.commit_hash AS v2_commit, " +
                                "SecondCommit.commit_date AS v2_commit_date, " +
                                "SecondCommit.author_date AS v2_author_date, " +
                                "SecondFile.f_path AS v2_path, " +
                                "SecondFile.containing_class AS v2_class, " +
                                "SecondFile.containing_method AS v2_method, " +
                                "SecondFile.method_declaration AS method_declaration, " +
                                "SecondFile.method_body AS method_body, " +
                                "SecondFile.f_comment AS v2_comment, " +
                                "COALESCE(rmv.type, 'N/A') AS refactoring " +
                                "FROM SATD " +
                                "INNER JOIN SATDInFile AS FirstFile ON SATD.first_file = FirstFile.f_id " +
                                "INNER JOIN SATDInFile AS SecondFile ON SATD.second_file = SecondFile.f_id " +
                                "INNER JOIN Commits AS FirstCommit ON SATD.first_commit = FirstCommit.commit_hash " +
                                "INNER JOIN Commits AS SecondCommit ON SATD.second_commit = SecondCommit.commit_hash " +
                                "INNER JOIN Projects ON SATD.p_id = Projects.p_id " +
                                "LEFT JOIN RefactoringsRmv rmv ON SecondCommit.commit_hash = rmv.commit_hash " + // FIXED alias usage
                                "ORDER BY satd_id DESC;"
                    )
                    tableModel.rowCount = 0
                    val seenRows = mutableSetOf<String>()

                    while (rs.next()) {
                        val satd_id = rs.getInt("satd_id")
                        val f_comment = rs.getString("v2_comment")
                        val containing_class = rs.getString("v2_class")
                        val containing_method = rs.getString("v2_method")
                        val resolution = rs.getString("resolution")
                        val refactoring = rs.getString("refactoring")

                        val rowKey = "$satd_id|$f_comment|$containing_class|$containing_method"
                        if (seenRows.contains(rowKey)) {
                            continue // Skip duplicate
                        }

                        seenRows.add(rowKey)
//                        println("Row: $rowKey")

                        tableModel.addRow(
                            arrayOf(
                                satd_id,
                                f_comment,
                                containing_class,
                                containing_method,
                                resolution,
                                refactoring
                            )
                        )
                    }

                    adjustColumnWidths(table)
                }
            }
        } catch (e: Exception) {
            label.text = "Error loading data: ${e.message}"
        }
    }


    fun initialize(label: JBLabel, projectName: String) {

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


    }

    fun loadDatabase(
        tableModel: DefaultTableModel,
        label: JBLabel,
        table: JTable,
        projectName: String,

        ) {

        tableModel.rowCount = 0
        val databasePath = PathManager.getConfigPath() + "/databases/$projectName.db"

        try {
            Class.forName("org.sqlite.JDBC")
            println("Driver loaded successfully.")
        } catch (e: ClassNotFoundException) {
            label.text = "SQLite JDBC Driver not found. Check your classpath."
            return
        }

        fillTableModel(databasePath, tableModel, table, label)

    }


    fun initializeAndConnectDatabase(
        tableModel: DefaultTableModel,
        label: JBLabel,
        table: JTable,
        project: Project
    ) {
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
//                                val username = GitCredentialsHolder.getInstance().getUsername()?.ifEmpty { null }
//                                val pat = GitCredentialsHolder.getInstance().getPassword()?.ifEmpty { null }
//
//                                if (username == null) {
//                                    showUnauthorizedGitNotification(project, "Username")
//                                    throw AuthorizationException("GitHub Username is not provided.")
//                                }
//                                if (pat == null) {
//                                    showUnauthorizedGitNotification(project, "Personal Access Token (PAT)")
//                                    throw AuthorizationException("GitHub PAT is not provided.")
//                                }
//
//                                val isAuthenticated = GitCredentialsHolder.getInstance().verifyGitHubAuthentication()
//                                if (!isAuthenticated) {
//                                    showUnauthorizedGitNotification(project, "Authentication Error")
//                                    throw AuthorizationException("GitHub Authentication Error.")
//                                }

//                                val processBuilder = ProcessBuilder(
//                                    "java", "-jar", "$libPath/target/satd-analyzer-jar-with-all-dependencies.jar",
//                                    "-r", "$libPath/test_repo.csv", "-d", databasePath, "-u", username, "-p", pat
//                                )
                                val processBuilder = ProcessBuilder(
                                    "java",
                                    "--enable-native-access=ALL-UNNAMED",
                                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                                    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",

                                    "-jar",
                                    "$libPath/target/satd-analyzer-jar-with-all-dependencies.jar",
                                    "-r", "$libPath/test_repo.csv", "-d", databasePath
                                )


                                val logDir = File(System.getProperty("user.home"), ".technical_debt/logs")
                                if (!logDir.exists()) logDir.mkdirs()

                                processBuilder.redirectOutput(File(logDir, "satd_stdout.log"))
                                processBuilder.redirectError(File(logDir, "satd_stderr.log"))

                                val process = processBuilder.start()
                                Runtime.getRuntime().addShutdownHook(Thread {
                                    if (process.isAlive) {
                                        process.destroy()
                                        process.waitFor(5, TimeUnit.SECONDS)
                                        if (process.isAlive) process.destroyForcibly()
                                    }
                                })

                                while (process.isAlive) {
                                    if (indicator.isCanceled) {
                                        process.destroy()
                                        process.waitFor(5, TimeUnit.SECONDS)
                                        if (process.isAlive) process.destroyForcibly()
                                        return
                                    }
                                    Thread.sleep(200)
                                }

                                val exitCode = process.waitFor()
                                if (exitCode == 0) {
                                    SwingUtilities.invokeLater {
                                        fillTableModel(databasePath, tableModel, table, label)
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