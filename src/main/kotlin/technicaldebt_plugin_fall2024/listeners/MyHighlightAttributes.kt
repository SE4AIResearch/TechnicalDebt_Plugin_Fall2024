package technicaldebt_plugin_fall2024.listeners

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

object MyHighlightAttributes {
    val SATD_HIGHLIGHT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SATD_HIGHLIGHT",
        TextAttributes(Color.WHITE, Color(247, 215, 52, 110), null, null, Font.BOLD)
    )
}