package technicaldebt_plugin_fall2024.settings.git

import com.intellij.openapi.components.*
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project

@State(
    name = "GitHubSettings",
    storages = [Storage("TechnicalDebtPlugin_GitHubSettings.xml")]
)
@Service(Level.APP)
class GitHubSettingsManager : PersistentStateComponent<GitHubSettingsManager.State> {

    data class State(
        var username: String = "",
        var token: String = ""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getGitHubUsername(): String = state.username
    fun setGitHubUsername(value: String) {
        state.username = value
    }

    fun getGitHubToken(): String = state.token
    fun setGitHubToken(value: String) {
        state.token = value
    }

    companion object {
        fun getInstance(): GitHubSettingsManager = service()
    }
}
