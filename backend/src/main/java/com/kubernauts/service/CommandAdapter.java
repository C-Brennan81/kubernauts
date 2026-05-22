package com.kubernauts.service;

import com.kubernauts.model.StationState;
import com.kubernauts.model.PodStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstracts all state and command operations so CommandParser works identically
 * in both simulation and cluster mode.
 * 
 * Implementations:
 * - SimulationAdapter: uses GameStateService + H2 database
 * - KubernetesAdapter: uses KubernetesService + real K8s API
 */
public interface CommandAdapter {
    
    // ── State building ────────────────────────────────────────────────────────
    StationState buildState(Long sessionId);
    StationState buildState(Long sessionId, int scoreDelta);
    
    // ── Pod operations ────────────────────────────────────────────────────────
    List<Map<String, Object>> getPods(Long sessionId, String deployment);
    Map<String, Object> describePod(Long sessionId, String podName);
    String getLogs(Long sessionId, String podName);
    void deletePod(Long sessionId, String podName);
    void setPodStatus(Long sessionId, String podName, PodStatus status);
    void spawnPods(Long sessionId, String deployment, int count);
    List<Map<String, Object>> getAllPods(Long sessionId);
    
    // ── Node operations ───────────────────────────────────────────────────────
    List<Map<String, Object>> getNodes(Long sessionId);
    void cordonNode(Long sessionId, String nodeName);
    
    // ── Deployment operations ─────────────────────────────────────────────────
    void scaleDeployment(Long sessionId, String deployment, int replicas);
    void rolloutUndo(Long sessionId, String deployment);
    String rolloutStatus(Long sessionId, String deployment);

    // ── Advanced pod operations ───────────────────────────────────────────────
    String execPod(Long sessionId, String podName, String command);
    void labelPod(Long sessionId, String podName, String key, String value);
    List<Map<String, Object>> getEvents(Long sessionId, String podName);
    
    // ── Scoring and progression ───────────────────────────────────────────────
    int addScore(Long sessionId, int points);
    void advanceScenario(Long sessionId);
    
    // ── Command unlocks ───────────────────────────────────────────────────────
    void unlockCommand(Long sessionId, String command);
    boolean isCommandUnlocked(Long sessionId, String command);
    Set<String> getUnlockedCommands(Long sessionId);
}
