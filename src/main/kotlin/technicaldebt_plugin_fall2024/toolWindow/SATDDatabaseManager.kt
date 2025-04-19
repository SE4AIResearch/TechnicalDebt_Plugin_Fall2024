package technicaldebt_plugin_fall2024.toolWindow.Without

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import technicaldebt_plugin_fall2024.toolWindow.SATDToolWindowFactory.Companion.adjustColumnWidths
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.sql.DriverManager
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class SATDDatabaseManager {
    data class SATDInfo(
        val resolution: String?,
        val startLine: Int?,
        val endLine: Int?,
        val filePath: String?,
        val refactoring: String?,
        val containing_method: String? = null
    )

    fun initialize(label: JBLabel, projectName: String) {

        val sqlFilePath = PathManager.getPluginsPath() + "/TechnicalDebt_Plugin_Fall2024/sql/create_tables_including_refactorings.sql"
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
        project : Project
        ) {

        tableModel.rowCount = 0
        val databasePath = PathManager.getConfigPath() + "/databases/${project.name}.db"

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
                        val containing_class = rs.getString("containing_class")
                        val containing_method = rs.getString("containing_method")
                        val (resolution, _, _, _, refactoring) = getSATDTableInfo(project, f_id)
                        tableModel.addRow(arrayOf(f_id, f_comment, containing_class, containing_method, resolution, refactoring))
                    }

                    adjustColumnWidths(table)
                }
            }
        } catch (e: Exception) {
            label.text = "Error: " + e.message
        }

    }

    fun getSATDTableInfo(
        project: Project,
        fileID: Int
    ): SATDInfo {
        try {
            val databasePath = PathManager.getConfigPath() + "/databases/${project.name}.db"
            val url = "jdbc:sqlite:$databasePath"

            var satdInfo = SATDInfo(null, null, null, null, null)

            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { stmt ->
                    // Step 1: Get resolution and second commit from SATD
                    val getSATDInFileInfo = "SELECT start_line, end_line, f_path FROM SATDInFile WHERE f_id = ?"
                    val getSATDInfo = "SELECT resolution, second_commit FROM SATD WHERE second_file = ?"

                    conn.prepareStatement(getSATDInFileInfo).use { pstmt ->
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

                    conn.prepareStatement(getSATDInfo).use { pstmt ->
                        pstmt.setInt(1, fileID)
                        val rs = pstmt.executeQuery()

                        if (rs.next()) {
                            // cols 4 and 5
                            val resolution = rs.getString("resolution")
                            val secondCommit = rs.getString("second_commit")

                            // Step 2: Get refactoring from RefactoringsRmv using second_commit
                            val refactoringsQuery = "SELECT type FROM RefactoringsRmv WHERE commit_hash = ?"
                            conn.prepareStatement(refactoringsQuery).use { refStmt ->
                                refStmt.setString(1, secondCommit)
                                val refRs = refStmt.executeQuery()

                                val refactoring =
                                    if (refRs.next()) {
                                        refRs.getString("type")
                                    } else {
                                        "N/A"
                                    }

                                satdInfo = satdInfo.copy(
                                    resolution = resolution,
                                    refactoring = refactoring,
                                    containing_method = refRs.getString("containing_method") ?: "N/A"
                                )
                            }
                        }
                    }

                    return satdInfo

                }
            }
        } catch (e: Exception) {
//            refactoringsLabel.text = "Error fetching refactorings: ${e.message}"
            error("Error fetching refactorings: ${e.message}")
        }

        return SATDInfo(null, null, null, null, null)
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

        val inputStream: InputStream?
        try {
            inputStream = FileInputStream(sqlFilePath)
        } catch (e: FileNotFoundException) {
            label.text = "Error: " + e.message
            return
        }

//        val indicator = ProgressManager.getInstance().progressIndicator
//        indicator.isIndeterminate = true;

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

//                    indicator?.let {
//                        it.text = "Database initialization complete."
//                    }

                    try {

                        //println("libPath = $libPath")
                        //println("JAR path = $libPath/target/satd-analyzer-jar-with-all-dependencies.jar")
                        //println("CSV path = $libPath/test_repo.csv")
                        //println("DB path = $databasePath")

//                        val processBuilder = ProcessBuilder(
//                            "java",
//                            "--add-opens",
//                            "java.base/java.lang=ALL-UNNAMED",
//                            "-jar",
//                            "$libPath/target/satd-analyzer-jar-with-all-dependencies.jar",
//                            "-r",
//                            "$libPath/test_repo.csv",
//                            "-d",
//                            databasePath
//                        )
//
//                        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
//                        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)

                        val task =
                            object : Task.Backgroundable(project, "Running SATD Analysis") {
                                override fun run(indicator: ProgressIndicator) {
                                    try {
                                        val processBuilder = ProcessBuilder(
                                            "java",
                                            "--add-opens",
                                            "java.base/java.lang=ALL-UNNAMED",
                                            "-jar",
                                            "$libPath/target/satd-analyzer-jar-with-all-dependencies.jar",
                                            "-r", "$libPath/test_repo.csv",
                                            "-d", databasePath
                                        )

                                        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                                        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)

                                        val process = processBuilder.start()
                                        val exitCode = process.waitFor()
                                        println("SATD Analyzer exited with code: $exitCode")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }.queue()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

//                    indicator?.let {
//                        it.text = "Database up to Date."
//                    }

                    val fetchQuerySATDInFile = """
                        SELECT SATDInFile.f_id, SATDInFile.f_comment, SATDInFile.containing_class, 
                               SATDInFile.containing_method, SATD.resolution, SATD.refactoring
                        FROM SATDInFile
                        JOIN SATD ON SATDInFile.satd_id = SATD.satd_id
                    """.trimIndent()

                    val SATDInFileRS = stmt.executeQuery(fetchQuerySATDInFile)

                    while (SATDInFileRS.next()) {
                        val f_id = SATDInFileRS.getInt("f_id")
                        val f_comment = SATDInFileRS.getString("f_comment")
                        val containing_class = SATDInFileRS.getString("containing_class")
                        val containing_method = SATDInFileRS.getString("containing_method")
                        val resolution = SATDInFileRS.getString("resolution")
                        val refactoring = SATDInFileRS.getString("refactoring")

                        println("Adding row: $f_id, $f_comment, $containing_class, $containing_method, $resolution, $refactoring")
                        tableModel.addRow(arrayOf(f_id, f_comment, containing_class, containing_method, resolution, refactoring))
                    }



                    adjustColumnWidths(table)

//                    indicator?.let {
//                        it.text = "Data fetching complete."
//                    }
                }
            }
        } catch (e: Exception) {
            label.text = "Connection failed: " + e.message
        }


    }
}