# Kubernauts — Design & Implementation Plan

## Overview

Kubernauts is a Kubernetes learning game disguised as a space station management simulator.
The player manages a failing station by issuing commands that map 1:1 to real `kubectl` syntax.
The ship AI, KLINK, narrates and taunts throughout.

This document covers the planned evolution from the current simulation-based prototype to a
production-ready, Helm-installable application capable of running against a real Kubernetes cluster.

---

## Current State (Baseline)

- 7 scenarios across 3 levels (~10–15 min playtime)
- Simulated pods/nodes in H2 in-memory database
- Spring Boot backend + React frontend
- Docker Compose for local development
- KLINK dialogue, escalation taunts, kubectl alias support

---

## Requirements

| # | Requirement |
|---|---|
| R1 | Challenging enough to occupy a player for ~1 hour |
| R2 | Goals must be achievable — clear progression, no dead ends |
| R3 | Difficulty ramps gradually from trivial to complex |
| R4 | Optionally run against a real Kubernetes cluster |
| R5 | Installable on any cluster via Helm |

---

## Phase 1 — Content & Difficulty

**Goal:** Expand gameplay to ~1 hour. No architecture changes.

### 1a. Expand to 20 scenarios across 5 levels

| Level | Name | Scenarios | Concepts Taught |
|---|---|---|---|
| 1 | Basic Recon | 3 | `get pods`, `describe pod`, `logs` |
| 2 | First Response | 4 | `delete pod`, `scale`, `get events` |
| 3 | Deployment Ops | 4 | `rollout undo`, `rollout status`, `cordon` |
| 4 | Advanced Triage | 5 | `exec`, `label`, `annotate`, compound failures |
| 5 | Incident Command | 4 | Full workflow, time pressure, incident mode |

Each level introduces exactly one new concept before requiring it in a win condition.
No scenario requires a command the player has not been taught.

### 1b. Discovery phase enforcement

A scenario cannot be won until the player has performed the required diagnostic step.
Example: scenario "Malfunction Loop" requires `scan crew-quarters` before `fix crew-alpha-1` is accepted.

This prevents players from guessing the answer without learning the diagnostic workflow.

### 1c. Score multiplier decay

- Each scenario starts with a 3x score multiplier
- Multiplier drops to 2x at 60s, 1x at 120s
- Final score = base reward × multiplier at time of completion
- KLINK comments on the multiplier dropping ("The timer is running. I'm not worried. You should be.")

### 1d. Incident mode (post-campaign)

After completing all 5 levels, incident mode activates:
- Random failures fire every 30–60 seconds
- No win condition — survival score accumulates
- KLINK becomes progressively more unhinged as failures stack
- Ends when alert level stays RED for 60 seconds

---

## Phase 2 — Real Cluster Support

**Goal:** In cluster mode, the game manages real Kubernetes workloads in a sandboxed namespace.
The player's commands are proxied through the backend using the Kubernetes Java client (fabric8).

### Architecture change

```
simulation mode (default)
  CommandParser → GameStateService → H2

cluster mode
  CommandParser → KubernetesService → real k8s API → sandboxed namespace
```

Toggle via `application.properties`:
```properties
game.mode=simulation   # or: cluster
game.session-namespace=kubernauts-sessions
```

### 2a. Add fabric8 Kubernetes client

```xml
<dependency>
  <groupId>io.fabric8</groupId>
  <artifactId>kubernetes-client</artifactId>
  <version>6.13.0</version>
</dependency>
```

### 2b. KubernetesService

Responsibilities:
- Create a namespace per session: `kubernauts-{sessionId}`
- Deploy intentionally broken workloads (CrashLoopBackOff, OOMKilled, Pending) using pre-defined manifests
- Proxy game commands to real kubectl operations via fabric8
- Return real pod/node state to `buildState()`
- Delete namespace on session end or timeout (30 min TTL)

### 2c. Cluster mode command mapping

