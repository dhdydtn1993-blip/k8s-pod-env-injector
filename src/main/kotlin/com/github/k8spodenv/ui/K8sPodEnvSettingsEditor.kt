package com.github.k8spodenv.ui

import com.github.k8spodenv.config.K8sPodEnvRunState
import com.github.k8spodenv.service.KubectlExecutor
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingWorker
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class K8sPodEnvSettingsEditor<T : RunConfigurationBase<*>> : SettingsEditor<T>() {

    private val kubectl = KubectlExecutor()
    private val enabledCheckBox = JBCheckBox("Enable K8s Pod Env Injection")
    private val contextCombo = ComboBox<String>().apply { isEditable = true }
    private val namespaceCombo = ComboBox<String>().apply { isEditable = true }
    private val deploymentCombo = ComboBox<String>().apply { isEditable = true }
    private val whitelistField = JBTextField().apply {
        emptyText.setText("e.g., DB_HOST, DB_PORT, REDIS_URL")
    }
    private val panel: JPanel

    init {
        enabledCheckBox.addActionListener { updateFieldsEnabled() }

        contextCombo.addPopupMenuListener(onPopupOpen { loadContexts() })
        namespaceCombo.addPopupMenuListener(onPopupOpen { loadNamespaces() })
        deploymentCombo.addPopupMenuListener(onPopupOpen { loadDeployments() })

        panel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox)
            .addLabeledComponent(JBLabel("Kube Context:"), contextCombo)
            .addLabeledComponent(JBLabel("Namespace:"), namespaceCombo)
            .addLabeledComponent(JBLabel("Deployment:"), deploymentCombo)
            .addLabeledComponent(JBLabel("Env Whitelist (comma-separated, empty = all):"), whitelistField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun onPopupOpen(action: () -> Unit): PopupMenuListener {
        return object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) { action() }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {}
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
        }
    }

    private fun loadContexts() {
        loadItems(contextCombo) { kubectl.getContexts().getOrDefault(emptyList()) }
    }

    private fun loadNamespaces() {
        val context = getComboText(contextCombo)
        if (context.isBlank()) return
        loadItems(namespaceCombo) { kubectl.getNamespaces(context).getOrDefault(emptyList()) }
    }

    private fun loadDeployments() {
        val context = getComboText(contextCombo)
        val namespace = getComboText(namespaceCombo).ifBlank { "default" }
        if (context.isBlank()) return
        loadItems(deploymentCombo) { kubectl.getDeployments(context, namespace).getOrDefault(emptyList()) }
    }

    private fun loadItems(combo: ComboBox<String>, fetcher: () -> List<String>) {
        object : SwingWorker<List<String>, Void>() {
            override fun doInBackground(): List<String> = fetcher()
            override fun done() {
                try {
                    val items = get()
                    val current = getComboText(combo)
                    combo.removeAllItems()
                    items.forEach { combo.addItem(it) }
                    if (current.isNotBlank()) combo.selectedItem = current
                } catch (_: Exception) {}
            }
        }.execute()
    }

    private fun getComboText(combo: ComboBox<String>): String {
        return (combo.editor.item as? String ?: combo.selectedItem as? String ?: "").trim()
    }

    private fun updateFieldsEnabled() {
        val enabled = enabledCheckBox.isSelected
        contextCombo.isEnabled = enabled
        namespaceCombo.isEnabled = enabled
        deploymentCombo.isEnabled = enabled
        whitelistField.isEnabled = enabled
    }

    override fun resetEditorFrom(config: T) {
        val state = config.getCopyableUserData(K8sPodEnvRunState.KEY) ?: K8sPodEnvRunState()
        enabledCheckBox.isSelected = state.enabled
        contextCombo.selectedItem = state.context
        namespaceCombo.selectedItem = state.namespace
        deploymentCombo.selectedItem = state.deployment
        whitelistField.text = state.whitelist
        updateFieldsEnabled()
    }

    override fun applyEditorTo(config: T) {
        val state = K8sPodEnvRunState(
            enabled = enabledCheckBox.isSelected,
            context = getComboText(contextCombo),
            namespace = getComboText(namespaceCombo).ifBlank { "default" },
            deployment = getComboText(deploymentCombo),
            whitelist = whitelistField.text.trim()
        )
        config.putCopyableUserData(K8sPodEnvRunState.KEY, state)
    }

    override fun createEditor(): JComponent = panel
}
