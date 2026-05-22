package com.kubernauts.model;

import lombok.Data;
import java.util.List;
import java.util.Set;

/** DTO — not persisted, built on the fly for the frontend */
@Data
public class StationState {
    private Long sessionId;
    private String playerName;
    private int score;
    private int scoreDelta;
    private int currentScenario;
    private int currentLevel;
    private int scoreMultiplier;        // 3, 2, or 1
    private long scenarioElapsedSeconds;
    private boolean incidentMode;
    private int survivalScore;
    private Set<String> unlockedCommands;
    private List<Pod> pods;
    private List<Node> nodes;
    private String alertLevel;
}
