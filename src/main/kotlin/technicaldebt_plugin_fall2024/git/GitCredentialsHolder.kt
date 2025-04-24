import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

val username = "GITHUB_USERNAME"
val pat = "GITHUB_PAT"

@Service(Service.Level.APP)
class GitCredentialsHolder {
    companion object {
        fun getInstance(): GitCredentialsHolder = service<GitCredentialsHolder>()
    }

    fun getUsername(key: String): String? {
        val attributes = createCredentialAttributes(username)
        val credentials = PasswordSafe.instance.get(attributes)
        return credentials?.userName ?: System.getenv(username)
    }

    fun getPassword(key: String): String? {
        val attributes = createCredentialAttributes(pat)
        val credentials = PasswordSafe.instance.get(attributes)
        return credentials?.getPasswordAsString() ?: System.getenv(pat)
    }

    private fun setCredentials(key: String, password: String) {
        val attributes = createCredentialAttributes(key)
        val credentials = Credentials("default", password)
        PasswordSafe.instance.set(attributes, credentials)
    }

    private fun createCredentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName("Git", key))
    }

    /**
     * Verifies that the stored credentials (Personal Access Token) authenticate successfully.
     * @param username GitHub username
     * @param key The key for retrieving the token from PasswordSafe
     * @return true if authentication succeeds, false otherwise
     */
    fun verifyGitHubAuthentication(username: String, key: String): Boolean {
        val user = getUsername(key) ?: return false
        val pat = getPassword(key) ?: return false
        if (user != username) return false
        val auth = "$username:$pat"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
        val url = URL("https://api.github.com/user")  // This requires authentication and returns 200 if valid

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
