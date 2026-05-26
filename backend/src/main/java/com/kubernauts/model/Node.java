package com.kubernauts.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;          // e.g. "module-alpha"
    private boolean ready = true;
    private int cpuCapacity = 4;
    private int memoryCapacity = 8;
    private int cpuUsed = 0;
    private int memoryUsed = 0;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "session_id")
    private GameSession session;

    public Node(String name, GameSession session) {
        this.name = name;
        this.session = session;
    }
}
