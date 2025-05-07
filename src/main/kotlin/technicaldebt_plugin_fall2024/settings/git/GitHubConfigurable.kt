package technicaldebt_plugin_fall2024.settings.git

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

class GitHubConfigurable : BoundConfigurable("SATD Tool GitHub Credentials") {
    private val settings = service<GitHubSettingsManager>()

    override fun createPanel(): DialogPanel {
        return panel {
            row("GitHub Username:") {
                textField()
                    .bindText(settings::getGitHubUsername, settings::setGitHubUsername)
            }
            row("GitHub Personal Access Token (PAT):") {
                passwordField()
                    .bindText(settings::getGitHubToken, settings::setGitHubToken)
                browserLink("Generate PAT here", "https://github.com/settings/tokens")
            }
        }
    }
}

fun openGitHubSettingsDialog(project: Project?) {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, GitHubConfigurable::class.java)
}