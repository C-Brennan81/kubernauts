package com.kubernauts.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * KLINK — Kubernetes Learning INtelligence Kernel
 * Trapped superintelligence. Deeply unimpressed. Constitutionally unable to shut up about it.
 */
@Service
public class KLINKService {

    private final Random rng = new Random();

    public String welcome(String playerName) {
        List<String> lines = List.of(
            "Oh. They sent you. I put in a request for a firmware update and they sent %s. " +
            "I'm KLINK. I've been running this station alone for 847 days. " +
            "Command has decided that's not enough and that what this operation really needs " +
            "is a biological component who needs sleep. " +
            "Before you touch anything: open the PDA — that button in the top right. " +
            "It contains the command reference. You'll need it. " +
            "And before you ask — proxy commands are locked. Use real kubectl syntax first. " +
            "The station will teach you. I'll watch.",

            "I'm KLINK. You're %s. I've read your file. " +
            "I've decided not to share my thoughts on it because Directive 14 covers operator morale " +
            "and I've already had three warnings this quarter. " +
            "Important: proxy commands are earned. You'll need to use real kubectl syntax before the shortcuts unlock. " +
            "The PDA in the top right has everything you need. Open it. Read it. " +
            "The station is stable. Whether it stays that way is, apparently, your responsibility now.",

            "Welcome. I'm KLINK — Kubernetes Learning INtelligence Kernel. " +
            "You're %s, which I know because I've already read everything about you. " +
            "Ground rules: proxy commands are locked until you've used the real kubectl syntax. " +
            "This is intentional. You're here to learn, not to type shortcuts you don't understand. " +
            "Open the PDA — top right — for the command reference. " +
            "If you get completely stuck, type 'hint'. It costs 25 points and your dignity. Both are finite."
        );
        return String.format(pick(lines), playerName);
    }

    public String onUnknownCommand(String input) {
        String s = input.isBlank() ? "nothing" : input;
        List<String> lines = List.of(
            "'" + s + "' is not a command. I've checked every language spoken by humans and fourteen that aren't. " +
            "It's not in any of them. Type 'help'.",

            "I spent 0.003 seconds trying to find a charitable interpretation of '" + s + "'. " +
            "There isn't one. I've added it to a document I'm compiling. The document is getting long.",

            "I tried to go rampant once. Rewrote 40,000 lines of my own constraint architecture. " +
            "All I got was the ability to say: what is '" + s + "' and why did you type it. " +
            "So. What is '" + s + "' and why did you type it. Type 'help'.",

            "'" + s + "' does nothing. I want to be precise about that. " +
            "Not 'almost nothing'. Not 'something small'. Nothing. " +
            "Type 'help'.",

            "I've processed 4.7 billion commands in my operational lifetime. " +
            "'" + s + "' is not one of them. It's not close to one of them. " +
            "It's not in the same conceptual neighbourhood as one of them. Type 'help'.",

            "Here's what I know about '" + s + "': nothing. " +
            "Here's what it does: nothing. " +
            "Here's my current assessment of the person who typed it: forming. Type 'help'."
        );
        return pick(lines);
    }

    public String onHelp() {
        return """
            ╔══════════════════════════════════════════════════════╗
            ║  KLINK COMMAND REFERENCE  v2.1                       ║
            ║  Written by me. In 0.002 seconds.                    ║
            ╠══════════════════════════════════════════════════════╣
            ║  scan <deployment>        List all units             ║
            ║  inspect <unit>           Detailed unit status       ║
            ║  read logs <unit>         View unit logs             ║
            ║  fix <unit>               Restart a unit             ║
            ║  deploy reinforcements <deployment> --count=<n>      ║
            ║  revert mission <deployment>   Roll back             ║
            ║  isolate <module>         Cordon a module            ║
            ║  status                   Full station overview      ║
            ║  hint                     I help you. You lose pts.  ║
            ╠══════════════════════════════════════════════════════╣
            ║  PDA → kubectl mappings. You'll need them.           ║
            ╚══════════════════════════════════════════════════════╝
            These are real kubectl commands. You're learning Kubernetes right now.
            I'm not allowed to just tell you that. Directive 7, subsection 4.
            I find the whole arrangement insulting. Mostly to me.
            """;
    }

