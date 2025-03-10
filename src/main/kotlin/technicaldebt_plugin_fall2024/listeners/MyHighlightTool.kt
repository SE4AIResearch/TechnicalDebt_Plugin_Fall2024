package technicaldebt_plugin_fall2024.listeners

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import technicaldebt_plugin_fall2024.SATD.SATDDetectorImpl

class MyHighlightTool : Annotator {

    private val satdDetector = SATDDetectorImpl()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Check if the element is a comment
        if (element is PsiComment) {
            val commentText = element.text

            // Log the comment text for debugging
            println("Annotating comment: $commentText")

            // Find the SATD keyword in the comment
            if (satdDetector.isSATD(commentText)) {
                // Highlight the SATD keyword
                val textRange = element.textRange

                holder.newAnnotation(HighlightSeverity.WARNING, "Technical debt marker found")
                    .range(textRange)
                    .textAttributes(MyHighlightAttributes.SATD_HIGHLIGHT)
                    .create()

                // Log the annotation creation for debugging
                println("Annotation created for: $commentText")
            }
        }
    }
}