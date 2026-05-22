package com.kubernauts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kubernauts.repository.GameSessionRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "game.mode", havingValue = "cluster")
public class SessionCleanupScheduler {

    private final KubernetesService k8s;
    private final GameSessionRepository sessionRepo;

    @Value("${game.session-ttl-minutes:30}")
    private int ttlMinutes;

    /** Delete namespaces for sessions older than TTL — runs every 5 minutes */
    @Scheduled(fixedDelay = 300_000)
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(ttlMinutes);
        sessionRepo.findAll().stream()
            .filter(s -> s.getStartedAt().isBefore(cutoff))
            .forEach(s -> {
                try {
                    k8s.deleteNamespace(s.getId());
                    log.info("TTL cleanup: deleted namespace for session {}", s.getId());
                } catch (Exception e) {
                    log.warn("Failed to delete namespace for session {}: {}", s.getId(), e.getMessage());
                }
            });
    }

    /** Called explicitly when a game session completes */
    public void cleanupSession(Long sessionId) {
        try {
            k8s.deleteNamespace(sessionId);
            log.info("Session complete: deleted namespace for session {}", sessionId);
        } catch (Exception e) {
            log.warn("Failed to delete namespace for session {}: {}", sessionId, e.getMessage());
        }
    }
}
