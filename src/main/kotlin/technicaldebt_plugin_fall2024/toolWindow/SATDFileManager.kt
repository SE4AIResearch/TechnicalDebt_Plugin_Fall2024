package technicaldebt_plugin_fall2024.toolWindow.Without

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.*
import java.util.stream.Collectors

class SATDFileManager {
    fun navigateToCode(project: Project, lineNumber: Int, path: String) {
        val homePath = project.basePath
        val fullPath = "$homePath/$path"
        val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
        if (file == null) {
            println("File not found: $fullPath")
            val message = "File not found at given path"
            val title = ""
            Messages.showErrorDialog(message, title)
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
                        var url = lines[j].trim { it <= ' ' }.substring(6).trim { it <= ' ' }
                        if(url.endsWith(".git")){
                            url = url.removeSuffix(".git");
                        }
                        return url
                    }
                }
            }
        }
        return null
    }

    fun writeTestRepo(path: String, project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val gitUrl = getGitHubUrl(project)
            try {
                BufferedWriter(FileWriter(path)).use { writer ->
                    writer.write(gitUrl)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
