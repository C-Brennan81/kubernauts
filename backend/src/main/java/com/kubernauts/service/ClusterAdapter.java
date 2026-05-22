package com.kubernauts.service;

import com.kubernauts.model.PodStatus;
import com.kubernauts.model.StationState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cluster mode adapter: delegates to KubernetesService for real cluster operations.
 * Used when game.mode=cluster.
 * 
 * Handles game state progression separately since the K8s cluster only manages workloads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "game.mode", havingValue = "cluster")
public class ClusterAdapter implements CommandAdapter {

    private final KubernetesService k8s;
    private final GameStateService gameState;

    @Override
    public StationState buildState(Long sessionId) {
        return gameState.buildState(sessionId);
    }

    @Override
    public StationState buildState(Long sessionId, int scoreDelta) {
        return gameState.buildState(sessionId, scoreDelta);
    }

    @Override
    public List<Map<String, Object>> getPods(Long sessionId, String deployment) {
        return k8s.getPods(sessionId, deployment);
    }

    @Override
    public Map<String, Object> describePod(Long sessionId, String podName) {
        return k8s.describePod(sessionId, podName);
    }

    @Override
    public String getLogs(Long sessionId, String podName) {
        return k8s.getLogs(sessionId, podName);
    }

    @Override
    public void deletePod(Long sessionId, String podName) {
        k8s.deletePod(sessionId, podName);
    }

    @Override
    public void setPodStatus(Long sessionId, String podName, PodStatus status) {
        // In cluster mode, pod state is managed by Kubernetes itself
        // This method is a no-op
        log.debug("setPodStatus called in cluster mode (no-op): {} -> {}", podName, status);
    }

    @Override
    public void spawnPods(Long sessionId, String deployment, int count) {
        // In cluster mode, spawn pods by scaling a deployment
        k8s.scaleDeployment(sessionId, deployment, count);
    }

    @Override
    public List<Map<String, Object>> getAllPods(Long sessionId) {
        return k8s.getAllPods(sessionId);
    }

    @Override
    public List<Map<String, Object>> getNodes(Long sessionId) {
        return k8s.getNodes();
    }

    @Override
    public void cordonNode(Long sessionId, String nodeName) {
        k8s.cordonNode(nodeName);
    }

    @Override
    public void scaleDeployment(Long sessionId, String deployment, int replicas) {
        k8s.scaleDeployment(sessionId, deployment, replicas);
    }

    @Override
    public void rolloutUndo(Long sessionId, String deployment) {
        k8s.rolloutUndo(sessionId, deployment);
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
}

