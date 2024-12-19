package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.listeners;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.markup.TextAttributes;


import java.awt.*;

public class MyHighlightAttributes {
    public static final TextAttributesKey SATD_HIGHLIGHT = TextAttributesKey.createTextAttributesKey(
            "SATD_HIGHLIGHT",
            new TextAttributes(Color.WHITE, new Color(247, 215, 52, 110), null, null, Font.BOLD)

    );
}
