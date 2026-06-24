package com.ptithcm.ptitmeet.api.dto.meeting;

public class ApprovalRequest {

    private final String participantId;
    private final String action;

    public ApprovalRequest(String participantId, String action) {
        this.participantId = participantId;
        this.action = action;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getAction() {
        return action;
    }
}
