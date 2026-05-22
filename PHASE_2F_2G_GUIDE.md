# Phase 2 Next Steps: 2f & 2g Implementation Guide

## Context
Phase 2e (mode-aware adapter) is now **COMPLETE**. CommandParser is now mode-agnostic and routes to either:
- **SimulationAdapter** (game.mode=simulation) — H2 in-memory database
- **KubernetesAdapter** (game.mode=cluster) — Real Kubernetes via fabric8

This unblocks 2f and 2g, which require the infrastructure to be in place.

---

## Phase 2f: Create Broken Workload Manifests

### Purpose
When a session starts in cluster mode, the backend needs to deploy intentionally broken Kubernetes workloads (CrashLoop, OOM, Pending) that the player must fix.

### Files to Create

#### 1. `backend/src/main/resources/k8s/crash-loop-deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: crew-quarters
  namespace: kubernauts-{sessionId}  # Will be replaced at runtime
spec:
  replicas: 2
  selector:
    matchLabels:
      app: crew-quarters
  template:
    metadata:
      labels:
        app: crew-quarters
    spec:
      containers:
      - name: crew-container
        image: CRASH_LOOP_IMAGE  # Image that exits with code 1
        resources:
          requests:
            memory: "64Mi"
            cpu: "250m"
          limits:
            memory: "128Mi"
            cpu: "500m"
```

**What makes it crash?** The image `kubernauts/crash-loop:latest` has an entrypoint that:
```bash
#!/bin/sh
exit 1  # Immediate exit, triggers CrashLoopBackOff
```

You can build this with minimal Dockerfile:
```dockerfile
FROM alpine:latest
ENTRYPOINT ["/bin/sh", "-c", "exit 1"]
```

#### 2. `backend/src/main/resources/k8s/oom-deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: life-support
  namespace: kubernauts-{sessionId}
spec:
  replicas: 2
  selector:
    matchLabels:
      app: life-support
  template:
    metadata:
      labels:
        app: life-support
    spec:
      containers:
      - name: life-support-container
        image: MEMORY_HOG_IMAGE
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "128Mi"  # Set lower than requested
            cpu: "500m"
```

**What triggers OOM?** The image runs code that allocates more memory than the limit:
```python
# Simplified Python OOM trigger
import os
data = []
while True:
    data.append(b'x' * 1024 * 1024)  # Allocate 1MB increments
```

#### 3. `backend/src/main/resources/k8s/pending-deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: navigation
  namespace: kubernauts-{sessionId}
spec:
  replicas: 2
  selector:
    matchLabels:
      app: navigation
  template:
    metadata:
      labels:
        app: navigation
    spec:
      nodeSelector:
        zone: "NONEXISTENT"  # No nodes match this selector
      containers:
      - name: navigation-container
        image: NORMAL_IMAGE
        resources:
          requests:
            memory: "64Mi"
            cpu: "100m"
```

**Why pending?** The nodeSelector matches no actual nodes → pods can't be scheduled → stay in Pending state.

### Implementation: Update KubernetesService

The method `deployBrokenWorkloads()` already exists:

```java
public void deployBrokenWorkloads(Long sessionId) {
    String ns = namespaceFor(sessionId);
    applyManifest(ns, "/k8s/crash-loop-deployment.yaml");
    applyManifest(ns, "/k8s/oom-deployment.yaml");
    applyManifest(ns, "/k8s/pending-deployment.yaml");
}

private void applyManifest(String namespace, String classpathResource) {
    try (var stream = getClass().getResourceAsStream(classpathResource)) {
        if (stream == null) {
            log.warn("Manifest not found: {}", classpathResource);
            return;
        }
        k8s.load(stream).inNamespace(namespace).createOrReplace();
    } catch (Exception e) {
        log.error("Failed to apply manifest {}: {}", classpathResource, e.getMessage());
    }
}
```

### When It's Called

In `GameController.startGame()` or in a new initialization method in GameStateService that cluster mode overrides:

```java
// Option 1: In GameController.startGame()
if (isClusterMode) {
    kubernetesService.createNamespace(session.getId());
    kubernetesService.deployBrokenWorkloads(session.getId());
}

// Option 2: Create a dedicated setup method
public void initializeSession(Long sessionId, boolean isClusterMode) {
    if (isClusterMode) {
        kubernetesService.createNamespace(sessionId);
        kubernetesService.deployBrokenWorkloads(sessionId);
    }
}
```

### Verification in Cluster Mode

```bash
# After starting a game session in cluster mode
kubectl get pods -n kubernauts-{sessionId}

# Should show:
# crew-quarters-xxx        0/1   CrashLoopBackOff
# life-support-xxx         0/1   OOMKilled
# navigation-xxx           0/1   Pending
```

---

## Phase 2g: Session Cleanup & TTL

### Purpose
Clean up resources when:
1. Player completes the game
2. Session times out (30 minutes idle)

### Implementation Option: Background Scheduler

#### 1. Create `SessionCleanupScheduler.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "game.mode", havingValue = "cluster")
public class SessionCleanupScheduler {

