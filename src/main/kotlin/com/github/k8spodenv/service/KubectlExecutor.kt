package com.github.k8spodenv.service

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class KubectlExecutor {

    companion object {
        private val KUBECTL_SEARCH_PATHS = listOf(
            "/opt/homebrew/bin/kubectl",
            "/usr/local/bin/kubectl",
            "/usr/bin/kubectl",
            "/snap/bin/kubectl"
        )

        fun findKubectl(): String {
            return KUBECTL_SEARCH_PATHS.firstOrNull { java.io.File(it).canExecute() }
                ?: "kubectl" // fallback to PATH
        }
    }

    fun execute(
        args: List<String>,
        commandOverride: String = "kubectl",
        timeoutSeconds: Long = 10
    ): Result<String> {
        return try {
            val command = listOf(commandOverride) + args
            val env = System.getenv("PATH") ?: ""
            val process = ProcessBuilder(command)
                .redirectErrorStream(false)
                .apply {
                    environment()["PATH"] = "$env:/opt/homebrew/bin:/usr/local/bin:/usr/bin"
                }
                .start()

            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return Result.failure(TimeoutException("kubectl timed out after ${timeoutSeconds}s"))
            }

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()

            if (process.exitValue() == 0) {
                Result.success(stdout)
            } else {
                Result.failure(RuntimeException(stderr.ifBlank { "exit code ${process.exitValue()}" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun kubectl(args: List<String>, timeoutSeconds: Long = 10): Result<String> {
        return execute(args, commandOverride = findKubectl(), timeoutSeconds = timeoutSeconds)
    }
}
