package com.kubernauts.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StartGameResponse {
    private Long sessionId;
    private String klinkMessage;
    private String klinkMessageType;
    private StationState state;
}
