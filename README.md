# KUBERNAUTS

A Kubernetes learning game disguised as a space station management simulator.
Manage your station, respond to crises, and learn kubectl — guided by KLINK,
the ship's AI (Kubernetes Learning INtelligence Kernel).

## Helm install

### Docker image publishing (Docker Hub)

Set these in GitHub before pushing so CI can publish images:

- Repository secret: `DOCKERHUB_TOKEN`

The workflow publishes:

- `docker.io/emberflaw/kubernauts-backend:main`
- `docker.io/emberflaw/kubernauts-frontend:main`

### Install from the published Helm repo

```bash
helm repo add kubernauts https://C-Brennan81.github.io/kubernauts
helm repo update
helm install kubernauts kubernauts/kubernauts
```

### Install from the local chart folder

```bash
# Simulation mode (no cluster permissions needed)
helm install kubernauts ./helm/kubernauts

# Cluster mode (game manages real Kubernetes workloads)
helm install kubernauts ./helm/kubernauts \
  --set game.mode=cluster \
  --set ingress.host=kubernauts.example.com

# With TLS
helm install kubernauts ./helm/kubernauts \
  --set ingress.host=kubernauts.example.com \
  --set ingress.tls=true

# Upgrade
helm upgrade kubernauts ./helm/kubernauts

# Uninstall
helm uninstall kubernauts
```

### One command (Docker)
```bash
cd kubernauts
docker compose up --build
```
Open http://localhost — that's it.

### Without Docker
```bash
# Terminal 1 — backend
cd backend && ./mvnw spring-boot:run

# Terminal 2 — frontend
cd frontend && npm install && npm run dev
```
Open http://localhost:5173.

---

## Commands

| Game Command | Kubernetes Equivalent |
|---|---|
| `scan <deployment>` | `kubectl get pods -l app=<deployment>` |
| `inspect <unit>` | `kubectl describe pod <name>` |
| `read logs <unit>` | `kubectl logs <name>` |
| `fix <unit>` | `kubectl delete pod <name>` (triggers restart) |
| `deploy reinforcements <deployment> --count=2` | `kubectl scale deployment <name> --replicas=2` |
| `revert mission <deployment>` | `kubectl rollout undo deployment/<name>` |
| `isolate <module>` | `kubectl cordon <node>` |
| `status` | `kubectl get nodes && kubectl get pods` |
| `hint` | Ask KLINK for help (-10 points) |

## Scenarios

1. **Malfunction Loop** — A unit is in CrashLoopBackOff. Diagnose and fix it.
2. **Ghost Unit** — A unit is stuck in Pending. Inspect to find out why.
3. **Memory Overload** — OOMKilled unit. Scale up the deployment.
4. **Module Failure** — A node went offline. Cordon it.
5. **Bad Deployment** — A bad rollout crashed everything. Roll it back.
