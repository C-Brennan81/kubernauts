package com.kubernauts.service;

import com.kubernauts.model.PodStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages real Kubernetes workloads per game session.
 * Active only when game.mode=cluster.
 * Each session gets an isolated namespace: kubernauts-{sessionId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "game.mode", havingValue = "cluster")
public class KubernetesService {

    private final KubernetesClient k8s;

    @Value("${game.session-namespace:kubernauts-sessions}")
    private String baseNamespace;

    // ── Namespace lifecycle ───────────────────────────────────────────────────

    public String namespaceFor(Long sessionId) {
        return baseNamespace + "-" + sessionId;
    }

    public void createNamespace(Long sessionId) {
        String ns = namespaceFor(sessionId);
        Namespace namespace = new NamespaceBuilder()
            .withNewMetadata()
                .withName(ns)
                .withLabels(Map.of("app", "kubernauts", "session", sessionId.toString()))
            .endMetadata()
            .build();
        k8s.namespaces().resource(namespace).createOrReplace();

        // Apply resource quota to prevent runaway usage
        ResourceQuota quota = new ResourceQuotaBuilder()
            .withNewMetadata().withName("session-quota").withNamespace(ns).endMetadata()
            .withNewSpec()
                .addToHard("pods", new Quantity("20"))
                .addToHard("cpu", new Quantity("2"))
                .addToHard("memory", new Quantity("2Gi"))
            .endSpec()
            .build();
        k8s.resourceQuotas().inNamespace(ns).resource(quota).createOrReplace();
        log.info("Created namespace {} for session {}", ns, sessionId);
    }

    public void deleteNamespace(Long sessionId) {
        String ns = namespaceFor(sessionId);
        k8s.namespaces().withName(ns).delete();
        log.info("Deleted namespace {} for session {}", ns, sessionId);
    }

    // ── Pod operations (proxied kubectl commands) ─────────────────────────────

    public List<Map<String, Object>> getPods(Long sessionId, String deploymentLabel) {
        String ns = namespaceFor(sessionId);
        return k8s.pods().inNamespace(ns)
            .withLabel("app", deploymentLabel)
            .list().getItems().stream()
            .map(this::podToMap)
            .collect(Collectors.toList());
    }

    public Map<String, Object> describePod(Long sessionId, String podName) {
        String ns = namespaceFor(sessionId);
        Pod pod = k8s.pods().inNamespace(ns).withName(podName).get();
        if (pod == null) return Map.of("error", "Pod not found: " + podName);
        return podToMap(pod);
    }

    public String getLogs(Long sessionId, String podName) {
        String ns = namespaceFor(sessionId);
        try {
            return k8s.pods().inNamespace(ns).withName(podName).getLog();
        } catch (Exception e) {
            return "[ERROR] Could not retrieve logs: " + e.getMessage();
        }
    }

    public void deletePod(Long sessionId, String podName) {
        String ns = namespaceFor(sessionId);
        k8s.pods().inNamespace(ns).withName(podName).delete();
    }

    public void scaleDeployment(Long sessionId, String deploymentName, int replicas) {
        String ns = namespaceFor(sessionId);
        k8s.apps().deployments().inNamespace(ns).withName(deploymentName)
            .scale(replicas);
    }

    public void rolloutUndo(Long sessionId, String deploymentName) {
        String ns = namespaceFor(sessionId);
        // fabric8: trigger rollback by patching the deployment's rollback annotation
        k8s.apps().deployments().inNamespace(ns).withName(deploymentName)
            .rolling().undo();
    }

    public void cordonNode(String nodeName) {
        k8s.nodes().withName(nodeName).edit(n -> new NodeBuilder(n)
            .editSpec().withUnschedulable(true).endSpec().build());
    }

    public List<Map<String, Object>> getAllPods(Long sessionId) {
        String ns = namespaceFor(sessionId);
        return k8s.pods().inNamespace(ns).list().getItems().stream()
            .map(this::podToMap)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getNodes() {
        return k8s.nodes().list().getItems().stream()
            .map(n -> Map.<String, Object>of(
                "name", n.getMetadata().getName(),
                "ready", isNodeReady(n),
                "unschedulable", Boolean.TRUE.equals(n.getSpec().getUnschedulable())
            ))
            .collect(Collectors.toList());
    }

    // ── Broken workload deployment ────────────────────────────────────────────

    public void deployBrokenWorkloads(Long sessionId) {
        String ns = namespaceFor(sessionId);
        // Load manifests from classpath and apply
        applyManifest(ns, "/k8s/crash-loop-deployment.yaml");
        applyManifest(ns, "/k8s/oom-deployment.yaml");
        applyManifest(ns, "/k8s/pending-deployment.yaml");
        applyManifest(ns, "/k8s/navigation-deployment.yaml");
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> podToMap(Pod pod) {
        String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";
        PodStatus gameStatus = mapPhase(pod);
        int restarts = pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null
            ? pod.getStatus().getContainerStatuses().stream()
                .mapToInt(ContainerStatus::getRestartCount).sum()
            : 0;
        return Map.of(
            "name", pod.getMetadata().getName(),
            "status", gameStatus.name(),
            "phase", phase,
            "restarts", restarts,
            "node", pod.getSpec().getNodeName() != null ? pod.getSpec().getNodeName() : "unassigned",
            "deployment", pod.getMetadata().getLabels().getOrDefault("app", "unknown")
        );
    }

    private PodStatus mapPhase(Pod pod) {
        if (pod.getStatus() == null) return PodStatus.UNKNOWN;
        String phase = pod.getStatus().getPhase();
        if ("Pending".equals(phase)) return PodStatus.PENDING;
        if ("Running".equals(phase)) {
            // Check for CrashLoopBackOff in container statuses
            if (pod.getStatus().getContainerStatuses() != null) {
                boolean crashLoop = pod.getStatus().getContainerStatuses().stream()
                    .anyMatch(cs -> cs.getState() != null
                        && cs.getState().getWaiting() != null
                        && "CrashLoopBackOff".equals(cs.getState().getWaiting().getReason()));
                if (crashLoop) return PodStatus.CRASH_LOOP;
                boolean oom = pod.getStatus().getContainerStatuses().stream()
                    .anyMatch(cs -> cs.getLastState() != null
                        && cs.getLastState().getTerminated() != null
                        && "OOMKilled".equals(cs.getLastState().getTerminated().getReason()));
                if (oom) return PodStatus.OOM_KILLED;
            }
            return PodStatus.RUNNING;
        }
        if ("Succeeded".equals(phase) || "Failed".equals(phase)) return PodStatus.TERMINATED;
        return PodStatus.UNKNOWN;
    }

    private boolean isNodeReady(io.fabric8.kubernetes.api.model.Node node) {
        if (node.getStatus() == null || node.getStatus().getConditions() == null) return false;
        return node.getStatus().getConditions().stream()
            .anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
    }
}
