package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.listeners;

import com.github.SE4AIResearch.technicaldebt_plugin_fall2024.SATD.SATDDetectorImpl;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.intellij.openapi.editor.colors.TextAttributesKey;

public class MyHighlightTool implements Annotator {

    private final SATDDetectorImpl satdDetector;
    public MyHighlightTool() {
        this.satdDetector = new SATDDetectorImpl();
    }
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // Check if the element is a comment
        if (element instanceof PsiComment) {
            String commentText = element.getText();

            // Find the SATD keyword in the comment
            if (satdDetector.isSATD(commentText)) {
                // Highlight the SATD keyword

                TextRange textRange = element.getTextRange();

                holder.newAnnotation(HighlightSeverity.WARNING, "Technical debt marker found")
                        .range(textRange)
                        .textAttributes(MyHighlightAttributes.SATD_HIGHLIGHT)
                        .create();

            }
        }
    }
}