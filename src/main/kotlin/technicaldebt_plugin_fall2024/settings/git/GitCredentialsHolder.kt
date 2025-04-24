import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

const val GITHUB_CREDENTIALS_KEY = "GITHUB_CREDENTIALS"

@Service(Service.Level.APP)
class GitCredentialsHolder {
    companion object {
        fun getInstance(): GitCredentialsHolder = service<GitCredentialsHolder>()
    }

    fun getUsername(): String? {
        val attributes = createCredentialAttributes(GITHUB_CREDENTIALS_KEY)
        val credentials = PasswordSafe.instance.get(attributes)
        return credentials?.userName
    }

    fun getPassword(): String? {
        val attributes = createCredentialAttributes(GITHUB_CREDENTIALS_KEY)
        val credentials = PasswordSafe.instance.get(attributes)
        return credentials?.getPasswordAsString()
    }

    fun setCredentials(key: String, username: String, pat: String) {
        val attributes = createCredentialAttributes(key)
        val credentials = Credentials(username, pat)
        PasswordSafe.instance.set(attributes, credentials)
    }

    private fun createCredentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName("Git", key))
    }

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
