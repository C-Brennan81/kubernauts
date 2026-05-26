package com.kubernauts.service;

import com.kubernauts.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses and executes game commands.
 * Handles both native game syntax and kubectl aliases.
 * Single responsibility: translate input → CommandResult.
 */
@Service
@RequiredArgsConstructor
public class CommandParser {

    private static final Set<String> VALID_COMMANDS = Set.of(
            "scan", "inspect", "read", "fix", "deploy", "revert", "isolate", "status",
            "exec", "label", "events", "rollout");

    private final CommandAdapter adapter;
    private final ScenarioEngine scenarioEngine;
    private final KLINKService klink;

    // Proxy command → kubectl equivalent shown when locked
    private static final Map<String, String> KUBECTL_EQUIVALENT = Map.of(
        "scan",    "kubectl get pods -l app=<deployment>",
        "inspect", "kubectl describe pod <unit>",
        "read",    "kubectl logs <unit>",
        "fix",     "kubectl delete pod <unit>",
        "deploy",  "kubectl scale deployment <dep> --replicas=<n>",
        "revert",  "kubectl rollout undo deployment/<dep>",
        "isolate", "kubectl cordon <module>",
        "status",  "kubectl get nodes"
    );

    private static final Map<String, String> KUBECTL_EQUIVALENT_EXT = Map.of(
        "exec",    "kubectl exec <pod> -- <cmd>",
        "label",   "kubectl label pod <pod> <key>=<val>",
        "events",  "kubectl get events --field-selector involvedObject.name=<pod>",
        "rollout", "kubectl rollout status deployment/<dep>"
    );

    public String normalise(String raw) {
        if (raw.matches("kubectl get pods.*-l app=(\\S+).*"))
            return "scan " + raw.replaceAll(".*-l app=(\\S+).*", "$1");
        if (raw.matches("kubectl get pods.*"))
            return "scan crew-quarters";
        if (raw.matches("kubectl describe pod (\\S+)"))
            return "inspect " + raw.replaceAll("kubectl describe pod (\\S+)", "$1");
        if (raw.matches("kubectl logs (\\S+)"))
            return "read logs " + raw.replaceAll("kubectl logs (\\S+)", "$1");
        if (raw.matches("kubectl delete pod (\\S+)"))
            return "fix " + raw.replaceAll("kubectl delete pod (\\S+)", "$1");
        if (raw.matches("kubectl scale deployment (\\S+) --replicas=(\\d+)")) {
            String dep = raw.replaceAll("kubectl scale deployment (\\S+) --replicas=(\\d+)", "$1");
            String n   = raw.replaceAll("kubectl scale deployment (\\S+) --replicas=(\\d+)", "$2");
            return "deploy reinforcements " + dep + " --count=" + n;
        }
        if (raw.matches("kubectl rollout undo deployment/(\\S+)"))
            return "revert mission " + raw.replaceAll("kubectl rollout undo deployment/(\\S+)", "$1");
        if (raw.matches("kubectl cordon (\\S+)"))
            return "isolate " + raw.replaceAll("kubectl cordon (\\S+)", "$1");
        if (raw.matches("kubectl get nodes.*"))
            return "status";
        if (raw.matches("kubectl exec (\\S+) -- (.+)")) {
            String pod = raw.replaceAll("kubectl exec (\\S+) -- (.+)", "$1");
            String cmd = raw.replaceAll("kubectl exec (\\S+) -- (.+)", "$2");
            return "exec " + pod + " " + cmd;
        }
        if (raw.matches("kubectl label pod (\\S+) (\\S+)=(\\S+)")) {
            String pod = raw.replaceAll("kubectl label pod (\\S+) (\\S+)=(\\S+)", "$1");
            String kv  = raw.replaceAll("kubectl label pod (\\S+) (\\S+)=(\\S+)", "$2=$3");
            return "label unit " + pod + " " + kv;
        }
        if (raw.matches("kubectl get events.*--field-selector.*involvedObject\\.name=(\\S+).*"))
            return "events " + raw.replaceAll(".*involvedObject\\.name=(\\S+).*", "$1");
        if (raw.matches("kubectl rollout status deployment/(\\S+)"))
            return "rollout status " + raw.replaceAll("kubectl rollout status deployment/(\\S+)", "$1");
        return raw;
    }

