package com.intellij.ml.llm.template.intentions

//import com.intellij.ml.llm.template.models.CodexRequestProvider
//import com.intellij.ml.llm.template.models.sendEditRequest
import com.intellij.openapi.ui.Messages
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.*
import com.intellij.ml.llm.template.models.gemini.GeminiChatMessage
import com.intellij.ml.llm.template.models.gemini.GeminiRequestBody
import com.intellij.ml.llm.template.models.ollama.OllamaBody
import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import com.intellij.ml.llm.template.settings.LLMSettingsManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import java.util.*
import com.intellij.ml.llm.template.ui.LLMOutputToolWindow

@Suppress("UnstableApiUsage")
abstract class ApplyTransformationIntention(
) : IntentionAction {

    companion object{
        fun updateDocument(project: Project, suggestion: String, document: Document, textRange: TextRange) {
            document.replaceString(textRange.startOffset, textRange.endOffset, suggestion)
            PsiDocumentManager.getInstance(project).commitDocument(document)
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            psiFile?.let {
                val reformatRange = TextRange(textRange.startOffset, textRange.startOffset + suggestion.length)
                CodeStyleManager.getInstance(project).reformatText(it, listOf(reformatRange))
            }
        }
    }

    private val logger = Logger.getInstance("#com.intellij.ml.llm")
    private fun extractBracketContent(ch: String, str: String): String {
        val sb = StringBuilder()
        var inBracket = false

        for (c in str) {
            when {
                (c.toString() == ch) && !inBracket -> {
                    inBracket = true
                    continue
                }
                (c.toString() == ch) && inBracket -> break
                inBracket -> sb.append(c)
            }
        }

        return sb.toString()
    }



    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.transformation.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && file != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return

        val document = editor.document
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        if (selectedText != null) {
            val textRange = TextRange.create(selectionModel.selectionStart, selectionModel.selectionEnd)
            transform(project, selectedText, editor, textRange)
        } else {
            val namedElement = getParentNamedElement(editor)
            if (namedElement != null) {
                val queryText = namedElement.text
                val textRange = namedElement.textRange
                selectionModel.setSelection(textRange.startOffset, textRange.endOffset)
                transform(project, queryText, editor, textRange)
            } else {
                selectionModel.selectLineAtCaret()
                val textRange = getLineTextRange(document, editor)
                transform(project, document.getText(textRange), editor, textRange)
            }
        }
    }

    private fun getLineTextRange(document: Document, editor: Editor): TextRange {
        val lineNumber = document.getLineNumber(editor.caretModel.offset)
        val startOffset = document.getLineStartOffset(lineNumber)
        val endOffset = document.getLineEndOffset(lineNumber)
        return TextRange.create(startOffset, endOffset)
    }

    private fun getParentNamedElement(editor: Editor): PsiNameIdentifierOwner? {
        val element = PsiUtilBase.getElementAtCaret(editor)
        return PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
    }

    private fun transform(project: Project, text: String, editor: Editor, textRange: TextRange) {
        val settings = LLMSettingsManager.getInstance()
        val satdType = extractBracketContent('$'.toString(), text)


        if (satdType.isEmpty()) {
            Messages.showWarningDialog(
                    project,
                    "SATD type not detected. Please ensure your code contains valid SATD.",
                    "SATD Detection Warning"
            )
            return
        }

        val instruction = getInstruction(project, editor, satdType) ?: return
        logger.info("Invoke transformat`ion action with '$instruction' instruction for '$text'")
        val task =
            object : Task.Backgroundable(project, LLMBundle.message("intentions.request.background.process.title")) {
                override fun run(indicator: ProgressIndicator) {

                    var prompt = ""
                    if(satdType.isNotEmpty())
                    {
                        prompt = "This code has SATDType {$satdType}. Output raw code fixing the SATDType: {$text}. Do NOT include any formatting delimiters such as '`'."
                    }
                    else
                    {
                        prompt = "No SATDType provided. Output raw code fixing the following issue: {$text}. Do NOT include any formatting delimiters such as '`'."
                    }
                    var response1:LLMBaseResponse? = null
                    var response2:LLMBaseResponse? = null


                    when (settings.provider) {
                        LLMSettingsManager.LLMProvider.GEMINI -> {
                            val provider = GeminiRequestProvider
                            val messages = listOf(
                                    GeminiChatMessage(role = "user", content = prompt ),
                            )

                            response1 = sendGeminiRequest(
                                    project,
                                    messages,
                                    model = provider.chatModel,
                                    llmRequestProvider = provider
                            )
                            val firstSuggestion = response1?.getSuggestions()?.firstOrNull()?.text ?: ""
                            val messages2 = listOf(
                                    GeminiChatMessage(role = "user", content = prompt),
                                    GeminiChatMessage(role = "assistant", content = firstSuggestion),
                                    GeminiChatMessage(role = "user", content = "Give me a different version of the fix. Use a different approach or style."),
                            )
                            response2 = sendGeminiRequest(
                                    project,
                                    messages2,
                                    model = provider.chatModel,
                                    llmRequestProvider = provider
                            )

//                            print("Here: $response")
                        }
                        LLMSettingsManager.LLMProvider.OLLAMA -> {
                            val provider = OllamaRequestProvider

                            val ollama = OllamaBody(provider.chatModel, prompt)

                            response1 = sendOllamaRequest(
                                    project,
                                    ollama.prompt,
                                    llmRequestProvider = provider,

                            )
                            val firstSuggestion = response1?.getSuggestions()?.firstOrNull()?.text ?: ""
                            val altPrompt = "$prompt\n\nPreviously suggested fix:\n$firstSuggestion\n\nNow provide an alternative fix using a different approach."
                            response2 = sendOllamaRequest(
                                    project,
                                    altPrompt,
                                    llmRequestProvider = provider
                            )
                        }


                        LLMSettingsManager.LLMProvider.OPENAI -> {
                            val provider = GPTRequestProvider


                            val messages = listOf(
                                    OpenAiChatMessage(role = "user", content = prompt ),
                            )

                            response1 = sendChatRequest(
                                    project,
                                    messages,
                                    model = provider.chatModel,
                                    llmRequestProvider = provider,
                                    temperature = 1.1,
                                    topP = 0.85
                            )
                            val response1Text = response1?.getSuggestions()?.firstOrNull()?.text ?: ""
                            val messages2 = listOf(
                                    OpenAiChatMessage(role = "user", content = prompt),
                                    OpenAiChatMessage(role = "assistant", content = response1Text),
                                    OpenAiChatMessage(role = "user", content = "Give me a different version of the fix. Try a new approach or improvement.")
                            )
                            response2 = sendChatRequest(
                                    project,
                                    messages2,
                                    model = provider.chatModel,
                                    llmRequestProvider = provider,
                                    temperature = 1.1,
                                    topP = 0.85
                            )



                        }
                    }

                        var updatedCode1 = "Response was not provided";
                        var updatedCode2 = "Response was not provided";


                        if (response1 != null) {
                            var suggestions = response1.getSuggestions()
                            if (suggestions.isEmpty()) {
                                logger.warn("No suggestions received for transformation.")
                            }
                            response1.getSuggestions().firstOrNull()?.let {
                                logger.info("Suggested change: $it")
                                updatedCode1 = it.text
                                if (updatedCode1.contains("```")) {
                                    updatedCode1 = updatedCode1
                                        .replace("```java", "")
                                        .replace("```", "")
                                }
                            }
                        }
                        if (response2 != null) {
                            var suggestions = response2.getSuggestions()
                            if (suggestions.isEmpty()) {
                                logger.warn("No suggestions received for transformation.")
                            }
                            response2.getSuggestions().firstOrNull()?.let {
                                logger.info("Suggested change: $it")
                                updatedCode2 = it.text

                                if (updatedCode2.contains("```")) {
                                    updatedCode2 = updatedCode2
                                            .replace("```java", "")
                                            .replace("```", "")
                                }
                            }
                        }


                        outputToSideWindow(updatedCode1, updatedCode2, editor, project, textRange)


                }

                }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }
}

    fun getInstruction(project: Project, editor: Editor, satdType: String) {}



    private fun outputToSideWindow(content1: String, content2:String, editor: Editor, project: Project, textRange: TextRange) {

        invokeLater {
            LLMOutputToolWindow.updateOutput(content1, content2, editor, project, textRange)
        }
    }


