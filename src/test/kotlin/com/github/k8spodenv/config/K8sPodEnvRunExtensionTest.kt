package com.github.k8spodenv.config

import org.jdom.Element
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class K8sPodEnvRunExtensionTest {

    @Test
    fun `serialization round-trip preserves state`() {
        val extension = K8sPodEnvRunExtension()

        val state = K8sPodEnvRunState(
            enabled = true,
            context = "gke_project_zone_cluster",
            namespace = "staging",
            deployment = "my-service"
        )

        val element = Element("test")
        extension.writeState(state, element)

        val restored = extension.readState(element)

        assertEquals(state.enabled, restored.enabled)
        assertEquals(state.context, restored.context)
        assertEquals(state.namespace, restored.namespace)
        assertEquals(state.deployment, restored.deployment)
    }

    @Test
    fun `readState returns defaults for empty element`() {
        val extension = K8sPodEnvRunExtension()
        val element = Element("test")

        val state = extension.readState(element)

        assertFalse(state.enabled)
        assertEquals("", state.context)
        assertEquals("default", state.namespace)
        assertEquals("", state.deployment)
    }

    @Test
    fun `writeState creates correct XML structure`() {
        val extension = K8sPodEnvRunExtension()
        val state = K8sPodEnvRunState(
            enabled = true,
            context = "my-ctx",
            namespace = "my-ns",
            deployment = "my-deploy"
        )

        val element = Element("test")
        extension.writeState(state, element)

        val child = element.getChild("k8s-pod-env")
        assertEquals("true", child.getAttributeValue("enabled"))
        assertEquals("my-ctx", child.getAttributeValue("context"))
        assertEquals("my-ns", child.getAttributeValue("namespace"))
        assertEquals("my-deploy", child.getAttributeValue("deployment"))
    }
}
