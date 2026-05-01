package com.ptithcm.ptitmeet.api.dto;

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
    public String getPassword() {
        return password;
    }
    public String getDisplayName(){
        return displayName;
    }
}