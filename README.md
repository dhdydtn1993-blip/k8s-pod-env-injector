# K8s Pod Env Injector

Kubernetes Pod의 환경변수를 IntelliJ Run Configuration에 자동으로 주입하는 플러그인입니다.

## 사전 요구사항

| 항목 | 설명 |
|------|------|
| **kubectl** | 로컬에 설치되어 있어야 합니다 (`brew install kubectl`) |
| **kubeconfig** | 클러스터 접근이 설정되어 있어야 합니다 (`gcloud container clusters get-credentials ...`) |
| **RBAC 권한** | namespace/deployment 조회 + `kubectl exec` 권한이 필요합니다 |
| **Running Pod** | 대상 Deployment에 Running 상태의 Pod가 1개 이상 있어야 합니다 |
| **IntelliJ** | IntelliJ IDEA Ultimate 2025.1 이상 |

> 터미널에서 `kubectl exec <pod-name> -- env` 가 정상 동작하면 플러그인도 동작합니다.

## 설치

1. [Releases](../../releases)에서 `k8s-pod-env-injector-x.x.x.zip` 다운로드
2. IntelliJ → `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...`
3. 다운로드한 zip 파일 선택 → IDE 재시작

## 사용 가이드

### 1. Run Configuration 설정

1. 상단 메뉴 `Run` → `Edit Configurations...` 클릭
2. Spring Boot Run Configuration 선택 (또는 새로 생성)
3. **K8s Pod Env** 탭 클릭

### 2. 플러그인 활성화

1. `Enable K8s Pod Env Injection` 체크박스 활성화
2. **Kube Context** 드롭다운 클릭 → 사용할 클러스터 선택
3. **Namespace** 드롭다운 클릭 → 대상 네임스페이스 선택
4. **Deployment** 드롭다운 클릭 → 환경변수를 가져올 Deployment 선택
5. `Apply` 또는 `OK` 클릭

> 드롭다운을 클릭하면 kubectl에서 자동으로 목록을 가져옵니다. 직접 타이핑도 가능합니다.

### 3. 앱 실행

평소처럼 Run 버튼을 클릭하면 됩니다.

- Pod에서 환경변수를 가져와서 자동으로 주입됩니다
- 주입 완료 시 풍선 알림이 표시됩니다
- **Show Details** 버튼을 클릭하면 주입된 환경변수 목록을 확인할 수 있습니다

### 4. Whitelist 설정 (선택사항)

특정 환경변수만 주입하고 싶을 때 사용합니다.

| Whitelist 값 | 동작 |
|---|---|
| 비어있음 (기본값) | Pod의 모든 환경변수 주입 (K8s 내부 변수 제외) |
| `DB_HOST, DB_PORT, REDIS_URL` | 지정한 키만 주입 |

콤마(`,`)로 구분하여 여러 키를 입력할 수 있습니다.

## 자동 제외되는 환경변수

Pod 전용이거나 로컬 환경에서 충돌을 일으킬 수 있는 변수는 자동으로 제외됩니다:

- `JAVA_TOOL_OPTIONS`, `PATH`, `HOME`, `HOSTNAME`, `TERM`, `SHLVL`, `PWD`, `LANG`, `LC_ALL`
- `KUBERNETES_*` 접두사
- `*_SERVICE_HOST`, `*_SERVICE_PORT` (K8s 서비스 디스커버리 변수)

## 우선순위

Run Configuration에서 직접 설정한 환경변수가 항상 우선합니다.

예를 들어 Run Configuration에 `DB_HOST=localhost`를 설정하면, Pod에서 가져온 `DB_HOST` 값은 무시됩니다.

## 캐시

- 가져온 환경변수는 **5분간 캐시**됩니다
- 같은 설정으로 반복 실행 시 kubectl을 다시 호출하지 않습니다
- Pod 환경변수가 변경되면 5분 후 자동 갱신됩니다

## 문제 해결

| 증상 | 원인 | 해결 |
|------|------|------|
| `Cannot run program 'kubectl'` | kubectl이 설치되지 않았거나 PATH에 없음 | `brew install kubectl` 후 IDE 재시작 |
| `No running pods found` | Deployment에 Running Pod가 없음 | `kubectl get pods -n <ns>` 로 확인 |
| `error: unable to upgrade connection` | kubectl exec 권한 없음 | 클러스터 관리자에게 RBAC 권한 요청 |
| 드롭다운이 비어있음 | kubeconfig 미설정 | `kubectl config get-contexts` 로 확인 |
| 환경변수가 주입되지 않음 | 체크박스 미활성화 또는 설정 미완료 | K8s Pod Env 탭에서 설정 확인 |

## 빌드

```bash
./gradlew buildPlugin
```

빌드 결과물: `build/distributions/k8s-pod-env-injector-x.x.x.zip`
