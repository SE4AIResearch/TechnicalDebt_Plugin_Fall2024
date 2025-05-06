package technicaldebt_plugin_fall2024.ui

import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * Utility object for providing standard IntelliJ icons used in the Technical Debt Plugin UI.
 */
object TechnicalDebtIcons {
    /**
     * Icon representing the Technical Debt Plugin, used in the title of the tool window.
     */
    val pluginIcon: Icon
        get() = AllIcons.Nodes.Plugin

    /**
     * Icon for settings buttons, used in buttons that open settings dialogs.
     */
    val settings: Icon
        get() = AllIcons.General.Settings

    /**
     * Icon for the documentation button, used to link to external documentation.
     */
    val documentation: Icon
        get() = AllIcons.General.Web

    /**
     * Icon for running or executing actions.
     */
    val run: Icon
        get() = AllIcons.Actions.Execute

    /**
     * Icon for thumbs up (like) actions.
     */
    val thumbsUp: Icon
        get() = AllIcons.General.InspectionsOK

    /**
     * Icon for thumbs down (dislike) actions.
     */
    val thumbsDown: Icon
        get() = AllIcons.General.InspectionsEye

    /**
     * Icon for copying text to the clipboard.
     */
    val copy: Icon
        get() = AllIcons.Actions.Copy

    /**
     * Icon for accepting/applying changes.
     */
    val accept: Icon
        get() = AllIcons.Actions.Checked

    /**
     * Icon for rejecting/discarding changes.
     */
    val reject: Icon
        get() = AllIcons.Actions.Cancel
}