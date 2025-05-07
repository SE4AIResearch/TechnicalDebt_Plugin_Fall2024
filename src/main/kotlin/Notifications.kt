import technicaldebt_plugin_fall2024.settings.openTechnicalDebtToolSettingsDialog
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

private fun createNotificationGroup(): NotificationGroup {
    return NotificationGroupManager.getInstance().getNotificationGroup("Technical Debt group")
}

internal fun showUnauthorizedNotification(project: Project) {
    val notification = createNotificationGroup().createNotification(
        LLMBundle.message("notification.unauthorized.title"),
        LLMBundle.message("notification.unauthorized.key.not.provided"),
        NotificationType.WARNING
    )

    val action = LLMBundle.message("notification.unauthorized.key.not.provided.action")
    notification.addAction(NotificationAction.createSimple(action) {
        openTechnicalDebtToolSettingsDialog(project)
        notification.expire()
    })
    notification.notify(project)
}

internal fun showAuthorizationFailedNotification(project: Project) {
    val notification = createNotificationGroup().createNotification(
        LLMBundle.message("notification.unauthorized.title"),
        LLMBundle.message("notification.unauthorized.key.is.invalid"),
        NotificationType.WARNING
    )

    val action = LLMBundle.message("notification.unauthorized.key.not.provided.action")
    notification.addAction(NotificationAction.createSimple(action) {
        openTechnicalDebtToolSettingsDialog(project)
        notification.expire()
    })
    notification.notify(project)
}

internal fun showRequestFailedNotification(project: Project, message: String) {
    createNotificationGroup().createNotification(
        LLMBundle.message("notification.request.failed.title"),
        message,
        NotificationType.WARNING
    ).notify(project)
}


internal fun showUnauthorizedGitNotification(project: Project, missingCredential: String) {
    val notification = createNotificationGroup().createNotification(
        "GitHub Authorization Failed",
        "$missingCredential is missing or invalid. Please provide the credential in the settings dialog.",
        NotificationType.WARNING
    )

    val action = "Configure GitHub Credentials"
    notification.addAction(NotificationAction.createSimple(action) {
        openTechnicalDebtToolSettingsDialog(project)
        notification.expire()
    })
    notification.notify(project)
}

