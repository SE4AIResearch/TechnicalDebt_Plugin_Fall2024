package technicaldebt_plugin_fall2024.toolWindow.Without
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages

class PluginReloader : DynamicPluginListener {
    private val logger = Logger.getInstance(PluginReloader::class.java)

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (pluginDescriptor.pluginId.idString == "technicaldebt_plugin_fall2024") {
            logger.info("Plugin is unloading... Preparing for restart.")

            // Show a prompt (optional) to inform the user about the restart
            invokeLater(ModalityState.NON_MODAL) {
                Messages.showMessageDialog(
                    "The Technical Debt Plugin is reloading...",
                    "Plugin Reload",
                    Messages.getInformationIcon()
                )
            }

            // Restart the IDE to reload the plugin
            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().restart()
            }
        }
    }
}