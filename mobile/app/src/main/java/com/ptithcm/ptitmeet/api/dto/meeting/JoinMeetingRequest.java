package com.ptithcm.ptitmeet.api.dto.meeting;

import com.google.gson.annotations.SerializedName;

public class JoinMeetingRequest {

    @SerializedName("password")
    private String password;

    @SerializedName("displayName")
    private String displayName;

    public JoinMeetingRequest(String password, String displayName) {
        this.password = password;
        this.displayName = displayName;
    }
}
