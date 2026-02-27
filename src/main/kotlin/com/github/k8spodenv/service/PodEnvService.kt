package com.github.k8spodenv.service

import java.util.concurrent.ConcurrentHashMap

class PodEnvService(private val kubectl: KubectlExecutor = KubectlExecutor()) {

    companion object {
        /** Env vars that are pod/container-specific and should never be injected locally */
        private val EXCLUDED_KEYS = setOf(
            "JAVA_TOOL_OPTIONS",
            "PATH",
            "HOME",
            "HOSTNAME",
            "TERM",
            "SHLVL",
            "PWD",
            "LANG",
            "LC_ALL"
        )

        /** Prefixes for Kubernetes-injected service discovery vars */
        private val EXCLUDED_PREFIXES = listOf(
            "KUBERNETES_",
            "KODA_CUSTODY_API_SERVICE_",  // k8s auto-generated service vars
        )

        fun shouldExclude(key: String): Boolean {
            if (key in EXCLUDED_KEYS) return true
            if (EXCLUDED_PREFIXES.any { key.startsWith(it) }) return true
            // Exclude k8s service vars pattern: *_SERVICE_HOST, *_SERVICE_PORT, *_PORT_*
            if (key.endsWith("_SERVICE_HOST") || key.endsWith("_SERVICE_PORT")) return true
            if (key.contains("_PORT_") && (key.endsWith("_TCP") || key.endsWith("_UDP") || key.endsWith("_PROTO") || key.endsWith("_ADDR"))) return true
            return false
        }
    }

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
        // Try both common label conventions: Helm standard and legacy
        val labelSelectors = listOf(
            "app.kubernetes.io/name=$deployment",
            "app=$deployment"
        )
        for (selector in labelSelectors) {
            val args = listOf(
                "get", "pods",
                "-l", selector,
                "-n", namespace,
                "--context=$context",
                "--field-selector=status.phase=Running",
                "-o", "name",
                "--no-headers"
            )
            val result = kubectl.kubectl(args)
            val pod = result.getOrNull()?.trim()?.lines()?.firstOrNull { it.isNotBlank() }
            if (pod != null) {
                return Result.success(pod.removePrefix("pod/"))
            }
        }
        return Result.failure(RuntimeException("No running pods found for deployment '$deployment' in $namespace"))
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
            .filterKeys { !shouldExclude(it) }
    }

    private fun <T> Result<T>.flatMap(transform: (T) -> Result<T>): Result<T> {
        return fold(
            onSuccess = { transform(it) },
            onFailure = { Result.failure(it) }
        )
    }
}