    public CommandResult execute(Long sessionId, String rawInput) {
        String raw = rawInput.trim().toLowerCase();
        String normalised = normalise(raw);
        boolean wasKubectl = !normalised.equals(raw);

        String[] parts = normalised.split("\\s+");
        if (parts.length == 0 || parts[0].isBlank())
            return result("", klink.onUnknownCommand(""), "system", sessionId, 0, false);

        StationState state = adapter.buildState(sessionId);
        int scenarioIdx = state.getCurrentScenario();

        // ── Proxy command lock check ──────────────────────────────────────────
        // If the player typed a proxy command (not kubectl) and it's not yet unlocked, block it
        String verb = parts[0];
        if (!wasKubectl && VALID_COMMANDS.contains(verb)
                && !adapter.isCommandUnlocked(sessionId, verb)) {
            String kubectl = KUBECTL_EQUIVALENT.containsKey(verb)
                ? KUBECTL_EQUIVALENT.get(verb)
                : KUBECTL_EQUIVALENT_EXT.getOrDefault(verb, "kubectl ...");
            String msg = klink.onLockedCommand(verb, kubectl);
            return result(msg, msg, "system", sessionId, 0, false);
        }

        // If kubectl was used successfully, normalised form is the proxy — mark as unlocked after execution
        String kubectlNote = wasKubectl
            ? "✓ kubectl syntax accepted\n\n"
            : "";

        return switch (verb) {
            case "help"   -> result(kubectlNote + klink.onHelp(), klink.onHelp(), "system", sessionId, 0, false);
            case "clear"  -> result("__CLEAR__", "", "system", sessionId, 0, false);
            case "hint"   -> handleHint(sessionId, scenarioIdx);
            case "status" -> handleStatus(sessionId, scenarioIdx, kubectlNote, wasKubectl);
            case "scan"   -> parts.length >= 2
                    ? handleScan(sessionId, scenarioIdx, parts[1], kubectlNote, wasKubectl)
                    : result("Usage: kubectl get pods -l app=<deployment>", "", "system", sessionId, 0, false);
            case "inspect" -> parts.length >= 2
                    ? handleInspect(sessionId, scenarioIdx, parts[1], kubectlNote, wasKubectl)
                    : result("Usage: kubectl describe pod <unit>", "", "system", sessionId, 0, false);
            case "read"   -> parts.length >= 3 && parts[1].equals("logs")
                    ? handleReadLogs(sessionId, scenarioIdx, parts[2], kubectlNote, wasKubectl)
                    : result("Usage: kubectl logs <unit>", "", "system", sessionId, 0, false);
            case "fix"    -> parts.length >= 2
                    ? handleFix(sessionId, scenarioIdx, parts[1], kubectlNote, wasKubectl)
                    : result("Usage: kubectl delete pod <unit>", "", "system", sessionId, 0, false);
            case "deploy" -> parts.length >= 3
                    ? handleDeploy(sessionId, scenarioIdx, parts[2], normalised, kubectlNote, wasKubectl)
                    : result("Usage: kubectl scale deployment <dep> --replicas=<n>", "", "system", sessionId, 0, false);
            case "revert" -> parts.length >= 3
                    ? handleRevert(sessionId, scenarioIdx, parts[2], kubectlNote, wasKubectl)
                    : result("Usage: kubectl rollout undo deployment/<dep>", "", "system", sessionId, 0, false);
            case "isolate" -> parts.length >= 2
                    ? handleIsolate(sessionId, scenarioIdx, parts[1], kubectlNote, wasKubectl)
                    : result("Usage: kubectl cordon <module>", "", "system", sessionId, 0, false);
            case "exec"    -> parts.length >= 3
                    ? handleExec(sessionId, scenarioIdx, parts[1], joinFrom(parts, 2), kubectlNote, wasKubectl)
                    : result("Usage: kubectl exec <pod> -- <cmd>", "", "system", sessionId, 0, false);
            case "label"   -> parts.length >= 4 && parts[1].equals("unit")
                    ? handleLabel(sessionId, scenarioIdx, parts[2], parts[3], kubectlNote, wasKubectl)
                    : result("Usage: kubectl label pod <pod> <key>=<val>", "", "system", sessionId, 0, false);
            case "events"  -> parts.length >= 2
                    ? handleEvents(sessionId, scenarioIdx, parts[1], kubectlNote, wasKubectl)
                    : result("Usage: kubectl get events --field-selector involvedObject.name=<pod>", "", "system", sessionId, 0, false);
            case "rollout" -> parts.length >= 3 && parts[1].equals("status")
                    ? handleRolloutStatus(sessionId, scenarioIdx, parts[2], kubectlNote, wasKubectl)
                    : result("Usage: kubectl rollout status deployment/<dep>", "", "system", sessionId, 0, false);
            default -> handleUnknown(sessionId, scenarioIdx, verb, raw);
        };
    }

