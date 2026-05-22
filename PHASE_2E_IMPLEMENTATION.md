# Phase 2e Implementation: Mode-Aware Adapter Refactoring

## Summary

Successfully implemented the adapter pattern to enable **CommandParser to work identically in both simulation and cluster modes**. The refactoring unblocks Phase 2 implementation by:

1. **Decoupling CommandParser from GameStateService** — it now uses the CommandAdapter interface
2. **Creating two implementations:**
   - **SimulationAdapter**: wraps GameStateService for H2 in-memory mode
   - **KubernetesAdapter**: wraps KubernetesService for real K8s operations
3. **Enabling conditional injection** based on `game.mode` property

## Files Changed

### 1. CommandAdapter Interface (EXPANDED)
**File**: `src/main/java/com/kubernauts/service/CommandAdapter.java`

Previously only had pod/node operations. Now includes:
- State building: `buildState(Long)`, `buildState(Long, int)`
- Pod operations: getPods, describePod, getLogs, deletePod, setPodStatus, spawnPods, getAllPods
- Node operations: getNodes, cordonNode
- Deployment operations: scaleDeployment, rolloutUndo
- Game progression: addScore, advanceScenario
- Command unlocks: unlockCommand, isCommandUnlocked, getUnlockedCommands

### 2. SimulationAdapter (COMPLETED)
**File**: `src/main/java/com/kubernauts/service/SimulationAdapter.java`

Implements CommandAdapter by delegating to GameStateService:
- Pod operations return maps from simulation state
- Log generation uses in-memory pod status
- Score and progression use H2 database
- Cordon is simulated by marking pods as UNKNOWN
- Activated when `game.mode=simulation` or property missing (default)

### 3. KubernetesAdapter (NEW)
**File**: `src/main/java/com/kubernauts/service/KubernetesAdapter.java`

Implements CommandAdapter by delegating to KubernetesService:
- Pod operations delegate to fabric8 K8s client
- Real logs retrieved from container stdout
- Scaling/rollout operations use K8s API
- Game state (score, progression) managed separately via GameStateService
- Activated when `game.mode=cluster`

### 4. CommandParser (REFACTORED)
**File**: `src/main/java/com/kubernauts/service/CommandParser.java`

**Before**: 
```java
private final GameStateService gameState;  // Only knows about H2
```

**After**:
```java
private final CommandAdapter adapter;  // Works with both mode implementations
```

All operations replaced:
- `gameState.buildState()` → `adapter.buildState()`
- `gameState.addScore()` → `adapter.addScore()`
- `gameState.setPodStatus()` → `adapter.setPodStatus()`
- `gameState.getPods()` → `adapter.getPods()`
- etc.

The command handlers (handleScan, handleStatus, handleFix, etc.) remain unchanged in logic—only their dependencies change.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  CommandParser                          │
│  (normalise, execute, handleScan, ..., etc)            │
│                                                         │
│              Injects: CommandAdapter                    │
└────────────────────┬──────────────────┬─────────────────┘
                     │                  │
        game.mode=simulation    game.mode=cluster
                     │                  │
      ┌──────────────▼──────┐  ┌────────▼────────────────┐
      │ SimulationAdapter   │  │  KubernetesAdapter      │
      │                     │  │                         │
      │ ┌─────────────────┐ │  │ ┌──────────────────┐   │
      │ │ GameStateService│ │  │ │ KubernetesService│   │
      │ │ (H2 database)   │ │  │ │ (fabric8 client) │   │
      │ └─────────────────┘ │  │ └──────────────────┘   │
      │                     │  │ ┌──────────────────┐   │
      │                     │  │ │ GameStateService │   │
      │                     │  │ │ (progression/score)  │
      │                     │  │ └──────────────────┘   │
      └─────────────────────┘  └─────────────────────────┘
             │                          │
             └──────────┬───────────────┘
                        │
            StationState / Pod / Node data
```

## How It Enables Phase 2

### 2a. ✅ Fabric8 client (already in pom.xml)
- KubernetesService uses it to interact with real K8s API

### 2b. ✅ KubernetesService
- Manages namespaces, applies broken workload manifests
- Proxies kubectl operations via fabric8
- Included in KubernetesAdapter calls

### 2c. ✅ Mode-aware command routing
- CommandParser doesn't know which implementation it's using
- SpringBoot injects the right adapter based on `game.mode`
- Commands automatically route to simulation or cluster logic

### 2d. RBAC requirements (Not Implemented Yet)
- Helm chart will define ClusterRole with needed permissions
- ServiceAccount binding in templates

### 2e. ✅ **THIS TASK** — Mode-aware adapter
- CommandParser now mode-agnostic
- SimulationAdapter for H2
- KubernetesAdapter for cluster mode

### 2f. Next: Create broken workload manifests
- YAML files in `src/main/resources/k8s/`
- `crash-loop-deployment.yaml`
- `oom-deployment.yaml`
- `pending-deployment.yaml`
- KubernetesService.deployBrokenWorkloads() loads them

### 2g. Next: Session cleanup & TTL
- On game complete: call `adapter.deleteNamespace()` equivalent
- Background job: periodic cleanup of old namespaces
- EscalationScheduler can be extended to handle this

## Configuration

Toggle modes via `application.properties`:

```properties
# Simulation mode (default)
game.mode=simulation
game.session-namespace=kubernauts-sessions

# OR Cluster mode
game.mode=cluster
game.session-namespace=kubernauts-sessions
```

## Testing

To verify the refactoring works:

1. **Simulation mode** (default):
   ```bash
   mvn test -Dtest=CommandParserNormaliseTest
   ```
   - Existing tests should pass unchanged
   - SimulationAdapter injected automatically

2. **Cluster mode** (requires K8s):
   ```bash
   mvn test -Dtest=SomeClusterTest -Dgame.mode=cluster
   ```
   - KubernetesAdapter injected
   - Tests would need K8s environment

## What's Unblocked

- ✅ CommandParser can now work with both H2 and real Kubernetes
- ✅ Dependency injection automatically selects the right implementation
- ✅ All game logic remains unchanged, only storage/execution layer abstracted
- ✅ Ready to implement broken manifests (2f) and cleanup (2g) without CommandParser changes

## Next Steps (2f & 2g)

### 2f: Create broken workload manifests
1. Create `backend/src/main/resources/k8s/` directory
2. Add manifest files:
   - `crash-loop-deployment.yaml` — container exits with code 1
   - `oom-deployment.yaml` — memory limit exceeded
   - `pending-deployment.yaml` — no schedulable nodes
   - (optional) `navigation-deployment.yaml` — healthy reference

3. Update `KubernetesService.deployBrokenWorkloads()` to load these on session start

### 2g: Session cleanup & TTL
1. Add SessionCleanupScheduler (scheduled background job)
   - Runs every 5 minutes
   - Deletes namespaces older than 30 minutes
   - Logs completion message via KLINK

2. Update GameController to call cleanup on game complete
   - If cluster mode: delete namespace immediately
   - If simulation: no cleanup needed

3. Update application.properties:
   ```properties
   game.session-ttl-minutes=30
   game.cleanup-interval-minutes=5
   ```

---

**Implementation Date**: May 22, 2026
**Status**: COMPLETE (2e) — Unblocks 2f & 2g

