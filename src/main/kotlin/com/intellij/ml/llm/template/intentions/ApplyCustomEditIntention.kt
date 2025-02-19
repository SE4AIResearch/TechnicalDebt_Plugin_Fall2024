package com.intellij.ml.llm.template.intentions

import com.intellij.ml.llm.template.LLMBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class ApplyCustomEditIntention : ApplyTransformationIntention() {
    override fun getInstruction(project: Project, editor: Editor, satdType : String): String? {
//        return Messages.showInputDialog(project, "Enter prompt:", "Codex", null)

        val prompt = "Fix $satdType in the given code excerpt"
        return prompt
    }

    override fun getText(): String = LLMBundle.message("intentions.apply.custom.edit.name")
    override fun getFamilyName(): String = text
}
