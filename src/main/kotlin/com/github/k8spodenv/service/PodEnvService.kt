package com.github.k8spodenv.service

import java.util.concurrent.ConcurrentHashMap

class PodEnvService(private val kubectl: KubectlExecutor = KubectlExecutor()) {

    private data class CacheKey(val context: String, val namespace: String, val deployment: String)
    private data class CacheEntry(val envVars: Map<String, String>, val timestamp: Long)

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()
    private val cacheTtlMs: Long = 5 * 60 * 1000 // 5 minutes

    fun fetchEnvVars(
        context: String,
        namespace: String,
        deployment: String
    ): Result<Map<String, String>> {
        val cacheKey = CacheKey(context, namespace, deployment)
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
            return Result.success(cached.envVars)
        }

        return findPod(context, namespace, deployment)
            .flatMap { podName -> execEnv(context, namespace, podName) }
            .map { raw -> parseEnv(raw) }
            .onSuccess { envVars ->
                cache[cacheKey] = CacheEntry(envVars, System.currentTimeMillis())
            }
    }

    fun clearCache() {
        cache.clear()
    }

    private fun findPod(context: String, namespace: String, deployment: String): Result<String> {
        val args = listOf(
            "get", "pods",
            "-l", "app=$deployment",
            "-n", namespace,
            "--context=$context",
            "--field-selector=status.phase=Running",
            "-o", "name",
            "--no-headers"
        )
        return kubectl.kubectl(args).flatMap { output ->
            val firstPod = output.trim().lines().firstOrNull { it.isNotBlank() }
            if (firstPod != null) {
                Result.success(firstPod.removePrefix("pod/"))
            } else {
                Result.failure(RuntimeException("No running pods found for deployment '$deployment' in $namespace"))
            }
        }
    }

    private fun execEnv(context: String, namespace: String, podName: String): Result<String> {
        val args = listOf(
            "exec", podName,
            "-n", namespace,
            "--context=$context",
            "--", "env"
        )
        return kubectl.kubectl(args)
    }

    private fun parseEnv(raw: String): Map<String, String> {
        return raw.trim().lines()
            .filter { it.contains('=') }
            .associate { line ->
                val idx = line.indexOf('=')
                line.substring(0, idx) to line.substring(idx + 1)
            }
    }

    private fun <T> Result<T>.flatMap(transform: (T) -> Result<T>): Result<T> {
        return fold(
            onSuccess = { transform(it) },
            onFailure = { Result.failure(it) }
        )
    }
}
