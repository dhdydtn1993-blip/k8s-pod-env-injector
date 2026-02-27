package com.github.k8spodenv.service

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class KubectlExecutor {

    fun execute(
        args: List<String>,
        commandOverride: String = "kubectl",
        timeoutSeconds: Long = 10
    ): Result<String> {
        return try {
            val command = listOf(commandOverride) + args
            val process = ProcessBuilder(command)
                .redirectErrorStream(false)
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
        return execute(args, commandOverride = "kubectl", timeoutSeconds = timeoutSeconds)
    }
}
