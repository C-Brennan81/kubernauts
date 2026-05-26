package com.kubernauts.service;

import com.kubernauts.model.PodStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScenarioEngine {

    private final GameStateService gameState;

    /**
     * requiredDiscovery: the command the player must run BEFORE the win condition
     * is checked. Empty string means no discovery required.
     */
    public record Scenario(
        int id,
        int level,
        String title,
        String klinkBriefing,
        String hint,
        String klinkSuccess,
        int scoreReward,
        String requiredDiscovery  // e.g. "scan:crew-quarters" or "inspect:nav-system-1"
    ) {}

    private static final List<Scenario> SCENARIOS = List.of(

        // ── LEVEL 1 — Basic Recon ─────────────────────────────────────────────
        new Scenario(0, 1, "First Contact",
            "LEVEL 1 — BASIC RECON\n\n" +
            "Welcome, operator. Something is wrong with the crew-quarters deployment. " +
            "I know what it is. You don't. Start by scanning it.",
            "Try: scan crew-quarters",
            "Good. You can read a manifest. kubectl get pods -l app=crew-quarters. +50 pts.",
            50, "scan:crew-quarters"),

        new Scenario(1, 1, "Malfunction Loop",
            "crew-alpha-1 is in a crash loop. You found it by scanning. Now fix it.",
            "Try: fix crew-alpha-1",
            "crew-alpha-1 stable. kubectl delete pod crew-alpha-1 — the deployment recreates it. +100 pts.",
            100, "scan:crew-quarters"),

        new Scenario(2, 1, "Ghost Unit",
            "nav-system-1 is pending. Not crashed — just stuck. Inspect it to find out why.",
            "Try: inspect nav-system-1",
            "Navigation restored. kubectl describe pod nav-system-1 shows you the events. +100 pts.",
            100, ""),

        // ── LEVEL 2 — First Response ──────────────────────────────────────────
        new Scenario(3, 2, "Read The Logs",
            "LEVEL 2 — FIRST RESPONSE\n\n" +
            "life-support-1 keeps crashing. Before you fix it, read its logs. " +
            "Fixing without diagnosing is how stations explode.",
            "Try: read logs life-support-1",
            "Logs read. kubectl logs life-support-1. Now you know why it's failing. +100 pts.",
            100, ""),

        new Scenario(4, 2, "Memory Overload",
            "life-support-1 was OOM killed. Restarting it won't help — it'll just die again. " +
            "Scale up the deployment so the load is shared.",
            "Try: deploy reinforcements life-support --count=3",
            "Life support scaled. kubectl scale deployment life-support --replicas=3. +150 pts.",
            150, "read:life-support-1"),

        new Scenario(5, 2, "Station Events",
            "Something is wrong with the navigation deployment but the pods look fine. " +
            "Check the station status for a full picture.",
            "Try: status",
            "Good use of status. kubectl get pods -A && kubectl get nodes. +100 pts.",
            100, ""),

        new Scenario(6, 2, "Double Fault",
            "crew-alpha-1 AND crew-alpha-2 are both in crash loops. " +
            "Scan first, then fix both.",
            "Try: scan crew-quarters  →  fix crew-alpha-1  →  fix crew-alpha-2",
            "Both units stable. Two deletes, one deployment. +200 pts.",
            200, "scan:crew-quarters"),

        // ── LEVEL 3 — Deployment Ops ──────────────────────────────────────────
        new Scenario(7, 3, "Bad Rollout",
            "LEVEL 3 — DEPLOYMENT OPS\n\n" +
            "A bad config was pushed to crew-quarters. All three units are crashing. " +
            "Don't fix them individually — roll back the deployment.",
            "Try: revert mission crew-quarters",
            "Rollback complete. kubectl rollout undo deployment/crew-quarters. +200 pts.",
            200, "scan:crew-quarters"),

        new Scenario(8, 3, "Offline Module",
            "module-beta has gone offline. Cordon it so the scheduler stops assigning units there.",
            "Try: isolate module-beta",
            "module-beta cordoned. kubectl cordon module-beta. +150 pts.",
            150, "status"),

        new Scenario(9, 3, "Cascade",
            "nav-system pushed a bad config AND life-support is OOM. Two problems. " +
            "Scan both deployments, then handle them.",
            "Try: revert mission navigation  →  deploy reinforcements life-support --count=2",
            "Both resolved. Multi-deployment triage. +300 pts.",
            300, "scan:navigation"),

        new Scenario(10, 3, "Rollout Status",
            "A new deployment is in progress for crew-quarters. " +
            "Check its rollout status before declaring victory.",
            "Try: status",
            "Status checked. Always verify after a rollout. kubectl rollout status deployment/crew-quarters. +150 pts.",
            150, ""),

        // ── LEVEL 4 — Advanced Triage ─────────────────────────────────────────
        new Scenario(11, 4, "The Quiet Failure",
            "LEVEL 4 — ADVANCED TRIAGE\n\n" +
            "Everything looks green but the crew is complaining. " +
            "Something is wrong that a simple scan won't show. Check the logs on life-support-1.",
            "Try: read logs life-support-1",
            "Logs revealed a silent error. kubectl logs catches what status misses. +200 pts.",
            200, ""),

        new Scenario(12, 4, "Three Down",
            "crew-alpha-1, nav-system-1, and life-support-1 are all failing simultaneously. " +
            "Scan everything, diagnose, then fix in order of criticality.",
            "Try: scan crew-quarters  →  inspect nav-system-1  →  fix all three",
            "All three restored. Triage under pressure. +400 pts.",
            400, "scan:crew-quarters"),

        new Scenario(13, 4, "Node Pressure",
            "module-alpha is under resource pressure. Cordon it and verify the units reschedule.",
            "Try: isolate module-alpha  →  status",
            "module-alpha cordoned, units rescheduled. kubectl cordon + verify. +250 pts.",
            250, "status"),

        new Scenario(14, 4, "Rollback Chain",
            "A bad deployment to navigation caused a cascade. " +
            "Roll it back, then scale life-support to compensate.",
            "Try: revert mission navigation  →  deploy reinforcements life-support --count=3",
            "Chain resolved. Rollback then scale — a common real-world pattern. +350 pts.",
            350, "scan:navigation"),

        new Scenario(15, 4, "Full Audit",
            "I need a complete station audit. Status, then fix everything you find. " +
            "No hints. You know the commands.",
            "Try: status  →  fix everything broken",
            "Full audit complete. This is what on-call looks like. +400 pts.",
            400, "status"),

        // ── LEVEL 5 — Incident Command ────────────────────────────────────────
        new Scenario(16, 5, "Red Alert",
            "LEVEL 5 — INCIDENT COMMAND\n\n" +
            "RED ALERT. Four units down across three deployments. " +
            "You have 90 seconds before the multiplier hits 1x. " +
            "Scan everything. Fix in order. Go.",
            "Scan all deployments, fix all failing units",
            "Station stabilised under pressure. +500 pts.",
            500, "scan:crew-quarters"),

        new Scenario(17, 5, "The Domino",
            "module-gamma offline. nav-system OOM. crew-quarters bad rollout. " +
            "Three independent failures. Handle them in the right order.",
            "Try: isolate module-gamma  →  deploy reinforcements navigation  →  revert mission crew-quarters",
            "All three resolved in sequence. Order matters. +500 pts.",
            500, "status"),

        new Scenario(18, 5, "Silent Cascade",
            "The station looks stable but response times are degrading. " +
            "Read logs on all critical systems, find the root cause, fix it.",
            "Try: read logs on each system  →  fix the root cause",
            "Root cause found and fixed. Logs are your best diagnostic tool. +500 pts.",
            500, ""),

        new Scenario(19, 5, "Final Stand",
            "Everything is failing. This is the final scenario. " +
            "Full audit, full fix, full verification. No hints. No multiplier mercy. " +
            "Show me what you've learned.",
            "Use everything you know",
            "Station fully operational. Campaign complete. You're a Kubernaut. +1000 pts.",
            1000, "status")
    );

    // ── Discovery phase ───────────────────────────────────────────────────────

    /** Record that the player performed a diagnostic action for this scenario */
    public void recordDiscovery(Long sessionId, int scenarioIdx, String action, String target) {
        String key = scenarioIdx + ":" + action + ":" + target;
        gameState.addDiscovery(sessionId, key);
    }

    /** Check if the required discovery has been performed before allowing win */
    public boolean discoveryComplete(Long sessionId, int scenarioIdx) {
        String required = SCENARIOS.get(scenarioIdx).requiredDiscovery();
        if (required.isEmpty()) return true;
        String[] parts = required.split(":");
        String key = scenarioIdx + ":" + parts[0] + ":" + parts[1];
        return gameState.hasDiscovery(sessionId, key);
    }

    // ── Score multiplier ──────────────────────────────────────────────────────

    public int getMultiplier(Long sessionId) {
        long elapsed = gameState.getScenarioElapsed(sessionId);
        if (elapsed < 60) return 3;
        if (elapsed < 120) return 2;
        return 1;
    }

    // ── Scenario setup ────────────────────────────────────────────────────────

    public void setupScenario(Long sessionId, int idx) {
        // Reset all pods to RUNNING first, then break what this scenario needs
        gameState.resetAllPods(sessionId);
        switch (idx) {
            case 0, 1 -> gameState.setPodStatus(sessionId, "crew-alpha-1", PodStatus.CRASH_LOOP);
            case 2    -> gameState.setPodStatus(sessionId, "nav-system-1", PodStatus.PENDING);
            case 3, 4 -> gameState.setPodStatus(sessionId, "life-support-1", PodStatus.OOM_KILLED);
            case 5    -> gameState.setPodStatus(sessionId, "nav-system-1", PodStatus.CRASH_LOOP);
            case 6    -> {
                gameState.setPodStatus(sessionId, "crew-alpha-1", PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "crew-alpha-2", PodStatus.CRASH_LOOP);
            }
            case 7    -> {
                gameState.setPodStatus(sessionId, "crew-alpha-1", PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "crew-alpha-2", PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "crew-beta-1",  PodStatus.CRASH_LOOP);
            }
            case 8    -> gameState.setPodStatus(sessionId, "crew-beta-1", PodStatus.UNKNOWN);
            case 9    -> {
                gameState.setPodStatus(sessionId, "nav-system-1",   PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "life-support-1", PodStatus.OOM_KILLED);
            }
            case 10   -> gameState.setPodStatus(sessionId, "crew-alpha-1", PodStatus.PENDING);
            case 11   -> gameState.setPodStatus(sessionId, "life-support-1", PodStatus.CRASH_LOOP);
            case 12   -> {
                gameState.setPodStatus(sessionId, "crew-alpha-1",   PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "nav-system-1",   PodStatus.PENDING);
                gameState.setPodStatus(sessionId, "life-support-1", PodStatus.OOM_KILLED);
            }
            case 13   -> gameState.setPodStatus(sessionId, "crew-alpha-2", PodStatus.UNKNOWN);
            case 14   -> {
                gameState.setPodStatus(sessionId, "nav-system-1",   PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "life-support-1", PodStatus.OOM_KILLED);
            }
            case 15   -> {
                gameState.setPodStatus(sessionId, "crew-alpha-1",   PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "nav-system-1",   PodStatus.PENDING);
                gameState.setPodStatus(sessionId, "life-support-2", PodStatus.OOM_KILLED);
            }
            case 16   -> {
                gameState.setPodStatus(sessionId, "crew-alpha-1",   PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "crew-alpha-2",   PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "nav-system-1",   PodStatus.OOM_KILLED);
                gameState.setPodStatus(sessionId, "life-support-1", PodStatus.CRASH_LOOP);
            }
            case 17   -> {
                gameState.setPodStatus(sessionId, "crew-beta-1",    PodStatus.UNKNOWN);
                gameState.setPodStatus(sessionId, "nav-system-1",   PodStatus.OOM_KILLED);
                gameState.setPodStatus(sessionId, "crew-alpha-1",   PodStatus.CRASH_LOOP);
            }
            case 18   -> {
                gameState.setPodStatus(sessionId, "life-support-1", PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "life-support-2", PodStatus.CRASH_LOOP);
            }
            case 19   -> {
                gameState.setPodStatus(sessionId, "crew-alpha-1",   PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "crew-alpha-2",   PodStatus.CRASH_LOOP);
                gameState.setPodStatus(sessionId, "crew-beta-1",    PodStatus.UNKNOWN);
                gameState.setPodStatus(sessionId, "nav-system-1",   PodStatus.PENDING);
                gameState.setPodStatus(sessionId, "life-support-1", PodStatus.OOM_KILLED);
                gameState.setPodStatus(sessionId, "life-support-2", PodStatus.CRASH_LOOP);
            }
        }
    }

    // ── Win conditions ────────────────────────────────────────────────────────

    public boolean checkWinCondition(Long sessionId, int idx, String command, Map<String, String> params) {
        if (!discoveryComplete(sessionId, idx)) return false;
        String target = params.getOrDefault("target", "");
        return switch (idx) {
            case 0  -> command.equals("scan")    && "crew-quarters".equals(target);
            case 1  -> command.equals("fix")     && "crew-alpha-1".equals(target);
            case 2  -> command.equals("inspect") && "nav-system-1".equals(target);
            case 3  -> command.equals("read")    && "life-support-1".equals(target);
            case 4  -> command.equals("deploy")  && "life-support".equals(target);
            case 5  -> command.equals("status");
            case 6  -> command.equals("fix")     && "crew-alpha-2".equals(target);
            case 7  -> command.equals("revert")  && "crew-quarters".equals(target);
            case 8  -> command.equals("isolate") && "module-beta".equals(target);
            case 9  -> command.equals("deploy")  && "life-support".equals(target);
            case 10 -> command.equals("status");
            case 11 -> command.equals("read")    && "life-support-1".equals(target);
            case 12 -> command.equals("fix")     && Set.of("crew-alpha-1","nav-system-1","life-support-1").contains(target);
            case 13 -> command.equals("status");
            case 14 -> command.equals("deploy")  && "life-support".equals(target);
            case 15 -> command.equals("status");
            case 16 -> command.equals("fix")     && "life-support-1".equals(target);
            case 17 -> command.equals("revert")  && "crew-quarters".equals(target);
            case 18 -> command.equals("fix")     && "life-support-1".equals(target);
            case 19 -> command.equals("status");
            default -> false;
        };
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Scenario getScenario(int index) {
        if (index < 0 || index >= SCENARIOS.size()) return null;
        return SCENARIOS.get(index);
    }

    public int getLevelForScenario(int idx) {
        if (idx >= SCENARIOS.size()) return 5;
        return SCENARIOS.get(idx).level();
    }

    public boolean isLevelStart(int idx) {
        if (idx <= 0 || idx >= SCENARIOS.size()) return false;
        return SCENARIOS.get(idx).level() > SCENARIOS.get(idx - 1).level();
    }

    public int totalScenarios() { return SCENARIOS.size(); }
}
