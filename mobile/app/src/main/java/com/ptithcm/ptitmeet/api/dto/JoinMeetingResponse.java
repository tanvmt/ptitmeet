package com.ptithcm.ptitmeet.api.dto;

import com.google.gson.annotations.SerializedName;

public class JoinMeetingResponse {
    @SerializedName("token")
    private String token; // Token LiveKit

    @SerializedName("serverUrl")
    private String serverUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("role")
    private String role;

    @SerializedName("message")
    private String message;

    // Getters
    public String getToken() { return token; }
    public String getServerUrl() { return serverUrl; }
    public String getRole() { return role; }
}