# K8s Pod Env Injector

An IntelliJ plugin that fetches environment variables from Kubernetes Pods and automatically injects them into Run Configurations.

## Prerequisites

| Requirement | Description |
|-------------|-------------|
| **kubectl** | Must be installed locally (`brew install kubectl`) |
| **kubeconfig** | Cluster access must be configured (`gcloud container clusters get-credentials ...`) |
| **RBAC Permissions** | Permissions to list namespaces/deployments and `kubectl exec` into pods |
| **Running Pod** | At least one Running pod in the target Deployment |
| **IntelliJ** | IntelliJ IDEA Ultimate 2025.1 or later |

> If `kubectl exec <pod-name> -- env` works in your terminal, the plugin will work too.

## Installation

1. Download `k8s-pod-env-injector-x.x.x.zip` from [Releases](../../releases)
2. IntelliJ → `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
3. Select the downloaded zip file → Restart IDE

## Usage Guide

### 1. Open Run Configuration

1. Go to `Run` → `Edit Configurations...`
2. Select your Spring Boot Run Configuration (or create a new one)
3. Click the **K8s Pod Env** tab

### 2. Enable the Plugin

1. Check **Enable K8s Pod Env Injection**
2. Click the **Kube Context** dropdown → Select your cluster
3. Click the **Namespace** dropdown → Select the target namespace
4. Click the **Deployment** dropdown → Select the Deployment to fetch env vars from
5. Click `Apply` or `OK`

> Dropdowns are auto-populated from kubectl. You can also type values manually.

### 3. Run Your Application

Just click the Run button as usual.

- Environment variables are fetched from the pod and injected automatically
- A balloon notification shows the number of injected variables
- Click **Show Details** to view the full list of injected variables

### 4. Whitelist (Optional)

Use the whitelist to inject only specific environment variables.

| Whitelist Value | Behavior |
|-----------------|----------|
| Empty (default) | Injects all pod env vars (excluding K8s internal vars) |
| `DB_HOST, DB_PORT, REDIS_URL` | Injects only the specified keys |

Separate multiple keys with commas (`,`).

## Auto-Excluded Variables

Variables that are pod-specific or could conflict with the local environment are automatically excluded:

- `JAVA_TOOL_OPTIONS`, `PATH`, `HOME`, `HOSTNAME`, `TERM`, `SHLVL`, `PWD`, `LANG`, `LC_ALL`
- `KUBERNETES_*` prefix
- `*_SERVICE_HOST`, `*_SERVICE_PORT` (K8s service discovery variables)

## Priority

Manually configured environment variables in Run Configuration always take priority.

For example, if you set `DB_HOST=localhost` in Run Configuration, the `DB_HOST` value from the pod will be ignored.

## Caching

- Fetched environment variables are **cached for 5 minutes**
- Repeated runs with the same configuration won't re-invoke kubectl
- Changes to pod env vars are automatically picked up after the cache expires

## Troubleshooting

| Symptom | Cause | Solution |
|---------|-------|----------|
| `Cannot run program 'kubectl'` | kubectl not installed or not in PATH | `brew install kubectl` and restart IDE |
| `No running pods found` | No Running pods in the Deployment | Check with `kubectl get pods -n <namespace>` |
| `error: unable to upgrade connection` | Insufficient kubectl exec permissions | Request RBAC permissions from cluster admin |
| Dropdowns are empty | kubeconfig not configured | Verify with `kubectl config get-contexts` |
| Env vars not injected | Plugin not enabled or incomplete config | Check settings in the K8s Pod Env tab |

## Build

```bash
./gradlew buildPlugin
```

Output: `build/distributions/k8s-pod-env-injector-x.x.x.zip`
