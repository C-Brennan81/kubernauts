package com.kubernauts.service;

import com.kubernauts.model.PodStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class IncidentModeScheduler {

    private final GameStateService gameState;
    private final KLINKService klink;
    private final SimpMessagingTemplate ws;

    private final Set<Long> activeSessions = ConcurrentHashMap.newKeySet();
    private final Map<Long, Long> redAlertSince = new ConcurrentHashMap<>();
    private final Random rng = new Random();

    private static final List<String> PODS = List.of(
        "crew-alpha-1","crew-alpha-2","crew-beta-1",
        "nav-system-1","nav-system-2","life-support-1","life-support-2");
    private static final List<PodStatus> FAILURES = List.of(
        PodStatus.CRASH_LOOP, PodStatus.OOM_KILLED, PodStatus.PENDING);

    public void activate(Long sessionId) {
        activeSessions.add(sessionId);
        gameState.activateIncidentMode(sessionId);
        ws.convertAndSend("/topic/klink/" + sessionId,
            Map.of("message", klink.onIncidentModeStart(), "type", "incident"));
    }

    public void deactivate(Long sessionId) {
        activeSessions.remove(sessionId);
        redAlertSince.remove(sessionId);
    }

    @Scheduled(fixedDelay = 10_000)
    public void tick() {
        activeSessions.forEach(sessionId -> {
            try {
                var state = gameState.buildState(sessionId);

                if ("RED".equals(state.getAlertLevel())) {
                    long since = redAlertSince.computeIfAbsent(sessionId, k -> System.currentTimeMillis());
                    if (System.currentTimeMillis() - since > 60_000) {
                        deactivate(sessionId);
                        ws.convertAndSend("/topic/klink/" + sessionId,
                            Map.of("message", klink.onIncidentModeEnd(state.getSurvivalScore()), "type", "incident"));
                        ws.convertAndSend("/topic/incident/" + sessionId, "ended");
                        return;
                    }
                } else {
                    redAlertSince.remove(sessionId);
                    gameState.addSurvivalScore(sessionId, 10);
                }

                // ~1-in-4 ticks fire a random failure
                if (rng.nextInt(4) == 0) {
                    String pod = PODS.get(rng.nextInt(PODS.size()));
                    PodStatus failure = FAILURES.get(rng.nextInt(FAILURES.size()));
                    gameState.setPodStatus(sessionId, pod, failure);
                    ws.convertAndSend("/topic/klink/" + sessionId,
                        Map.of("message", klink.onIncidentFailure(pod), "type", "incident"));
                    ws.convertAndSend("/topic/state/" + sessionId, gameState.buildState(sessionId));
                }
            } catch (Exception ignored) {}
        });
    }
}
