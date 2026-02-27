package com.github.k8spodenv.config

import com.github.k8spodenv.service.PodEnvService
import com.github.k8spodenv.ui.K8sPodEnvSettingsEditor
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.SettingsEditor
import org.jdom.Element

class K8sPodEnvRunExtension : RunConfigurationExtension() {

    private val podEnvService = PodEnvService()

    companion object {
        private const val ELEMENT_NAME = "k8s-pod-env"
        private const val ATTR_ENABLED = "enabled"
        private const val ATTR_CONTEXT = "context"
        private const val ATTR_NAMESPACE = "namespace"
        private const val ATTR_DEPLOYMENT = "deployment"
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return true
    }

    override fun isEnabledFor(
        applicableConfiguration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?
    ): Boolean {
        val state = applicableConfiguration.getCopyableUserData(K8sPodEnvRunState.KEY)
        return state?.isConfigured() == true
    }

    override fun <T : RunConfigurationBase<*>> updateJavaParameters(
        configuration: T,
        params: JavaParameters,
        runnerSettings: RunnerSettings?
    ) {
        val state = configuration.getCopyableUserData(K8sPodEnvRunState.KEY) ?: return
        if (!state.isConfigured()) return

        val result = podEnvService.fetchEnvVars(
            context = state.context,
            namespace = state.namespace,
            deployment = state.deployment
        )

        result.fold(
            onSuccess = { envVars ->
                val existingEnv = params.env
                var injectedCount = 0
                for ((key, value) in envVars) {
                    if (!existingEnv.containsKey(key)) {
                        params.addEnv(key, value)
                        injectedCount++
                    }
                }
                notify(
                    configuration,
                    "Injected $injectedCount env vars from pod (${state.deployment})",
                    NotificationType.INFORMATION
                )
            },
            onFailure = { error ->
                notify(
                    configuration,
                    "Failed to fetch env vars: ${error.message}",
                    NotificationType.WARNING
                )
            }
        )
    }

    override fun <P : RunConfigurationBase<*>> createEditor(configuration: P): SettingsEditor<P> {
        return K8sPodEnvSettingsEditor()
    }

    override fun getEditorTitle(): String = "K8s Pod Env"

    override fun getSerializationId(): String = "com.github.k8spodenv"

    override fun readExternal(runConfiguration: RunConfigurationBase<*>, element: Element) {
        val state = readState(element)
        runConfiguration.putCopyableUserData(K8sPodEnvRunState.KEY, state)
    }

    override fun writeExternal(runConfiguration: RunConfigurationBase<*>, element: Element) {
        val state = runConfiguration.getCopyableUserData(K8sPodEnvRunState.KEY) ?: return
        writeState(state, element)
    }

    fun readState(element: Element): K8sPodEnvRunState {
        val child = element.getChild(ELEMENT_NAME) ?: return K8sPodEnvRunState()
        return K8sPodEnvRunState(
            enabled = child.getAttributeValue(ATTR_ENABLED)?.toBoolean() ?: false,
            context = child.getAttributeValue(ATTR_CONTEXT) ?: "",
            namespace = child.getAttributeValue(ATTR_NAMESPACE) ?: "default",
            deployment = child.getAttributeValue(ATTR_DEPLOYMENT) ?: ""
        )
    }

    fun writeState(state: K8sPodEnvRunState, element: Element) {
        val child = Element(ELEMENT_NAME)
        child.setAttribute(ATTR_ENABLED, state.enabled.toString())
        child.setAttribute(ATTR_CONTEXT, state.context)
        child.setAttribute(ATTR_NAMESPACE, state.namespace)
        child.setAttribute(ATTR_DEPLOYMENT, state.deployment)
        element.addContent(child)
    }

    private fun notify(configuration: RunConfigurationBase<*>, message: String, type: NotificationType) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("K8s Pod Env Injector")
                .createNotification(message, type)
                .notify(configuration.project)
        } catch (_: Exception) {
            // Notification group not registered, silently ignore
        }
    }
}