| Game command | fabric8 operation |
|---|---|
| `scan <deployment>` | `client.pods().inNamespace(ns).withLabel("app", dep).list()` |
| `inspect <unit>` | `client.pods().inNamespace(ns).withName(name).get()` |
| `read logs <unit>` | `client.pods().inNamespace(ns).withName(name).getLog()` |
| `fix <unit>` | `client.pods().inNamespace(ns).withName(name).delete()` |
| `deploy reinforcements <dep> --count=N` | `client.apps().deployments().inNamespace(ns).withName(dep).scale(N)` |
| `revert mission <dep>` | `client.apps().deployments().inNamespace(ns).withName(dep).rolling().undo()` |
| `isolate <module>` | `client.nodes().withName(node).cordon()` |
| `status` | `client.pods().inNamespace(ns).list()` + `client.nodes().list()` |

### 2d. RBAC requirements

The game's ServiceAccount needs:
- `create`, `delete` on `namespaces`
- `*` on `pods`, `deployments`, `replicasets` within session namespaces
- `get`, `patch` on `nodes` (for cordon)
- `get` on `events`

Namespace resource quotas are applied to each session namespace to prevent runaway resource usage.

### 2e. Session cleanup

- Namespace deleted on `onGameComplete`
- Background job deletes namespaces older than 30 minutes (handles abandoned sessions)
- Cleanup logged and reported to KLINK ("Session namespace purged. I've already forgotten you existed.")

---

## Phase 3 — Helm Chart

**Goal:** Single `helm install` deploys the full application to any cluster.

### Chart structure

```
helm/kubernauts/
├── Chart.yaml
├── values.yaml
└── templates/
    ├── deployment-backend.yaml
    ├── deployment-frontend.yaml
    ├── service-backend.yaml
    ├── service-frontend.yaml
    ├── ingress.yaml
    ├── configmap.yaml          — application.properties
    ├── serviceaccount.yaml
    ├── clusterrole.yaml        — namespace + workload management
    ├── clusterrolebinding.yaml
    └── hpa.yaml                — optional, off by default
```

### values.yaml (key fields)

```yaml
image:
  backend:  ghcr.io/kubernauts/backend:latest
  frontend: ghcr.io/kubernauts/frontend:latest

ingress:
  enabled: true
  host: kubernauts.example.com
  tls: false

game:
  mode: simulation        # simulation | cluster
  sessionNamespace: kubernauts-sessions
  sessionTtlMinutes: 30

resources:
  backend:
    requests: { cpu: 250m, memory: 512Mi }
    limits:   { cpu: 500m, memory: 1Gi }
```

### Install

```bash
# Simulation mode (no cluster permissions needed)
helm install kubernauts ./helm/kubernauts

# Cluster mode (game manages real workloads)
helm install kubernauts ./helm/kubernauts \
  --set game.mode=cluster \
  --set ingress.host=kubernauts.example.com
```

---

## Phase 4 — Polish

**Goal:** Quality of life improvements after core functionality is stable.

### 4a. New command support

| Command | kubectl equivalent | Introduced |
|---|---|---|
| `exec <unit> <cmd>` | `kubectl exec <pod> -- <cmd>` | Level 4 |
| `label unit <unit> <key>=<val>` | `kubectl label pod <pod> <key>=<val>` | Level 4 |
| `events <unit>` | `kubectl get events --field-selector involvedObject.name=<pod>` | Level 2 |
| `rollout status <dep>` | `kubectl rollout status deployment/<dep>` | Level 3 |

### 4b. Leaderboard

- Persist top scores per scenario and overall in a `leaderboard` table
- `GET /api/leaderboard` returns top 10
- Displayed on game complete screen
- KLINK comments on the player's rank ("You're 7th. I've updated the document.")

### 4c. First-time tutorial overlay

- Shown on first visit (localStorage flag)
- Three slides: what the game is, how commands work, what KLINK is
- Skippable

---

## Implementation Order

```
Phase 1a → 1b → 1c → 1d   (content, no dependencies)
Phase 2a → 2b → 2c → 2d → 2e   (requires Phase 1 complete)
Phase 3   (can be done in parallel with Phase 2)
Phase 4   (after Phase 2 stable)
```

---

## Out of Scope

- Multiplayer
- User accounts / authentication
- Persistent player profiles across devices
- Support for non-standard Kubernetes distributions (OpenShift, etc.) — may work, not tested
