package com.ptithcm.ptitmeet.api.dto.meeting;

import com.google.gson.annotations.SerializedName;

public class ParticipantResponse {

    @SerializedName("participantId")
    private String participantId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("email")
    private String email;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("requestTime")
    private String requestTime;

    public String getParticipantId() {
        return participantId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public String getRequestTime() {
        return requestTime;
    }
}
