package technicaldebt_plugin_fall2024.toolWindow.Without

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import technicaldebt_plugin_fall2024.AuthorizationException
import com.technicaldebt_plugin_fall2024.models.CredentialsHolder
import com.technicaldebt_plugin_fall2024.showUnauthorizedGitNotification
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
import kotlin.text.ifEmpty

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
                    val rs = stmt.executeQuery("SELECT SATD.satd_id, " +
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
                            "LEFT JOIN RefactoringsRmv rmv ON resolution_commit = rmv.commit_hash" +
                            "ORDER BY satd_id DESC;")
                    while (rs.next()) {
                        val satd_id = rs.getInt("satd_id")
                        val f_comment = rs.getString("v2_comment")
                        val containing_class = rs.getString("containing_class")
                        val containing_method = rs.getString("v2_method")
                        val resolution = rs.getString("resolution")
                        val refactoring = rs.getString("refactoring") ?: "N/A"
                        tableModel.addRow(arrayOf(satd_id, f_comment, containing_class, containing_method, resolution, refactoring))
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
                                val username = GitCredentialsHolder.getInstance().getUsername()?.ifEmpty { null }

                                if (username == null) {
                                    showUnauthorizedGitNotification(project, "Username")
                                    throw AuthorizationException("GitHub Username is not provided. Please provide it in the settings menu under 'Technical Debt' section.")
                                }

                                val pat = GitCredentialsHolder.getInstance().getPassword()?.ifEmpty { null }

                                if (pat == null) {
                                    showUnauthorizedGitNotification(project, "Personal Access Token (PAT)")
                                    throw AuthorizationException("GitHub Personal Access Token (PAT) is not provided. Please provide it in the settings menu under 'Technical Debt' section.")
                                }


                                //otherwise try to verify and display Authentication Error if fails
                                val credentialsHolder = GitCredentialsHolder.getInstance()
                                val isAuthenticated = credentialsHolder.verifyGitHubAuthentication()

                                if (!isAuthenticated) {
                                    showUnauthorizedGitNotification(project, "Username or Personal Access Token (PAT)")
                                    throw AuthorizationException("GitHub Authentication Error. Please check your credentials and try again.")
                                }



                                val processBuilder = ProcessBuilder(
                                    "java",
                                    "-jar", "$libPath/target/satd-analyzer-jar-with-all-dependencies.jar",
                                    "-r", "$libPath/test_repo.csv",
                                    "-d", databasePath,
                                    "-u", username,
                                    "-p", pat
                                )

                                val logDir = File(System.getProperty("user.home"), ".technical_debt/logs")
                                if (!logDir.exists()) logDir.mkdirs()

                                processBuilder.redirectOutput(
                                    File(
                                        System.getProperty("user.home"),
                                        ".technical_debt/logs/satd_stdout.log"
                                    )
                                )
                                processBuilder.redirectError(
                                    File(
                                        System.getProperty("user.home"),
                                        ".technical_debt/logs/satd_stderr.log"
                                    )
                                )


                                val process = processBuilder.start()
                                // handle intellij shutdonw
                                Runtime.getRuntime().addShutdownHook(Thread {
                                    if (process.isAlive) {
                                        println("IDE is shutting down, killing external process...")
                                        process.destroy()
                                        process.waitFor(5, TimeUnit.SECONDS)
                                        if (process.isAlive) {
                                            println("Process still alive, killing forcibly...")
                                            process.destroyForcibly()
                                        }
                                    }

                                })

                                // handle process cancel
                                while (process?.isAlive == true) {
                                    if (indicator.isCanceled) {
                                        println("Task was canceled, destroying process...")
                                        process.destroy()
                                        process.waitFor(5, TimeUnit.SECONDS)
                                        if (process.isAlive) {
                                            println("Process still alive, killing forcibly...")
                                            process.destroyForcibly()
                                        }
                                        return
                                    }
                                    Thread.sleep(200)
                                }


                                val exitCode = process.waitFor()
                                println("SATD Analyzer exited with code: $exitCode")

                                if (exitCode == 0) {
                                    val newRows = mutableListOf<Array<Any>>()
                                    val query = "SELECT SATD.satd_id, " +
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
                                            "LEFT JOIN RefactoringsRmv rmv ON resolution_commit = rmv.commit_hash" +
                                            "ORDER BY satd_id DESC;"

                                    DriverManager.getConnection(url).use { conn ->
                                        conn.createStatement().use { stmt ->
                                            val rs = stmt.executeQuery(query)
                                            while (rs.next()) {
                                                val satd_id = rs.getInt("satd_id")
                                                val f_comment = rs.getString("v2_comment")
                                                val containing_class = rs.getString("containing_class")
                                                val containing_method = rs.getString("v2_method")
                                                val resolution = rs.getString("resolution")
                                                val refactoring = rs.getString("refactoring") ?: "N/A"

                                                newRows.add(
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


