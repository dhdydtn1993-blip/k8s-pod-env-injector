package com.github.k8spodenv.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PodEnvServiceTest {

    private lateinit var kubectl: KubectlExecutor
    private lateinit var service: PodEnvService

    @BeforeEach
    fun setup() {
        kubectl = mockk()
        service = PodEnvService(kubectl)
    }

    @Test
    fun `fetchEnvVars returns parsed env map`() {
        every { kubectl.kubectl(match { it.contains("get") && it.contains("pods") }, any()) } returns
            Result.success("pod/my-service-abc123\n")

        every { kubectl.kubectl(match { it.contains("exec") }, any()) } returns
            Result.success("DB_HOST=localhost\nDB_PORT=5432\nSPRING_PROFILES_ACTIVE=dev\n")

        val result = service.fetchEnvVars(
            context = "gke_project_zone_cluster",
            namespace = "default",
            deployment = "my-service"
        )

        assertTrue(result.isSuccess)
        val envMap = result.getOrThrow()
        assertEquals("localhost", envMap["DB_HOST"])
        assertEquals("5432", envMap["DB_PORT"])
        assertEquals("dev", envMap["SPRING_PROFILES_ACTIVE"])
    }

    @Test
    fun `fetchEnvVars returns failure when no pods found`() {
        every { kubectl.kubectl(any(), any()) } returns Result.success("")

        val result = service.fetchEnvVars(
            context = "ctx",
            namespace = "ns",
            deployment = "deploy"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No running pods") == true)
    }

    @Test
    fun `fetchEnvVars uses cache on second call`() {
        every { kubectl.kubectl(match { it.contains("get") && it.contains("pods") }, any()) } returns
            Result.success("pod/my-service-abc123\n")
        every { kubectl.kubectl(match { it.contains("exec") }, any()) } returns
            Result.success("KEY=value\n")

        val result1 = service.fetchEnvVars("ctx", "ns", "deploy")
        val result2 = service.fetchEnvVars("ctx", "ns", "deploy")

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(result1.getOrThrow(), result2.getOrThrow())

        verify(exactly = 1) { kubectl.kubectl(match { it.contains("exec") }, any()) }
    }

    @Test
    fun `fetchEnvVars handles env values containing equals sign`() {
        every { kubectl.kubectl(match { it.contains("get") && it.contains("pods") }, any()) } returns
            Result.success("pod/my-service-abc123\n")
        every { kubectl.kubectl(match { it.contains("exec") }, any()) } returns
            Result.success("JAVA_OPTS=-Xmx512m -Dkey=val\n")

        val result = service.fetchEnvVars("ctx", "ns", "deploy")
        assertTrue(result.isSuccess)
        assertEquals("-Xmx512m -Dkey=val", result.getOrThrow()["JAVA_OPTS"])
    }

    @Test
    fun `fetchEnvVars propagates kubectl failure`() {
        every { kubectl.kubectl(any(), any()) } returns
            Result.failure(RuntimeException("connection refused"))

        val result = service.fetchEnvVars("ctx", "ns", "deploy")
        assertTrue(result.isFailure)
    }
}
