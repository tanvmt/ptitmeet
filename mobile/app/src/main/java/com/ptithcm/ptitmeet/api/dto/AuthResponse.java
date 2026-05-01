package com.ptithcm.ptitmeet.api.dto;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.ptitmeet.models.User;

public class AuthResponse {
    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("user")
    private User user;

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public User getUser() { return user; }
}