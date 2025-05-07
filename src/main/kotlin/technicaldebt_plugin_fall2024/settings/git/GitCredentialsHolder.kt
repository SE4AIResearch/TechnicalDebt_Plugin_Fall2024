import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import technicaldebt_plugin_fall2024.settings.git.GitHubSettingsManager
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

@Service(Service.Level.APP)
class GitCredentialsHolder {
    companion object {
        fun getInstance(): GitCredentialsHolder = service<GitCredentialsHolder>()
    }

    private val settings = service<GitHubSettingsManager>()

    fun getUsername(): String? = settings.getGitHubUsername().ifEmpty { null }

    fun getPassword(): String? = settings.getGitHubToken().ifEmpty { null }

    fun verifyGitHubAuthentication(): Boolean {
        val username = getUsername() ?: return false
        val pat = getPassword() ?: return false

        val auth = "$username:$pat"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
        val url = URL("https://api.github.com/user")

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Basic $encodedAuth")

        return try {
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }
}