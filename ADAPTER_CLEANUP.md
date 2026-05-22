# Phase 2e: Which Adapter Files to Use

## ✅ Official Implementation (Use These)

### 1. **CommandAdapter.java** — The interface
- Location: `src/main/java/com/kubernauts/service/CommandAdapter.java`
- Status: **KEEP** ✅
- Definition of all 20 methods that both adapters must implement

### 2. **SimulationAdapter.java** — H2 mode (default)
- Location: `src/main/java/com/kubernauts/service/SimulationAdapter.java`
- Status: **KEEP** ✅
- Annotation: `@ConditionalOnProperty(name = "game.mode", havingValue = "simulation", matchIfMissing = true)`
- Activated when: `game.mode=simulation` or property not set
- Implementation: Delegates to GameStateService

### 3. **ClusterAdapter.java** — Kubernetes mode
- Location: `src/main/java/com/kubernauts/service/ClusterAdapter.java`
- Status: **KEEP** ✅ (just updated to be complete)
- Annotation: `@ConditionalOnProperty(name = "game.mode", havingValue = "cluster")`
- Activated when: `game.mode=cluster`
- Implementation: Delegates to KubernetesService + GameStateService

### 4. **CommandParser.java** — The command executor
- Location: `src/main/java/com/kubernauts/service/CommandParser.java`
- Status: **KEEP** ✅ (refactored)
- Uses: `CommandAdapter adapter` (injected by Spring)
- No longer uses: `GameStateService` (now via adapter)

---

## ⚠️ Duplicate File (Delete This)

### KubernetesAdapter.java — OLD/DUPLICATE
- Location: `src/main/java/com/kubernauts/service/KubernetesAdapter.java`
- Status: **DELETE** ❌
- Why: ClusterAdapter and KubernetesAdapter are identical now
- Having both causes Spring bean conflict (both match `@ConditionalOnProperty` for cluster mode)

**Action**: Delete `KubernetesAdapter.java` to avoid duplicate bean definition error.

---

## Runtime Behavior

### When game.mode=simulation (Default)
```
Spring Container:
  ✓ Loads SimulationAdapter bean
  ✗ Does NOT load ClusterAdapter

CommandParser:
  Injects: SimulationAdapter
  Routes commands to: GameStateService
  Storage: H2 in-memory database
```

### When game.mode=cluster
```
Spring Container:
  ✗ Does NOT load SimulationAdapter
  ✓ Loads ClusterAdapter bean

CommandParser:
  Injects: ClusterAdapter
  Routes commands to: KubernetesService (for workloads) + GameStateService (for scoring)
  Storage: Real Kubernetes cluster + H2 databases for progression
```

---

## Cleanup Action

To avoid Spring startup errors, delete the duplicate:

```bash
rm backend/src/main/java/com/kubernauts/service/KubernetesAdapter.java
```

Or if you can't delete, at least Spring will log which one it picked and it's safe (just leaves dead code).

---

## Verification

Both adapters should pass these checks:

1. **Implements CommandAdapter**: ✅
2. **Has @Service annotation**: ✅
3. **Has @ConditionalOnProperty**: ✅
4. **Implements all 20 methods**: ✅
5. **No duplicate condition conflicts**: ✓ (after deleting KubernetesAdapter)

---

## Files Summary Table

| File | Type | Status | Action |
|------|------|--------|--------|
| CommandAdapter.java | Interface | KEEP | Keep as is |
| CommandParser.java | Service | KEEP | Keep refactored version |
| SimulationAdapter.java | Implementation | KEEP | Keep for game.mode=simulation |
| ClusterAdapter.java | Implementation | KEEP | Keep (now complete) |
| KubernetesAdapter.java | Implementation | DUPLICATE | Delete ❌ |

---

**Total refactored files**: 4
**Total new docs**: 3 (PHASE_2E_*, PHASE_2F_2G_GUIDE, this file)
**Ready for**: Phase 2f (broken manifests) or 2g (session cleanup)

