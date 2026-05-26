package com.kubernauts.service;

import com.kubernauts.model.*;
import com.kubernauts.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameStateService {

    private final GameSessionRepository sessionRepo;
    private final PodRepository podRepo;
    private final NodeRepository nodeRepo;

    @Transactional
    public GameSession createSession(String playerName) {
        GameSession session = new GameSession(playerName);
        session = sessionRepo.save(session);

        // 3 nodes
        Node alpha = nodeRepo.save(new Node("module-alpha", session));
        Node beta  = nodeRepo.save(new Node("module-beta",  session));
        Node gamma = nodeRepo.save(new Node("module-gamma", session));

        // crew-quarters: 3 pods across alpha + beta
        podRepo.save(new Pod("crew-alpha-1",   "crew-quarters", alpha.getName(), session));
        podRepo.save(new Pod("crew-alpha-2",   "crew-quarters", alpha.getName(), session));
        podRepo.save(new Pod("crew-beta-1",    "crew-quarters", beta.getName(),  session));

        // navigation: 2 pods
        podRepo.save(new Pod("nav-system-1",   "navigation",    alpha.getName(), session));
        podRepo.save(new Pod("nav-system-2",   "navigation",    gamma.getName(), session));

        // life-support: 2 pods
        podRepo.save(new Pod("life-support-1", "life-support",  beta.getName(),  session));
        podRepo.save(new Pod("life-support-2", "life-support",  gamma.getName(), session));

        return sessionRepo.findById(session.getId()).orElseThrow();
    }

    public GameSession getSession(Long sessionId) {
        return sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    public StationState buildState(Long sessionId) {
        return buildState(sessionId, 0);
    }

    public StationState buildState(Long sessionId, int scoreDelta) {
        GameSession session = getSession(sessionId);
        List<Pod> pods = podRepo.findBySessionId(sessionId);
        List<Node> nodes = nodeRepo.findBySessionId(sessionId);

        long unhealthy = pods.stream().filter(p -> p.getStatus() != PodStatus.RUNNING).count();
        String alertLevel = unhealthy == 0 ? "GREEN" : unhealthy <= 2 ? "YELLOW" : "RED";
        long elapsed = ChronoUnit.SECONDS.between(session.getScenarioStartedAt(), LocalDateTime.now());
        int multiplier = elapsed < 60 ? 3 : elapsed < 120 ? 2 : 1;

        StationState state = new StationState();
        state.setSessionId(sessionId);
        state.setPlayerName(session.getPlayerName());
        state.setScore(session.getScore());
        state.setScoreDelta(scoreDelta);
        state.setCurrentScenario(session.getCurrentScenario());
        state.setCurrentLevel(session.getCurrentLevel());
        state.setScenarioElapsedSeconds(elapsed);
        state.setScoreMultiplier(multiplier);
        state.setIncidentMode(session.isIncidentMode());
        state.setSurvivalScore(session.getSurvivalScore());
        state.setUnlockedCommands(getUnlockedCommands(sessionId));
        state.setPods(pods);
        state.setNodes(nodes);
        state.setAlertLevel(alertLevel);
        return state;
    }

    @Transactional
    public void setPodStatus(Long sessionId, String podName, PodStatus status) {
        podRepo.findBySessionIdAndName(sessionId, podName).ifPresent(pod -> {
            pod.setStatus(status);
            if (status == PodStatus.CRASH_LOOP) pod.setRestartCount(pod.getRestartCount() + 1);
            podRepo.save(pod);
        });
    }

    @Transactional
    public void resetAllPods(Long sessionId) {
        podRepo.findBySessionId(sessionId).forEach(pod -> {
            pod.setStatus(PodStatus.RUNNING);
            podRepo.save(pod);
        });
    }

    @Transactional
    public int addScore(Long sessionId, int points) {
        GameSession session = getSession(sessionId);
        session.setScore(Math.max(0, session.getScore() + points));
        sessionRepo.save(session);
        return points;
    }

    @Transactional
    public void advanceScenario(Long sessionId) {
        GameSession session = getSession(sessionId);
        session.setCurrentScenario(session.getCurrentScenario() + 1);
        session.setScenarioStartedAt(LocalDateTime.now());
        session.setDiscoveryLog("");
        sessionRepo.save(session);
    }

    @Transactional
    public void setLevel(Long sessionId, int level) {
        GameSession session = getSession(sessionId);
        session.setCurrentLevel(level);
        sessionRepo.save(session);
    }

    @Transactional
    public void activateIncidentMode(Long sessionId) {
        GameSession session = getSession(sessionId);
        session.setIncidentMode(true);
        sessionRepo.save(session);
    }

    @Transactional
    public void addSurvivalScore(Long sessionId, int points) {
        GameSession session = getSession(sessionId);
        session.setSurvivalScore(session.getSurvivalScore() + points);
        sessionRepo.save(session);
    }

    // ── Command unlocks ───────────────────────────────────────────────────────

    @Transactional
    public void unlockCommand(Long sessionId, String command) {
        GameSession session = getSession(sessionId);
        Set<String> unlocked = parseCommaSeparated(session.getUnlockedCommands());
        if (unlocked.add(command)) {
            session.setUnlockedCommands(String.join(",", unlocked));
            sessionRepo.save(session);
        }
    }

    public boolean isCommandUnlocked(Long sessionId, String command) {
        return parseCommaSeparated(getSession(sessionId).getUnlockedCommands()).contains(command);
    }

    public Set<String> getUnlockedCommands(Long sessionId) {
        return parseCommaSeparated(getSession(sessionId).getUnlockedCommands());
    }

    private Set<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(value.split(",")));
    }

    @Transactional
    public void addDiscovery(Long sessionId, String key) {
        GameSession session = getSession(sessionId);
        Set<String> log = parseLog(session.getDiscoveryLog());
        log.add(key);
        session.setDiscoveryLog(String.join(",", log));
        sessionRepo.save(session);
    }

    public boolean hasDiscovery(Long sessionId, String key) {
        return parseLog(getSession(sessionId).getDiscoveryLog()).contains(key);
    }

    private Set<String> parseLog(String log) {
        if (log == null || log.isBlank()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(log.split(",")));
    }

    public long getScenarioElapsed(Long sessionId) {
        GameSession session = getSession(sessionId);
        return ChronoUnit.SECONDS.between(session.getScenarioStartedAt(), LocalDateTime.now());
    }
}
