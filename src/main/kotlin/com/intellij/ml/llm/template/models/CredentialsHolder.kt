package com.intellij.ml.llm.template.models

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service


private const val OPENAI_ORGANIZATION = "OPENAI_ORGANIZATION"
private const val OPEN_AI_KEY = "OPENAI_AI_KEY"
private const val GEMINI_AI_KEY = "GEMINI_AI_KEY"

@Service(Service.Level.APP)
class CredentialsHolder {
    companion object {
        fun getInstance(): CredentialsHolder = service<CredentialsHolder>()
    }

    fun getGeminiKey(): String?
    {
        return getCredentials(GEMINI_AI_KEY)
    }

    fun setGeminiApiKey(apiKey: String) {
        setCredentials(GEMINI_AI_KEY, apiKey)
    }
    fun getOpenAiApiKey(): String? {
        return getCredentials(OPEN_AI_KEY)
    }

    fun setOpenAiApiKey(apiKey: String) {
        setCredentials(OPEN_AI_KEY, apiKey)
    }

    fun getOpenAiOrganization(): String? {
        return getCredentials(OPENAI_ORGANIZATION)
    }

    fun setOpenAiOrganization(organizationKey: String) {
        setCredentials(OPENAI_ORGANIZATION, organizationKey)
    }

    private fun getCredentials(key: String): String? {
        val attributes = createCredentialAttributes(key)
        val credentials = PasswordSafe.instance.get(attributes)
        return credentials?.getPasswordAsString() ?: System.getenv(key)
    }

    private fun setCredentials(key: String, password: String) {
        val attributes = createCredentialAttributes(key)
        val credentials = Credentials("default", password)
        PasswordSafe.instance.set(attributes, credentials)
    }

    private fun createCredentialAttributes(key: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName("LLM", key))
    }
}