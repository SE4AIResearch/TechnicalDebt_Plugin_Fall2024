package technicaldebt_plugin_fall2024.settings.git

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

const val GITHUB_CREDENTIALS_KEY = "GITHUB_CREDENTIALS"

@Service(Service.Level.APP)
class GitHubSettingsManager {
    private var username: String = ""
    private var token: String = ""

    fun getGitHubUsername(): String = username
    fun setGitHubUsername(value: String) {
        username = value
    }

    fun getGitHubToken(): String = token
    fun setGitHubToken(value: String) {
        token = value
    }

    fun applyCredentials() {
        val attributes = CredentialAttributes(generateServiceName("Git", GITHUB_CREDENTIALS_KEY))
        val credentials = Credentials(username, token)
        PasswordSafe.instance.set(attributes, credentials)
    }

    companion object {
        fun getInstance(): GitHubSettingsManager = service<GitHubSettingsManager>()
    }
}
