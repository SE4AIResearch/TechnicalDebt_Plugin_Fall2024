package technicaldebt_plugin_fall2024.ui

import com.intellij.CommonBundle
import java.util.ResourceBundle

/**
 * Utility class for accessing localized labels in the Technical Debt Plugin.
 */
object PluginLabelsBundle {
    // Load the resource bundle for the plugin's labels (assumes a file named "messages.PluginLabels.properties")
    private val bundle: ResourceBundle by lazy {
        ResourceBundle.getBundle("messages.PluginLabels")
    }

    /**
     * Retrieves a localized label for the given key.
     *
     * @param key The key for the label in the resource bundle.
     * @return The localized string, or a placeholder if the key is not found.
     */
    fun get(key: String): String {
        return try {
            bundle.getString(key)
        } catch (e: Exception) {
            CommonBundle.message("label.missing", key) // Fallback for missing keys
        }
    }
}