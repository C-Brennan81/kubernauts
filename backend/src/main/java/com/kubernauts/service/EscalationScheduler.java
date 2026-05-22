package com.kubernauts.service;

import com.kubernauts.controller.GameController;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EscalationScheduler {

    private final GameController gameController;
    private final GameStateService gameState;
    private final KLINKService klink;
    private final SimpMessagingTemplate ws;

    @Scheduled(fixedDelay = 10_000)
    public void checkEscalation() {
        gameController.getEscalationTiers().forEach((sessionId, tier) -> {
            try {
                long elapsed = gameState.getScenarioElapsed(sessionId);
                int newTier = elapsed >= 90 ? 3 : elapsed >= 60 ? 2 : elapsed >= 30 ? 1 : 0;
                if (newTier > tier) {
                    gameController.getEscalationTiers().put(sessionId, newTier);
                    ws.convertAndSend("/topic/klink/" + sessionId,
                            Map.of("message", klink.onEscalation(newTier), "type", "escalation"));
                }
            } catch (Exception ignored) {}
        });
    }
}