    private final KubernetesService k8s;
    private final GameSessionRepository sessionRepo;

    @Value("${game.session-ttl-minutes:30}")
    private int sessionTtlMinutes;

    @Scheduled(fixedRateString = "${game.cleanup-interval-minutes:5}", timeUnit = TimeUnit.MINUTES)
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(sessionTtlMinutes);
        List<GameSession> expired = sessionRepo.findByCreatedAtBefore(cutoff);
        
        for (GameSession session : expired) {
            try {
                k8s.deleteNamespace(session.getId());
                sessionRepo.delete(session);
                log.info("Cleaned up session {} (expired)", session.getId());
            } catch (Exception e) {
                log.error("Failed to cleanup session {}", session.getId(), e);
            }
        }
    }
}
```

#### 2. Add createdAt to GameSession entity

```java
@Entity
@Data
@NoArgsConstructor
public class GameSession {
    @Id
    @GeneratedValue
    private Long id;
    
    // ... existing fields ...
    
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    public GameSession(String playerName) {
        this.playerName = playerName;
        this.createdAt = LocalDateTime.now();
        // ... rest of init ...
    }
}
```

#### 3. Add repository method

```java
@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    List<GameSession> findByCreatedAtBefore(LocalDateTime cutoff);
}
```

### Implementation Option 2: Call on Game Complete

Simpler approach — just delete when the game ends:

In `GameController.command()`:

```java
if (result.isScenarioComplete() && nextIdx >= scenarioEngine.totalScenarios()) {
    // ... existing game complete logic ...
    
    // Cleanup namespace
    if (isClusterMode()) {
        kubernetesService.deleteNamespace(sessionId);
        klink.message("Session namespace purged. I've already forgotten you existed.");
    }
}
```

Or create a dedicated method:

```java
public void onGameComplete(Long sessionId) {
    GameSession session = gameState.getSession(sessionId);
    if (isClusterMode()) {
        kubernetesService.deleteNamespace(sessionId);
    }
    // sessionRepo.delete(session);  // Optional: delete from DB too
}
```

### Update application.properties

```properties
# Cluster mode specifics
game.mode=cluster
game.session-namespace=kubernauts-sessions
game.session-ttl-minutes=30
game.cleanup-interval-minutes=5
```

### KLINK Integration

Add message on cleanup:

```java
public String onSessionCleanup(Long sessionId) {
    return "Session " + sessionId + " purged. I've already forgotten you existed.";
}
```

---

## Dependency: Repository Methods

Both options require a way to access sessions. Ensure GameSessionRepository has:

```java
@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    List<GameSession> findByCreatedAtBefore(LocalDateTime cutoff);
    Optional<GameSession> findById(Long id);
}
```

---

## Testing Strategy

### 2f: Verify Manifest Deployment

```java
@Test
@EnabledIfEnvironmentVariable(named = "K8S_ENABLED", matches = ".*")
void testBrokenWorkloadsDeployed() throws Exception {
    k8sService.createNamespace(sessionId);
    k8sService.deployBrokenWorkloads(sessionId);
    
    Thread.sleep(2000);  // Wait for deployment
    
    List<Pod> pods = k8s.pods().inNamespace(ns).list().getItems();
    assertThat(pods).hasSize(6);  // 2 per deployment
    assertThat(pods)
        .filteredOn("app", "crew-quarters")
        .allMatch(p -> isCrashLoopStatus(p));
}
```

### 2g: Verify Cleanup

```java
@Test
void testSessionCleanupSchedule() throws Exception {
    // Create old session
    GameSession old = new GameSession("TestPlayer");
    old.setCreatedAt(LocalDateTime.now().minusHours(1));
    sessionRepo.save(old);
    
    // Run cleanup
    cleanupScheduler.cleanupExpiredSessions();
    
    // Verify deleted
    assertThat(sessionRepo.findById(old.getId())).isEmpty();
}
```

---

## Quick Checklist

- [ ] Create `src/main/resources/k8s/` directory
- [ ] Add three manifest YAML files (crash-loop, oom, pending)
- [ ] Verify `KubernetesService.deployBrokenWorkloads()` implementation
- [ ] Call deployment on session start (if cluster mode)
- [ ] Add `createdAt` field to GameSession
- [ ] Add repository method: `findByCreatedAtBefore()`
- [ ] Create SessionCleanupScheduler OR add cleanup to `onGameComplete()`
- [ ] Update `application.properties` with TTL config
- [ ] Test manifest deployment in cluster
- [ ] Test cleanup scheduling

---

**Ready to implement?** Pick 2f OR 2g and it should just work now that CommandParser is mode-aware.

