package com.technicaldebt_plugin_fall2024.settings

import com.intellij.openapi.components.*

@State(
    name = "GitHubSettings",
    storages = [Storage("TechnicalDebtPluginGitHubSettings.xml")]
)
@Service(Service.Level.APP)
class GitHubSettingsManager : PersistentStateComponent<GitHubSettingsManager.State> {

    data class State(
        var githubUsername: String = "",
        var githubToken: String = ""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getGitHubUsername(): String = state.githubUsername
    fun setGitHubUsername(username: String) {
        state.githubUsername = username
    }

    fun getGitHubToken(): String = state.githubToken
    fun setGitHubToken(token: String) {
        state.githubToken = token
    }
}
