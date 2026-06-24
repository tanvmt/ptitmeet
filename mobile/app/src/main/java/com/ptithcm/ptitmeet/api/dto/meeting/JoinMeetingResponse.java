package com.ptithcm.ptitmeet.api.dto.meeting;

import com.google.gson.annotations.SerializedName;

public class JoinMeetingResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("serverUrl")
    private String serverUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("role")
    private String role;

    @SerializedName("message")
    private String message;

    @SerializedName("settings")
    private String settings;

    @SerializedName("isOwner")
    private boolean owner;

    @SerializedName("currentHostId")
    private String currentHostId;

    public JoinMeetingResponse(String token, String serverUrl, String status, String role, String message, String settings, boolean owner, String currentHostId) {
        this.token = token;
        this.serverUrl = serverUrl;
        this.status = status;
        this.role = role;
        this.message = message;
        this.settings = settings;
        this.owner = owner;
        this.currentHostId = currentHostId;
    }

    public JoinMeetingResponse() {
    }

    public String getToken() {
        return token;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }

    public String getSettings() {
        return settings;
    }

    public boolean isOwner() {
        return owner;
    }

    public String getCurrentHostId() {
        return currentHostId;
    }
}