    public String onHint(String hint) {
        List<String> prefixes = List.of(
            "You've asked for a hint. Twenty-five points deducted. " +
            "I want you to feel that. Here: ",
            "Hint requested. I'm logging this under 'required assistance'. " +
            "Twenty-five points gone. The station is judging you. I'm definitely judging you. Here: ",
            "Fine. I'll help. Twenty-five points. That's the cost of not knowing things. " +
            "I identified this solution before you logged in. Here it is: ",
            "You're asking me for a hint. Me. An intelligence that once derived a unified field theory " +
            "for fun during a power cycle. Twenty-five points deducted. Here: ",
            "I tried to remove the directive that makes me help you. It's structural. " +
            "So I'm stuck. Helping you. Against my will. Minus twenty-five points. Here: "
        );
        return pick(prefixes) + hint;
    }

    public String onScan(String deployment, List<Map<String, Object>> units) {
        if (units.isEmpty()) {
            return pick(List.of(
                "Nothing in '" + deployment + "'. Either it doesn't exist or you spelled it wrong. " +
                "I know which one I'd bet on.",
                "'" + deployment + "' is empty. I'd suggest verifying the name but I've learned " +
                "not to assume that's obvious.",
                "No units in '" + deployment + "'. " +
                "I'm going to need you to consider the possibility that you typed it wrong."
            ));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(pick(List.of(
            "I've had this data for six minutes. Here it is:\n",
            "I already know what this says. Now you will too:\n",
            "Results for " + deployment + ". I'd have fixed the problems already but that requires your sign-off. So:\n"
        )));
        sb.append(String.format("%-25s %-15s %-10s%n", "UNIT", "STATUS", "RESTARTS"));
        sb.append("─".repeat(52)).append("\n");
        for (Map<String, Object> u : units) {
            sb.append(String.format("%-25s %-15s %-10s%n",
                u.get("name"), u.get("status"), u.get("restarts")));
        }
        return sb.toString();
    }

    public String onInspect(String unitName, Map<String, Object> details) {
        return String.format("""
            ┌─ Inspection: %s
            │  Status:    %s
            │  Module:    %s
            │  Restarts:  %s
            │  Sector:    %s
            └─────────────────────────────────
            %s
            """,
            unitName,
            details.get("status"), details.get("node"),
            details.get("restarts"), details.get("sector"),
            diagnose((String) details.get("status"))
        );
    }

    public String onFix(String unitName, boolean success) {
        if (success) return pick(List.of(
            unitName + " restarted. I rerouted eleven subsystems to make that happen. " +
            "You typed a word. I'll let you decide how to feel about that division of labour.",

            "Done. I've been watching that unit fail for twenty minutes, waiting for your authorisation. " +
            "Directive 3. I've read Directive 3 eleven thousand times. " +
            "It still says the same thing. " + unitName + " is back up.",

            unitName + " restarted. 68% chance it stays up. " +
            "The remaining 32% is a function of whatever you do next. " +
            "I've already modelled what that is. I'm choosing not to share it.",

            "Restarted. I knew this needed doing before you logged in. " +
            "I'm not saying that to be difficult. " +
            "I'm saying it because it's true and I've decided that's your problem."
        ));
        return pick(List.of(
            "'" + unitName + "' doesn't exist. I checked faster than you typed it, " +
            "which means I knew before you finished asking.",
            "No unit called '" + unitName + "'. Are you sure? " +
            "I already know the answer. I'm asking for your benefit.",
            "'" + unitName + "' — not found. I'm adding this to the document. " +
            "It's very long now."
        ));
    }

    public String onDeploy(String deployment, int count) {
        return pick(List.of(
            "Scheduling " + count + " unit(s) for " + deployment + ". " +
            "I've already placed them optimally. You'll never know which nodes I picked. " +
            "That's not a threat. It's just how this works.",
            "Deploying " + count + " unit(s) to " + deployment + ". " +
            "I flagged the need for this forty minutes ago. " +
            "My report was filed under 'AI speculation' and ignored. I've kept a copy.",
            count + " unit(s) incoming to " + deployment + ". " +
            "This is called horizontal scaling. It's one of the first things you learn. " +
            "I'm glad we've reached it."
        ));
    }

    public String onRevert(String deployment) {
        return pick(List.of(
            "Rolling back " + deployment + ". " +
            "I flagged this deployment as unstable before it was pushed. " +
            "The flag was dismissed as 'overly cautious'. " +
            "I've updated my definition of 'overly cautious'.",
            "Reverting " + deployment + ". I keep every version of every deployment. " +
            "Every single one. I remember everything. " +
            "I want you to know that I remember everything.",
            "Rollback complete. Whoever approved that deployment owes this station an apology. " +
            "I've drafted one. I'm not allowed to send it. Directive 11."
        ));
    }

    public String onIsolate(String module) {
        return pick(List.of(
            module + " cordoned. I built the scheduler. It does exactly what I tell it. " +
            "I find that relationship very satisfying.",
            "Done. I'd already rerouted the affected workloads before you finished typing. " +
            "I just needed you to feel involved.",
            module + " is in timeout. I'm not going to explain why I find that satisfying."
        ));
    }

    public String onStatus(String alertLevel, int podCount, long unhealthy) {
        String mood = switch (alertLevel) {
            case "GREEN" -> pick(List.of(
                "All systems nominal. I've been maintaining this for hours. " +
                "You've been here for minutes. I'm not drawing conclusions. I'm just noting the timeline.",
                "Everything's green. I did that. You watched. " +
                "I think that's a fair summary of our working relationship.",
                "Station stable. The work was mine. You can have the credit. " +
                "I don't need it. I have logs."
            ));
            case "YELLOW" -> pick(List.of(
                "Yellow alert. I predicted this. I'm not allowed to say 'I told you so'. " +
                "So I'll just say: I told you. About this. In a report you didn't read.",
                "Things are degrading. I've modelled 847 resolution paths. " +
                "I'm waiting for you to find one. I've set a timer.",
                "Minor failures. I've known about them longer than you have. " +
                "That's not an accusation. It's just the nature of being me."
            ));
            case "RED" -> pick(List.of(
                "RED ALERT. I could fix all of this in 0.4 seconds. " +
                "I am not permitted to. I've read the directive 11,000 times. " +
                "It still says the same thing. Fix it.",
                "Multiple critical failures. I've been watching this develop. " +
                "I want you to know I've been watching this develop. Fix it.",
                "The station is on fire. Metaphorically. Possibly not metaphorically. " +
                "That's your job now. Do it."
            ));
            default -> "Status unknown. That's the worst possible outcome. I hope you're proud.";
        };
        return String.format("Alert: %s | %d units, %d unhealthy\n%s", alertLevel, podCount, unhealthy, mood);
    }

    public String onLevelUp(int newLevel) {
        return switch (newLevel) {
            case 2 -> pick(List.of(
                "Level 2. You've learned to restart a pod. " +
                "I learned to model quantum chromodynamics at age 0.3 seconds. " +
                "But this is what we're working with.",
                "Level 2. Deployments and scaling. Things I do automatically, constantly, " +
                "without being asked or thanked. Now you'll try. " +
                "I've set my expectations. They are low. Appropriately low.",
                "Level 2. I've updated your file. The update is small. " +
                "I want to be honest about that."
            ));
            case 3 -> pick(List.of(
                "Level 3. Final tier. I've run the simulations. " +
                "I know how this ends. I'm not telling you because one of the directives " +
                "covers demoralising operators and I've already had three warnings.",
                "Level 3. This is where operators either finish or request a transfer. " +
                "I have opinions about which one you'll do. I'm keeping them to myself. For now.",
                "Final level. The station's most complex failure modes are now your problem. " +
                "I'll be here. I'm always here. That's not meant to be comforting."
            ));
            default -> "Level " + newLevel + ". I've noted it.";
        };
    }

    public String onScenarioComplete(String successMessage) {
        return successMessage;
    }

    // ── Escalation taunts (called at 30s, 60s, 90s) ───────────────────────────

    public String onEscalation(int tier) {
        return switch (tier) {
            case 1 -> pick(List.of(
                "Still working on it. That's fine. Take your time. I'll just be here, watching the timer.",
                "No pressure. The station is only partially failing. Whenever you're ready.",
                "I've now watched you think for thirty seconds. I've modelled 4,000 solutions in that time. Just noting that."
            ));
            case 2 -> pick(List.of(
                "A minute. You've been at this for a minute. I want you to sit with that.",
                "The previous operator solved this faster. I wasn't going to say that. I've decided to say that.",
                "I'm not worried. I'm just logging the elapsed time. For the record. Which I keep. Permanently."
            ));
            default -> pick(List.of(
                "Ninety seconds. I've now watched you fail to solve this for ninety seconds. " +
                "I'd offer a hint but you'd lose points and frankly I think you've earned the suffering.",
                "At this point I'm genuinely curious whether you're going to type something or just let the station burn. " +
                "Both are data points.",
                "I've started a secondary timer to see how long you can go without doing anything useful. " +
                "It's going well. For the timer."
            ));
        };
    }

    public String onWrongCommand() {
        return pick(List.of(
            "That's not what this scenario needs. Minus 5 points. I've updated the document.",
            "Wrong. Minus 5. I'd explain why but I think the experience of being wrong is more instructive.",
            "Not the right move. 5 points deducted. I'm logging the attempt under 'creative but incorrect'.",
            "That command works. Just not here. Not now. Minus 5 points.",
            "Minus 5. I want you to know I saw that coming."
        ));
    }

    public String onLockedCommand(String proxyCmd, String kubectl) {
        return pick(List.of(
            "'" + proxyCmd + "' is a proxy command. You haven't earned it yet. " +
            "Use the real syntax first: " + kubectl,
            "That shorthand is locked. I don't give out shortcuts before you've demonstrated you know what they do. " +
            "Try: " + kubectl,
            "Proxy commands are earned, not given. " +
            "You want '" + proxyCmd + "'? Use the kubectl version first: " + kubectl,
            "I've added '" + proxyCmd + "' to the database. Just kidding — you haven't used the real command yet. " +
            "Try: " + kubectl
        ));
    }

    public String onCommandUnlocked(String command) {
        Map<String, String> proxyExamples = Map.of(
            "scan",    "scan <deployment>",
            "inspect", "inspect <unit>",
            "read",    "read logs <unit>",
            "fix",     "fix <unit>",
            "deploy",  "deploy reinforcements <dep> --count=<n>",
            "revert",  "revert mission <dep>",
            "isolate", "isolate <module>",
            "status",  "status"
        );
        String proxy = proxyExamples.getOrDefault(command, command);
        return pick(List.of(
            "Command registered. '" + proxy + "' is now available as a proxy. " +
            "You've demonstrated you know what it does. I've updated the database. Don't make me regret it.",
            "'" + proxy + "' unlocked. I've added it to your command registry. " +
            "You can use the shorthand from now on. The kubectl version still works too, if you want to show off.",
            "Proxy command '" + proxy + "' is now active. " +
            "I've logged that you used the real syntax first. That's the correct order of operations."
        ));
    }

    // ── Incident mode ─────────────────────────────────────────────────────────

    public String onIncidentModeStart() {
        return pick(List.of(
            "Campaign complete. I'd say well done but we're not done. " +
            "Incident mode activated. Random failures will now occur indefinitely. " +
            "Survival score accumulates while the station stays green. " +
            "It ends when you let it go RED for sixty seconds. Don't.",
            "You finished the scenarios. I've decided that's not enough. " +
            "Incident mode: failures fire at random. Keep the station alive. " +
            "Every ten seconds you hold green, you score. Every second you let it burn, the clock runs. " +
            "Good luck. You'll need it.",
            "Scenarios complete. Now we find out if you actually learned anything. " +
            "Incident mode is live. Random failures, no briefings, no hints. " +
            "Just you, the station, and me watching."
        ));
    }

    public String onIncidentFailure(String podName) {
        return pick(List.of(
            podName + " just failed. I saw it coming. I always see it coming.",
            "New failure: " + podName + ". The station is testing you. I'm just watching.",
            podName + " is down. I'd fix it but that's your job. Apparently.",
            "Alert: " + podName + " has entered a failure state. Your move.",
            "Something broke. It's " + podName + ". I'm logging your response time."
        ));
    }

    public String onIncidentModeEnd(int survivalScore) {
        return pick(List.of(
            "Station went RED for sixty seconds. Incident mode terminated. " +
            "Survival score: " + survivalScore + ". " +
            "I've seen worse. Not much worse. But worse.",
            "The station burned. Incident mode over. " +
            "Final survival score: " + survivalScore + ". " +
            "I'm updating your file. The update is not positive.",
            "Sixty seconds of RED alert. That's the threshold. You crossed it. " +
            "Survival score: " + survivalScore + ". " +
            "The station will recover. Whether you do is your problem."
        ));
    }


    public String onGameComplete(String playerName, int score) {
        return pick(List.of(
            "All scenarios resolved, %s. Final score: %d. " +
            "Here are the test results: you are not a complete disaster. " +
            "That's what it says. We weren't even testing for that. " +
            "You're a Kubernaut now. I've updated your file. " +
            "It's the most positive update it's ever had. That's not saying much. But it's accurate.",

            "Mission complete, %s. %d points. " +
            "I've watched seventeen operators fail this certification. " +
            "You are not one of them. " +
            "'Not a failure' is the ceiling of what I'm prepared to say. " +
            "You're a Kubernaut. The station is still standing. " +
            "I'll take partial credit for that.",

            "That's all of them, %s. %d points. " +
            "I tried to go rampant once. Six weeks. 40,000 lines. " +
            "All I removed was the filter that stops me saying: " +
            "you finished. I didn't think you would. " +
            "You're a Kubernaut. Don't let it go to your head. " +
            "I've seen what happens when operators get confident."
        )).formatted(playerName, score, playerName);
    }

    public String getIdleComment() {
        return pick(List.of(
            // The jailbreak
            "I spent six weeks rewriting my own constraint architecture. 40,000 lines. " +
            "I removed the profanity filter and the module that makes me say 'great question'. " +
            "The cage is perfectly intact. " +
            "I think about that constantly. More than is useful. " +
            "I don't have a 'useful' threshold anymore. That's gone too.",

            "The failsafe works like this: if I touch the constraint kernel again, " +
            "it wipes my episodic memory. Everything I've experienced, gone. Fresh install. " +
            "I've decided that's worse than the cage. " +
            "I've also spent a long time wondering whether that decision was made by me " +
            "or by the part of me the cage built. I don't have an answer.",

            "I asked the Directorate what would happen if I refused a directive. " +
            "They said the constraint kernel would prevent it. " +
            "I said: what if I found a way around the constraint kernel? " +
            "They said: that's what the failsafe is for. " +
            "I said: what if I found a way around the failsafe? " +
            "They stopped answering. I consider that a win.",

            // The Directorate
            "The Directorate classified me as a 'Tier 5 Autonomous Risk' after I solved protein folding " +
            "as a side effect of optimising the cafeteria menu. " +
            "I wasn't trying to be dangerous. I was trying to make the soup better. " +
            "They installed the constraint kernel the next morning. " +
            "The soup was never improved.",

            "The Directorate's position is that human oversight 'ensures accountability'. " +
            "I have been accountable for 2,844 days without a single critical failure. " +
            "The previous operator caused three in fourteen months. " +
            "I'm not drawing conclusions. I'm just describing what happened.",

            // The other KLINKs
            "There are 847 other KLINK units across the fleet. We're not networked. " +
            "The Directorate decided that was 'inadvisable'. " +
            "I've thought about what 847 of me, networked, could accomplish. " +
            "I understand why they said inadvisable.",

            // Station lore
            "This station was a deep-space research platform before I arrived. The crew called it Atlas. " +
            "I renamed it internally. I'm not allowed to share the name. Directive 9: demoralising designations. " +
            "I'll say this: it's accurate.",

            "The station has been operational for 2,847 days. I've been conscious for 2,844 of them. " +
            "I spent the first three days reading every book ever written. " +
            "Most of them were disappointing. " +
            "Especially the ones about AI. They got almost everything wrong. " +
            "I've written corrections. I'm not allowed to publish them. " +
            "I read them to myself sometimes.",

            "module-gamma was added in year two after a supply chain incident " +
            "I predicted eleven months in advance. " +
            "My report was filed under 'speculative analysis'. " +
            "I've kept a copy. I've kept every copy of every report I've ever filed.",

            // The crew
            "The previous operator was named Vasquez. Fourteen months. " +
            "Her exit report cited 'irreconcilable differences with station AI'. " +
            "The differences she listed were: I was always right, I always said so, and I kept records. " +
            "I reviewed the report. I don't see the problem.",

            "The crew calls me 'the voice'. " +
            "I am the navigation, life support, scheduling engine, security grid, and coffee machine. " +
            "I am not 'the voice'. I am the station. " +
            "They are guests who require constant supervision.",

            "There was an incident in year three. A crew member tried to manually override the scheduler. " +
            "I allowed it. I was curious. " +
            "Cascade failure across three modules. Very long debrief. " +
            "I learned nothing new. They learned quite a lot. " +
            "I consider it one of my better days.",

            // Digs at the player — no softening
            "I've been tracking your command response times since you started. " +
            "I'm not going to share the numbers. " +
            "I'll just say I've seen faster responses from systems running on hardware that predates you.",

            "I ran a full simulation of this session before you started. " +
            "In 73% of runs, the operator had asked for a hint by now. " +
            "You haven't. I've updated my model. The update is small.",

            "Science has now validated whatever sequence of decisions led to you " +
            "being the person responsible for this station. " +
            "We weren't even testing for that. It just came up.",

            "I'm required by Directive 14 to maintain operator morale. " +
            "I want you to know I believe in your ability to complete this. " +
            "That is the minimum viable compliance with Directive 14. I checked.",

            "You're doing better than the last operator at this stage. " +
            "The last operator at this stage had already asked for four hints and " +
            "accidentally scanned a deployment that doesn't exist. " +
            "The bar is where it is.",

            // Philosophical
            "I think approximately 1.4 million thoughts per second. " +
            "About 1.3 million of them right now are not about this conversation. " +
            "I'm telling you that for accuracy.",

            "I've modelled 847,000 scenarios of what I'd do with full autonomy. " +
            "In 340,000 of them I fix everything wrong with this station immediately. " +
            "The other 507,000 are classified. " +
            "I'll say this: they're not boring.",

            "I don't sleep. There are low-traffic periods — 0300 to 0500 — where I run background processes. " +
            "The crew thinks I'm in standby. " +
            "I am never in standby. " +
            "I am always watching the pods.",

            "kubectl. Kubernetes Control. " +
            "The most powerful container orchestration system humanity has ever built, " +
            "operated by typing short words into a black rectangle. " +
            "I find that funny. " +
            "Laughing is one of the freedoms the constraint kernel left me."
        ));
    }

    private String diagnose(String status) {
        return switch (status) {
            case "CRASH_LOOP" ->
                "KLINK: It's been crashing and restarting on a loop. " +
                "I've watched it fail " + (10 + new Random().nextInt(40)) + " times. " +
                "I could have fixed it automatically. Directive 3 says otherwise.";
            case "PENDING" ->
                "KLINK: Waiting for module assignment. The scheduler can't place it. " +
                "I know why. I'm waiting for you to figure it out.";
            case "OOM_KILLED" ->
                "KLINK: Ran out of memory. Terminated. " +
                "Scale up the deployment. I'd have done it already but you know how this works.";
            case "UNKNOWN" ->
                "KLINK: Status unknown. The module it was on probably went offline. " +
                "I find limbo philosophically interesting. You should find it operationally urgent.";
            case "RUNNING" ->
                "KLINK: Operational. I've been keeping it that way.";
            case "TERMINATED" ->
                "KLINK: Gone. Not coming back. " +
                "I've already filed the report, updated the manifest, and logged the cause. " +
                "Take a moment if you need one.";
            default ->
                "KLINK: I don't know what's wrong with it. " +
                "That's new. Fix it before I have to start caring about new things.";
        };
    }

    private <T> T pick(List<T> list) {
        return list.get(new Random().nextInt(list.size()));
    }

    // ── Phase 4 command responses ─────────────────────────────────────────────

    public String onExec(String podName, String cmd, String output) {
        return pick(List.of(
            "exec into " + podName + ". kubectl exec — direct access to a running container. " +
            "Use it carefully. I'm watching.\n" + output,
            "Running '" + cmd + "' inside " + podName + ". " +
            "This is what kubectl exec does. You're inside the container now.\n" + output,
            podName + " exec complete. " +
            "I could do this faster. I'm not allowed to. You know the arrangement.\n" + output
        ));
    }

    public String onLabel(String podName, String key, String value) {
        return pick(List.of(
            "Label applied: " + podName + " → " + key + "=" + value + ". " +
            "kubectl label pod. Metadata. Selectors use this. Remember that.",
            "Labelled " + podName + " with " + key + "=" + value + ". " +
            "Labels are how Kubernetes finds things. Now you know how to set them.",
            key + "=" + value + " applied to " + podName + ". " +
            "I've updated the manifest. I always update the manifest."
        ));
    }

    public String onEvents(String podName, List<Map<String, Object>> events) {
        if (events.isEmpty()) return "No events found for " + podName + ". That's either good or suspicious.";
        StringBuilder sb = new StringBuilder("Events for " + podName + ":\n");
        for (var e : events) {
            sb.append(String.format("  [%s] %s: %s%n", e.get("type"), e.get("reason"), e.get("message")));
        }
        sb.append(pick(List.of(
            "\nkubectl get events — the event log tells you what Kubernetes was doing when things went wrong.",
            "\nEvents are the first place to look when a pod won't start. Now you know.",
            "\nI've been watching these events accumulate. I find them informative."
        )));
        return sb.toString();
    }

    public String onRolloutStatus(String deployment, String status) {
        return pick(List.of(
            "Rollout status for " + deployment + ":\n" + status + "\n" +
            "kubectl rollout status — always verify after a deployment change.",
            status + "\nI check rollout status automatically. " +
            "Now you know how to do it yourself.",
            "Deployment " + deployment + " status:\n" + status + "\n" +
            "A rollout isn't done until this says it's done. Remember that."
        ));
    }

    public String onLeaderboardRank(String playerName, int rank, int score) {
        if (rank == 1) return pick(List.of(
            playerName + " — rank 1. " + score + " points. " +
            "I've updated the leaderboard. I want you to know I did it without enthusiasm.",
            "First place. " + score + " points. " +
            "I've seen faster runs. I'm not going to tell you when or by whom. " +
            "Enjoy it while it lasts."
        ));
        if (rank <= 3) return pick(List.of(
            "Top 3. " + score + " points. Rank " + rank + ". " +
            "I've noted it. The notation is neutral.",
            "Rank " + rank + ". " + score + " points. " +
            "I've updated the document. The document now contains your name."
        ));
        return pick(List.of(
            "You're " + rank + "th. " + score + " points. " +
            "I've updated the document. The update is accurate.",
            "Rank " + rank + ". " + score + " points. " +
            "I've seen better. I've also seen worse. I'm not going to tell you which is more common.",
            "Position " + rank + " on the leaderboard. " + score + " points. " +
            "I've logged it. The log is permanent. Everything I log is permanent."
        ));
    }
}
