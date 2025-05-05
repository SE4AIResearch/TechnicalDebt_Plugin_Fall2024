package technicaldebt_plugin_fall2024.toolWindow.Without

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import java.io.*
import java.util.stream.Collectors

class SATDFileManager {
    fun parseMethodSignature(signature: String): Pair<String, List<String>> {
        val nameEndIndex = signature.indexOf('(')
        val methodName = signature.substring(0, nameEndIndex).trim()
        val paramString = signature.substring(nameEndIndex + 1, signature.indexOf(')')).trim()

        val paramTypes = if (paramString.isEmpty()) {
            emptyList()
        } else {
            paramString.split(',').map { it.trim() }
        }

        return Pair(methodName, paramTypes)
    }

    fun navigateToCode(project: Project, methodName: String?, path: String): Boolean {
        val homePath = project.basePath
        val fullPath = "$homePath/$path"
        val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
        if (file == null) {
            println("File not found: $fullPath")
            val message = "File not found at given path"
            val title = ""
            Messages.showErrorDialog(message, title)
            return false
        }
        FileEditorManager.getInstance(project).openFile(file, true)
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            println("No editor is currently open")
            return false
        }
        val caretModel = editor.caretModel
        if (methodName != null) {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile != null) {
                val methods = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod::class.java)
                val (parsedName, parsedParams) = parseMethodSignature(methodName)
                val method = methods.find { m ->
                    m.name == parsedName &&
                            m.parameterList.parametersCount == parsedParams.size &&
                            m.parameterList.parameters.map { it.type.presentableText } == parsedParams
                }
                if (method != null) {
                    val offset = method.textOffset
                    val logicalPosition = editor.offsetToLogicalPosition(offset)
                    editor.caretModel.moveToLogicalPosition(logicalPosition)
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                    return true
                } else {
                    Messages.showErrorDialog("Method '$methodName' not found in file", "")
                    return false
                }
            }
        }
        return true

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