    // ── Command handlers ──────────────────────────────────────────────────────

    private CommandResult handleHint(Long sessionId, int scenarioIdx) {
        ScenarioEngine.Scenario s = scenarioEngine.getScenario(scenarioIdx);
        if (s == null) return result("No active scenario.", "", "system", sessionId, 0, false);
        adapter.addScore(sessionId, -25);
        String msg = klink.onHint(s.hint());
        return result(msg, msg, "mission", sessionId, -25, false);
    }

    private CommandResult handleStatus(Long sessionId, int scenarioIdx, String prefix, boolean wasKubectl) {
        scenarioEngine.recordDiscovery(sessionId, scenarioIdx, "status", "");
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "status") : null;
        StationState st = adapter.buildState(sessionId);
        long unhealthy = st.getPods().stream().filter(p -> p.getStatus() != PodStatus.RUNNING).count();
        String msg = klink.onStatus(st.getAlertLevel(), st.getPods().size(), unhealthy);
        if (unlockMsg != null) msg += "\n\n" + unlockMsg;
        return checkWin(sessionId, scenarioIdx, "status", Map.of(), prefix + msg, msg, 0);
    }

    private CommandResult handleScan(Long sessionId, int scenarioIdx, String deployment, String prefix, boolean wasKubectl) {
        scenarioEngine.recordDiscovery(sessionId, scenarioIdx, "scan", deployment);
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "scan") : null;
        List<Map<String, Object>> units = adapter.getPods(sessionId, deployment);
        String output = prefix + klink.onScan(deployment, units);
        if (unlockMsg != null) output += "\n\n" + unlockMsg;
        // klinkMsg empty — scan output belongs in terminal only
        return checkWin(sessionId, scenarioIdx, "scan", Map.of("target", deployment), output, "", 0);
    }

    private CommandResult handleInspect(Long sessionId, int scenarioIdx, String unitName, String prefix, boolean wasKubectl) {
        scenarioEngine.recordDiscovery(sessionId, scenarioIdx, "inspect", unitName);
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "inspect") : null;
        Map<String, Object> details = adapter.describePod(sessionId, unitName);
        if (details.containsKey("error")) {
            return result("Unit '" + unitName + "' not found.", klink.onFix(unitName, false), "system", sessionId, 0, false);
        }
        String output = prefix + klink.onInspect(unitName, details);
        if (unlockMsg != null) output += "\n\n" + unlockMsg;
        return checkWin(sessionId, scenarioIdx, "inspect", Map.of("target", unitName), output, "", 0);
    }

    private CommandResult handleReadLogs(Long sessionId, int scenarioIdx, String unitName, String prefix, boolean wasKubectl) {
        scenarioEngine.recordDiscovery(sessionId, scenarioIdx, "read", unitName);
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "read") : null;
        String logs = adapter.getLogs(sessionId, unitName);
        String output = prefix + logs + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "read", Map.of("target", unitName), output, "", 0);
    }

    private CommandResult handleFix(Long sessionId, int scenarioIdx, String unitName, String prefix, boolean wasKubectl) {
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "fix") : null;
        Map<String, Object> pod = adapter.describePod(sessionId, unitName);
        boolean exists = !pod.containsKey("error");
        if (exists) adapter.deletePod(sessionId, unitName);
        String msg = klink.onFix(unitName, exists) + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "fix", Map.of("target", unitName), prefix + msg, msg, 0);
    }

    private CommandResult handleDeploy(Long sessionId, int scenarioIdx, String deployment, String normalised, String prefix, boolean wasKubectl) {
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "deploy") : null;
        int count = parseCount(normalised);
        adapter.spawnPods(sessionId, deployment, count);
        String msg = klink.onDeploy(deployment, count) + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "deploy", Map.of("target", deployment), prefix + msg, msg, 0);
    }

    private CommandResult handleRevert(Long sessionId, int scenarioIdx, String deployment, String prefix, boolean wasKubectl) {
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "revert") : null;
        adapter.rolloutUndo(sessionId, deployment);
        String msg = klink.onRevert(deployment) + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "revert", Map.of("target", deployment), prefix + msg, msg, 0);
    }

    private CommandResult handleIsolate(Long sessionId, int scenarioIdx, String module, String prefix, boolean wasKubectl) {
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "isolate") : null;
        adapter.cordonNode(sessionId, module);
        String msg = klink.onIsolate(module) + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "isolate", Map.of("target", module), prefix + msg, msg, 0);
    }

    private CommandResult handleExec(Long sessionId, int scenarioIdx, String podName, String cmd, String prefix, boolean wasKubectl) {
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "exec") : null;
        String output = adapter.execPod(sessionId, podName, cmd);
        String msg = klink.onExec(podName, cmd, output) + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "exec", Map.of("target", podName), prefix + msg, msg, 0);
    }

    private CommandResult handleLabel(Long sessionId, int scenarioIdx, String podName, String kv, String prefix, boolean wasKubectl) {
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "label") : null;
        String[] kvParts = kv.split("=", 2);
        String key = kvParts[0], value = kvParts.length > 1 ? kvParts[1] : "";
        adapter.labelPod(sessionId, podName, key, value);
        String msg = klink.onLabel(podName, key, value) + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "label", Map.of("target", podName, "key", key, "value", value), prefix + msg, msg, 0);
    }

    private CommandResult handleEvents(Long sessionId, int scenarioIdx, String podName, String prefix, boolean wasKubectl) {
        scenarioEngine.recordDiscovery(sessionId, scenarioIdx, "events", podName);
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "events") : null;
        var events = adapter.getEvents(sessionId, podName);
        String msg = klink.onEvents(podName, events) + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "events", Map.of("target", podName), prefix + msg, msg, 0);
    }

    private CommandResult handleRolloutStatus(Long sessionId, int scenarioIdx, String deployment, String prefix, boolean wasKubectl) {
        scenarioEngine.recordDiscovery(sessionId, scenarioIdx, "rollout", deployment);
        String unlockMsg = wasKubectl ? unlockAndAnnounce(sessionId, "rollout") : null;
        String status = adapter.rolloutStatus(sessionId, deployment);
        String msg = klink.onRolloutStatus(deployment, status) + (unlockMsg != null ? "\n\n" + unlockMsg : "");
        return checkWin(sessionId, scenarioIdx, "rollout", Map.of("target", deployment), prefix + msg, msg, 0);
    }

    private CommandResult handleUnknown(Long sessionId, int scenarioIdx, String verb, String raw) {
        if (VALID_COMMANDS.contains(verb)) {
            adapter.addScore(sessionId, -5);
            String penalty = klink.onWrongCommand();
            return result("Command executed but not what this scenario needs.\n" + penalty,
                    penalty, "system", sessionId, -5, false);
        }
        return result("Unknown command: " + raw, klink.onUnknownCommand(raw), "system", sessionId, 0, false);
    }

    // ── Unlock helper ─────────────────────────────────────────────────────────

    private String unlockAndAnnounce(Long sessionId, String command) {
        if (!adapter.isCommandUnlocked(sessionId, command)) {
            adapter.unlockCommand(sessionId, command);
            return klink.onCommandUnlocked(command);
        }
        return null;
    }

    // ── Win condition check ───────────────────────────────────────────────────

    CommandResult checkWin(Long sessionId, int scenarioIdx, String cmd,
                            Map<String, String> params, String output, String klinkMsg, int delta) {
        if (!scenarioEngine.checkWinCondition(sessionId, scenarioIdx, cmd, params))
            return result(output, klinkMsg, "mission", sessionId, delta, false);

        ScenarioEngine.Scenario s = scenarioEngine.getScenario(scenarioIdx);
        int multiplier = scenarioEngine.getMultiplier(sessionId);
        int reward = s.scoreReward() * multiplier;
        adapter.addScore(sessionId, reward);
        adapter.advanceScenario(sessionId);
        return result(output, klinkMsg, "mission", sessionId, reward, true);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private int parseCount(String raw) {
        try {
            String[] p = raw.split("--count=");
            if (p.length > 1) return Integer.parseInt(p[1].trim());
        } catch (NumberFormatException ignored) {}
        return 1;
    }

    private String joinFrom(String[] parts, int from) {
        return String.join(" ", java.util.Arrays.copyOfRange(parts, from, parts.length));
    }

    private CommandResult result(String output, String klinkMsg, String type,
                                   Long sessionId, int scoreDelta, boolean scenarioComplete) {
        return new CommandResult(output, klinkMsg, type,
                adapter.buildState(sessionId, scoreDelta), scenarioComplete);
    }
}
