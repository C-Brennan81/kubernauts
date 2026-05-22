package com.kubernauts.service;

import com.kubernauts.model.PodStatus;
import com.kubernauts.model.StationState;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "game.mode", havingValue = "simulation", matchIfMissing = true)
public class SimulationAdapter implements CommandAdapter {

    private final GameStateService gameState;

    @Override
    public List<Map<String, Object>> getPods(Long sessionId, String deployment) {
        return gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getDeploymentName().equals(deployment))
            .map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("name", p.getName());
                m.put("status", p.getStatus().name());
                m.put("restarts", p.getRestartCount());
                m.put("node", p.getNodeName());
                m.put("deployment", p.getDeploymentName());
                return m;
            }).toList();
    }

    @Override
    public Map<String, Object> describePod(Long sessionId, String podName) {
        return gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getName().equals(podName))
            .findFirst()
            .map(p -> Map.<String, Object>of(
                "name", p.getName(),
                "status", p.getStatus().name(),
                "node", p.getNodeName(),
                "restarts", p.getRestartCount(),
                "sector", p.getSector()
            ))
            .orElse(Map.of("error", "Pod not found: " + podName));
    }

    @Override
    public String getLogs(Long sessionId, String podName) {
        return gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getName().equals(podName)).findFirst()
            .map(pod -> switch (pod.getStatus()) {
                case CRASH_LOOP -> "[ERROR] Process exited with code 1\n[ERROR] Restarting... (attempt " + pod.getRestartCount() + ")\n[ERROR] Back-off 30s restarting failed container";
                case OOM_KILLED -> "[ERROR] Out of memory: Kill process\n[ERROR] Memory limit exceeded\n[WARN]  Container terminated by OOM killer";
                case PENDING    -> "[INFO]  Waiting for module assignment\n[WARN]  No suitable module found\n[INFO]  Retrying scheduling...";
                default         -> "[INFO]  Unit operational\n[INFO]  All systems nominal";
            })
            .orElse("No logs found for '" + podName + "'.");
    }

    @Override
    public void deletePod(Long sessionId, String podName) {
        gameState.setPodStatus(sessionId, podName, PodStatus.RUNNING);
    }

    @Override
    public void scaleDeployment(Long sessionId, String deployment, int replicas) {
        // In simulation, mark OOM pods as running up to replicas count
        gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getDeploymentName().equals(deployment) && p.getStatus() == PodStatus.OOM_KILLED)
            .limit(replicas)
            .forEach(p -> gameState.setPodStatus(sessionId, p.getName(), PodStatus.RUNNING));
    }

    @Override
    public void rolloutUndo(Long sessionId, String deployment) {
        gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getDeploymentName().equals(deployment))
            .forEach(p -> gameState.setPodStatus(sessionId, p.getName(), PodStatus.RUNNING));
    }

    @Override
    public void cordonNode(Long sessionId, String nodeName) {
        // Simulation: mark pods on that node as UNKNOWN
        gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getNodeName().equals(nodeName) && p.getStatus() != PodStatus.RUNNING)
            .forEach(p -> gameState.setPodStatus(sessionId, p.getName(), PodStatus.UNKNOWN));
    }

    @Override
    public List<Map<String, Object>> getAllPods(Long sessionId) {
        return gameState.buildState(sessionId).getPods().stream()
            .map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("name", p.getName());
                m.put("status", p.getStatus().name());
                m.put("restarts", p.getRestartCount());
                m.put("node", p.getNodeName());
                m.put("deployment", p.getDeploymentName());
                return m;
            }).toList();
    }

    @Override
    public List<Map<String, Object>> getNodes(Long sessionId) {
        return gameState.buildState(sessionId).getNodes().stream()
            .map(n -> Map.<String, Object>of(
                "name", n.getName(),
                "ready", n.isReady(),
                "unschedulable", false
            )).toList();
    }

    @Override
    public StationState buildState(Long sessionId) {
        return gameState.buildState(sessionId);
    }

    @Override
    public StationState buildState(Long sessionId, int scoreDelta) {
        return gameState.buildState(sessionId, scoreDelta);
    }

    @Override
    public void setPodStatus(Long sessionId, String podName, PodStatus status) {
        gameState.setPodStatus(sessionId, podName, status);
    }

    @Override
    public void spawnPods(Long sessionId, String deployment, int count) {
        gameState.buildState(sessionId).getPods().stream()
                .filter(p -> p.getDeploymentName().equals(deployment) && p.getStatus() == PodStatus.OOM_KILLED)
                .limit(count)
                .forEach(p -> gameState.setPodStatus(sessionId, p.getName(), PodStatus.RUNNING));
    }

    @Override
    public int addScore(Long sessionId, int points) {
        return gameState.addScore(sessionId, points);
    }

    @Override
    public void advanceScenario(Long sessionId) {
        gameState.advanceScenario(sessionId);
    }

    @Override
    public void unlockCommand(Long sessionId, String command) {
        gameState.unlockCommand(sessionId, command);
    }

    @Override
    public boolean isCommandUnlocked(Long sessionId, String command) {
        return gameState.isCommandUnlocked(sessionId, command);
    }

    @Override
    public Set<String> getUnlockedCommands(Long sessionId) {
        return gameState.getUnlockedCommands(sessionId);
    }

    @Override
    public String rolloutStatus(Long sessionId, String deployment) {
        long total = gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getDeploymentName().equals(deployment)).count();
        long ready = gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getDeploymentName().equals(deployment) && p.getStatus() == PodStatus.RUNNING).count();
        if (ready == total && total > 0)
            return "deployment \"" + deployment + "\" successfully rolled out\n" + ready + "/" + total + " replicas ready.";
        return "Waiting for deployment \"" + deployment + "\" rollout to finish: " + ready + "/" + total + " replicas ready.";
    }

    @Override
    public String execPod(Long sessionId, String podName, String command) {
        return gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getName().equals(podName)).findFirst()
            .map(pod -> pod.getStatus() == PodStatus.RUNNING
                ? "[exec] " + podName + "# " + command + "\n[simulation] command executed successfully."
                : "[exec] error: pod " + podName + " is not running (status: " + pod.getStatus() + ")")
            .orElse("[exec] error: pod '" + podName + "' not found.");
    }

    @Override
    public void labelPod(Long sessionId, String podName, String key, String value) {
        // Simulation: no-op — labels are metadata only, no state change needed
    }

    @Override
    public List<Map<String, Object>> getEvents(Long sessionId, String podName) {
        return gameState.buildState(sessionId).getPods().stream()
            .filter(p -> p.getName().equals(podName)).findFirst()
            .map(pod -> switch (pod.getStatus()) {
                case CRASH_LOOP -> List.of(
                    Map.<String, Object>of("type", "Warning", "reason", "BackOff",
                        "message", "Back-off restarting failed container " + podName));
                case OOM_KILLED -> List.of(
                    Map.<String, Object>of("type", "Warning", "reason", "OOMKilling",
                        "message", "Memory limit exceeded for container in pod " + podName));
                case PENDING -> List.of(
                    Map.<String, Object>of("type", "Warning", "reason", "FailedScheduling",
                        "message", "0/3 nodes are available: insufficient resources."));
                default -> List.of(
                    Map.<String, Object>of("type", "Normal", "reason", "Started",
                        "message", "Started container " + podName));
            })
            .orElse(List.of(Map.of("type", "Warning", "reason", "NotFound", "message", "Pod not found: " + podName)));
    }
}
