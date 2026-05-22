package com.kubernauts.controller;

import com.kubernauts.model.*;
import com.kubernauts.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameStateService gameState;
    private final ScenarioEngine scenarioEngine;
    private final KLINKService klink;
    private final CommandParser commandParser;
    private final IncidentModeScheduler incidentMode;
    private final SimpMessagingTemplate ws;
    private final Optional<SessionCleanupScheduler> sessionCleanup;

    // Escalation tier per session — reset on scenario advance
    final Map<Long, Integer> escalationTier = new ConcurrentHashMap<>();

    @PostMapping("/start")
    public ResponseEntity<StartGameResponse> startGame(@RequestBody StartGameRequest body) {
        String playerName = body.getPlayerName() == null || body.getPlayerName().isBlank()
                ? "Kubernaut" : body.getPlayerName().trim();

        GameSession session = gameState.createSession(playerName);
        scenarioEngine.setupScenario(session.getId(), 0);
        escalationTier.put(session.getId(), 0);

        String welcome = klink.welcome(playerName) + "\n\n"
                + scenarioEngine.getScenario(0).klinkBriefing();
        ws.convertAndSend("/topic/klink/" + session.getId(),
                Map.of("message", welcome, "type", "mission"));

        return ResponseEntity.ok(new StartGameResponse(
                session.getId(), welcome, "mission",
                gameState.buildState(session.getId())));
    }

    @GetMapping("/state/{sessionId}")
    public ResponseEntity<StationState> getState(@PathVariable Long sessionId) {
        return ResponseEntity.ok(gameState.buildState(sessionId));
    }

    @GetMapping("/klink/idle")
    public ResponseEntity<Map<String, String>> idleComment() {
        return ResponseEntity.ok(Map.of("message", klink.getIdleComment(), "type", "idle"));
    }

    @PostMapping("/command/{sessionId}")
    public ResponseEntity<CommandResult> command(
            @PathVariable Long sessionId,
            @RequestBody CommandRequest body) {

        String raw = body.getCommand() == null ? "" : body.getCommand();
        CommandResult result = commandParser.execute(sessionId, raw);

        // If scenario just completed, handle level-up and next briefing
        if (result.isScenarioComplete()) {
            int nextIdx = gameState.getSession(sessionId).getCurrentScenario();
            escalationTier.put(sessionId, 0);

            if (nextIdx >= scenarioEngine.totalScenarios()) {
                GameSession s = gameState.getSession(sessionId);
                String gameOver = klink.onGameComplete(s.getPlayerName(), s.getScore());
                ws.convertAndSend("/topic/klink/" + sessionId,
                        Map.of("message", gameOver, "type", "mission"));
                escalationTier.remove(sessionId);
                // Activate incident mode after campaign
                incidentMode.activate(sessionId);
                // Cleanup cluster namespace if in cluster mode
                sessionCleanup.ifPresent(c -> c.cleanupSession(sessionId));
            } else {
                boolean levelUp = scenarioEngine.isLevelStart(nextIdx);
                if (levelUp) {
                    int newLevel = scenarioEngine.getLevelForScenario(nextIdx);
                    gameState.setLevel(sessionId, newLevel);
                    ws.convertAndSend("/topic/levelup/" + sessionId, String.valueOf(newLevel));
                }
                String levelUpMsg = levelUp
                        ? "\n\n" + klink.onLevelUp(scenarioEngine.getLevelForScenario(nextIdx))
                        : "";
                ScenarioEngine.Scenario prev = scenarioEngine.getScenario(nextIdx - 1);
                String successMsg = klink.onScenarioComplete(prev.klinkSuccess())
                        + levelUpMsg + "\n\n"
                        + scenarioEngine.getScenario(nextIdx).klinkBriefing();
                scenarioEngine.setupScenario(sessionId, nextIdx);
                ws.convertAndSend("/topic/klink/" + sessionId,
                        Map.of("message", successMsg, "type", "mission"));
            }
        } else {
            ws.convertAndSend("/topic/klink/" + sessionId,
                    Map.of("message", result.getKlinkMessage(), "type", result.getKlinkMessageType()));
        }

        return ResponseEntity.ok(result);
    }

    // Called by EscalationScheduler
    public Map<Long, Integer> getEscalationTiers() {
        return escalationTier;
    }
}
