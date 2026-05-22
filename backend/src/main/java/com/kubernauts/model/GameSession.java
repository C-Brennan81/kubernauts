package com.kubernauts.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;
    private int score = 0;
    private int currentScenario = 0;
    private int currentLevel = 1;
    private boolean active = true;
    private boolean incidentMode = false;
    private int survivalScore = 0;
    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime scenarioStartedAt = LocalDateTime.now();

    // Discovery phase: comma-separated set of "scenarioIdx:action" completed
    @Column(length = 1000)
    private String discoveryLog = "";

    // Unlocked proxy commands: comma-separated e.g. "scan,fix,inspect"
    @Column(length = 500)
    private String unlockedCommands = "";

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Pod> pods = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Node> nodes = new ArrayList<>();

    public GameSession(String playerName) {
        this.playerName = playerName;
    }
}
