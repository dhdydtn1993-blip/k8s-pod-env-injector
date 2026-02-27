package com.github.k8spodenv.config

import com.intellij.openapi.util.Key

data class K8sPodEnvRunState(
    var enabled: Boolean = false,
    var context: String = "",
    var namespace: String = "default",
    var deployment: String = ""
) {
    fun isConfigured(): Boolean = enabled && context.isNotBlank() && deployment.isNotBlank()

    companion object {
        val KEY: Key<K8sPodEnvRunState> = Key.create("com.github.k8spodenv.runState")
    }
}
