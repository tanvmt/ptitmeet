package com.ptithcm.ptitmeet.api.dto.auth;

import com.google.gson.annotations.SerializedName;
import com.ptithcm.ptitmeet.api.dto.user.UserResponse;

public class AuthResponse {

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("user")
    private UserResponse user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public UserResponse getUser() {
        return user;
    }
}
