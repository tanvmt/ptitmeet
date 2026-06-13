package com.ptithcm.ptitmeet.api.dto.user;

public class UpdateProfileRequest {

    private final String fullName;
    private final String avatarUrl;

    public UpdateProfileRequest(String fullName, String avatarUrl) {
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
    }
}
