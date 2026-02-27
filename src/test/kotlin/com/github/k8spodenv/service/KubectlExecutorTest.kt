package com.github.k8spodenv.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KubectlExecutorTest {

    private val executor = KubectlExecutor()

    @Test
    fun `execute returns stdout on success`() {
        val result = executor.execute(listOf("hello"), commandOverride = "echo")
        assertTrue(result.isSuccess)
        assertEquals("hello", result.getOrThrow().trim())
    }

    @Test
    fun `execute returns failure on non-zero exit`() {
        val result = executor.execute(
            listOf("/nonexistent_path_that_does_not_exist_12345"),
            commandOverride = "ls"
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `execute returns failure on timeout`() {
        val result = executor.execute(
            listOf("30"),
            commandOverride = "sleep",
            timeoutSeconds = 1
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("timed out") == true)
    }
}
