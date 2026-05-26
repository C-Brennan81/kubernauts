package com.kubernauts.model;

import lombok.Data;

@Data
public class CommandResult {
    private String output;
    private String klinkMessage;
    private String klinkMessageType; // "mission" | "idle" | "system"
    private StationState state;
    private boolean scenarioComplete;

    public CommandResult(String output, String klinkMessage, String type, StationState state, boolean scenarioComplete) {
        this.output = output;
        this.klinkMessage = klinkMessage;
        this.klinkMessageType = type;
        this.state = state;
        this.scenarioComplete = scenarioComplete;
    }
}
