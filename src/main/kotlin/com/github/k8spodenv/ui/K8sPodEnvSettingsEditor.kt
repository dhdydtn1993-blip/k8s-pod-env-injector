package com.github.k8spodenv.ui

import com.github.k8spodenv.config.K8sPodEnvRunState
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class K8sPodEnvSettingsEditor<T : RunConfigurationBase<*>> : SettingsEditor<T>() {

    private val enabledCheckBox = JBCheckBox("Enable K8s Pod Env Injection")
    private val contextField = JBTextField()
    private val namespaceField = JBTextField()
    private val deploymentField = JBTextField()
    private val panel: JPanel

    init {
        enabledCheckBox.addActionListener { updateFieldsEnabled() }

        panel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox)
            .addLabeledComponent(JBLabel("Kube Context:"), contextField)
            .addLabeledComponent(JBLabel("Namespace:"), namespaceField)
            .addLabeledComponent(JBLabel("Deployment:"), deploymentField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun updateFieldsEnabled() {
        val enabled = enabledCheckBox.isSelected
        contextField.isEnabled = enabled
        namespaceField.isEnabled = enabled
        deploymentField.isEnabled = enabled
    }

    override fun resetEditorFrom(config: T) {
        val state = config.getCopyableUserData(K8sPodEnvRunState.KEY) ?: K8sPodEnvRunState()
        enabledCheckBox.isSelected = state.enabled
        contextField.text = state.context
        namespaceField.text = state.namespace
        deploymentField.text = state.deployment
        updateFieldsEnabled()
    }

    override fun applyEditorTo(config: T) {
        val state = K8sPodEnvRunState(
            enabled = enabledCheckBox.isSelected,
            context = contextField.text.trim(),
            namespace = namespaceField.text.trim().ifBlank { "default" },
            deployment = deploymentField.text.trim()
        )
        config.putCopyableUserData(K8sPodEnvRunState.KEY, state)
    }

    override fun createEditor(): JComponent = panel
}
