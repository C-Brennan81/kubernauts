package com.kubernauts.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Pod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;          // e.g. "crew-alpha-7"
    private String deploymentName; // e.g. "crew-quarters"
    private String nodeName;

    @Enumerated(EnumType.STRING)
    private PodStatus status = PodStatus.RUNNING;

    private int restartCount = 0;
    private String sector = "default"; // maps to k8s namespace

    @ManyToOne
    @JoinColumn(name = "session_id")
    private GameSession session;

    public Pod(String name, String deploymentName, String nodeName, GameSession session) {
        this.name = name;
        this.deploymentName = deploymentName;
        this.nodeName = nodeName;
        this.session = session;
    }
}
